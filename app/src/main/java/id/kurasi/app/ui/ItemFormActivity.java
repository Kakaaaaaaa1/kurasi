package id.kurasi.app.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import id.kurasi.app.R;
import id.kurasi.app.data.Models;
import id.kurasi.app.data.Models.Collection;
import id.kurasi.app.data.Models.FieldDef;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.SetDef;
import id.kurasi.app.data.Repo;
import id.kurasi.app.notify.Notifier;
import id.kurasi.app.ui.view.CardRenderer;
import id.kurasi.app.ui.view.CardView;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.ImageStore;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.Rarity;

/** Form tambah/ubah item. Kartu di atas ikut berubah secara langsung saat diisi. */
public class ItemFormActivity extends BaseActivity {
    private Repo repo;
    private List<Collection> cols;
    private Item item = new Item();
    private boolean editing;

    private LinearLayout form, tierBar;
    private CardView preview;
    private TextView tierCaption;
    private EditText etName, etPrice, etTarget, etNote, etSold;
    private ImageView thumb;
    private final Map<Long, Supplier<String>> getters = new HashMap<>();
    private File pendingCamera;
    private String initialSig;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) importPhoto(uri);
            });
    private final ActivityResultLauncher<Uri> takePhoto = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && pendingCamera != null) finishCamera();
            });
    private final ActivityResultLauncher<String> askCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else toast("Izin kamera ditolak. Aktifkan lewat Pengaturan HP untuk memakai kamera, atau pilih foto dari Galeri.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_form);
        repo = Repo.get(this);
        cols = repo.collections();
        if (cols.isEmpty()) {
            toast("Buat koleksi dulu sebelum menambah item");
            startActivity(new Intent().setClassName(this, "id.kurasi.app.ui.CollectionsActivity"));
            finish();
            return;
        }

        form = findViewById(R.id.form);
        tierBar = findViewById(R.id.tierBar);
        preview = findViewById(R.id.preview);
        tierCaption = findViewById(R.id.tierCaption);
        preview.setCompact(false);

        long id = getIntent().getLongExtra("id", 0);
        if (id > 0) {
            Item loaded = repo.item(id);
            if (loaded != null) {
                item = loaded;
                editing = true;
            }
        }
        if (!editing) {
            item.collectionId = getIntent().getLongExtra("collectionId", 0);
            if (item.collectionId == 0) item.collectionId = cols.get(0).id;
            item.acquiredAt = System.currentTimeMillis();
            if (getIntent().getBooleanExtra("wishlist", false)) item.status = Models.S_WISHLIST;
        }

        ((TextView) findViewById(R.id.title)).setText(editing ? "UBAH ITEM" : "ITEM BARU");
        findViewById(R.id.btnBack).setOnClickListener(v -> closeOrAsk(this::isDirty));
        guardBack(this::isDirty);
        View del = findViewById(R.id.btnDelete);
        if (editing) {
            del.setVisibility(View.VISIBLE);
            del.setOnClickListener(v -> new NeoSheet(this, "Hapus item?")
                    .addButton("Ya, hapus", 2, () -> {
                        repo.deleteItem(item.id);
                        toast("Item dihapus");
                        finish();
                    })
                    .addButton("Batal", 1, null)
                    .show());
        }
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        buildForm();
        initialSig = signature();
    }

    /** Ringkasan isian saat ini, dipakai untuk mengetahui apakah ada perubahan yang belum disimpan. */
    private String signature() {
        syncFromViews();
        return item.name + "|" + item.price + "|" + item.targetPrice + "|" + item.note + "|" + item.condition
                + "|" + item.status + "|" + item.collectionId + "|" + item.setId + "|" + item.photo
                + "|" + item.acquiredAt + "|" + item.soldPrice + "|" + item.tierOverride
                + "|" + new java.util.TreeMap<>(item.values);
    }

    private boolean isDirty() {
        return initialSig != null && !initialSig.equals(signature());
    }

    private Collection current() {
        for (Collection c : cols) if (c.id == item.collectionId) return c;
        item.collectionId = cols.get(0).id;
        return cols.get(0);
    }

    // ============================================================ FORM

    private void buildForm() {
        syncFromViews();
        form.removeAllViews();
        getters.clear();
        etName = etPrice = etTarget = etNote = etSold = null;
        Collection cur = current();
        boolean wish = Models.S_WISHLIST.equals(item.status);

        // Koleksi
        form.addView(Ui.label(this, "Koleksi"));
        List<String> names = new ArrayList<>();
        int sel = 0;
        for (int i = 0; i < cols.size(); i++) {
            names.add(cols.get(i).name);
            if (cols.get(i).id == cur.id) sel = i;
        }
        form.addView(chipGroup(names, sel, false, i -> {
            syncFromViews();
            item.collectionId = cols.get(i).id;
            item.setId = 0;
            buildForm();
        }));

        // Status
        form.addView(Ui.label(this, "Status"));
        String[] st = {Models.S_OWNED, Models.S_WISHLIST, Models.S_SOLD};
        int ss = 0;
        for (int i = 0; i < st.length; i++) if (st[i].equals(item.status)) ss = i;
        List<String> stLabels = new ArrayList<>();
        stLabels.add("Dimiliki");
        stLabels.add("Wishlist");
        stLabels.add("Terjual");
        form.addView(chipGroup(stLabels, ss, false, i -> {
            syncFromViews();
            item.status = st[i];
            buildForm();
        }));

        // Foto
        form.addView(Ui.label(this, "Foto"));
        LinearLayout photoRow = new LinearLayout(this);
        photoRow.setOrientation(LinearLayout.HORIZONTAL);
        photoRow.setGravity(Gravity.CENTER_VERTICAL);
        thumb = new ImageView(this);
        thumb.setBackgroundResource(R.drawable.bg_input);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setPadding(dp(3), dp(3), dp(3), dp(3));
        thumb.setClipToOutline(true);
        thumb.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(14));
            }
        });
        photoRow.addView(thumb, new LinearLayout.LayoutParams(dp(76), dp(76)));
        updateThumb();
        // ketuk foto untuk menghapusnya
        thumb.setOnClickListener(v -> {
            if (item.photo == null || item.photo.isEmpty()) return;
            new NeoSheet(this, "Foto item")
                    .addButton("Hapus foto", 2, () -> {
                        item.photo = "";
                        updateThumb();
                        updatePreview();
                    })
                    .addButton("Batal", 1, null)
                    .show();
        });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0, dp(52), 1);
        bl.setMarginStart(dp(12));
        photoRow.addView(Ui.button(this, "Kamera", 1, v -> openCamera()), bl);
        LinearLayout.LayoutParams bl2 = new LinearLayout.LayoutParams(0, dp(52), 1);
        bl2.setMarginStart(dp(12));
        photoRow.addView(Ui.button(this, "Galeri", 1, v -> pickPhoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build())), bl2);
        form.addView(photoRow);

        // Nama + harga
        form.addView(Ui.label(this, "Nama"));
        etName = input("Nama item", item.name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(Ui.label(this, wish ? "Harga sekarang" : "Harga beli"));
        etPrice = input("Rp 0", item.price == 0 ? "" : String.format(new java.util.Locale("id", "ID"), "%,d", item.price),
                InputType.TYPE_CLASS_NUMBER);
        etPrice.addTextChangedListener(new MoneyWatcher(etPrice, this::updatePreview));
        if (wish) {
            form.addView(Ui.label(this, "Target harga (kamu diingatkan kalau harganya sudah di bawah ini)"));
            etTarget = input("Rp 0", item.targetPrice == 0 ? ""
                    : String.format(new java.util.Locale("id", "ID"), "%,d", item.targetPrice), InputType.TYPE_CLASS_NUMBER);
            etTarget.addTextChangedListener(new MoneyWatcher(etTarget, null));
        }
        if (Models.S_SOLD.equals(item.status)) {
            form.addView(Ui.label(this, "Harga jual (opsional, untuk melihat untung rugi)"));
            etSold = input("Rp 0", item.soldPrice == 0 ? ""
                    : String.format(new java.util.Locale("id", "ID"), "%,d", item.soldPrice), InputType.TYPE_CLASS_NUMBER);
            etSold.addTextChangedListener(new MoneyWatcher(etSold, null));
        }

        // Kondisi
        form.addView(Ui.label(this, "Kondisi"));
        List<String> conds = new ArrayList<>();
        int cs = 0;
        for (int i = 0; i < Models.CONDITIONS.length; i++) {
            conds.add(Fmt.cap(Models.CONDITIONS[i]));
            if (Models.CONDITIONS[i].equals(item.condition)) cs = i;
        }
        form.addView(chipGroup(conds, cs, false, i -> {
            item.condition = Models.CONDITIONS[i];
            updatePreview();
        }));

        // Tingkat kelangkaan: otomatis dari skor, atau diatur sendiri
        form.addView(Ui.label(this, "Tingkat kelangkaan"));
        List<String> tiers = new ArrayList<>();
        tiers.add("Otomatis");
        for (Rarity r : Rarity.values()) tiers.add(Fmt.cap(r.label));
        form.addView(chipGroup(tiers, item.tierOverride + 1, false, i -> {
            item.tierOverride = i - 1;
            updatePreview();
        }));

        // Tanggal beli
        if (!wish) {
            form.addView(Ui.label(this, "Tanggal beli"));
            form.addView(dateButton(item.acquiredAt, ms -> {
                item.acquiredAt = ms;
                updatePreview();
            }));
        }

        // Set
        List<SetDef> sets = repo.sets();
        if (!wish && !sets.isEmpty()) {
            form.addView(Ui.label(this, "Bagian dari set"));
            List<String> sl = new ArrayList<>();
            sl.add("Tanpa set");
            int si = 0;
            for (int i = 0; i < sets.size(); i++) {
                sl.add(sets.get(i).name);
                if (sets.get(i).id == item.setId) si = i + 1;
            }
            form.addView(chipGroup(sl, si, false, i -> item.setId = i == 0 ? 0 : sets.get(i - 1).id));
        }

        // Field dinamis koleksi
        for (FieldDef f : cur.fields) addDynamicField(f);

        // Catatan
        form.addView(Ui.label(this, "Catatan"));
        etNote = input("Opsional: tempat beli, cerita di baliknya, dll.", item.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etNote.setSingleLine(false);
        etNote.setMinLines(2);
        etNote.setGravity(Gravity.TOP);
        etNote.setPadding(dp(14), dp(12), dp(14), dp(12));

        updatePreview();
    }

    private EditText input(String hint, String value, int type) {
        EditText e = new EditText(this, null, 0, R.style.Neo_Input);
        e.setHint(hint);
        e.setInputType(type);
        // kolom satu baris memakai tombol "Next" di keyboard supaya bisa lanjut ke isian berikutnya
        if ((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) {
            e.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        }
        e.setText(value);
        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePreview();
            }
        });
        form.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private void addDynamicField(FieldDef f) {
        form.addView(Ui.label(this, f.name));
        String existing = item.values.get(f.id);
        switch (f.type) {
            case Models.T_CHOICE: {
                List<String> opts = f.optionList();
                final String[] holder = {existing == null ? "" : existing};
                int sel = opts.indexOf(holder[0]);
                form.addView(chipGroup(opts, sel, true, i -> {
                    holder[0] = i < 0 ? "" : opts.get(i);
                    updatePreview();
                }));
                getters.put(f.id, () -> holder[0]);
                break;
            }
            case Models.T_DATE: {
                long ms = 0;
                try {
                    ms = existing == null || existing.isEmpty() ? 0 : Long.parseLong(existing);
                } catch (NumberFormatException ignored) {
                }
                final long[] holder = {ms};
                form.addView(dateButton(ms, v -> {
                    holder[0] = v;
                    updatePreview();
                }));
                getters.put(f.id, () -> holder[0] > 0 ? String.valueOf(holder[0]) : "");
                break;
            }
            default: {
                int type = Models.T_NUMBER.equals(f.type) ? InputType.TYPE_CLASS_NUMBER
                        : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                EditText e = input(f.name, existing == null ? "" : existing, type);
                getters.put(f.id, () -> e.getText().toString());
            }
        }
    }

    private View dateButton(long ms, Consumer<Long> onPick) {
        TextView t = new TextView(this, null, 0, R.style.Neo_Input);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setText(ms > 0 ? Fmt.date(ms) : "Pilih tanggal");
        t.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (ms > 0) c.setTimeInMillis(ms);
            new DatePickerDialog(this, (dp, y, m, d) -> {
                Calendar x = Calendar.getInstance();
                x.set(y, m, d, 12, 0, 0);
                t.setText(Fmt.date(x.getTimeInMillis()));
                onPick.accept(x.getTimeInMillis());
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return t;
    }

    /** Barisan chip, pilih satu. Jika allowClear, klik chip terpilih akan membatalkan pilihan (-1). */
    private View chipGroup(List<String> labels, int selected, boolean allowClear, Consumer<Integer> onPick) {
        HorizontalScrollView sv = new HorizontalScrollView(this);
        sv.setHorizontalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final List<TextView> chips = new ArrayList<>();
        final int[] cur = {selected};
        for (int i = 0; i < labels.size(); i++) {
            final int idx = i;
            TextView c = Ui.chip(this, labels.get(i), i == selected, null);
            c.setOnClickListener(v -> {
                int next = (allowClear && cur[0] == idx) ? -1 : idx;
                cur[0] = next;
                for (int k = 0; k < chips.size(); k++) chips.get(k).setSelected(k == next);
                onPick.accept(next);
            });
            chips.add(c);
            row.addView(c);
        }
        sv.addView(row);
        return sv;
    }

    // ============================================================ DATA

    /** Salin isi kolom ke objek item. */
    private void syncFromViews() {
        if (etName != null) item.name = etName.getText().toString().trim();
        if (etPrice != null) item.price = Fmt.parseMoney(etPrice.getText());
        if (etTarget != null) item.targetPrice = Fmt.parseMoney(etTarget.getText());
        if (etNote != null) item.note = etNote.getText().toString().trim();
        if (etSold != null) item.soldPrice = Fmt.parseMoney(etSold.getText());
        for (Map.Entry<Long, Supplier<String>> e : getters.entrySet()) item.values.put(e.getKey(), e.getValue().get());
    }

    private void updatePreview() {
        if (preview == null || form == null || etName == null) return;
        syncFromViews();
        Collection cur = current();
        long value = Models.S_WISHLIST.equals(item.status) || item.value <= 0 ? item.price : item.value;
        item.score = Rarity.withOverride(Rarity.score(value, item.condition, item.acquiredAt), item.tierOverride);
        String codeBackup = item.code;
        if (item.code.isEmpty()) item.code = cur.prefix + "-NEW";
        CardRenderer.Data d = CardRenderer.Data.from(this, item, cur);
        item.code = codeBackup;
        preview.setData(d);

        Rarity tier = Rarity.of(item.score);
        tierBar.removeAllViews();
        Rarity[] all = Rarity.values();
        for (int i = 0; i < all.length; i++) {
            View seg = new View(this);
            seg.setBackground(Neo.flat(this, i <= tier.ordinal() ? all[i].color : Neo.PAPER, 8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            if (i > 0) lp.setMarginStart(dp(6));
            tierBar.addView(seg, lp);
        }
        tierCaption.setText("SKOR " + item.score + " · " + tier.label
                + "  |  LEGENDARY MULAI SKOR " + Rarity.LEGENDARY.minScore
                + (item.tierOverride >= 0
                ? "\nTIER DIATUR MANUAL. PILIH OTOMATIS UNTUK MENGHITUNG DARI HARGA"
                : item.price <= 0
                ? "\nHARGA BELUM DIISI, JADI SKOR MASIH RENDAH"
                : "\nSKOR NAIK DARI HARGA, KONDISI, DAN USIA BARANG"));
    }

    private void save() {
        syncFromViews();
        if (item.name.isEmpty()) {
            etName.setError("Nama itemnya belum diisi");
            etName.requestFocus();
            return;
        }
        Collection cur = current();
        item.collectionId = cur.id;
        Map<Long, String> keep = new HashMap<>();
        for (FieldDef f : cur.fields) if (item.values.containsKey(f.id)) keep.put(f.id, item.values.get(f.id));
        item.values = keep;
        if (Models.S_WISHLIST.equals(item.status)) item.setId = 0;

        repo.saveItem(item);
        if (Models.S_WISHLIST.equals(item.status) && item.targetPrice > 0 && item.price > 0
                && item.price <= item.targetPrice) {
            Notifier.wishlistHit(this, item);
        }
        toast(editing ? "Perubahan disimpan" : "Kartu dibuat: " + item.code);
        finish();
    }

    // ============================================================ FOTO

    private void updateThumb() {
        Bitmap b = ImageStore.load(item.photo, 300);
        if (b != null) {
            thumb.setImageBitmap(b);
            thumb.setImageTintList(null);
        } else {
            thumb.setImageResource(R.drawable.ic_image);
            thumb.setImageTintList(android.content.res.ColorStateList.valueOf(Neo.INK));
        }
        if (b == null) thumb.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        else thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    // Membaca, memperkecil, dan menyimpan foto itu berat, jadi dikerjakan di thread terpisah.
    private final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();

    private void importPhoto(Uri uri) {
        io.execute(() -> {
            try {
                String path = ImageStore.importUri(this, uri);
                runOnUiThread(() -> applyPhoto(path));
            } catch (Exception e) {
                runOnUiThread(() -> toast("Gagal membuka foto"));
            }
        });
    }

    private void applyPhoto(String path) {
        if (isFinishing() || isDestroyed()) return;
        item.photo = path;
        updateThumb();
        updatePreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
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
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", pendingCamera);
        takePhoto.launch(uri);
    }

    private void finishCamera() {
        final File f = pendingCamera;
        io.execute(() -> {
            try {
                ImageStore.normalizeInPlace(f);
                runOnUiThread(() -> applyPhoto(f.getAbsolutePath()));
            } catch (Exception e) {
                runOnUiThread(() -> toast("Gagal menyimpan foto"));
            }
        });
    }
}
