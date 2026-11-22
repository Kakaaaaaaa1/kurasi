package id.kurasi.app.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import id.kurasi.app.R;

/**
 * Warna dan helper gambar bergaya neobrutalism (outline tebal, bayangan keras).
 *
 * Ada dua jenis warna:
 * - Warna tema (INK, BG, PAPER, MUTED): berubah di mode terang dan gelap. Diisi ulang oleh init()
 *   setiap layar dibuat, dari res/values dan res/values-night.
 * - Warna tetap (LIME, CYAN, ..., ON_FILL, WHITE): sama di kedua mode. Isi kartu berwarna selalu
 *   memakai teks ON_FILL (gelap) supaya terbaca.
 */
public final class Neo {
    private Neo() {}

    // ---- Warna tema (diisi oleh init)
    public static int INK = 0xFF111112;     // teks dan garis di atas latar halaman
    public static int BG = 0xFFF4F4ED;      // latar halaman
    public static int PAPER = 0xFFFFFFFF;   // permukaan kartu, input, chip
    public static int MUTED = 0xFF55574D;   // teks sekunder

    // ---- Warna tetap
    public static final int ON_FILL = 0xFF111112;      // teks dan garis di atas isi berwarna
    public static final int MUTED_ON_FILL = 0xFF55574D;
    public static final int LIGHT = 0xFFF4F4ED;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF111112;
    public static final int LIME = 0xFFD2FF00;
    public static final int CYAN = 0xFF00E5FF;
    public static final int LAVENDER = 0xFFB69CFF;
    public static final int ORANGE = 0xFFFF6B00;
    public static final int PINK = 0xFFFF3D81;
    public static final int MINT = 0xFF3DDC97;
    public static final int YELLOW = 0xFFFFD23F;
    public static final int COMMON = 0xFFD9D9CF;
    public static final int HATCH_B = 0xFFECECE2;

    /** Pilihan warna koleksi. */
    public static final int[] PALETTE = {LIME, CYAN, LAVENDER, ORANGE, PINK, MINT, YELLOW};

    /** Baca warna tema dari resource sesuai mode terang/gelap saat ini. Panggil dengan context Activity. */
    public static void init(Context c) {
        INK = c.getColor(R.color.ink);
        BG = c.getColor(R.color.bg);
        PAPER = c.getColor(R.color.paper);
        MUTED = c.getColor(R.color.muted);
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    /** Kotak bulat berisi warna, outline tema, dan bayangan keras di kanan bawah. */
    public static Drawable card(Context c, int fill, float radiusDp, float shadowDp) {
        float r = dp(c, radiusDp);
        int s = dp(c, shadowDp);
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(INK);
        shadow.setCornerRadius(r);
        GradientDrawable front = new GradientDrawable();
        front.setColor(fill);
        front.setCornerRadius(r);
        front.setStroke(dp(c, 2.5f), INK);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{shadow, front});
        ld.setLayerInset(0, s, s, 0, 0);
        ld.setLayerInset(1, 0, 0, s, s);
        return ld;
    }

    /** Pil/kotak berisi warna dengan outline tema, tanpa bayangan. */
    public static Drawable flat(Context c, int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(dp(c, 2f), INK);
        return g;
    }

    /** Seperti flat(), tetapi outline selalu gelap. Untuk elemen di dalam wadah berwarna. */
    public static Drawable flatOnFill(Context c, int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(dp(c, 2f), ON_FILL);
        return g;
    }

    /** Tombol bertepi bawah dengan warna isi bebas. */
    public static Drawable button(Context c, int fill, float radiusDp) {
        float r = dp(c, radiusDp);
        int e = dp(c, 5);
        GradientDrawable edge = new GradientDrawable();
        edge.setColor(INK);
        edge.setCornerRadius(r);
        GradientDrawable front = new GradientDrawable();
        front.setColor(fill);
        front.setCornerRadius(r);
        front.setStroke(dp(c, 2.5f), INK);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{edge, front});
        ld.setLayerInset(0, 0, e, 0, 0);
        ld.setLayerInset(1, 0, 0, 0, e);
        return ld;
    }

    /**
     * Beri semua teks di dalam [root] warna ON_FILL. Dipakai untuk wadah berisi warna (lime, pink, ...)
     * supaya teksnya tetap gelap dan terbaca di mode gelap.
     */
    public static void onFill(View root) {
        if (root instanceof TextView) {
            ((TextView) root).setTextColor(ON_FILL);
        } else if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) onFill(g.getChildAt(i));
        }
    }
}
