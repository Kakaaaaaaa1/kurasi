package id.kurasi.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import id.kurasi.app.R;
import id.kurasi.app.util.Prefs;

/**
 * Layar utama: tiga halaman (Binder, Scan, Statistik) dalam satu pager, dengan dock kaca di bawahnya.
 * Menggeser lensa di dock menggeser halamannya secara langsung, dan sebaliknya.
 */
public class BinderActivity extends BaseActivity {
    public static final int PAGE_BINDER = 0, PAGE_SCAN = 1, PAGE_STATS = 2;
    /** Diisi notifikasi "versi baru": langsung tawarkan pembaruan saat Binder dibuka. */
    public static final String EXTRA_UPDATE = "update";

    private ViewPager2 pager;

    private final androidx.activity.result.ActivityResultLauncher<String> askNotif = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        pager = findViewById(R.id.pager);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case PAGE_SCAN: return new ScanFragment();
                    case PAGE_STATS: return new StatsFragment();
                    default: return new BinderFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });
        // semua halaman dibuat dari awal supaya terlihat sebagian saat sedang digeser
        pager.setOffscreenPageLimit(2);
        Dock.bind(this, pager);

        if (!Prefs.isOnboarded(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
        }

        id.kurasi.app.util.Updater.cleanup(this);
        if (Prefs.isOnboarded(this)) {
            UpdatePrompt.autoCheck(this, getIntent().getBooleanExtra(EXTRA_UPDATE, false));
        }

        // tombol kembali dari Scan atau Statistik pulang ke Binder dulu, baru keluar aplikasi
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pager.getCurrentItem() != PAGE_BINDER) pager.setCurrentItem(PAGE_BINDER, true);
                else finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // minta izin notifikasi sekali saja (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= 33
                && androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                && Prefs.isOnboarded(this)
                && !Prefs.wasNotified(this, "asked_notif")) {
            Prefs.markNotified(this, "asked_notif");
            askNotif.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    public void goToPage(int page) {
        pager.setCurrentItem(page, true);
    }
}
