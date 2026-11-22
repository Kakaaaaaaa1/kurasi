package id.kurasi.app.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Collection;
import id.kurasi.app.data.Models.FieldDef;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.Loan;
import id.kurasi.app.data.Models.SetDef;
import id.kurasi.app.data.Repo;
import id.kurasi.app.ui.view.CardRenderer;
import id.kurasi.app.ui.view.CardView;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.Neo;

/** Halaman satu kartu: kartu besar, aksi cepat, dan ringkasan status. */
public class CardActivity extends BaseActivity {
    private Repo repo;
    private Item item;
    private Collection col;
    private CardView card;
    private LinearLayout panels;
    private long itemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        repo = Repo.get(this);
        itemId = getIntent().getLongExtra("id", 0);
        card = findViewById(R.id.card);
        panels = findViewById(R.id.panels);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEdit).setOnClickListener(v ->
                startActivity(new Intent(this, ItemFormActivity.class).putExtra("id", itemId)));
        findViewById(R.id.btnShare).setOnClickListener(v -> shareCard());
        findViewById(R.id.btnLabel).setOnClickListener(v ->
                startActivity(new Intent().setClassName(this, "id.kurasi.app.ui.LabelActivity").putExtra("id", itemId)));
        findViewById(R.id.btnLoan).setOnClickListener(v -> LoanSheet.show(this, item, this::load));
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        item = repo.item(itemId);
        if (item == null) {
            finish();
            return;
        }
        col = repo.collection(item.collectionId);
        ((TextView) findViewById(R.id.codePill)).setText("KARTU " + item.code);
        card.setCompact(false);
        card.setData(CardRenderer.Data.from(this, item, col));
        ((TextView) findViewById(R.id.btnLoan)).setText(item.loaned ? "KEMBALIKAN" : "PINJAMKAN");
        buildPanels();
    }

    private void buildPanels() {
        panels.removeAllViews();

        Loan loan = repo.activeLoan(item.id);
        String status;
        switch (item.status) {
            case "WISHLIST": status = "Wishlist" + (item.targetPrice > 0 ? " · target " + Fmt.jt(item.targetPrice) : ""); break;
            case "SOLD":
                status = "Terjual";
                if (item.soldPrice > 0) {
                    status += " " + Fmt.rp(item.soldPrice);
                    if (item.price > 0) {
                        long diff = item.soldPrice - item.price;
                        status += (diff >= 0 ? " (untung " : " (rugi ") + Fmt.rp(Math.abs(diff)) + ")";
                    }
                }
                break;
            default: status = "Dimiliki · di rak";
        }
        if (loan != null) {
            status = "Dipinjam " + loan.borrower + " · kembali " + Fmt.date(loan.dueAt);
        }
        panels.addView(row("Status", status, loan != null && loan.dueAt < System.currentTimeMillis() ? Neo.ORANGE : Neo.PAPER));

        // Nilai
        if (!"WISHLIST".equals(item.status)) {
            String delta = "";
            if (item.price > 0 && item.value != item.price) {
                int pct = Math.round((item.value - item.price) * 100f / item.price);
                delta = " (" + (pct >= 0 ? "+" : "") + pct + "%)";
            }
            // item yang sudah dijual tidak punya "nilai sekarang"; hasil jualnya sudah tampil di Status
            if (!"SOLD".equals(item.status)) {
                panels.addView(row("Nilai sekarang", Fmt.rp(item.value) + delta, item.value >= item.price ? Neo.MINT : Neo.PINK));
            }
            LinearLayout.LayoutParams lp = Ui.fullWidth(this, 54, "SOLD".equals(item.status) ? 10 : 14);
            panels.addView(Ui.button(this, "Nilai, nota, dan garansi", 1,
                    v -> startActivity(new Intent().setClassName(this, "id.kurasi.app.ui.ValueActivity").putExtra("id", itemId))), lp);
        }

        // Riwayat peminjaman (maksimal 5 terbaru)
        List<Loan> history = repo.loanHistory(item.id);
        if (!history.isEmpty()) {
            panels.addView(Ui.label(this, "Riwayat pinjam · " + history.size()));
            long now = System.currentTimeMillis();
            for (int i = 0; i < Math.min(5, history.size()); i++) {
                panels.addView(loanRow(history.get(i), now), i == 0 ? wrapTopParams(0) : wrapTopParams(8));
            }
        }

        // Rincian field dinamis
        if (col != null) {
            StringBuilder sb = new StringBuilder();
            for (FieldDef f : col.fields) {
                String v = item.values.get(f.id);
                if (v == null || v.isEmpty()) continue;
                if ("DATE".equals(f.type)) {
                    try {
                        v = Fmt.date(Long.parseLong(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (sb.length() > 0) sb.append("\n");
                sb.append(f.name).append(": ").append(v);
            }
            if (item.setId > 0) {
                List<SetDef> sets = repo.sets();
                for (SetDef s : sets) {
                    if (s.id == item.setId) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append("Set: ").append(s.name).append(" (").append(s.count).append("/").append(s.targetCount).append(")");
                    }
                }
            }
            if (!item.note.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Catatan: ").append(item.note);
            }
            if (sb.length() > 0) {
                panels.addView(row("Rincian", sb.toString(), Neo.PAPER));
            }
        }
    }

    private LinearLayout.LayoutParams wrapTopParams(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    /** Satu baris riwayat: nama peminjam, tanggal, dan status (dipinjam, terlambat, atau kembali). */
    private View loanRow(Loan l, long now) {
        boolean returned = l.returnedAt > 0;
        boolean late = !returned && l.dueAt < now;
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(Neo.flat(this, Neo.PAPER, 14));
        box.setPadding(dp(14), dp(10), dp(12), dp(10));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this, null, 0, R.style.Neo_Body);
        name.setText(l.borrower);
        TextView when = new TextView(this, null, 0, R.style.Neo_Label);
        when.setText(("DIPINJAM " + Fmt.date(l.loanedAt) + (returned ? " · KEMBALI " + Fmt.date(l.returnedAt)
                : " · TEMPO " + Fmt.date(l.dueAt))).toUpperCase(java.util.Locale.ROOT));
        col.addView(name);
        col.addView(when);
        box.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tag = new TextView(this, null, 0, R.style.Neo_Mono);
        tag.setTextSize(10);
        tag.setText(returned ? "KEMBALI" : late ? "TERLAMBAT" : "DIPINJAM");
        tag.setBackground(Neo.flat(this, returned ? Neo.MINT : late ? Neo.PINK : Neo.ORANGE, 99));
        tag.setTextColor(Neo.ON_FILL);
        tag.setPadding(dp(8), dp(1), dp(8), dp(1));
        box.addView(tag);
        return box;
    }

    private View row(String label, String value, int fill) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.flat(this, fill, 16));
        box.setPadding(dp(14), dp(10), dp(14), dp(12));
        TextView l = new TextView(this, null, 0, R.style.Neo_Label);
        l.setText(label.toUpperCase(java.util.Locale.ROOT));
        TextView v = new TextView(this, null, 0, R.style.Neo_Body);
        v.setText(value);
        v.setGravity(Gravity.START);
        if (fill != Neo.PAPER) {
            l.setTextColor(Neo.MUTED_ON_FILL);
            v.setTextColor(Neo.ON_FILL);
        }
        box.addView(l);
        box.addView(v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        box.setLayoutParams(lp);
        return box;
    }

    /** Ekspor kartu sebagai gambar PNG lalu buka menu bagikan. */
    private void shareCard() {
        try {
            Bitmap bmp = CardRenderer.toBitmap(this, CardRenderer.Data.from(this, item, col), 1000);
            File dir = new File(getCacheDir(), "share");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File f = new File(dir, "kartu_" + item.code + ".png");
            try (FileOutputStream out = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", f);
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TEXT, item.name + " · " + item.code + " · skor " + item.score)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Bagikan kartu"));
        } catch (Exception e) {
            toast("Gagal membagikan kartu");
        }
    }
}
