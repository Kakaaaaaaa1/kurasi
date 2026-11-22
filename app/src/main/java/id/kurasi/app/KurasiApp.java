package id.kurasi.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import id.kurasi.app.notify.ReminderWorker;
import id.kurasi.app.util.Prefs;

public class KurasiApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        applyTheme(Prefs.themeMode(this));
        ReminderWorker.schedule(this);
    }

    /** mode: 0 ikuti sistem, 1 terang, 2 gelap. Layar yang terbuka dibuat ulang otomatis. */
    public static void applyTheme(int mode) {
        AppCompatDelegate.setDefaultNightMode(mode == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                : mode == 2 ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
