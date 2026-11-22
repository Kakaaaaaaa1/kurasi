package id.kurasi.app.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.ValueRecord;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.ImageStore;
import id.kurasi.app.util.Neo;

/** Riwayat nilai item, foto nota pembelian, dan garansi toko dengan pengingat. */
public class ValueActivity extends BaseActivity {
    private static final long DAY = 24L * 3600 * 1000;

    private Repo repo;
    private Item item;
    private LinearLayout content;
    private EditText etStore;
    private File pendingCamera;

    // Foto diproses di thread terpisah supaya layar tidak membeku.
    private final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<PickVisualMediaRequest> pick = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                io.execute(() -> {
                    try {
                        String path = ImageStore.importUri(this, uri);
                        runOnUiThread(() -> setReceipt(path));
                    } catch (Exception e) {
                        runOnUiThread(() -> toast("Gagal membuka foto"));
                    }
                });
            });
    private final ActivityResultLauncher<Uri> take = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (!ok || pendingCamera == null) return;
                final File f = pendingCamera;
                io.execute(() -> {
                    try {
                        ImageStore.normalizeInPlace(f);
                        runOnUiThread(() -> setReceipt(f.getAbsolutePath()));
                    } catch (Exception e) {
                        runOnUiThread(() -> toast("Gagal menyimpan foto"));
                    }
                });
            });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
    private final ActivityResultLauncher<String> askCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else toast("Izin kamera ditolak. Aktifkan lewat Pengaturan HP, atau pilih foto dari Galeri.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Neo.BG);
        sv.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(24), dp(32));
        sv.addView(content);
        setContentView(sv);
        repo = Repo.get(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        item = repo.item(getIntent().getLongExtra("id", 0));
        if (item == null) {
            finish();
            return;
        }
        build();
    }

    private void build() {
        content.removeAllViews();

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = new ImageView(this, null, 0, R.style.Neo_IconButton);
        back.setImageResource(R.drawable.ic_back);
        back.setContentDescription("Kembali");
        back.setOnClickListener(v -> finish());
        head.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView sub = new TextView(this, null, 0, R.style.Neo_Label);
        sub.setText(item.code + " · " + item.name.toUpperCase(java.util.Locale.ROOT));
        sub.setSingleLine(true);
        TextView t = new TextView(this, null, 0, R.style.Neo_Title);
        t.setText("NILAI DAN NOTA");
        t.setTextSize(34);
        titles.addView(sub);
        titles.addView(t);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tl.setMarginStart(dp(14));
        head.addView(titles, tl);
        content.addView(head);

        buildValue();
        buildReceipt();
        buildWarranty();

        content.addView(Ui.button(this, "Simpan nota dan garansi", 0, v -> {
            item.store = etStore.getText().toString().trim();
            repo.saveItem(item);
            toast("Tersimpan");
            finish();
        }), Ui.fullWidth(this, 58, 22));
    }

    // ----------------------------------------------------------- nilai

    private void buildValue() {
        List<ValueRecord> vals = repo.values(item.id);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.card(this, Neo.PAPER, 22, 5));
        box.setPadding(dp(16), dp(12), dp(21), dp(19));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView l = new TextView(this, null, 0, R.style.Neo_Mono);
        l.setText("NILAI SEKARANG");
        top.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (item.price > 0) {
            int pct = Math.round((item.value - item.price) * 100f / item.price);
            TextView tag = new TextView(this, null, 0, R.style.Neo_Mono);
            tag.setText((pct >= 0 ? "+" : "") + pct + "%");
            tag.setBackground(Neo.flat(this, pct >= 0 ? Neo.MINT : Neo.PINK, 99));
            tag.setPadding(dp(10), 0, dp(10), 0);
            tag.setTextColor(Neo.ON_FILL);
            top.addView(tag);
        }
        box.addView(top);

        LinearLayout big = new LinearLayout(this);
        big.setGravity(Gravity.BOTTOM);
        TextView n = new TextView(this, null, 0, R.style.Neo_Display);
        n.setText(Fmt.juta(item.value));
        n.setTextSize(60);
        big.addView(n);
        TextView u = new TextView(this, null, 0, R.style.Neo_Display);
        u.setText(" JUTA RP");
        u.setTextSize(26);
        u.setPadding(dp(6), 0, 0, dp(6));
        big.addView(u, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView buy = new TextView(this, null, 0, R.style.Neo_Label);
        buy.setText("BELI " + Fmt.jt(item.price).toUpperCase(java.util.Locale.ROOT));
        big.addView(buy);
        box.addView(big);

        // grafik batang dari catatan terakhir
        int from = Math.max(0, vals.size() - 6);
        long max = 1;
        for (int i = from; i < vals.size(); i++) max = Math.max(max, vals.get(i).value);
        LinearLayout chart = new LinearLayout(this);
        chart.setGravity(Gravity.BOTTOM);
        for (int i = from; i < vals.size(); i++) {
            View bar = new View(this);
            boolean last = i == vals.size() - 1;
            bar.setBackground(Neo.flat(this, last ? Neo.LIME : Neo.COMMON, 6));
            int h = Math.max(dp(10), (int) (dp(60) * vals.get(i).value / (float) max));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, h, 1);
            if (i > from) lp.setMarginStart(dp(6));
            chart.addView(bar, lp);
        }
        box.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));
        TextView foot = new TextView(this, null, 0, R.style.Neo_Label);
        foot.setText("NILAI DIPERBARUI MANUAL · " + vals.size() + " CATATAN");
        foot.setPadding(0, dp(6), 0, 0);
        box.addView(foot);

        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bl.topMargin = dp(18);
        content.addView(box, bl);
        content.addView(Ui.button(this, "+ Catat nilai baru", 1, v -> valueSheet()), Ui.fullWidth(this, 54, 16));
    }

    private void valueSheet() {
        NeoSheet s = new NeoSheet(this, "Catat nilai baru");
        s.body().addView(Ui.label(this, "Nilai pasar sekarang"));
        EditText e = new EditText(this, null, 0, R.style.Neo_Input);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setHint("Rp 0");
        e.addTextChangedListener(new MoneyWatcher(e, null));
        s.body().addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        s.addButton("Simpan nilai", 0, null);
        s.body().getChildAt(s.body().getChildCount() - 1).setOnClickListener(v -> {
            long val = Fmt.parseMoney(e.getText());
            if (val <= 0) {
                e.setError("Isi nilainya");
                return;
            }
            repo.addValue(item.id, val, System.currentTimeMillis());
            s.dismiss();
            item = repo.item(item.id);
            build();
        });
        s.show();
    }

    // ----------------------------------------------------------- nota

    private void buildReceipt() {
        content.addView(Ui.label(this, "Nota pembelian"));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.flat(this, Neo.PAPER, 18));
        box.setPadding(dp(12), dp(12), dp(12), dp(14));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView img = new ImageView(this);
        img.setBackground(Neo.flat(this, Neo.BG, 10));
        img.setPadding(dp(3), dp(3), dp(3), dp(3));
        Bitmap b = ImageStore.load(item.receiptPhoto, 400);
        if (b != null) {
            img.setImageBitmap(b);
            img.setImageTintList(null);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setOnClickListener(v -> viewReceipt());
        } else {
            img.setImageResource(R.drawable.ic_receipt);
            img.setImageTintList(android.content.res.ColorStateList.valueOf(Neo.INK));
            img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        row.addView(img, new LinearLayout.LayoutParams(dp(72), dp(92)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView hint = new TextView(this, null, 0, R.style.Neo_Label);
        hint.setText("NAMA TOKO");
        etStore = new EditText(this, null, 0, R.style.Neo_Input);
        etStore.setHint("Opsional");
        etStore.setText(item.store);
        info.addView(hint);
        info.addView(etStore, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        TextView meta = new TextView(this, null, 0, R.style.Neo_Label);
        meta.setText(item.acquiredAt > 0 ? Fmt.date(item.acquiredAt).toUpperCase(java.util.Locale.ROOT) + " · " + Fmt.rp(item.price).toUpperCase(java.util.Locale.ROOT) : "");
        info.addView(meta);
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        il.setMarginStart(dp(12));
        row.addView(info, il);
        box.addView(row);

        LinearLayout btns = new LinearLayout(this);
        TextView cam = Ui.button(this, "Foto nota", 1, v -> openCamera());
        TextView gal = Ui.button(this, "Galeri", 1, v -> pick.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, dp(50), 1);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, dp(50), 1);
        b2.setMarginStart(dp(10));
        btns.addView(cam, b1);
        btns.addView(gal, b2);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bl.topMargin = dp(12);
        box.addView(btns, bl);
        content.addView(box);
    }

    private void setReceipt(String path) {
        item.store = etStore == null ? item.store : etStore.getText().toString().trim();
        item.receiptPhoto = path;
        repo.saveItem(item);
        build();
    }

    private void viewReceipt() {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", new File(item.receiptPhoto));
            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) {
            toast("Tidak bisa membuka nota");
        }
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            askCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        pendingCamera = ImageStore.newPhotoFile(this);
        take.launch(FileProvider.getUriForFile(this, getPackageName() + ".files", pendingCamera));
    }

    // ----------------------------------------------------------- garansi

    private void buildWarranty() {
        content.addView(Ui.label(this, "Garansi toko"));
        long now = System.currentTimeMillis();
        boolean set = item.warrantyUntil > 0;
        long days = set ? (long) Math.ceil((item.warrantyUntil - now) / (double) DAY) : 0;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.flat(this, !set ? Neo.PAPER : days < 0 ? Neo.COMMON : days <= 30 ? Neo.ORANGE : Neo.YELLOW, 18));
        box.setPadding(dp(14), dp(12), dp(14), dp(14));

        TextView big = new TextView(this, null, 0, R.style.Neo_Display);
        big.setTextSize(38);
        big.setText(!set ? "BELUM DIATUR" : days < 0 ? "GARANSI HABIS" : "SISA " + days + " HARI");
        box.addView(big);
        TextView until = new TextView(this, null, 0, R.style.Neo_Mono);
        until.setText(set ? "SAMPAI " + Fmt.date(item.warrantyUntil).toUpperCase(java.util.Locale.ROOT) : "ATUR TANGGAL GARANSI BERAKHIR");
        box.addView(until);
        if (set && item.acquiredAt > 0 && item.warrantyUntil > item.acquiredAt) {
            int pct = (int) Math.max(0, Math.min(100, (now - item.acquiredAt) * 100 / (item.warrantyUntil - item.acquiredAt)));
            LinearLayout track = new LinearLayout(this);
            track.setBackground(Neo.flatOnFill(this, Neo.WHITE, 7));
            track.setPadding(dp(2), dp(2), dp(2), dp(2));
            View fill = new View(this);
            fill.setBackground(Neo.flatOnFill(this, Neo.ON_FILL, 5));
            track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, pct)));
            track.addView(new View(this), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(0, 100 - pct)));
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(14));
            pl.topMargin = dp(10);
            box.addView(track, pl);
        }
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (set) Neo.onFill(box);
        content.addView(box, bl);

        content.addView(Ui.button(this, set ? "Ubah tanggal garansi" : "Atur tanggal garansi", 1, v -> {
            Calendar c = Calendar.getInstance();
            if (set) c.setTimeInMillis(item.warrantyUntil);
            new DatePickerDialog(this, (dp, y, m, d) -> {
                Calendar x = Calendar.getInstance();
                x.set(y, m, d, 12, 0, 0);
                item.store = etStore.getText().toString().trim();
                item.warrantyUntil = x.getTimeInMillis();
                repo.saveItem(item);
                build();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        }), Ui.fullWidth(this, 54, 12));

        TextView note = new TextView(this, null, 0, R.style.Neo_Label);
        note.setText("PENGINGAT OTOMATIS 30 HARI SEBELUM GARANSI HABIS (JIKA NOTIFIKASI AKTIF)");
        note.setPadding(0, dp(10), 0, 0);
        content.addView(note);
    }
}
