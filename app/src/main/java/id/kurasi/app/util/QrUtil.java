package id.kurasi.app.util;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/** Membuat gambar QR dan membaca isi QR milik Kurasi. */
public final class QrUtil {
    private QrUtil() {}

    private static final String PREFIX = "kurasi:";

    public static String payload(String itemCode) {
        return PREFIX + itemCode;
    }

    /** Kembalikan kode item dari isi QR, atau null jika bukan QR Kurasi. */
    public static String parse(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        return t.startsWith(PREFIX) ? t.substring(PREFIX.length()) : null;
    }

    public static Bitmap bitmap(String content, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0);
            BitMatrix m = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            int w = m.getWidth(), h = m.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) px[y * w + x] = m.get(x, y) ? Neo.BLACK : Neo.WHITE;
            }
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            b.setPixels(px, 0, w, 0, 0, w, h);
            return b;
        } catch (WriterException e) {
            return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        }
    }
}
