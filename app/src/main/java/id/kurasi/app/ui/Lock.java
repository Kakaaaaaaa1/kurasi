package id.kurasi.app.ui;

import android.app.Activity;
import android.content.Intent;

import id.kurasi.app.util.Prefs;

/**
 * Status kunci aplikasi: minta PIN saat pertama dibuka dan setelah aplikasi ditinggal
 * (semua layar berhenti) lebih dari 30 detik. Pindah antar layar di dalam aplikasi tidak
 * dianggap meninggalkan aplikasi.
 */
public final class Lock {
    private Lock() {}

    public static boolean unlocked;
    static boolean showing;
    private static int started;     // jumlah layar (selain layar kunci) yang sedang terlihat
    private static long leftAt;
    private static final long GRACE_MS = 30_000;

    /** Dipanggil di onStart setiap layar. */
    public static void onStart(Activity a) {
        started++;
        // Jika sudah ada layar lain yang terlihat, aplikasi masih di depan: tidak perlu cek.
        if (started > 1 || !Prefs.hasPin(a) || showing) return;
        if (!unlocked || System.currentTimeMillis() - leftAt > GRACE_MS) {
            unlocked = false;
            showing = true;
            a.startActivity(new Intent(a, LockActivity.class));
        }
    }

    /** Dipanggil di onStop setiap layar. */
    public static void onStop() {
        started = Math.max(0, started - 1);
        if (started == 0) leftAt = System.currentTimeMillis();
    }
}
