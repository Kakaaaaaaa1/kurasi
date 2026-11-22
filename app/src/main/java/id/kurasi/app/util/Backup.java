package id.kurasi.app.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import id.kurasi.app.data.Repo;

/**
 * Cadangan data dalam satu berkas ZIP: data.json berisi seluruh database, folder photos/ berisi foto item dan nota.
 * Cadangan JSON lama (tanpa foto) tetap bisa dipulihkan.
 */
public final class Backup {
    private Backup() {}

    private static final String DATA = "data.json";
    private static final String PHOTOS = "photos/";

    /** Tulis cadangan ke folder cache/share dan kembalikan berkasnya. Jalankan di thread latar. */
    public static File write(Context c) throws IOException, JSONException {
        Repo repo = Repo.get(c);
        File dir = new File(c.getCacheDir(), "share");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File out = new File(dir, "kurasi_cadangan.zip");
        try (ZipOutputStream z = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
            // data.json harus pertama supaya saat memulihkan, datanya sudah terbaca sebelum foto
            z.putNextEntry(new ZipEntry(DATA));
            z.write(repo.dump().toString().getBytes(StandardCharsets.UTF_8));
            z.closeEntry();

            Set<String> done = new HashSet<>();
            for (String path : repo.photoPaths()) {
                File f = new File(path);
                if (!f.isFile() || !done.add(f.getName())) continue;
                z.putNextEntry(new ZipEntry(PHOTOS + f.getName()));
                try (InputStream in = new FileInputStream(f)) {
                    copy(in, z);
                }
                z.closeEntry();
            }
        }
        return out;
    }

    /**
     * Pulihkan dari ZIP atau JSON lama. Mengganti seluruh data yang ada.
     * Mengembalikan jumlah foto yang ikut dipulihkan. Jalankan di thread latar.
     */
    public static int read(Context c, InputStream raw) throws IOException, JSONException {
        BufferedInputStream in = new BufferedInputStream(raw);
        in.mark(4);
        int b0 = in.read(), b1 = in.read();
        in.reset();
        Repo repo = Repo.get(c);

        if (b0 != 'P' || b1 != 'K') {
            // cadangan JSON lama
            repo.restore(new JSONObject(readAll(in)));
            ImageStore.cleanup(c, repo.photoPaths());
            return 0;
        }

        Map<String, String> photoMap = new HashMap<>();
        try {
            ZipInputStream z = new ZipInputStream(in);
            JSONObject root = null;
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                String name = e.getName();
                if (DATA.equals(name)) {
                    root = new JSONObject(readAll(z));
                    if (!"kurasi".equals(root.optString("app"))) throw new JSONException("Bukan file cadangan Kurasi");
                } else if (root != null && !e.isDirectory() && name.startsWith(PHOTOS)) {
                    // hanya nama berkas yang dipakai, foto selalu ditulis ke folder foto aplikasi
                    String base = new File(name).getName();
                    File dst = ImageStore.newPhotoFile(c);
                    try (OutputStream out = new FileOutputStream(dst)) {
                        copy(z, out);
                    }
                    photoMap.put(base, dst.getAbsolutePath());
                }
            }
            if (root == null) throw new JSONException("Cadangan tidak berisi data");
            repo.restore(root, photoMap);
        } finally {
            // hapus foto yang tidak terpakai: sisa data lama, atau foto baru kalau pemulihan gagal
            ImageStore.cleanup(c, repo.photoPaths());
        }
        return photoMap.size();
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        copy(in, buf);
        return buf.toString("UTF-8");
    }

    /** Menyalin sampai habis tanpa menutup [in], supaya ZipInputStream bisa lanjut ke entri berikutnya. */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[16 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }
}
