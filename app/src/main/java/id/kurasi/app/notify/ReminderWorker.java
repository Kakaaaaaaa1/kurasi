package id.kurasi.app.notify;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import id.kurasi.app.data.Models;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.Loan;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Prefs;
import id.kurasi.app.util.Updater;
import id.kurasi.app.widget.KurasiWidget;

/**
 * Pemeriksaan harian: pinjaman jatuh tempo, garansi hampir habis, dan batas pengeluaran bulanan.
 * Tiap pengingat hanya dikirim sekali per hari/tahap supaya tidak berulang.
 */
public class ReminderWorker extends Worker {
    private static final long DAY = 24L * 3600 * 1000;
    private static final String UNIQUE = "kurasi_daily_check";

    public ReminderWorker(@NonNull Context c, @NonNull WorkerParameters p) {
        super(c, p);
    }

    public static void schedule(Context c) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(ReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS).build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, req);
    }

    public static void runNow(Context c) {
        WorkManager.getInstance(c).enqueue(new OneTimeWorkRequest.Builder(ReminderWorker.class).build());
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        KurasiWidget.refresh(c);
        if (!Prefs.notificationsOn(c)) return Result.success();
        Repo repo = Repo.get(c);
        long now = System.currentTimeMillis();
        String today = new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date(now));
        String month = today.substring(0, 6);

        // pinjaman yang jatuh tempo besok atau sudah lewat
        for (Loan l : repo.loansDueBefore(now + DAY)) {
            String key = "loan_" + l.id + "_" + today;
            if (Prefs.wasNotified(c, key)) continue;
            Prefs.markNotified(c, key);
            Notifier.loanDue(c, l, l.dueAt < now);
        }

        // garansi: 30 hari dan 7 hari sebelum habis
        for (Item it : repo.items(0, null, null)) {
            if (it.warrantyUntil <= now || Models.S_WISHLIST.equals(it.status)) continue;
            long days = (long) Math.ceil((it.warrantyUntil - now) / (double) DAY);
            String stage = days <= 7 ? "7" : days <= 30 ? "30" : null;
            if (stage == null) continue;
            String key = "war_" + it.id + "_" + stage;
            if (Prefs.wasNotified(c, key)) continue;
            Prefs.markNotified(c, key);
            Notifier.warranty(c, it, (int) days);
        }

        // batas pengeluaran bulanan: 80% dan 100%
        long limit = Prefs.monthlyLimit(c);
        if (limit > 0) {
            long used = repo.spendThisMonth();
            int pct = (int) (used * 100 / limit);
            String stage = pct >= 100 ? "100" : pct >= 80 ? "80" : null;
            if (stage != null) {
                String key = "lim_" + month + "_" + stage;
                if (!Prefs.wasNotified(c, key)) {
                    Prefs.markNotified(c, key);
                    Notifier.limit(c, pct, used, limit);
                }
            }
        }
        // versi baru di GitHub: beri tahu sekali per versi
        try {
            Updater.Info info = Updater.fetchNewer(c);
            if (info != null && !Prefs.wasNotified(c, "upd_" + info.version)) {
                Prefs.markNotified(c, "upd_" + info.version);
                Notifier.updateAvailable(c, info.version);
            }
        } catch (Exception ignored) {
            // tanpa internet atau GitHub tidak terjangkau: coba lagi di pemeriksaan berikutnya
        }
        return Result.success();
    }
}
