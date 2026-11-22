package id.kurasi.app.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import id.kurasi.app.util.Fonts;
import id.kurasi.app.util.Neo;

/** Teks judul besar berisi warna lime dengan garis hitam, direntangkan selebar layar. */
public class WordmarkView extends View {
    private String text = "BINDER";
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WordmarkView(Context c) {
        super(c);
        init();
    }

    public WordmarkView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        fill.setColor(Neo.LIME);
        stroke.setColor(Neo.INK);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setTypeface(Fonts.bebas(getContext()));
        stroke.setTypeface(Fonts.bebas(getContext()));
    }

    public void setText(String t) {
        text = t;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, Math.round(w * 0.2f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight(), w = getWidth();
        float size = h * 1.05f;
        fill.setTextSize(size);
        stroke.setTextSize(size);
        stroke.setStrokeWidth(h * 0.06f);
        float tw = fill.measureText(text);
        float inset = h * 0.04f;
        float sx = (w - 2 * inset) / tw;
        canvas.save();
        canvas.translate(inset, 0);
        canvas.scale(sx, 1f);
        float baseline = h * 0.9f;
        canvas.drawText(text, 0, baseline, stroke);
        canvas.drawText(text, 0, baseline, fill);
        canvas.restore();
    }
}
