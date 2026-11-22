package id.kurasi.app.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pembaruan lewat GitHub Releases: cek rilis terbaru, bandingkan dengan versi terpasang, lalu unduh APK-nya.
 * Pemasangan dikerjakan sistem Android, yang hanya menerima APK bertanda tangan sama dengan aplikasi terpasang.
 */
public final class Updater {
    private Updater() {}

    /** Repositori GitHub publik tempat APK rilis diunggah, format pemilik/nama. */
    public static final String REPO = "Kakaaaaaaa1/kurasi";

    private static final String LATEST = "https://api.github.com/repos/" + REPO + "/releases/latest";

    /** Satu rilis yang lebih baru dari versi terpasang. */
    public static final class Info {
        public final String version, notes, apkUrl;
        public final long size;

        Info(String version, String notes, String apkUrl, long size) {
            this.version = version;
            this.notes = notes;
            this.apkUrl = apkUrl;
            this.size = size;
        }
    }

    public interface Callback<T> {
        /** Tepat satu dari value atau error yang terisi, kecuali "tidak ada pembaruan" (keduanya null). */
        void done(T value, String error);
    }

    public interface Progress {
        void onProgress(long done, long total);
    }

    /** Pegangan unduhan yang sedang berjalan, dipakai untuk membatalkannya. */
    public static final class Handle {
        private final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();

        public void cancel() {
            cancelled.set(true);
        }

        boolean isCancelled() {
            return cancelled.get();
        }
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static Handler main;

    /** Dibuat saat pertama dipakai, bukan saat kelas dimuat, supaya logika murni bisa diuji di JVM biasa. */
    private static synchronized Handler main() {
        if (main == null) main = new Handler(Looper.getMainLooper());
        return main;
    }

    public static String currentVersion(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    // ------------------------------------------------------------------ logika murni (diuji unit test)

    /** True kalau [remote] lebih baru dari [local]. Awalan "v" dan akhiran seperti "-beta" diabaikan. */
    public static boolean isNewer(String remote, String local) {
        int[] a = parts(remote), b = parts(local);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    static int[] parts(String version) {
        String s = version == null ? "" : version.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("v")) s = s.substring(1);
        int cut = s.indexOf('-');
        if (cut >= 0) s = s.substring(0, cut);
        String[] seg = s.split("\\.");
        int[] out = new int[seg.length];
        for (int i = 0; i < seg.length; i++) {
            try {
                out[i] = Integer.parseInt(seg[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    /** Baca jawaban GitHub untuk "rilis terbaru". Mengembalikan null kalau rilis tidak punya berkas APK. */
    public static Info parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        String tag = o.getString("tag_name");
        String version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
        JSONArray assets = o.optJSONArray("assets");
        if (assets == null) return null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            if (a.optString("name").toLowerCase(Locale.ROOT).endsWith(".apk")) {
                return new Info(version, o.optString("body", "").trim(),
                        a.getString("browser_download_url"), a.optLong("size", 0));
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ jaringan

    /** Sinkron: rilis yang lebih baru dari versi terpasang, atau null. Dipakai pekerja harian di thread latar. */
    public static Info fetchNewer(Context c) throws IOException, JSONException {
        String json;
        try {
            json = httpGet(LATEST);
        } catch (FileNotFoundException noRelease) {
            return null; // repositori belum punya rilis
        }
        Info latest = parse(json);
        return latest != null && isNewer(latest.version, currentVersion(c)) ? latest : null;
    }

    /** Cek di thread latar, hasilnya dikembalikan di thread utama. */
    public static void check(Context c, Callback<Info> cb) {
        Context app = c.getApplicationContext();
        IO.execute(() -> {
            Info info = null;
            String error = null;
            try {
                info = fetchNewer(app);
            } catch (Exception e) {
                error = "Tidak bisa mengecek pembaruan. Periksa koneksi internetmu.";
            }
            final Info fi = info;
            final String fe = error;
            main().post(() -> cb.done(fi, fe));
        });
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Accept", "application/vnd.github+json");
            c.setRequestProperty("User-Agent", "Kurasi-Updater");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8");
            }
        } finally {
            c.disconnect();
        }
    }

    // ------------------------------------------------------------------ unduhan

    public static File updatesDir(Context c) {
        File d = new File(c.getCacheDir(), "updates");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    public static File apkFor(Context c, Info info) {
        return new File(updatesDir(c), "Kurasi-" + info.version + ".apk");
    }

    /** True kalau APK rilis ini sudah pernah diunduh penuh, jadi tidak perlu diunduh lagi. */
    public static boolean isDownloaded(Context c, Info info) {
        File f = apkFor(c, info);
        return f.isFile() && (info.size <= 0 || f.length() == info.size);
    }

    /** Unduh di thread latar. Hasil dan progres dikembalikan di thread utama. */
    public static Handle download(Context c, Info info, Progress progress, Callback<File> cb) {
        Context app = c.getApplicationContext();
        final Handle handle = new Handle();
        IO.execute(() -> {
            File dst = apkFor(app, info);
            File part = new File(dst.getPath() + ".part");
            String error = null;
            boolean cancelled = false;
            try {
                if (!info.apkUrl.startsWith("https://")) throw new IOException("Alamat unduhan tidak aman");
                HttpURLConnection conn = (HttpURLConnection) new URL(info.apkUrl).openConnection();
                try {
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("User-Agent", "Kurasi-Updater");
                    long total = conn.getContentLengthLong() > 0 ? conn.getContentLengthLong() : info.size;
                    long done = 0, lastReport = 0;
                    try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(part)) {
                        byte[] buf = new byte[32 * 1024];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                            done += n;
                            long now = System.currentTimeMillis();
                            if (now - lastReport > 120) {
                                lastReport = now;
                                final long d = done, t = total;
                                main().post(() -> progress.onProgress(d, t));
                            }
                            if (handle.isCancelled()) {
                                cancelled = true;
                                break;
                            }
                        }
                    }
                } finally {
                    conn.disconnect();
                }
                if (cancelled) {
                    //noinspection ResultOfMethodCallIgnored
                    part.delete();
                } else {
                    if (info.size > 0 && part.length() != info.size) throw new IOException("Unduhan tidak lengkap");
                    //noinspection ResultOfMethodCallIgnored
                    dst.delete();
                    if (!part.renameTo(dst)) throw new IOException("Gagal menyimpan berkas");
                }
            } catch (Exception e) {
                //noinspection ResultOfMethodCallIgnored
                part.delete();
                error = "Unduhan gagal. Periksa koneksi internetmu lalu coba lagi.";
            }
            final String fe = error;
            final boolean fc = cancelled;
            main().post(() -> {
                if (fc) return;
                cb.done(fe == null ? dst : null, fe);
            });
        });
        return handle;
    }

    /** Hapus sisa unduhan dan APK pembaruan yang versinya tidak lebih baru dari yang sedang berjalan. */
    public static void cleanup(Context c) {
        Context app = c.getApplicationContext();
        IO.execute(() -> {
            File[] files = updatesDir(app).listFiles();
            if (files == null) return;
            String current = currentVersion(app);
            for (File f : files) {
                String n = f.getName();
                boolean stale = n.endsWith(".part");
                if (!stale && n.startsWith("Kurasi-") && n.endsWith(".apk")) {
                    String v = n.substring("Kurasi-".length(), n.length() - ".apk".length());
                    stale = !isNewer(v, current);
                }
                if (stale) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        });
    }
}
