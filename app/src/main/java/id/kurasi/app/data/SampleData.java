package id.kurasi.app.data;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.kurasi.app.data.Models.Collection;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.SetDef;
import id.kurasi.app.util.Prefs;

/** Koleksi bawaan dan data contoh untuk demo. */
public final class SampleData {
    private SampleData() {}

    private static final long DAY = 24L * 3600 * 1000;

    /** Buat koleksi bawaan (Sneakers, Buku, Tanaman, Vinyl) saat aplikasi pertama dibuka. */
    public static void ensureDefaults(Context c) {
        if (!Prefs.isFirstRun(c)) return;
        Repo repo = Repo.get(c);
        if (repo.collections().isEmpty()) {
            for (String key : new String[]{"SNEAKERS", "BUKU", "TANAMAN", "VINYL"}) {
                repo.saveCollection(Templates.build(key));
            }
        }
        Prefs.setSeeded(c);
    }

    private static long collectionId(Repo repo, String key) {
        for (Collection col : repo.collections()) if (key.equals(col.template)) return col.id;
        return repo.saveCollection(Templates.build(key));
    }

    private static long fieldId(Repo repo, long collectionId, String name) {
        for (Models.FieldDef f : repo.fieldsOf(collectionId)) if (f.name.equals(name)) return f.id;
        return 0;
    }

    private static Item make(Repo repo, String key, String name, long price, String cond, int daysAgo,
                             String status, String... fieldPairs) {
        Item it = new Item();
        it.collectionId = collectionId(repo, key);
        it.name = name;
        it.price = price;
        it.condition = cond;
        it.status = status;
        it.acquiredAt = System.currentTimeMillis() - daysAgo * DAY;
        for (int i = 0; i + 1 < fieldPairs.length; i += 2) {
            long fid = fieldId(repo, it.collectionId, fieldPairs[i]);
            if (fid != 0) it.values.put(fid, fieldPairs[i + 1]);
        }
        return it;
    }

    /**
     * Isi aplikasi dengan data contoh. Kalau data contoh sudah ada (dikenali dari set "Seri Dune"),
     * tidak menambah lagi supaya binder tidak penuh item ganda. Mengembalikan true kalau data ditambahkan.
     */
    public static boolean load(Context c) {
        Repo repo = Repo.get(c);
        for (SetDef s : repo.sets()) if ("Seri Dune".equals(s.name)) return false;

        SetDef dune = new SetDef();
        dune.name = "Seri Dune";
        dune.targetCount = 6;
        repo.saveSet(dune);
        SetDef aj = new SetDef();
        aj.name = "Air Jordan 1, semua warna";
        aj.targetCount = 12;
        repo.saveSet(aj);

        Item a = make(repo, "SNEAKERS", "Air Jordan 1 Retro High", 2_850_000, "BARU", 12, Models.S_OWNED,
                "Ukuran", "42", "Merek", "Nike", "Warna", "Chicago");
        a.setId = aj.id;
        repo.saveItem(a);
        repo.saveItem(make(repo, "SNEAKERS", "New Balance 550", 1_900_000, "MULUS", 75, Models.S_OWNED,
                "Ukuran", "42", "Merek", "New Balance", "Warna", "Putih hijau"));
        repo.saveItem(make(repo, "SNEAKERS", "Converse Chuck 70", 850_000, "BEKAS", 150, Models.S_OWNED,
                "Ukuran", "41", "Merek", "Converse", "Warna", "Hitam"));

        Item w1 = make(repo, "SNEAKERS", "Asics Gel-Kayano 14", 2_020_000, "BARU", 3, Models.S_WISHLIST,
                "Ukuran", "42", "Merek", "Asics");
        w1.targetPrice = 2_200_000;
        repo.saveItem(w1);

        Item d1 = make(repo, "BUKU", "Dune, cetakan pertama", 1_200_000, "BAGUS", 120, Models.S_OWNED,
                "Penulis", "Frank Herbert", "Penerbit", "Chilton", "Tahun", "1965");
        d1.setId = dune.id;
        repo.saveItem(d1);
        Item d2 = make(repo, "BUKU", "Dune Messiah", 180_000, "BAGUS", 118, Models.S_OWNED,
                "Penulis", "Frank Herbert", "Tahun", "1969");
        d2.setId = dune.id;
        repo.saveItem(d2);
        Item d3 = make(repo, "BUKU", "Children of Dune", 160_000, "BAGUS", 100, Models.S_OWNED,
                "Penulis", "Frank Herbert", "Tahun", "1976");
        d3.setId = dune.id;
        repo.saveItem(d3);
        repo.saveItem(make(repo, "BUKU", "Laskar Pelangi", 90_000, "BAGUS", 200, Models.S_OWNED,
                "Penulis", "Andrea Hirata", "Tahun", "2005"));

        repo.saveItem(make(repo, "TANAMAN", "Monstera Deliciosa", 120_000, "BAGUS", 60, Models.S_OWNED,
                "Lokasi", "Ruang tamu", "Jadwal siram", "3 hari"));
        repo.saveItem(make(repo, "TANAMAN", "Sansevieria", 65_000, "BAGUS", 30, Models.S_OWNED,
                "Lokasi", "Kamar", "Jadwal siram", "Mingguan"));

        repo.saveItem(make(repo, "VINYL", "Kind of Blue, pressing 1959", 3_400_000, "BAGUS", 58, Models.S_OWNED,
                "Artis", "Miles Davis", "Tahun", "1959", "Pressing", "Columbia 6-eye"));
        Item w2 = make(repo, "VINYL", "Blue Note, pressing 1957", 1_100_000, "BAGUS", 2, Models.S_WISHLIST,
                "Artis", "Art Blakey", "Tahun", "1957");
        w2.targetPrice = 900_000;
        repo.saveItem(w2);

        Item cam = make(repo, "KAMERA", "Canon AE-1 Program", 3_100_000, "MULUS", 160, Models.S_OWNED,
                "Merek", "Canon", "Tipe film", "35mm", "Tahun", "1981");
        cam.store = "Toko Kamera Klasik";
        cam.warrantyUntil = System.currentTimeMillis() + 41 * DAY;
        repo.saveItem(cam);
        repo.addValue(cam.id, 3_300_000, System.currentTimeMillis() - 120 * DAY);
        repo.addValue(cam.id, 3_500_000, System.currentTimeMillis() - 80 * DAY);
        repo.addValue(cam.id, 3_700_000, System.currentTimeMillis() - 40 * DAY);
        repo.addValue(cam.id, 3_900_000, System.currentTimeMillis() - 5 * DAY);

        Item w3 = make(repo, "KAMERA", "Nikon FM2", 4_800_000, "MULUS", 1, Models.S_WISHLIST,
                "Merek", "Nikon", "Tahun", "1982");
        w3.targetPrice = 4_000_000;
        repo.saveItem(w3);

        Prefs.setSeeded(c);
        return true;
    }

    /** Hapus semua data (koleksi, item, set). Foto di penyimpanan dibiarkan. */
    public static void clearAll(Context c) {
        Repo repo = Repo.get(c);
        for (Collection col : repo.collections()) repo.deleteCollection(col.id);
        List<SetDef> sets = repo.sets();
        for (SetDef s : sets) repo.deleteSet(s.id);
    }

    /** Dipakai di daftar: peta id koleksi ke nama untuk menampilkan label cepat. */
    public static Map<Long, String> collectionNames(Repo repo) {
        Map<Long, String> m = new HashMap<>();
        for (Collection col : repo.collections()) m.put(col.id, col.name);
        return m;
    }
}
