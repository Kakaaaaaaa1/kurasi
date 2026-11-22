package id.kurasi.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Format angka dan tanggal untuk tampilan. */
public final class Fmt {
    private Fmt() {}

    private static final Locale ID = new Locale("id", "ID");

    /** 2200000 -> "Rp 2.200.000" */
    public static String rp(long v) {
        return "Rp " + String.format(ID, "%,d", v);
    }

    /** 2850000 -> "2,85 jt", 120000 -> "120 rb" */
    public static String jt(long v) {
        if (v >= 1_000_000) {
            String s = String.format(ID, "%.2f", v / 1_000_000.0);
            if (s.endsWith(",00")) s = s.substring(0, s.length() - 3);
            else if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            return s + " jt";
        }
        return (v / 1000) + " rb";
    }

    /** Hanya angkanya dalam juta: 48200000 -> "48,2" */
    public static String juta(long v) {
        String s = String.format(ID, "%.1f", v / 1_000_000.0);
        return s.endsWith(",0") ? s.substring(0, s.length() - 2) : s;
    }

    public static String rpJt(long v) {
        return "Rp " + jt(v);
    }

    public static String monthYear(long ms) {
        return new SimpleDateFormat("MMM yyyy", ID).format(new Date(ms));
    }

    public static String date(long ms) {
        return new SimpleDateFormat("d MMM yyyy", ID).format(new Date(ms));
    }

    public static String monthShort(long ms) {
        return new SimpleDateFormat("MMM", ID).format(new Date(ms)).toUpperCase(ID);
    }

    /** "Rp 2.200.000" / "2200000" -> 2200000. Karakter non-digit diabaikan. */
    public static long parseMoney(CharSequence s) {
        if (s == null) return 0;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
        }
        if (b.length() == 0 || b.length() > 15) return 0;
        return Long.parseLong(b.toString());
    }

    public static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(ID) + s.substring(1).toLowerCase(ID);
    }
}
