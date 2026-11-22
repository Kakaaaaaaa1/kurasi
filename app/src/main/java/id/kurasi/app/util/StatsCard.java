package id.kurasi.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

import java.util.List;
import java.util.Locale;

import id.kurasi.app.data.Models.Stat;

/** Gambar ringkasan koleksi yang bisa dibagikan ("kartu pamer"). */
public final class StatsCard {
    private StatsCard() {}

    public static Bitmap build(Context c, int itemCount, long totalSpend, long totalValue, List<Stat> byCollection,
                               int legendary, int epic, int rare) {
        int w = 1080, h = 1350;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.drawColor(Neo.LIGHT);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setColor(Neo.ON_FILL);

        // kartu utama
        RectF card = new RectF(60, 60, w - 90, h - 90);
        fill.setColor(Neo.ON_FILL);
        cv.drawRoundRect(new RectF(card.left + 24, card.top + 24, card.right + 24, card.bottom + 24), 56, 56, fill);
        fill.setColor(Neo.LIME);
        cv.drawRoundRect(card, 56, 56, fill);
        line.setStrokeWidth(8);
        cv.drawRoundRect(card, 56, 56, line);

        TextPaint mono = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mono.setTypeface(Fonts.monoBold(c));
        mono.setColor(Neo.ON_FILL);
        TextPaint bebas = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bebas.setTypeface(Fonts.bebas(c));
        bebas.setColor(Neo.ON_FILL);

        float x = card.left + 64;
        mono.setTextSize(34);
        cv.drawText("KURASI · KOLEKSIKU", x, card.top + 110, mono);

        bebas.setTextSize(300);
        String total = Fmt.juta(totalValue);
        cv.drawText(total, x, card.top + 400, bebas);
        float tw = bebas.measureText(total);
        bebas.setTextSize(90);
        cv.drawText("JUTA RP", x + tw + 24, card.top + 400, bebas);
        mono.setTextSize(30);
        cv.drawText("NILAI KOLEKSI SEKARANG", x, card.top + 450, mono);

        mono.setTextSize(34);
        cv.drawText(itemCount + " ITEM · MODAL " + Fmt.rpJt(totalSpend).toUpperCase(Locale.ROOT), x, card.top + 520, mono);
        cv.drawText(legendary + " LEGENDARY · " + epic + " EPIC · " + rare + " RARE", x, card.top + 570, mono);

        // bar per koleksi
        long max = 1;
        for (Stat s : byCollection) max = Math.max(max, s.amount);
        float y = card.top + 650;
        int shown = 0;
        float barX = x + 300, barW = card.right - 64 - barX - 130;
        for (Stat s : byCollection) {
            if (s.amount <= 0 || shown >= 5) continue;
            bebas.setTextSize(52);
            // nama koleksi yang panjang dipotong supaya tidak menabrak batang
            cv.drawText(android.text.TextUtils.ellipsize(s.label.toUpperCase(Locale.ROOT), bebas, barX - x - 20,
                    android.text.TextUtils.TruncateAt.END).toString(), x, y + 46, bebas);
            RectF bar = new RectF(barX, y, barX + Math.max(40, barW * s.amount / max), y + 62);
            fill.setColor(s.color);
            cv.drawRoundRect(bar, 22, 22, fill);
            line.setStrokeWidth(6);
            cv.drawRoundRect(bar, 22, 22, line);
            mono.setTextSize(30);
            cv.drawText(Fmt.jt(s.amount), bar.right + 16, y + 42, mono);
            y += 100;
            shown++;
        }
        return bmp;
    }
}
