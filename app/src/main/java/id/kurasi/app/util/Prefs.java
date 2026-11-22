package id.kurasi.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;

/** Pengaturan sederhana di SharedPreferences. */
public final class Prefs {
    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("kurasi", Context.MODE_PRIVATE);
    }

    public static boolean isFirstRun(Context c) {
        return !sp(c).getBoolean("seeded", false);
    }

    public static void setSeeded(Context c) {
        sp(c).edit().putBoolean("seeded", true).apply();
    }

    /** Tutorial pertama sudah dilihat atau dilewati. */
    public static boolean isOnboarded(Context c) {
        return sp(c).getBoolean("onboarded", false);
    }

    public static void setOnboarded(Context c, boolean done) {
        sp(c).edit().putBoolean("onboarded", done).apply();
    }

    /** Tema: 0 ikuti sistem, 1 terang, 2 gelap. */
    public static int themeMode(Context c) {
        return sp(c).getInt("theme", 0);
    }

    public static void setThemeMode(Context c, int mode) {
        sp(c).edit().putInt("theme", mode).apply();
    }

    /** Batas pengeluaran per bulan (rupiah), 0 = tidak diatur. */
    public static long monthlyLimit(Context c) {
        return sp(c).getLong("limit", 0);
    }

    public static void setMonthlyLimit(Context c, long v) {
        sp(c).edit().putLong("limit", v).apply();
    }

    public static boolean notificationsOn(Context c) {
        return sp(c).getBoolean("notif", true);
    }

    public static void setNotificationsOn(Context c, boolean on) {
        sp(c).edit().putBoolean("notif", on).apply();
    }

    // ---- PIN
    public static boolean hasPin(Context c) {
        return !sp(c).getString("pin", "").isEmpty();
    }

    public static void setPin(Context c, String pin) {
        sp(c).edit().putString("pin", pin == null || pin.isEmpty() ? "" : hash(pin)).apply();
    }

    public static boolean checkPin(Context c, String pin) {
        return hash(pin).equals(sp(c).getString("pin", ""));
    }

    public static boolean useBiometric(Context c) {
        return sp(c).getBoolean("bio", false);
    }

    public static void setUseBiometric(Context c, boolean on) {
        sp(c).edit().putBoolean("bio", on).apply();
    }

    public static long lastUpdateCheck(Context c) {
        return sp(c).getLong("update_check", 0);
    }

    public static void setLastUpdateCheck(Context c, long at) {
        sp(c).edit().putLong("update_check", at).apply();
    }

    /** Pengingat sekali sehari: simpan jenis notifikasi yang sudah terkirim. */
    public static boolean wasNotified(Context c, String key) {
        return sp(c).getBoolean("n_" + key, false);
    }

    public static void markNotified(Context c, String key) {
        sp(c).edit().putBoolean("n_" + key, true).apply();
    }

    private static String hash(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("kurasi:" + pin).getBytes("UTF-8"));
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format(java.util.Locale.ROOT, "%02x", x));
            return b.toString();
        } catch (Exception e) {
            return pin;
        }
    }
}
