package id.kurasi.app.ui;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models;
import id.kurasi.app.data.Models.Collection;
import id.kurasi.app.data.Models.FieldDef;
import id.kurasi.app.data.Repo;
import id.kurasi.app.data.Templates;
import id.kurasi.app.util.Neo;

/** Buat atau ubah koleksi: nama, warna, dan detail yang dicatat per item (tulisan, angka, tanggal, pilihan). */
public class CollectionEditorActivity extends ScrollScreenActivity {
    private static final String[] TYPES = {Models.T_TEXT, Models.T_NUMBER, Models.T_DATE, Models.T_CHOICE};
    private static final String[] TYPE_LABELS = {"Tulisan", "Angka", "Tanggal", "Pilihan"};
    private static final String[] TYPE_HINTS = {
            "Bebas diketik, misalnya nama penulis atau merek.",
            "Hanya angka, misalnya tahun terbit atau ukuran.",
            "Dipilih dari kalender, misalnya tanggal rilis.",
            "Pilih satu dari daftar yang kamu tentukan."};

    private Collection col;
    private boolean editing, ready;
    private String lastTemplateName = "";
    private String initialSig;

    @Override
    protected String screenTitle() {
        return getIntent().getLongExtra("id", 0) > 0 ? "Ubah koleksi" : "Koleksi baru";
    }

    @Override
    protected void build() {
        Repo repo = Repo.get(this);
        if (!ready) {
            long id = getIntent().getLongExtra("id", 0);
            col = id > 0 ? repo.collection(id) : null;
            editing = col != null;
            if (col == null) col = Templates.build("");
            ready = true;
            initialSig = signature();
            guardBack(this::isDirty);
        }

        if (!editing) {
            content.addView(Ui.label(this, "Mulai dari template"));
            HorizontalScrollView sv = new HorizontalScrollView(this);
            sv.setHorizontalScrollBarEnabled(false);
            LinearLayout row = new LinearLayout(this);
            final List<TextView> chips = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            keys.add("");
            List<String> names = new ArrayList<>();
            names.add("Kosong");
            for (Templates.Template t : Templates.ALL) {
                keys.add(t.key);
                names.add(t.name);
            }
            for (int i = 0; i < names.size(); i++) {
                final String key = keys.get(i);
                TextView c = Ui.chip(this, names.get(i), key.equals(col.template), null);
                c.setOnClickListener(v -> applyTemplate(key));
                chips.add(c);
                row.addView(c);
            }
            sv.addView(row);
            content.addView(sv);
        }

        content.addView(Ui.label(this, "Nama koleksi"));
        EditText name = new EditText(this, null, 0, R.style.Neo_Input);
        name.setHint("Contoh: Parfum, Action Figure");
        name.setText(col.name);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        name.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        name.addTextChangedListener(watch(s -> col.name = s));
        content.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        content.addView(Ui.label(this, "Warna"));
        LinearLayout sw = new LinearLayout(this);
        for (int color : Neo.PALETTE) {
            View v = new View(this);
            boolean sel = color == col.color;
            v.setBackground(sel ? Neo.card(this, color, 22, 3) : Neo.flat(this, color, 22));
            v.setOnClickListener(x -> {
                col.color = color;
                refresh();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
            lp.setMarginEnd(dp(10));
            sw.addView(v, lp);
        }
        // dibungkus HorizontalScrollView supaya semua warna bisa dipilih di layar sempit
        HorizontalScrollView swScroll = new HorizontalScrollView(this);
        swScroll.setHorizontalScrollBarEnabled(false);
        swScroll.setClipToPadding(false);
        swScroll.setPadding(0, 0, 0, dp(6));
        swScroll.addView(sw);
        content.addView(swScroll);

        content.addView(Ui.label(this, "Detail item · " + col.fields.size()));
        TextView detailHint = new TextView(this, null, 0, R.style.Neo_Body);
        detailHint.setText("Apa saja yang mau kamu catat tentang tiap item? Misalnya Penulis dan Tahun terbit untuk buku, "
                + "atau Ukuran dan Aroma untuk parfum. Nama, foto, harga, dan kondisi sudah otomatis ada.");
        detailHint.setTextSize(13);
        content.addView(detailHint, wrapTop(2));
        for (FieldDef f : new ArrayList<>(col.fields)) content.addView(fieldCard(f), wrapTop(10));
        content.addView(Ui.button(this, "+ Tambah detail", 1, v -> {
            FieldDef f = new FieldDef();
            f.name = "";
            col.fields.add(f);
            refresh();
        }), Ui.fullWidth(this, 52, 12));

        content.addView(Ui.button(this, "Simpan koleksi", 0, v -> save()), Ui.fullWidth(this, 58, 22));
        if (editing) {
            content.addView(Ui.button(this, "Hapus koleksi", 2, v -> new NeoSheet(this, "Hapus koleksi?")
                    .addButton("Ya, hapus koleksi dan semua itemnya", 2, () -> {
                        Repo.get(this).deleteCollection(col.id);
                        finish();
                    })
                    .addButton("Batal", 1, null)
                    .show()), Ui.fullWidth(this, 54, 12));
        }
    }

    /** Ringkasan isian saat ini, untuk mengetahui apakah ada perubahan yang belum disimpan. */
    private String signature() {
        StringBuilder sb = new StringBuilder(col.name).append('|').append(col.color);
        for (FieldDef f : col.fields) {
            sb.append('|').append(f.name).append(':').append(f.type).append(':').append(f.options);
        }
        return sb.toString();
    }

    private boolean isDirty() {
        return initialSig != null && !initialSig.equals(signature());
    }

    @Override
    protected void onBackTapped() {
        closeOrAsk(this::isDirty);
    }

    private void refresh() {
        // bangun ulang tampilan tanpa memuat ulang data dari database
        rebuild();
    }

    private void applyTemplate(String key) {
        Collection t = Templates.build(key);
        boolean nameUntouched = col.name.isEmpty() || col.name.equals(lastTemplateName);
        if (nameUntouched) col.name = t.name;
        lastTemplateName = t.name;
        col.color = t.color;
        col.template = t.template;
        col.prefix = t.prefix;
        col.fields = t.fields;
        refresh();
    }

    private View fieldCard(FieldDef f) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.flat(this, Neo.PAPER, 16));
        box.setPadding(dp(12), dp(10), dp(12), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        EditText n = new EditText(this, null, 0, R.style.Neo_Input);
        n.setHint("Nama detail, misalnya Penulis");
        n.setText(f.name);
        n.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        n.addTextChangedListener(watch(s -> f.name = s));
        top.addView(n, new LinearLayout.LayoutParams(0, dp(46), 1));
        ImageView del = new ImageView(this, null, 0, R.style.Neo_IconButton);
        del.setImageResource(R.drawable.ic_trash);
        del.setContentDescription("Hapus detail");
        del.setOnClickListener(v -> {
            for (Iterator<FieldDef> it = col.fields.iterator(); it.hasNext(); ) {
                if (it.next() == f) it.remove();
            }
            refresh();
        });
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(46), dp(46));
        dl.setMarginStart(dp(10));
        top.addView(del, dl);
        box.addView(top);

        TextView typeLabel = new TextView(this, null, 0, R.style.Neo_Label);
        typeLabel.setText("ISINYA BERUPA");
        typeLabel.setPadding(dp(2), dp(10), 0, 0);
        box.addView(typeLabel);

        HorizontalScrollView sv = new HorizontalScrollView(this);
        sv.setHorizontalScrollBarEnabled(false);
        LinearLayout types = new LinearLayout(this);
        final List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < TYPES.length; i++) {
            final int idx = i;
            TextView c = Ui.chip(this, TYPE_LABELS[i], TYPES[i].equals(f.type), null);
            c.setOnClickListener(v -> {
                f.type = TYPES[idx];
                refresh();
            });
            chips.add(c);
            types.addView(c);
        }
        sv.addView(types);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sl.topMargin = dp(4);
        box.addView(sv, sl);

        // penjelasan singkat sesuai jenis yang dipilih
        TextView typeHint = new TextView(this, null, 0, R.style.Neo_Body);
        int ti = 0;
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(f.type)) ti = i;
        typeHint.setText(TYPE_HINTS[ti]);
        typeHint.setTextSize(12);
        typeHint.setPadding(dp(2), dp(8), 0, 0);
        box.addView(typeHint);

        if (Models.T_CHOICE.equals(f.type)) {
            TextView hint = new TextView(this, null, 0, R.style.Neo_Label);
            hint.setText("DAFTAR PILIHAN, PISAHKAN DENGAN KOMA");
            hint.setPadding(0, dp(10), 0, dp(4));
            box.addView(hint);
            EditText o = new EditText(this, null, 0, R.style.Neo_Input);
            o.setHint("Kecil, Sedang, Besar");
            o.setText(f.options);
            o.addTextChangedListener(watch(s -> f.options = s));
            box.addView(o, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        }
        return box;
    }

    private interface OnText {
        void on(String s);
    }

    private TextWatcher watch(OnText cb) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                cb.on(s.toString().trim());
            }
        };
    }

    private void save() {
        if (col.name.trim().isEmpty()) {
            toast("Nama koleksinya belum diisi");
            return;
        }
        List<FieldDef> clean = new ArrayList<>();
        int skipped = 0;
        for (FieldDef f : col.fields) {
            if (f.name.trim().isEmpty()) {
                skipped++;
                continue;
            }
            if (Models.T_CHOICE.equals(f.type) && f.optionList().isEmpty()) {
                toast("Detail \"" + f.name + "\" butuh minimal satu pilihan");
                return;
            }
            clean.add(f);
        }
        col.fields = clean;
        if (!editing || col.prefix.isEmpty()) {
            if (col.prefix.isEmpty() || Templates.byKey(col.template) == null) col.prefix = Templates.prefixFor(col.name);
        }
        Repo.get(this).saveCollection(col);
        toast(skipped > 0 ? "Koleksi disimpan. Detail tanpa nama tidak ikut disimpan." : "Koleksi disimpan");
        finish();
    }
}
