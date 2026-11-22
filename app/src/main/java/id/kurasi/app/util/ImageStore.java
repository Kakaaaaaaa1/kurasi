package id.kurasi.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.LruCache;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/** Menyimpan foto item di penyimpanan aplikasi (diperkecil) dan memuatnya dengan cache. */
public final class ImageStore {
    private ImageStore() {}

    private static final int MAX_SIDE = 1280;

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    public static File photosDir(Context c) {
        File d = new File(c.getFilesDir(), "photos");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    public static File newPhotoFile(Context c) {
        return new File(photosDir(c), UUID.randomUUID() + ".jpg");
    }

    /** Salin gambar dari Uri (galeri) ke penyimpanan aplikasi. Mengembalikan path file. */
    public static String importUri(Context c, Uri uri) throws IOException {
        File raw = new File(c.getCacheDir(), "import_" + UUID.randomUUID());
        try (InputStream in = c.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(raw)) {
            if (in == null) throw new IOException("Tidak bisa membuka gambar");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        File dst = newPhotoFile(c);
        try {
            normalize(raw, dst);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            raw.delete();
        }
        return dst.getAbsolutePath();
    }

    /** Perkecil dan putar sesuai EXIF, hasilnya ditimpa ke file yang sama (untuk foto kamera). */
    public static void normalizeInPlace(File f) throws IOException {
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        normalize(f, tmp);
        if (!f.delete() || !tmp.renameTo(f)) throw new IOException("Gagal menyimpan foto");
    }

    private static void normalize(File src, File dst) throws IOException {
        Bitmap bmp = decode(src.getAbsolutePath(), MAX_SIDE);
        if (bmp == null) throw new IOException("Gambar tidak valid");
        int rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(src.getAbsolutePath());
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: rotation = 90; break;
                case ExifInterface.ORIENTATION_ROTATE_180: rotation = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: rotation = 270; break;
                default: break;
            }
        } catch (IOException ignored) {
        }
        if (rotation != 0) {
            Matrix m = new Matrix();
            m.postRotate(rotation);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        }
        try (OutputStream out = new FileOutputStream(dst)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
        }
    }

    /** Muat gambar dengan sisi terpanjang sekitar [maxSide] piksel. Hasil di-cache. Bisa null. */
    public static Bitmap load(String path, int maxSide) {
        if (path == null || path.isEmpty()) return null;
        String key = path + "@" + maxSide;
        Bitmap hit = CACHE.get(key);
        if (hit != null) return hit;
        Bitmap b = decode(path, maxSide);
        if (b != null) CACHE.put(key, b);
        return b;
    }

    /** Hapus berkas foto yang tidak dipakai lagi oleh item mana pun (misalnya sisa data sebelum dipulihkan). */
    public static void cleanup(Context c, java.util.Collection<String> keepPaths) {
        java.util.Set<String> keep = new java.util.HashSet<>();
        for (String p : keepPaths) keep.add(new File(p).getName());
        File[] files = photosDir(c).listFiles();
        if (files != null) {
            for (File f : files) {
                //noinspection ResultOfMethodCallIgnored
                if (!keep.contains(f.getName())) f.delete();
            }
        }
        CACHE.evictAll();
    }

    public static void evict(String path) {
        CACHE.evictAll();
    }

    private static Bitmap decode(String path, int maxSide) {
        if (!new File(path).exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int longest = Math.max(o.outWidth, o.outHeight);
        int sample = 1;
        while (longest / (sample * 2) >= maxSide) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, o);
    }
}
