package id.kurasi.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Locale;

import id.kurasi.app.R;
import id.kurasi.app.util.Prefs;
import id.kurasi.app.util.Updater;

/** Alur pembaruan di layar: tawarkan versi baru, unduh dengan progres, lalu serahkan ke pemasang Android. */
public final class UpdatePrompt {
    private UpdatePrompt() {}

    private static final long AUTO_INTERVAL = 3L * 3600 * 1000; // cek otomatis paling sering tiap 3 jam
    private static boolean autoCheckedThisLaunch;

    /** Cek diam-diam saat aplikasi dibuka. Layar baru muncul kalau ada versi yang lebih baru. */
    public static void autoCheck(BaseActivity a, boolean force) {
        long now = System.currentTimeMillis();
        if (!force) {
            if (autoCheckedThisLaunch || now - Prefs.lastUpdateCheck(a) < AUTO_INTERVAL) return;
        }
        autoCheckedThisLaunch = true;
        Prefs.setLastUpdateCheck(a, now);
        check(a, false);
    }

    /** manual = true: beri tahu pengguna juga saat sudah terbaru atau gagal cek. */
    public static void check(BaseActivity a, boolean manual) {
        if (manual) a.toast("Mengecek pembaruan…");
        Updater.check(a, (info, error) -> {
            if (a.isFinishing() || a.isDestroyed()) return;
            if (error != null) {
                if (manual) a.toast(error);
            } else if (info == null) {
                if (manual) a.toast("Kamu sudah memakai versi terbaru (" + Updater.currentVersion(a) + ").");
            } else {
                offer(a, info);
            }
        });
    }

    private static void offer(BaseActivity a, Updater.Info info) {
        NeoSheet s = new NeoSheet(a, "Versi " + info.version + " tersedia");
        TextView t = new TextView(a, null, 0, R.style.Neo_Body);
        String notes = info.notes.isEmpty() ? "Perbaikan dan peningkatan." : info.notes;
        if (notes.length() > 500) notes = notes.substring(0, 500) + "…";
        String size = info.size > 0 ? String.format(Locale.ROOT, " · %.1f MB", info.size / 1048576.0) : "";
        t.setText(notes + "\n\nVersi kamu: " + Updater.currentVersion(a) + size);
        t.setTextSize(14);
        s.body().addView(t);
        s.addButton("Unduh dan pasang", 0, () -> download(a, info));
        s.addButton("Nanti", 1, null);
        s.show();
    }

    private static void download(BaseActivity a, Updater.Info info) {
        // APK yang sudah pernah diunduh penuh langsung dipasang, tidak perlu unduh ulang
        if (Updater.isDownloaded(a, info)) {
            install(a, Updater.apkFor(a, info));
            return;
        }
        NeoSheet s = new NeoSheet(a, "Mengunduh versi " + info.version);
        TextView t = new TextView(a, null, 0, R.style.Neo_Mono);
        t.setText("MENYIAPKAN UNDUHAN…");
        s.body().addView(t);
        final Updater.Handle[] handle = {null};
        final boolean[] finished = {false};
        s.onDismiss(() -> {
            // sheet ditutup sebelum selesai = unduhan dibatalkan
            if (!finished[0] && handle[0] != null) handle[0].cancel();
        });
        s.addButton("Batalkan", 1, null);
        s.show();
        handle[0] = Updater.download(a, info,
                (done, total) -> t.setText(total > 0
                        ? String.format(Locale.ROOT, "%d%% · %.1f DARI %.1f MB", done * 100 / total,
                        done / 1048576.0, total / 1048576.0)
                        : String.format(Locale.ROOT, "%.1f MB", done / 1048576.0)),
                (file, error) -> {
                    finished[0] = true;
                    s.dismiss();
                    if (a.isFinishing() || a.isDestroyed()) return;
                    if (error != null) a.toast(error);
                    else install(a, file);
                });
    }

    private static void install(BaseActivity a, File apk) {
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
            NeoSheet s = new NeoSheet(a, "Izinkan pemasangan");
            TextView t = new TextView(a, null, 0, R.style.Neo_Body);
            t.setText("Android meminta izin sekali supaya Kurasi bisa memasang pembaruan. Nyalakan \"Izinkan dari "
                    + "sumber ini\", lalu kembali ke Kurasi dan ketuk Cek pembaruan lagi. Berkasnya sudah diunduh, "
                    + "jadi tidak perlu mengunduh ulang.");
            t.setTextSize(14);
            s.body().addView(t);
            s.addButton("Buka pengaturan izin", 0, () -> a.startActivity(
                    new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName()))));
            s.addButton("Batal", 1, null);
            s.show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".files", apk);
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(i);
        } catch (Exception e) {
            a.toast("Tidak bisa membuka pemasang. Pasang manual dari berkas: " + apk.getName());
        }
    }
}
