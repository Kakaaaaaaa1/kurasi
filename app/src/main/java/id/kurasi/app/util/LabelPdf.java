package id.kurasi.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextPaint;
import android.text.TextUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import id.kurasi.app.data.Models.Item;

/** Membuat PDF A4 berisi label QR (kolom x baris) dan adapter untuk mencetaknya. */
public final class LabelPdf {
    private LabelPdf() {}

    public static final int[] SIZES_MM = {40, 50, 70}; // lebar label: kecil, sedang, besar

    // A4 dalam poin (1/72 inci)
    private static final int PAGE_W = 595, PAGE_H = 842;
    private static final float MM = 72f / 25.4f;

    /** Jumlah label yang muat dalam satu halaman A4 untuk lebar label tertentu. */
    public static int perPage(int widthMm) {
        float lw = widthMm * MM, lh = lw * 0.5f;
        float margin = 20 * MM, gap = 3 * MM;
        int cols = Math.max(1, (int) ((PAGE_W - 2 * margin + gap) / (lw + gap)));
        int rows = Math.max(1, (int) ((PAGE_H - 2 * margin + gap) / (lh + gap)));
        return cols * rows;
    }

    /** Satu label: lebar [mm] dan tinggi setengahnya. Daftar item diulang sampai [count] label. */
    public static PdfDocument build(Context c, List<Item> items, int widthMm, boolean showName,
                                    boolean showPrice, int count) {
        PdfDocument doc = new PdfDocument();
        float lw = widthMm * MM, lh = lw * 0.5f;
        float margin = 20 * MM;
        float gap = 3 * MM;
        int cols = Math.max(1, (int) ((PAGE_W - 2 * margin + gap) / (lw + gap)));
        int rows = Math.max(1, (int) ((PAGE_H - 2 * margin + gap) / (lh + gap)));
        int perPage = cols * rows;
        int pages = (int) Math.ceil(count / (float) perPage);

        int idx = 0;
        for (int p = 0; p < pages; p++) {
            PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create());
            Canvas cv = page.getCanvas();
            for (int i = 0; i < perPage && idx < count; i++, idx++) {
                int col = i % cols, row = i / cols;
                float x = margin + col * (lw + gap), y = margin + row * (lh + gap);
                drawLabel(c, cv, items.get(idx % items.size()), x, y, lw, lh, showName, showPrice);
            }
            doc.finishPage(page);
        }
        return doc;
    }

    private static void drawLabel(Context c, Canvas cv, Item it, float x, float y, float w, float h,
                                  boolean showName, boolean showPrice) {
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setColor(Neo.ON_FILL);
        line.setStrokeWidth(1.2f);
        cv.drawRoundRect(new RectF(x, y, x + w, y + h), 6, 6, line);

        float pad = h * 0.1f;
        float qr = h - 2 * pad;
        Bitmap bmp = QrUtil.bitmap(QrUtil.payload(it.code), 300);
        cv.drawBitmap(bmp, null, new RectF(x + pad, y + pad, x + pad + qr, y + pad + qr), null);

        float tx = x + pad + qr + pad;
        float tw = x + w - pad - tx;
        TextPaint code = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        code.setTypeface(Fonts.bebas(c));
        code.setColor(Neo.ON_FILL);
        code.setTextSize(h * 0.3f);
        cv.drawText(it.code, tx, y + pad + h * 0.26f, code);
        float yy = y + pad + h * 0.26f;
        TextPaint small = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        small.setTypeface(Fonts.monoBold(c));
        small.setColor(Neo.ON_FILL);
        small.setTextSize(Math.max(5.5f, h * 0.1f));
        if (showName) {
            yy += h * 0.2f;
            cv.drawText(TextUtils.ellipsize(it.name, small, tw, TextUtils.TruncateAt.END).toString(), tx, yy, small);
        }
        if (showPrice) {
            yy += h * 0.16f;
            cv.drawText(Fmt.rpJt(it.price), tx, yy, small);
        }
        small.setColor(Neo.MUTED_ON_FILL);
        cv.drawText("KURASI", tx, y + h - pad, small);
    }

    public static void write(PdfDocument doc, OutputStream out) throws IOException {
        doc.writeTo(out);
    }

    /** Adapter untuk PrintManager. */
    public static PrintDocumentAdapter printAdapter(PdfDocument doc, String name) {
        return new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldA, PrintAttributes newA, CancellationSignal cancel,
                                 LayoutResultCallback cb, Bundle extras) {
                if (cancel.isCanceled()) {
                    cb.onLayoutCancelled();
                    return;
                }
                cb.onLayoutFinished(new PrintDocumentInfo.Builder(name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(doc.getPages().size()).build(), true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor dest, CancellationSignal cancel,
                                WriteResultCallback cb) {
                try (FileOutputStream out = new FileOutputStream(dest.getFileDescriptor())) {
                    doc.writeTo(out);
                    cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (IOException e) {
                    cb.onWriteFailed(e.getMessage());
                }
            }
        };
    }
}
