package id.kurasi.app.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import id.kurasi.app.R;
import id.kurasi.app.data.SampleData;
import id.kurasi.app.ui.view.CardRenderer;
import id.kurasi.app.ui.view.CardView;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.Prefs;
import id.kurasi.app.util.QrUtil;
import id.kurasi.app.util.Rarity;

/**
 * Tutorial singkat 4 halaman. Halaman bisa digeser dengan jari, atau pakai tombol Lanjut dan Kembali.
 * Tampil sekali saat pertama dibuka, dan bisa dibuka lagi dari Pengaturan.
 */
public class OnboardingActivity extends BaseActivity {
    private static final int PAGES = 4;

    private ViewPager2 pager;
    private LinearLayout dots, actions;
    private TextView skip, back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Neo.BG);

        // baris atas: kembali di kiri, lewati di kanan
        back = new TextView(this, null, 0, R.style.Neo_Mono);
        back.setText("‹ KEMBALI");
        back.setPadding(dp(24), dp(14), dp(20), dp(14));
        back.setOnClickListener(v -> goTo(pager.getCurrentItem() - 1));
        skip = new TextView(this, null, 0, R.style.Neo_Mono);
        skip.setText("LEWATI");
        skip.setPadding(dp(20), dp(14), dp(24), dp(14));
        skip.setOnClickListener(v -> finish());
        LinearLayout top = new LinearLayout(this);
        top.addView(back);
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        top.addView(skip);
        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // halaman yang bisa digeser dengan jari
        pager = new ViewPager2(this);
        pager.setOffscreenPageLimit(3); // empat halaman: semua dibuat dari awal
        pager.setAdapter(new SlideAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                refreshChrome(position);
            }
        });
        // pembungkus bertinggi pasti: tanpa ini, ukuran pertama pager bisa melebihi layar dan mendorong
        // titik halaman serta tombol bawah keluar dari layar
        FrameLayout pagerBox = new FrameLayout(this);
        pagerBox.setClipChildren(true);
        pagerBox.addView(pager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(pagerBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        root.addView(dots, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(dp(20), dp(12), dp(24), dp(20));
        root.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        refreshChrome(0);
        // titik dan tombol bawah dibuat ulang sekali setelah layout pertama selesai; tanpa ini keduanya
        // tidak tergambar pada pembukaan pertama sampai halaman digeser
        root.post(() -> refreshChrome(pager.getCurrentItem()));

        // tombol kembali sistem mundur satu halaman dulu, baru keluar dari tutorial
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pager.getCurrentItem() > 0) goTo(pager.getCurrentItem() - 1);
                else finish();
            }
        });
    }

    private void goTo(int p) {
        if (p < 0 || p >= PAGES) return;
        pager.setCurrentItem(p, true);
    }

    /** Perbarui titik penanda, tombol bawah, dan tombol atas sesuai halaman yang tampil. */
    private void refreshChrome(int p) {
        dots.removeAllViews();
        for (int i = 0; i < PAGES; i++) {
            final int target = i;
            View d = new View(this);
            d.setBackground(Neo.flat(this, i == p ? Neo.LIME : Neo.PAPER, 8));
            d.setContentDescription("Halaman " + (i + 1));
            d.setOnClickListener(v -> goTo(target));
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(i == p ? 30 : 16), dp(16));
            dl.setMargins(dp(4), dp(4), dp(4), dp(4));
            dots.addView(d, dl);
        }

        back.setVisibility(p == 0 ? View.INVISIBLE : View.VISIBLE);
        skip.setVisibility(p == PAGES - 1 ? View.INVISIBLE : View.VISIBLE);

        actions.removeAllViews();
        if (p < PAGES - 1) {
            actions.addView(Ui.button(this, "Lanjut", 0, v -> goTo(pager.getCurrentItem() + 1)),
                    Ui.fullWidth(this, 58, 0));
        } else {
            actions.addView(Ui.button(this, "Isi data contoh", 0, v -> {
                SampleData.load(this);
                finish();
            }), Ui.fullWidth(this, 58, 0));
            actions.addView(Ui.button(this, "Mulai kosong", 1, v -> finish()), Ui.fullWidth(this, 58, 12));
        }
    }

    /** Satu halaman tutorial: gambar contoh, judul, dan penjelasan. */
    private View buildSlide(int p) {
        String title, body;
        View visual;
        switch (p) {
            case 0:
                title = "SETIAP BARANG JADI KARTU";
                body = "Catat barang koleksimu, dan Kurasi membuatkan kartunya. Skor naik kalau barangnya mahal, "
                        + "kondisinya bagus, dan usianya tua, dari Common sampai Legendary. Semua kartu tersimpan di Binder.";
                visual = sampleCard();
                break;
            case 1:
                title = "TEMPEL QR, SCAN, PINJAMKAN";
                body = "Cetak label QR dan tempel di barangnya. Scan untuk membuka kartu atau mencatat "
                        + "siapa yang meminjam dan kapan harus kembali.";
                visual = qrSticker();
                break;
            case 2:
                title = "PANTAU PENGELUARAN";
                body = "Lihat total pengeluaran dan batas bulanan. Ada juga wishlist dengan target harga, "
                        + "serta catatan nilai, nota, dan garansi.";
                visual = bars();
                break;
            default:
                title = "SIAP MULAI";
                body = "Mulai dari binder kosong, atau isi dulu dengan data contoh untuk mencoba semua fitur.";
                visual = sampleCard();
        }

        LinearLayout slide = new LinearLayout(this);
        slide.setOrientation(LinearLayout.VERTICAL);
        slide.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL); // isi di tengah, bukan menempel di atas
        slide.setPadding(dp(24), dp(4), dp(28), dp(24));

        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vl.topMargin = dp(8);
        if (p == 1) vl.width = ViewGroup.LayoutParams.MATCH_PARENT; // stiker QR selebar layar
        // kartu contoh diberi lebar tetap supaya teks dan tombol di bawahnya tetap muat
        if (p == 0) vl.width = dp(210);
        if (p == PAGES - 1) vl.width = dp(150);
        slide.addView(visual, vl);

        TextView t = new TextView(this, null, 0, R.style.Neo_Title);
        t.setText(title);
        t.setTextSize(36);
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = dp(26);
        slide.addView(t, tl);

        TextView b = new TextView(this, null, 0, R.style.Neo_Body);
        b.setText(body);
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bl.topMargin = dp(10);
        slide.addView(b, bl);

        // di layar kecil atau font besar, isi halaman bisa digulir tanpa mengganggu geseran antar halaman
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(slide, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return sv;
    }

    /** Setiap halaman punya tipe tampilan sendiri, jadi tidak ada halaman yang dipakai ulang untuk halaman lain. */
    private final class SlideAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemCount() {
            return PAGES;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(buildSlide(viewType)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            // isi halaman statis, tidak ada yang perlu diikat
        }
    }

    /** Kartu contoh Legendary, digambar dengan renderer yang sama seperti kartu asli. */
    private View sampleCard() {
        CardRenderer.Data d = new CardRenderer.Data();
        d.code = "SNK-014";
        d.name = "Air Jordan 1 Retro";
        d.tier = Rarity.LEGENDARY;
        d.score = 92;
        d.stats = new String[][]{{"UKURAN", "42"}, {"KONDISI", "BARU"}, {"HARGA", "2,85 JT"}};
        CardView c = new CardView(this);
        c.setCompact(false);
        c.setData(d);
        c.setLayoutParams(new LinearLayout.LayoutParams(dp(210), ViewGroup.LayoutParams.WRAP_CONTENT));
        return c;
    }

    /** Stiker label QR contoh. */
    private View qrSticker() {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(Neo.card(this, Neo.PAPER, 18, 5));
        box.setPadding(dp(16), dp(16), dp(21), dp(21));

        Bitmap qr = QrUtil.bitmap(QrUtil.payload("SNK-014"), 400);
        ImageView img = new ImageView(this);
        img.setImageBitmap(qr);
        img.setContentDescription("Contoh kode QR");
        img.setBackgroundColor(Neo.WHITE);
        img.setPadding(dp(6), dp(6), dp(6), dp(6));
        box.addView(img, new LinearLayout.LayoutParams(dp(110), dp(110)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView a = new TextView(this, null, 0, R.style.Neo_Label);
        a.setText("KURASI · SNEAKERS");
        TextView n = new TextView(this, null, 0, R.style.Neo_Display);
        n.setText("AIR JORDAN 1");
        n.setTextSize(24);
        TextView code = new TextView(this, null, 0, R.style.Neo_Display);
        code.setText("SNK-014");
        code.setTextSize(24);
        code.setBackground(Neo.flat(this, Neo.LIME, 10));
        code.setTextColor(Neo.ON_FILL);
        code.setPadding(dp(8), dp(1), dp(8), 0);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cl.topMargin = dp(8);
        col.addView(a);
        col.addView(n);
        col.addView(code, cl);
        // kolom teks mengambil sisa lebar supaya teks tidak terpotong
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        ll.setMarginStart(dp(14));
        box.addView(col, ll);
        return box;
    }

    /** Grafik batang contoh berwarna. */
    private View bars() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.card(this, Neo.PAPER, 18, 5));
        box.setPadding(dp(16), dp(16), dp(21), dp(21));
        int[] colors = {Neo.LIME, Neo.CYAN, Neo.LAVENDER, Neo.MINT};
        String[] names = {"SNEAKERS", "KAMERA", "VINYL", "BUKU"};
        int[] widths = {200, 130, 100, 50};
        for (int i = 0; i < colors.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView n = new TextView(this, null, 0, R.style.Neo_Display);
            n.setText(names[i]);
            n.setTextSize(18);
            row.addView(n, new LinearLayout.LayoutParams(dp(86), ViewGroup.LayoutParams.WRAP_CONTENT));
            View bar = new View(this);
            bar.setBackground(Neo.flat(this, colors[i], 8));
            row.addView(bar, new LinearLayout.LayoutParams(dp(widths[i]), dp(22)));
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) rl.topMargin = dp(8);
            box.addView(row, rl);
        }
        return box;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // keluar dengan cara apa pun (lewati, selesai, tombol kembali) dianggap sudah melihat tutorial
        if (isFinishing()) Prefs.setOnboarded(this, true);
    }
}
