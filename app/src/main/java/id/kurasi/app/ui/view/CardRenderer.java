package id.kurasi.app.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.util.Locale;

import id.kurasi.app.data.Models;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.Fonts;
import id.kurasi.app.util.ImageStore;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.Rarity;

/**
 * Menggambar kartu koleksi langsung di Canvas. Dipakai oleh tampilan layar (CardView)
 * dan oleh ekspor gambar untuk dibagikan, jadi hasilnya selalu sama.
 */
public final class CardRenderer {
    private CardRenderer() {}

    /** Data yang dibutuhkan kartu. */
    public static class Data {
        public String code = "";
        public String name = "";
        public String priceText = "";
        public Rarity tier = Rarity.COMMON;
        public int score;
        public Bitmap photo;
        /** Tiga kotak statistik: {label, nilai}. */
        public String[][] stats = new String[3][2];

        public static Data from(Context c, Models.Item it, Models.Collection col) {
            Data d = new Data();
            d.code = it.code.isEmpty() ? "NEW" : it.code;
            d.name = it.name.isEmpty() ? "NAMA ITEM" : it.name;
            d.tier = Rarity.of(it.score);
            d.score = it.score;
            d.priceText = Fmt.rpJt(it.price).toUpperCase(Locale.ROOT) + " · " + it.condition;
            d.photo = ImageStore.load(it.photo, 700);

            String l1 = "STATUS";
            String v1 = Models.S_WISHLIST.equals(it.status) ? "WISHLIST"
                    : Models.S_SOLD.equals(it.status) ? "TERJUAL" : "DIMILIKI";
            if (col != null) {
                for (Models.FieldDef f : col.fields) {
                    String v = it.values.get(f.id);
                    if (v != null && !v.trim().isEmpty()) {
                        l1 = f.name.toUpperCase(Locale.ROOT);
                        v1 = v;
                        break;
                    }
                }
            }
            d.stats[0] = new String[]{l1, v1};
            d.stats[1] = new String[]{"KONDISI", it.condition};
            d.stats[2] = new String[]{"HARGA", Fmt.jt(it.price).toUpperCase(Locale.ROOT)};
            return d;
        }
    }

    /** Rasio tinggi/lebar kartu penuh dan kartu mini. */
    public static final float RATIO_FULL = 1.4f;
    public static final float RATIO_COMPACT = 1.33f;

    private static TextPaint text(Context c, int kind, float size, int color) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(kind == 0 ? Fonts.bebas(c) : kind == 1 ? Fonts.monoBold(c) : Fonts.monoMedium(c));
        p.setTextSize(size);
        p.setColor(color);
        if (kind != 0) p.setLetterSpacing(0.03f);
        return p;
    }

    public static Bitmap toBitmap(Context c, Data d, int widthPx) {
        int h = Math.round(widthPx * RATIO_FULL);
        Bitmap bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888);
        draw(c, new Canvas(bmp), widthPx, h, false, d, true);
        return bmp;
    }

    public static void draw(Context ctx, Canvas cv, float w, float h, boolean compact, Data d) {
        draw(ctx, cv, w, h, compact, d, false);
    }

    /** export = true: untuk gambar yang dibagikan, selalu memakai garis gelap (tidak mengikuti tema). */
    public static void draw(Context ctx, Canvas cv, float w, float h, boolean compact, Data d, boolean export) {
        final int outline = export ? Neo.ON_FILL : Neo.INK;
        float f = compact ? w / 170f : w / 300f;
        float s = (compact ? 4 : 6) * f;       // bayangan keras
        float stroke = (compact ? 2.5f : 3f) * f;
        float radius = (compact ? 18 : 24) * f;
        float fw = w - s, fh = h - s;           // ukuran kartu tanpa bayangan
        float pad = (compact ? 8 : 14) * f;

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setColor(outline);

        // bayangan + badan kartu
        fill.setColor(outline);
        cv.drawRoundRect(new RectF(s, s, w, h), radius, radius, fill);
        fill.setColor(d.tier.color);
        RectF body = new RectF(stroke / 2, stroke / 2, fw - stroke / 2, fh - stroke / 2);
        cv.drawRoundRect(body, radius, radius, fill);
        line.setStrokeWidth(stroke);
        cv.drawRoundRect(body, radius, radius, line);

        // header: kode kiri, tier kanan
        float hs = (compact ? 10 : 11) * f;
        TextPaint mono = text(ctx, 1, hs, Neo.ON_FILL);
        float pillH = hs * 2.1f;
        float cy = pad + pillH / 2;
        cv.drawText(TextUtils.ellipsize(d.code, mono, fw * 0.5f, TextUtils.TruncateAt.END).toString(),
                pad, cy + hs * 0.36f, mono);
        TextPaint pillText = text(ctx, 1, hs, Neo.LIGHT);
        String tierLabel = d.tier.label;
        float pw = pillText.measureText(tierLabel) + hs * 1.6f;
        fill.setColor(Neo.ON_FILL);
        cv.drawRoundRect(new RectF(fw - pad - pw, pad, fw - pad, pad + pillH), pillH, pillH, fill);
        cv.drawText(tierLabel, fw - pad - pw + hs * 0.8f, cy + hs * 0.36f, pillText);

        // blok bawah: nama + (harga | statistik)
        float nameSize = (compact ? 25 : 44) * f;
        TextPaint namePaint = text(ctx, 0, nameSize, Neo.ON_FILL);
        StaticLayout nameLayout = StaticLayout.Builder
                .obtain(d.name.toUpperCase(Locale.ROOT), 0, d.name.length(), namePaint, Math.max(1, Math.round(fw - 2 * pad)))
                .setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END)
                .setLineSpacing(0, 0.95f).setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
        float gap = (compact ? 8 : 12) * f;
        float lowerH = nameLayout.getHeight();
        float statH = 0;
        if (compact) {
            lowerH += 4 * f + (10 * f);
        } else {
            statH = 48 * f;
            lowerH += gap + statH;
        }
        float photoTop = pad + pillH + gap;
        float photoBottom = fh - pad - lowerH - gap;
        RectF photo = new RectF(pad, photoTop, fw - pad, Math.max(photoTop + 20 * f, photoBottom));
        drawPhoto(ctx, cv, photo, (compact ? 12 : 16) * f, (compact ? 2 : 2.5f) * f, f, d.photo);

        // skor (hanya kartu penuh)
        if (!compact) drawScore(ctx, cv, photo, f, d.score);

        // nama
        cv.save();
        cv.translate(pad, photo.bottom + gap);
        nameLayout.draw(cv);
        cv.restore();
        float afterName = photo.bottom + gap + nameLayout.getHeight();

        if (compact) {
            TextPaint price = text(ctx, 1, 10 * f, Neo.ON_FILL);
            String t = TextUtils.ellipsize(d.priceText, price, fw - 2 * pad, TextUtils.TruncateAt.END).toString();
            cv.drawText(t, pad, afterName + 4 * f + 9 * f, price);
        } else {
            float bw = (fw - 2 * pad - 2 * 8 * f) / 3f;
            float top = afterName + gap;
            for (int i = 0; i < 3; i++) {
                float l = pad + i * (bw + 8 * f);
                RectF box = new RectF(l, top, l + bw, top + statH);
                fill.setColor(Neo.WHITE);
                cv.drawRoundRect(box, 12 * f, 12 * f, fill);
                line.setColor(Neo.ON_FILL);
                line.setStrokeWidth(2 * f);
                cv.drawRoundRect(box, 12 * f, 12 * f, line);
                TextPaint lab = text(ctx, 1, 10 * f, Neo.MUTED_ON_FILL);
                cv.drawText(TextUtils.ellipsize(d.stats[i][0], lab, bw - 16 * f, TextUtils.TruncateAt.END).toString(),
                        l + 10 * f, top + 14 * f, lab);
                float vs = 26 * f;
                TextPaint val = text(ctx, 0, vs, Neo.ON_FILL);
                String v = d.stats[i][1].toUpperCase(Locale.ROOT);
                while (val.measureText(v) > bw - 16 * f && vs > 14 * f) {
                    vs -= f;
                    val.setTextSize(vs);
                }
                // isian yang sangat panjang (misalnya nama penulis) dipotong dengan "…", isi lengkapnya ada di Rincian
                v = TextUtils.ellipsize(v, val, bw - 16 * f, TextUtils.TruncateAt.END).toString();
                cv.drawText(v, l + 10 * f, top + statH - 9 * f, val);
            }
        }
    }

    private static void drawPhoto(Context ctx, Canvas cv, RectF r, float radius, float stroke, float f, Bitmap photo) {
        Path clip = new Path();
        clip.addRoundRect(r, radius, radius, Path.Direction.CW);
        cv.save();
        cv.clipPath(clip);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Neo.WHITE);
        cv.drawRect(r, p);
        if (photo != null) {
            float sx = r.width() / photo.getWidth(), sy = r.height() / photo.getHeight();
            float sc = Math.max(sx, sy);
            float dw = photo.getWidth() * sc, dh = photo.getHeight() * sc;
            RectF dst = new RectF(r.centerX() - dw / 2, r.centerY() - dh / 2, r.centerX() + dw / 2, r.centerY() + dh / 2);
            Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            cv.drawBitmap(photo, null, dst, bp);
        } else {
            p.setColor(Neo.HATCH_B);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(6 * f);
            float step = 12 * f * 1.4142f;
            for (float x = r.left - r.height(); x < r.right; x += step) {
                cv.drawLine(x, r.bottom, x + r.height(), r.top, p);
            }
            TextPaint t = text(ctx, 1, 10 * f, Neo.MUTED_ON_FILL);
            String label = "FOTO";
            cv.drawText(label, r.centerX() - t.measureText(label) / 2, r.centerY() + 4 * f, t);
        }
        cv.restore();
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setColor(Neo.ON_FILL);
        line.setStrokeWidth(stroke);
        cv.drawRoundRect(r, radius, radius, line);
    }

    private static void drawScore(Context ctx, Canvas cv, RectF photo, float f, int score) {
        float bw = 96 * f, bh = 52 * f;
        float cx = photo.right + 4 * f - bw / 2 + 10 * f;
        float cy = photo.bottom + 8 * f - bh / 2 + 6 * f;
        cv.save();
        cv.rotate(-4, cx, cy);
        RectF box = new RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Neo.ON_FILL);
        cv.drawRoundRect(new RectF(box.left + 3 * f, box.top + 3 * f, box.right + 3 * f, box.bottom + 3 * f),
                14 * f, 14 * f, p);
        p.setColor(Neo.ORANGE);
        cv.drawRoundRect(box, 14 * f, 14 * f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Neo.ON_FILL);
        p.setStrokeWidth(2.5f * f);
        cv.drawRoundRect(box, 14 * f, 14 * f, p);
        TextPaint lab = text(ctx, 1, 10 * f, Neo.ON_FILL);
        cv.drawText("SKOR", box.left + 12 * f, cy + 4 * f, lab);
        TextPaint num = text(ctx, 0, 44 * f, Neo.ON_FILL);
        String sc = String.valueOf(score);
        cv.drawText(sc, box.right - 12 * f - num.measureText(sc), cy + 16 * f, num);
        cv.restore();
    }
}
