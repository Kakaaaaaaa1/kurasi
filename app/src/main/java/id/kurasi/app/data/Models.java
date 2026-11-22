package id.kurasi.app.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Kumpulan model data sederhana (tanpa logika) yang dipakai di seluruh aplikasi. */
public final class Models {
    private Models() {}

    // Tipe field dinamis
    public static final String T_TEXT = "TEXT";
    public static final String T_NUMBER = "NUMBER";
    public static final String T_DATE = "DATE";
    public static final String T_CHOICE = "CHOICE";

    // Status item
    public static final String S_OWNED = "OWNED";
    public static final String S_WISHLIST = "WISHLIST";
    public static final String S_SOLD = "SOLD";

    // Kondisi item (urut dari terbaik)
    public static final String[] CONDITIONS = {"BARU", "MULUS", "BAGUS", "BEKAS"};

    public static class Collection {
        public long id;
        public String name = "";
        public int color;
        public String template = "";
        public String prefix = "";
        public int position;
        public List<FieldDef> fields = new ArrayList<>();
    }

    public static class FieldDef {
        public long id;
        public long collectionId;
        public String name = "";
        public String type = T_TEXT;
        /** Pilihan dipisah koma, hanya dipakai untuk tipe CHOICE. */
        public String options = "";
        public int position;

        public List<String> optionList() {
            List<String> out = new ArrayList<>();
            for (String s : options.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) out.add(t);
            }
            return out;
        }
    }

    public static class Item {
        public long id;
        public long collectionId;
        public String code = "";
        public String name = "";
        public String photo = "";
        public long price;
        public long value;
        public String condition = "BARU";
        public long acquiredAt;
        public String status = S_OWNED;
        public long targetPrice;
        public String note = "";
        public int score;
        public long setId;
        public String store = "";
        public String receiptPhoto = "";
        public long warrantyUntil;
        public long createdAt;
        /** Harga jual, hanya berarti kalau status Terjual. 0 = belum dicatat. */
        public long soldPrice;
        /** Tier yang diatur sendiri (urutan Rarity). -1 = otomatis dari skor. */
        public int tierOverride = -1;
        /** Nilai field dinamis: id field -> isi. */
        public Map<Long, String> values = new HashMap<>();
        // Diisi dari JOIN saat daftar dimuat
        public String collectionName = "";
        public int collectionColor;
        public boolean loaned;
    }

    public static class Loan {
        public long id;
        public long itemId;
        public String borrower = "";
        public long loanedAt;
        public long dueAt;
        public long returnedAt;
        public String itemName = "";
        public String itemCode = "";
    }

    public static class ValueRecord {
        public long id;
        public long itemId;
        public long value;
        public long at;
    }

    public static class SetDef {
        public long id;
        public String name = "";
        public int targetCount;
        public int count;
    }

    public static class Stat {
        public String label = "";
        public int color;
        public long amount;
        public int count;
    }
}
