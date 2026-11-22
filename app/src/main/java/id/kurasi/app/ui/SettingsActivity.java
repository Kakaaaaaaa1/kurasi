package id.kurasi.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import id.kurasi.app.R;
import id.kurasi.app.data.Repo;
import id.kurasi.app.data.SampleData;
import id.kurasi.app.notify.ReminderWorker;
import id.kurasi.app.util.Backup;
import id.kurasi.app.util.CsvExport;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.Prefs;
import id.kurasi.app.widget.KurasiWidget;

/** Pengaturan: pengingat, kunci PIN, ekspor/cadangan data, dan data contoh. */
public class SettingsActivity extends ScrollScreenActivity {

    private final ActivityResultLauncher<String> askNotif = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) toast("Izin notifikasi ditolak, pengingat tidak akan muncul");
                rebuild();
            });

    private final ActivityResultLauncher<String[]> openBackup = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) confirmRestore(uri);
            });

    @Override
    protected String screenTitle() {
        return "Pengaturan";
    }

    @Override
    protected void build() {
        // ---- Tampilan
        content.addView(Ui.label(this, "Tampilan"));
        final String[] themeNames = {"Ikuti sistem", "Terang", "Gelap"};
        content.addView(action("Tema: " + themeNames[Prefs.themeMode(this)],
                "Pilih tampilan terang, gelap, atau ikuti pengaturan HP.", () -> {
                    int cur = Prefs.themeMode(this);
                    NeoSheet s = new NeoSheet(this, "Tema");
                    for (int i = 0; i < themeNames.length; i++) {
                        final int mode = i;
                        s.addButton((cur == i ? "• " : "") + themeNames[i], cur == i ? 0 : 1, () -> {
                            Prefs.setThemeMode(this, mode);
                            id.kurasi.app.KurasiApp.applyTheme(mode);
                        });
                    }
                    s.show();
                }));
        content.addView(action("Lihat tutorial", "Ulangi pengenalan singkat aplikasi.",
                () -> startActivity(new Intent(this, OnboardingActivity.class))), wrapTop(10));

        // ---- Pengingat
        content.addView(Ui.label(this, "Pengingat"));
        content.addView(toggle("Notifikasi pengingat",
                "Jatuh tempo pinjaman, garansi hampir habis, dan batas pengeluaran bulanan.",
                Prefs.notificationsOn(this), on -> {
                    Prefs.setNotificationsOn(this, on);
                    if (on && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        askNotif.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        rebuild();
                    }
                }));
        content.addView(action("Cek pengingat sekarang", "Jalankan pemeriksaan tanpa menunggu jadwal harian.", () -> {
            ReminderWorker.runNow(this);
            toast("Pemeriksaan dijalankan");
        }), wrapTop(10));

        // ---- Keamanan
        content.addView(Ui.label(this, "Keamanan"));
        boolean hasPin = Prefs.hasPin(this);
        content.addView(action(hasPin ? "Ubah PIN" : "Pasang PIN 4 angka",
                "Aplikasi meminta PIN saat dibuka atau setelah ditinggal 30 detik.", this::pinSheet));
        if (hasPin) {
            boolean bioOk = BiometricManager.from(this).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
            if (bioOk) {
                content.addView(toggle("Buka dengan sidik jari atau wajah", "PIN tetap bisa dipakai sebagai cadangan.",
                        Prefs.useBiometric(this), on -> {
                            Prefs.setUseBiometric(this, on);
                            rebuild();
                        }), wrapTop(10));
            }
            content.addView(action("Hapus PIN", "Nonaktifkan kunci aplikasi.", () -> {
                Prefs.setPin(this, "");
                Prefs.setUseBiometric(this, false);
                rebuild();
            }), wrapTop(10));
        }

        // ---- Data
        content.addView(Ui.label(this, "Data"));
        content.addView(action("Ekspor ke CSV", "Buka di Excel atau Google Sheets.", this::exportCsv));
        content.addView(action("Cadangkan data (ZIP)", "Simpan seluruh koleksi beserta fotonya. Bisa dipulihkan di HP lain.",
                this::exportBackup), wrapTop(10));
        content.addView(action("Pulihkan dari cadangan", "Pilih berkas cadangan (.zip atau .json lama). Mengganti semua data yang ada sekarang.",
                () -> openBackup.launch(new String[]{"application/zip", "application/json", "text/plain", "*/*"})), wrapTop(10));
        content.addView(action("Muat data contoh", "Isi binder dengan contoh untuk mencoba semua fitur.", () -> {
            boolean added = SampleData.load(this);
            KurasiWidget.refresh(this);
            toast(added ? "Data contoh dimuat" : "Data contoh sudah ada di binder");
        }), wrapTop(10));
        content.addView(action("Hapus semua data", "Menghapus koleksi, item, set, pinjaman, dan riwayat.", () ->
                new NeoSheet(this, "Hapus semua data?")
                        .addButton("Ya, hapus semuanya", 2, () -> {
                            SampleData.clearAll(this);
                            KurasiWidget.refresh(this);
                            toast("Semua data dihapus");
                        })
                        .addButton("Batal", 1, null)
                        .show()), wrapTop(10));

        // ---- Tentang
        content.addView(Ui.label(this, "Tentang"));
        content.addView(action("Cek pembaruan",
                "Versi " + id.kurasi.app.util.Updater.currentVersion(this) + ". Cek apakah ada versi baru, lalu pasang langsung dari sini.",
                () -> UpdatePrompt.check(this, true)));

        TextView about = new TextView(this, null, 0, R.style.Neo_Label);
        about.setText("KURASI " + id.kurasi.app.util.Updater.currentVersion(this) + " · KARTU KOLEKSI DENGAN QR");
        about.setPadding(0, dp(24), 0, 0);
        content.addView(about);
    }

    private interface OnToggle {
        void on(boolean v);
    }

    private LinearLayout toggle(String title, String desc, boolean on, OnToggle cb) {
        LinearLayout box = row(title, desc);
        TextView tag = new TextView(this, null, 0, R.style.Neo_Mono);
        tag.setText(on ? "ON" : "OFF");
        tag.setBackground(Neo.flat(this, on ? Neo.MINT : Neo.COMMON, 99));
        tag.setTextColor(Neo.ON_FILL);
        tag.setPadding(dp(12), dp(3), dp(12), dp(3));
        box.addView(tag);
        box.setOnClickListener(v -> cb.on(!on));
        return box;
    }

    private LinearLayout action(String title, String desc, Runnable r) {
        LinearLayout box = row(title, desc);
        box.setOnClickListener(v -> r.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        box.setLayoutParams(lp);
        return box;
    }

    private LinearLayout row(String title, String desc) {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(Neo.flat(this, Neo.PAPER, 16));
        box.setPadding(dp(14), dp(10), dp(14), dp(12));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this, null, 0, R.style.Neo_Body);
        t.setText(title);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        TextView d = new TextView(this, null, 0, R.style.Neo_Label);
        d.setText(desc);
        d.setTextSize(11);
        d.setLetterSpacing(0);
        col.addView(t);
        col.addView(d);
        box.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return box;
    }

    // ---------------------------------------------------------- PIN

    private void pinSheet() {
        NeoSheet s = new NeoSheet(this, "PIN baru");
        s.body().addView(Ui.label(this, "PIN 4 angka"));
        EditText a = pinField();
        s.body().addView(a, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        s.body().addView(Ui.label(this, "Ulangi PIN"));
        EditText b = pinField();
        s.body().addView(b, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        s.addButton("Simpan PIN", 0, null);
        s.body().getChildAt(s.body().getChildCount() - 1).setOnClickListener(v -> {
            String p = a.getText().toString();
            if (p.length() != 4) {
                a.setError("PIN harus 4 angka");
                return;
            }
            if (!p.equals(b.getText().toString())) {
                b.setError("PIN tidak sama");
                return;
            }
            Prefs.setPin(this, p);
            Lock.unlocked = true;
            s.dismiss();
            toast("PIN dipasang");
            rebuild();
        });
        s.show();
    }

    private EditText pinField() {
        EditText e = new EditText(this, null, 0, R.style.Neo_Input);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
        e.setGravity(Gravity.CENTER);
        e.setTextSize(24);
        return e;
    }

    // ---------------------------------------------------------- DATA

    private void share(File f, String mime, String chooser) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", f);
        Intent send = new Intent(Intent.ACTION_SEND).setType(mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, chooser));
    }

    private File shareFile(String name, String text) throws Exception {
        File dir = new File(getCacheDir(), "share");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File f = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return f;
    }

    private void exportCsv() {
        try {
            share(shareFile("kurasi_export.csv", CsvExport.build(Repo.get(this))), "text/csv", "Ekspor CSV");
        } catch (Exception e) {
            toast("Gagal mengekspor");
        }
    }

    /** Cadangan ZIP (data + foto). Disalin di thread latar karena foto bisa banyak. */
    private void exportBackup() {
        toast("Menyiapkan cadangan…");
        new Thread(() -> {
            try {
                File f = Backup.write(this);
                runOnUiThread(() -> {
                    if (!isFinishing()) share(f, "application/zip", "Cadangan Kurasi");
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast("Gagal membuat cadangan"));
            }
        }).start();
    }

    private void confirmRestore(Uri uri) {
        new NeoSheet(this, "Pulihkan data?")
                .addButton("Ya, ganti semua data", 2, () -> {
                    toast("Memulihkan data…");
                    new Thread(() -> {
                        try (InputStream in = getContentResolver().openInputStream(uri)) {
                            if (in == null) throw new java.io.IOException("Berkas tidak bisa dibuka");
                            int photos = Backup.read(this, in);
                            runOnUiThread(() -> {
                                KurasiWidget.refresh(this);
                                toast(photos > 0 ? "Data dan " + photos + " foto dipulihkan" : "Data dipulihkan");
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> toast("File cadangan tidak valid"));
                        }
                    }).start();
                })
                .addButton("Batal", 1, null)
                .show();
    }
}
