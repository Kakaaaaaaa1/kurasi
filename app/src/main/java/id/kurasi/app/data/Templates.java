package id.kurasi.app.data;

import java.util.ArrayList;
import java.util.List;

import id.kurasi.app.util.Neo;

/** Template koleksi siap pakai: nama, warna, awalan kode, dan field bawaan. */
public final class Templates {
    private Templates() {}

    public static class Template {
        public final String key;
        public final String name;
        public final int color;
        public final String prefix;
        public final String[][] fields; // {nama, tipe, opsi}

        Template(String key, String name, int color, String prefix, String[][] fields) {
            this.key = key;
            this.name = name;
            this.color = color;
            this.prefix = prefix;
            this.fields = fields;
        }
    }

    public static final Template[] ALL = {
            new Template("SNEAKERS", "Sneakers", Neo.ORANGE, "SNK", new String[][]{
                    {"Ukuran", Models.T_CHOICE, "38,39,40,41,42,43,44,45"},
                    {"Merek", Models.T_TEXT, ""},
                    {"Warna", Models.T_TEXT, ""}}),
            new Template("BUKU", "Buku", Neo.CYAN, "BUK", new String[][]{
                    {"Penulis", Models.T_TEXT, ""},
                    {"Penerbit", Models.T_TEXT, ""},
                    {"Tahun", Models.T_NUMBER, ""}}),
            new Template("TANAMAN", "Tanaman", Neo.MINT, "TNM", new String[][]{
                    {"Lokasi", Models.T_TEXT, ""},
                    {"Jadwal siram", Models.T_CHOICE, "Harian,3 hari,Mingguan"}}),
            new Template("VINYL", "Vinyl", Neo.LAVENDER, "VNL", new String[][]{
                    {"Artis", Models.T_TEXT, ""},
                    {"Tahun", Models.T_NUMBER, ""},
                    {"Pressing", Models.T_TEXT, ""}}),
            new Template("KAMERA", "Kamera Analog", Neo.YELLOW, "KAM", new String[][]{
                    {"Merek", Models.T_TEXT, ""},
                    {"Tipe film", Models.T_CHOICE, "35mm,120,Digital"},
                    {"Tahun", Models.T_NUMBER, ""}})
    };

    public static Template byKey(String key) {
        for (Template t : ALL) if (t.key.equals(key)) return t;
        return null;
    }

    /** Buat koleksi baru (belum disimpan) dari template; key kosong = koleksi kosong. */
    public static Models.Collection build(String key) {
        Models.Collection c = new Models.Collection();
        Template t = byKey(key);
        if (t == null) {
            c.color = Neo.LIME;
            c.prefix = "ITM";
            return c;
        }
        c.name = t.name;
        c.color = t.color;
        c.template = t.key;
        c.prefix = t.prefix;
        List<Models.FieldDef> fs = new ArrayList<>();
        for (int i = 0; i < t.fields.length; i++) {
            Models.FieldDef f = new Models.FieldDef();
            f.name = t.fields[i][0];
            f.type = t.fields[i][1];
            f.options = t.fields[i][2];
            f.position = i;
            fs.add(f);
        }
        c.fields = fs;
        return c;
    }

    /** Awalan kode untuk koleksi kustom: 3 huruf pertama nama, kapital. */
    public static String prefixFor(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length() && b.length() < 3; i++) {
            char ch = Character.toUpperCase(name.charAt(i));
            if (ch >= 'A' && ch <= 'Z') b.append(ch);
        }
        while (b.length() < 3) b.append('X');
        return b.toString();
    }
}
