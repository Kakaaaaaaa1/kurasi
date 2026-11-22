package id.kurasi.app.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import id.kurasi.app.util.Neo;

/** View kartu koleksi. Mode compact dipakai di grid Binder, mode penuh di halaman kartu. */
public class CardView extends View {
    private CardRenderer.Data data;
    private boolean compact;

    public CardView(Context c) {
        super(c);
    }

    public CardView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
        requestLayout();
        invalidate();
    }

    public void setData(CardRenderer.Data d) {
        this.data = d;
        setContentDescription(describe(d));
        invalidate();
    }

    /** Kartu digambar di Canvas, jadi pembaca layar (TalkBack) perlu deskripsi tertulis. */
    private static String describe(CardRenderer.Data d) {
        if (d == null) return null;
        StringBuilder sb = new StringBuilder("Kartu ").append(d.name).append(", ")
                .append(d.tier.label).append(", skor ").append(d.score);
        for (String[] s : d.stats) {
            if (s != null && s[0] != null && s[1] != null && !s[1].isEmpty()) {
                sb.append(", ").append(s[0].toLowerCase(java.util.Locale.ROOT)).append(' ').append(s[1]);
            }
        }
        return sb.toString();
    }

    public CardRenderer.Data getData() {
        return data;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED || w == 0) w = Neo.dp(getContext(), 300);
        float ratio = compact ? CardRenderer.RATIO_COMPACT : CardRenderer.RATIO_FULL;
        setMeasuredDimension(w, Math.round(w * ratio));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (data == null) return;
        CardRenderer.draw(getContext(), canvas, getWidth(), getHeight(), compact, data);
    }
}
