package id.kurasi.app.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import id.kurasi.app.util.Neo;

/**
 * Dock berbahan "kaca": isi halaman di belakangnya diburamkan (Android 12+), diberi tint warna kertas,
 * kilau tipis di atas, dan tepi tinta tipis. Di Android lama, blur diganti tint yang lebih pekat.
 * Menggambar isi saudara-saudaranya (yang berada di belakang dock) ke RenderNode berefek blur,
 * lalu dihitung ulang saat halaman di-scroll.
 */
public class GlassDock extends LinearLayout {
    private static final boolean CAN_BLUR = Build.VERSION.SDK_INT >= 31;

    private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pill = new RectF();
    private final Path clip = new Path();
    private Object blurNode; // RenderNode, disimpan sebagai Object supaya kelas ini aman dimuat di Android lama

    private final List<RecyclerView> lists = new ArrayList<>();
    private final List<ScrollView> scrolls = new ArrayList<>();
    private final RecyclerView.OnScrollListener onListScroll = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView rv, int dx, int dy) {
            invalidate();
        }
    };

    // ---- lensa tab yang bisa digeser (3 tab pertama; anak ke-4 adalah tombol tambah)
    /** Dipanggil saat tab diketuk. */
    public interface OnTabSelected {
        void onSelected(int index);
    }

    /** Dipanggil saat lensa digeser dengan jari. Pager di belakangnya mengikuti jari secara langsung. */
    public interface DragListener {
        void onDragStart();

        /** Perubahan posisi lensa dalam satuan tab (1.0 = satu tab ke kanan). */
        void onDrag(float deltaIndex);

        void onDragEnd(int targetIndex);
    }

    private static final int TABS = 3;
    private final Paint lens = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lensRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] tabTint = new int[TABS];
    private OnTabSelected tapListener;
    private DragListener dragListener;
    private float progress; // posisi lensa dalam satuan tab (0..2), diisi pager saat halaman bergeser
    private float lensX = -1, lensScale = 1f, downX, lastIdx;
    private boolean dragging;
    private int lastNear = -1;
    private ValueAnimator scaleAnim;

    public GlassDock(Context c) {
        this(c, null);
    }

    public GlassDock(Context c, AttributeSet a) {
        super(c, a);
        setWillNotDraw(false);
        rim.setStyle(Paint.Style.STROKE);
        outline.setStyle(Paint.Style.STROKE);
        shadow.setStyle(Paint.Style.FILL);
        lensRim.setStyle(Paint.Style.STROKE);
    }

    public void setListeners(OnTabSelected tap, DragListener drag) {
        tapListener = tap;
        dragListener = drag;
    }

    /** Posisi lensa mengikuti pager: 0 = Binder, 1 = Scan, 2 = Statistik, dengan pecahan saat halaman bergeser. */
    public void setProgress(float p) {
        progress = Math.max(0, Math.min(TABS - 1, p));
        if (dragging || getChildCount() <= TABS || getWidth() == 0) return;
        lensX = xOf(progress);
        updateTints();
        invalidate();
    }

    private float tabCenter(int i) {
        View f = getChildAt(i);
        return f.getLeft() + f.getWidth() / 2f;
    }

    /** Titik tengah lensa untuk posisi tab pecahan. */
    private float xOf(float idx) {
        int i = Math.max(0, Math.min(TABS - 1, (int) Math.floor(idx)));
        int j = Math.min(i + 1, TABS - 1);
        return tabCenter(i) + (tabCenter(j) - tabCenter(i)) * (idx - i);
    }

    /** Kebalikan xOf: posisi tab pecahan untuk koordinat x. */
    private float idxOf(float x) {
        float c0 = tabCenter(0), span = tabCenter(1) - c0;
        return span <= 0 ? 0 : (x - c0) / span;
    }

    private int nearest(float x) {
        return Math.round(Math.max(0, Math.min(TABS - 1, idxOf(x))));
    }

    /** Ikon tab yang sedang berada di bawah lensa memakai warna aksen, yang lain warna tinta. */
    private void updateTints() {
        int accent = getContext().getColor(id.kurasi.app.R.color.accent_on_ink);
        for (int i = 0; i < TABS; i++) {
            boolean under = Math.abs(lensX - tabCenter(i)) < dp(26);
            int color = under ? accent : Neo.INK;
            if (tabTint[i] == color) continue;
            tabTint[i] = color;
            View icon = ((ViewGroup) getChildAt(i)).getChildAt(0);
            if (icon instanceof android.widget.ImageView) {
                ((android.widget.ImageView) icon).setImageTintList(android.content.res.ColorStateList.valueOf(color));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!dragging && getChildCount() > TABS) {
            lensX = xOf(progress);
            updateTints();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        // sentuhan di area tab diambil alih supaya bisa ditekan maupun digeser; tombol tambah tetap normal
        return e.getActionMasked() == MotionEvent.ACTION_DOWN && getChildCount() > TABS
                && e.getX() < getChildAt(TABS).getLeft();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging && Math.abs(x - downX) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    dragging = true;
                    lensX = xOf(progress);
                    lastIdx = idxOf(lensX);
                    lastNear = nearest(lensX);
                    animateScale(1.18f);
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    if (dragListener != null) dragListener.onDragStart();
                }
                if (dragging) {
                    lensX = Math.max(tabCenter(0), Math.min(tabCenter(TABS - 1), x));
                    float idx = idxOf(lensX);
                    float delta = idx - lastIdx;
                    lastIdx = idx;
                    int near = nearest(lensX);
                    if (near != lastNear) {
                        lastNear = near;
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    // halaman di belakang ikut bergeser sebesar gerakan lensa
                    if (dragListener != null) dragListener.onDrag(delta);
                    updateTints();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean up = e.getActionMasked() == MotionEvent.ACTION_UP;
                if (dragging) {
                    dragging = false;
                    animateScale(1f);
                    if (dragListener != null) dragListener.onDragEnd(up ? nearest(lensX) : Math.round(progress));
                } else if (up && tapListener != null) {
                    tapListener.onSelected(nearest(x));
                }
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void animateScale(float to) {
        if (scaleAnim != null) scaleAnim.cancel();
        scaleAnim = ValueAnimator.ofFloat(lensScale, to);
        scaleAnim.setDuration(160);
        scaleAnim.addUpdateListener(a -> {
            lensScale = (float) a.getAnimatedValue();
            invalidate();
        });
        scaleAnim.start();
    }

    /** Daftar atau scroll yang digeser di belakang dock; blur dihitung ulang tiap isinya bergeser. */
    public void watch(RecyclerView rv) {
        if (lists.contains(rv)) return;
        lists.add(rv);
        rv.addOnScrollListener(onListScroll);
    }

    public void watch(ScrollView sv) {
        if (scrolls.contains(sv)) return;
        scrolls.add(sv);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> invalidate());
    }

    private float dp(float v) {
        return Neo.dp(getContext(), v);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // blur harus dihitung ulang saat isi di belakang dock bergeser
        if (getParent() instanceof ViewGroup) {
            ViewGroup p = (ViewGroup) getParent();
            for (int i = 0; i < p.indexOfChild(this); i++) collectScrollers(p.getChildAt(i));
        }
        for (RecyclerView rv : lists) rv.addOnScrollListener(onListScroll);
        for (ScrollView sv : scrolls) sv.setOnScrollChangeListener((v, x, y, ox, oy) -> invalidate());
    }

    @Override
    protected void onDetachedFromWindow() {
        for (RecyclerView rv : lists) rv.removeOnScrollListener(onListScroll);
        for (ScrollView sv : scrolls) sv.setOnScrollChangeListener(null);
        lists.clear();
        scrolls.clear();
        super.onDetachedFromWindow();
    }

    private void collectScrollers(View v) {
        if (v instanceof RecyclerView) lists.add((RecyclerView) v);
        else if (v instanceof ScrollView) scrolls.add((ScrollView) v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collectScrollers(g.getChildAt(i));
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        // bidang kaca, menyisakan ruang di dalam batas view untuk bayangan lembut
        pill.set(dp(3), dp(3), w - dp(3), h - dp(9));
        float r = pill.height() / 2f;

        // bayangan lembut: beberapa lapis tipis semi-transparan
        for (int i = 4; i >= 1; i--) {
            shadow.setColor((0x16 << 24));
            c.drawRoundRect(pill.left + dp(i), pill.top + dp(i), pill.right - dp(i), pill.bottom + dp(i + 1), r, r, shadow);
        }

        boolean blurred = CAN_BLUR && c.isHardwareAccelerated() && getParent() instanceof ViewGroup;
        clip.reset();
        clip.addRoundRect(pill, r, r, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        if (blurred) Blur.draw(this, c, pill);

        // tint warna kertas (ikut mode terang atau gelap). Tanpa blur dibuat lebih pekat supaya teks tetap terbaca.
        boolean darkTheme = (Neo.INK & 0xFF) > 0x80; // ink terang berarti mode gelap
        int base = Neo.PAPER & 0x00FFFFFF;
        int tintAlpha = blurred ? (darkTheme ? 0xB0 : 0x80) : 0xDD;
        tint.setColor((tintAlpha << 24) | base);
        c.drawRect(pill, tint);

        // kilau di bagian atas: putih memudar ke bening
        int glow = darkTheme ? 0x30 : 0x88;
        sheen.setShader(new LinearGradient(0, pill.top, 0, pill.top + pill.height() * 0.6f,
                (glow << 24) | 0xFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(pill, sheen);
        c.restore();

        // tepi dalam yang terang (kesan ketebalan kaca), lalu tepi tinta tipis di luar
        rim.setStrokeWidth(dp(1));
        rim.setColor(darkTheme ? 0x40FFFFFF : 0xB0FFFFFF);
        c.drawRoundRect(pill.left + dp(2), pill.top + dp(2), pill.right - dp(2), pill.bottom - dp(2), r, r, rim);
        outline.setStrokeWidth(dp(2));
        outline.setColor((0xD8 << 24) | (Neo.INK & 0x00FFFFFF));
        c.drawRoundRect(pill.left + dp(1), pill.top + dp(1), pill.right - dp(1), pill.bottom - dp(1), r, r, outline);

        // lensa tab aktif: lingkaran tinta yang membesar sedikit saat digeser, tepinya diberi kilau tipis
        if (lensX >= 0 && getChildCount() > TABS) {
            float cy = pill.centerY();
            float rad = dp(23) * lensScale;
            lens.setColor(Neo.INK);
            c.drawCircle(lensX, cy, rad, lens);
            lensRim.setStrokeWidth(dp(1.2f));
            lensRim.setColor(darkTheme ? 0x66000000 : 0x66FFFFFF);
            c.drawCircle(lensX, cy, rad - dp(1.6f), lensRim);
        }
    }

    /** Bagian yang hanya jalan di Android 12+: gambar isi di belakang dock lalu buramkan. */
    @RequiresApi(31)
    private static final class Blur {
        static void draw(GlassDock d, Canvas c, RectF area) {
            int w = d.getWidth(), h = d.getHeight();
            RenderNode node = (RenderNode) d.blurNode;
            if (node == null) {
                node = new RenderNode("glass-dock");
                d.blurNode = node;
            }
            node.setPosition(0, 0, w, h);
            float radius = d.dp(14);
            node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));

            Canvas rc = node.beginRecording(w, h);
            try {
                rc.drawColor(Neo.BG);
                ViewGroup p = (ViewGroup) d.getParent();
                int me = p.indexOfChild(d);
                for (int i = 0; i < me; i++) {
                    View ch = p.getChildAt(i);
                    if (ch.getVisibility() != View.VISIBLE) continue;
                    rc.save();
                    rc.translate(ch.getLeft() - d.getLeft() + ch.getTranslationX(),
                            ch.getTop() - d.getTop() + ch.getTranslationY());
                    ch.draw(rc);
                    rc.restore();
                }
            } finally {
                node.endRecording();
            }
            c.drawRenderNode(node);
        }
    }
}
