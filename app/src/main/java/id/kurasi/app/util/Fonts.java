package id.kurasi.app.util;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import id.kurasi.app.R;

/** Cache Typeface untuk dipakai saat menggambar langsung di Canvas. */
public final class Fonts {
    private Fonts() {}

    private static Typeface bebas, monoBold, monoMedium;

    public static Typeface bebas(Context c) {
        if (bebas == null) bebas = load(c, R.font.bebas_neue, Typeface.DEFAULT_BOLD);
        return bebas;
    }

    public static Typeface monoBold(Context c) {
        if (monoBold == null) monoBold = load(c, R.font.mono_bold, Typeface.MONOSPACE);
        return monoBold;
    }

    public static Typeface monoMedium(Context c) {
        if (monoMedium == null) monoMedium = load(c, R.font.mono_medium, Typeface.MONOSPACE);
        return monoMedium;
    }

    private static Typeface load(Context c, int res, Typeface fallback) {
        try {
            Typeface t = ResourcesCompat.getFont(c.getApplicationContext(), res);
            return t != null ? t : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
