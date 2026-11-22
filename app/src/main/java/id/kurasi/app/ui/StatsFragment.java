package id.kurasi.app.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Stat;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.Prefs;
import id.kurasi.app.util.Rarity;
import id.kurasi.app.util.StatsCard;

/** Ringkasan pengeluaran: total, batas bulanan, per koleksi, grafik 6 bulan, dan kartu untuk dibagikan. */
public class StatsFragment extends ScreenFragment {
    private Repo repo;
    private LinearLayout content;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_stats, container, false);
        repo = Repo.get(ctx());
        content = root.findViewById(R.id.content);
        // blur dock di belakang halaman ini dihitung ulang saat halaman di-scroll
        View dock = requireActivity().findViewById(R.id.dock);
        if (dock instanceof id.kurasi.app.ui.view.GlassDock && root instanceof ViewGroup && ((ViewGroup) root).getChildAt(0) instanceof android.widget.ScrollView) {
            ((id.kurasi.app.ui.view.GlassDock) dock).watch((android.widget.ScrollView) ((ViewGroup) root).getChildAt(0));
        }
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // dibangun ulang tiap halaman ini menjadi halaman aktif, supaya angkanya selalu terbaru
        build();
    }

    private void build() {
        content.removeAllViews();
        long spend = repo.totalSpend(), value = repo.totalValue();
        int owned = repo.ownedCount();

        // judul
        LinearLayout head = new LinearLayout(ctx());
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(ctx(), null, 0, R.style.Neo_Title);
        title.setText("STATISTIK");
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView pill = new TextView(ctx(), null, 0, R.style.Neo_Mono);
        pill.setText(owned + " ITEM");
        pill.setBackgroundResource(R.drawable.bg_pill);
        pill.setPadding(dp(12), dp(5), dp(12), dp(5));
        head.addView(pill);
        content.addView(head);

        // total
        LinearLayout total = new LinearLayout(ctx());
        total.setOrientation(LinearLayout.VERTICAL);
        total.setBackground(Neo.card(ctx(), Neo.LIME, 22, 5));
        total.setPadding(dp(16), dp(12), dp(21), dp(19));
        TextView l = new TextView(ctx(), null, 0, R.style.Neo_Mono);
        l.setText("TOTAL PENGELUARAN");
        total.addView(l);
        LinearLayout big = new LinearLayout(ctx());
        big.setGravity(Gravity.BOTTOM);
        TextView n = new TextView(ctx(), null, 0, R.style.Neo_Display);
        n.setText(Fmt.juta(spend));
        n.setTextSize(88);
        big.addView(n);
        TextView u = new TextView(ctx(), null, 0, R.style.Neo_Display);
        u.setText(" JUTA RP");
        u.setTextSize(32);
        u.setPadding(dp(8), 0, 0, dp(8));
        big.addView(u);
        total.addView(big);
        String delta = "";
        // persentase membandingkan nilai sekarang dengan modal barang yang masih dimiliki
        long keptSpend = repo.ownedSpend();
        if (keptSpend > 0) {
            int pct = Math.round((value - keptSpend) * 100f / keptSpend);
            delta = " (" + (pct >= 0 ? "+" : "") + pct + "%)";
        }
        TextView sub = new TextView(ctx(), null, 0, R.style.Neo_Mono);
        sub.setText("NILAI SEKARANG " + Fmt.rpJt(value).toUpperCase(java.util.Locale.ROOT) + delta);
        total.addView(sub);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = dp(16);
        Neo.onFill(total);
        content.addView(total, tl);

        buildSold();
        buildLimit();
        buildPerCollection();
        buildMonthly();
        buildCounters();

        TextView share = Ui.button(ctx(), "Bagikan kartu statistik", 0, v -> shareCard(spend, value, owned));
        content.addView(share, Ui.fullWidth(ctx(), 58, 24));
    }

    /** Ringkasan item yang sudah dijual: untung atau rugi dari harga jual yang sudah dicatat. */
    private void buildSold() {
        int sold = repo.soldCount();
        if (sold == 0) return;
        int priced = repo.soldPricedCount();
        long profit = repo.soldProfit();

        LinearLayout box = new LinearLayout(ctx());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.flat(ctx(), priced == 0 ? Neo.PAPER : profit >= 0 ? Neo.MINT : Neo.PINK, 18));
        box.setPadding(dp(14), dp(12), dp(14), dp(14));
        TextView l = new TextView(ctx(), null, 0, R.style.Neo_Mono);
        l.setText("HASIL PENJUALAN · " + sold + " ITEM TERJUAL");
        TextView v = new TextView(ctx(), null, 0, R.style.Neo_Body);
        if (priced == 0) {
            v.setText("Isi harga jual di item yang terjual untuk melihat untung atau ruginya.");
            v.setTextSize(14);
        } else {
            v.setText((profit >= 0 ? "Untung " : "Rugi ") + Fmt.rp(Math.abs(profit))
                    + (priced < sold ? " dari " + priced + " item yang harga jualnya dicatat" : ""));
            v.setTextSize(17);
            v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
        }
        box.addView(l);
        box.addView(v);
        if (priced > 0) Neo.onFill(box); // latar berwarna tetap, jadi teksnya selalu gelap (juga di mode gelap)
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        content.addView(box, lp);
    }

    private void buildLimit() {
        long limit = Prefs.monthlyLimit(ctx());
        long used = repo.spendThisMonth();
        LinearLayout box = new LinearLayout(ctx());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.flat(ctx(), Neo.PAPER, 18));
        box.setPadding(dp(14), dp(12), dp(14), dp(14));
        box.setOnClickListener(v -> limitSheet());

        LinearLayout top = new LinearLayout(ctx());
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(ctx(), null, 0, R.style.Neo_Mono);
        t.setText(limit > 0 ? "BATAS BULAN INI · " + Fmt.rpJt(limit).toUpperCase(java.util.Locale.ROOT) : "BATAS PENGELUARAN BULANAN");
        top.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (limit > 0) {
            int pct = (int) Math.min(999, used * 100 / limit);
            TextView tag = new TextView(ctx(), null, 0, R.style.Neo_Mono);
            tag.setText(pct >= 100 ? "MELEWATI BATAS" : pct >= 80 ? "HAMPIR PENUH" : "AMAN");
            tag.setBackground(Neo.flat(ctx(), pct >= 100 ? Neo.PINK : pct >= 80 ? Neo.ORANGE : Neo.MINT, 99));
            tag.setTextColor(Neo.ON_FILL);
            tag.setPadding(dp(8), dp(1), dp(8), dp(1));
            top.addView(tag);
        }
        box.addView(top);

        if (limit > 0) {
            int pct = (int) Math.min(100, used * 100 / limit);
            box.addView(progress(pct, pct >= 100 ? Neo.PINK : pct >= 80 ? Neo.ORANGE : Neo.MINT), barParams(10));
            TextView s = new TextView(ctx(), null, 0, R.style.Neo_Label);
            s.setText("TERPAKAI " + Fmt.jt(used).toUpperCase(java.util.Locale.ROOT) + " · SISA " + Fmt.jt(Math.max(0, limit - used)).toUpperCase(java.util.Locale.ROOT));
            box.addView(s);
        } else {
            TextView s = new TextView(ctx(), null, 0, R.style.Neo_Body);
            s.setText("Ketuk untuk mengatur batas. Kamu akan diingatkan saat pengeluaran bulan ini hampir penuh.");
            s.setTextSize(13);
            box.addView(s);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(18);
        content.addView(box, lp);
    }

    private LinearLayout.LayoutParams barParams(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16));
        lp.topMargin = dp(topDp);
        lp.bottomMargin = dp(6);
        return lp;
    }

    /** Bar progres bergaris: isi [pct]% berwarna [color]. */
    private View progress(int pct, int color) {
        LinearLayout track = new LinearLayout(ctx());
        track.setBackground(Neo.flat(ctx(), Neo.BG, 8));
        track.setPadding(dp(2), dp(2), dp(2), dp(2));
        View fill = new View(ctx());
        fill.setBackground(Neo.flat(ctx(), color, 6));
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, pct)));
        View rest = new View(ctx());
        track.addView(rest, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(0, 100 - pct)));
        return track;
    }

    private void buildPerCollection() {
        content.addView(Ui.label(ctx(), "Per koleksi"));
        List<Stat> stats = repo.spendByCollection();
        long max = 1;
        for (Stat s : stats) max = Math.max(max, s.amount);
        for (Stat s : stats) {
            LinearLayout row = new LinearLayout(ctx());
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = new TextView(ctx(), null, 0, R.style.Neo_Display);
            name.setText(s.label.toUpperCase(java.util.Locale.ROOT));
            name.setTextSize(18);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(name, new LinearLayout.LayoutParams(dp(104), ViewGroup.LayoutParams.WRAP_CONTENT));
            int pct = (int) Math.max(4, s.amount * 100 / max);
            LinearLayout track = new LinearLayout(ctx());
            View bar = new View(ctx());
            bar.setBackground(Neo.flat(ctx(), s.color, 8));
            track.addView(bar, new LinearLayout.LayoutParams(0, dp(22), pct));
            track.addView(new View(ctx()), new LinearLayout.LayoutParams(0, dp(22), 100 - pct));
            row.addView(track, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView val = new TextView(ctx(), null, 0, R.style.Neo_Mono);
            val.setText(Fmt.jt(s.amount).toUpperCase(java.util.Locale.ROOT));
            val.setGravity(Gravity.END);
            LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(dp(66), ViewGroup.LayoutParams.WRAP_CONTENT);
            vl.setMarginStart(dp(8));
            row.addView(val, vl);
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rl.topMargin = dp(6);
            content.addView(row, rl);
        }
        if (stats.isEmpty()) {
            TextView e = new TextView(ctx(), null, 0, R.style.Neo_Body);
            e.setText("Belum ada data. Tambah item dengan harga beli, lalu pengeluaranmu muncul di sini.");
            content.addView(e);
        }
    }

    private void buildMonthly() {
        content.addView(Ui.label(ctx(), "Pengeluaran 6 bulan terakhir · juta rp"));
        long[] m = repo.monthlySpend(6);
        long max = 1;
        for (long v : m) max = Math.max(max, v);
        LinearLayout chart = new LinearLayout(ctx());
        chart.setOrientation(LinearLayout.HORIZONTAL);
        chart.setGravity(Gravity.BOTTOM);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -5);
        for (int i = 0; i < m.length; i++) {
            LinearLayout col = new LinearLayout(ctx());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            TextView val = new TextView(ctx(), null, 0, R.style.Neo_Mono);
            val.setTextSize(10);
            val.setText(m[i] == 0 ? "0" : Fmt.juta(m[i]));
            col.addView(val);
            View bar = new View(ctx());
            boolean last = i == m.length - 1;
            bar.setBackground(Neo.flat(ctx(), last ? Neo.LIME : Neo.PAPER, 6));
            int h = Math.max(dp(8), (int) (dp(90) * m[i] / (float) max));
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h);
            bl.topMargin = dp(3);
            bl.bottomMargin = dp(3);
            col.addView(bar, bl);
            TextView mo = new TextView(ctx(), null, 0, R.style.Neo_Label);
            mo.setText(Fmt.monthShort(cal.getTimeInMillis()));
            col.addView(mo);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) cl.setMarginStart(dp(8));
            chart.addView(col, cl);
            cal.add(Calendar.MONTH, 1);
        }
        content.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(138)));
    }

    private void buildCounters() {
        int[] tiers = repo.tierCounts();
        LinearLayout row = new LinearLayout(ctx());
        row.addView(counter(String.valueOf(tiers[Rarity.LEGENDARY.ordinal()]), "LEGENDARY", Neo.LIME));
        row.addView(counter(String.valueOf(tiers[Rarity.EPIC.ordinal()]), "EPIC", Neo.LAVENDER));
        row.addView(counter(String.valueOf(tiers[Rarity.RARE.ordinal()]), "RARE", Neo.CYAN));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(18);
        content.addView(row, lp);
    }

    private View counter(String num, String label, int color) {
        LinearLayout box = new LinearLayout(ctx());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.card(ctx(), color, 16, 3));
        box.setPadding(dp(12), dp(8), dp(15), dp(11));
        TextView n = new TextView(ctx(), null, 0, R.style.Neo_Display);
        n.setText(num);
        n.setTextSize(34);
        TextView l = new TextView(ctx(), null, 0, R.style.Neo_Mono);
        l.setTextSize(10);
        l.setText(label);
        // satu baris saja: di layar kecil atau font besar, teks mengecil alih-alih patah ("LEGENDAR/Y")
        l.setSingleLine(true);
        l.setLetterSpacing(0f);
        l.setPadding(0, 0, dp(4), 0); // sisa ruang supaya huruf terakhir tidak terpotong
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                l, 6, 9, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        box.addView(n);
        // lebar penuh supaya ukuran teks otomatis punya batas lebar yang pasti
        box.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Neo.onFill(box);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMarginEnd(dp(8));
        box.setLayoutParams(lp);
        return box;
    }

    private void limitSheet() {
        NeoSheet s = new NeoSheet(ctx(), "Batas bulanan");
        s.body().addView(Ui.label(ctx(), "Batas pengeluaran per bulan"));
        EditText e = new EditText(ctx(), null, 0, R.style.Neo_Input);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setHint("Rp 15.000.000");
        long cur = Prefs.monthlyLimit(ctx());
        if (cur > 0) e.setText(String.valueOf(cur));
        e.addTextChangedListener(new MoneyWatcher(e, null));
        s.body().addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        s.addButton("Simpan batas", 0, null);
        View save = s.body().getChildAt(s.body().getChildCount() - 1);
        save.setOnClickListener(v -> {
            Prefs.setMonthlyLimit(ctx(), Fmt.parseMoney(e.getText()));
            s.dismiss();
            build();
        });
        if (cur > 0) {
            s.addButton("Hapus batas", 1, () -> {
                Prefs.setMonthlyLimit(ctx(), 0);
                build();
            });
        }
        s.show();
    }

    private void shareCard(long spend, long value, int owned) {
        try {
            int[] t = repo.tierCounts();
            Bitmap bmp = StatsCard.build(ctx(), owned, spend, value, repo.spendByCollection(),
                    t[Rarity.LEGENDARY.ordinal()], t[Rarity.EPIC.ordinal()], t[Rarity.RARE.ordinal()]);
            File dir = new File(getCacheDir(), "share");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File f = new File(dir, "statistik.png");
            try (FileOutputStream out = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(ctx(), getPackageName() + ".files", f);
            Intent send = new Intent(Intent.ACTION_SEND).setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Bagikan statistik"));
        } catch (Exception e) {
            toast("Gagal membuat gambar");
        }
    }
}
