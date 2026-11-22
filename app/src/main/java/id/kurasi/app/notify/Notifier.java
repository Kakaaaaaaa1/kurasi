package id.kurasi.app.notify;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.Loan;
import id.kurasi.app.ui.BinderActivity;
import id.kurasi.app.ui.CardActivity;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.Prefs;

/** Kirim notifikasi lokal: target harga wishlist, jatuh tempo pinjaman, garansi, dan batas bulanan. */
public final class Notifier {
    private Notifier() {}

    private static final String CHANNEL = "kurasi_reminders";

    private static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL,
                    c.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    public static boolean canPost(Context c) {
        if (!Prefs.notificationsOn(c)) return false;
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** itemId > 0 membuka kartunya, selain itu membuka Binder. */
    public static void post(Context c, int id, String title, String text, long itemId) {
        if (!canPost(c)) return;
        ensureChannel(c);
        Intent i = itemId > 0 ? new Intent(c, CardActivity.class).putExtra("id", itemId)
                : new Intent(c, BinderActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(c).notify(id, b.build());
        } catch (SecurityException ignored) {
        }
    }

    /** Notifikasi versi baru. Mengetuknya membuka Binder dan langsung menawarkan pembaruan. */
    public static void updateAvailable(Context c, String version) {
        if (!canPost(c)) return;
        ensureChannel(c);
        Intent i = new Intent(c, BinderActivity.class).putExtra(BinderActivity.EXTRA_UPDATE, true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 50_000, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Kurasi versi " + version + " tersedia")
                .setContentText("Ketuk untuk melihat apa yang baru dan memperbarui.")
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(c).notify(50_000, b.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void wishlistHit(Context c, Item it) {
        post(c, (int) (10_000 + it.id), "Harga turun: " + it.name,
                "Sekarang " + Fmt.rp(it.price) + ", sudah di bawah target " + Fmt.rp(it.targetPrice) + ".", it.id);
    }

    public static void loanDue(Context c, Loan l, boolean late) {
        post(c, (int) (20_000 + l.id),
                late ? "Pinjaman terlambat" : "Pinjaman jatuh tempo besok",
                l.itemName + " dipinjam " + l.borrower + ", jatuh tempo " + Fmt.date(l.dueAt) + ".", l.itemId);
    }

    public static void warranty(Context c, Item it, int days) {
        post(c, (int) (30_000 + it.id), "Garansi hampir habis",
                it.name + ": sisa " + days + " hari (sampai " + Fmt.date(it.warrantyUntil) + ").", it.id);
    }

    public static void limit(Context c, int pct, long used, long limit) {
        post(c, 40_000, pct >= 100 ? "Pengeluaran melewati batas" : "Pengeluaran hampir mencapai batas",
                "Bulan ini sudah " + Fmt.rp(used) + " dari batas " + Fmt.rp(limit) + " (" + pct + "%).", 0);
    }
}
