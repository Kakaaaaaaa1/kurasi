package id.kurasi.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import id.kurasi.app.data.Models.Collection;
import id.kurasi.app.data.Models.FieldDef;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.Loan;
import id.kurasi.app.data.Models.SetDef;
import id.kurasi.app.data.Models.Stat;
import id.kurasi.app.data.Models.ValueRecord;
import id.kurasi.app.util.Rarity;

/** Semua akses database lewat sini. Satu instance untuk seluruh aplikasi. */
public final class Repo {
    private static Repo instance;
    private final Db helper;

    private Repo(Context c) {
        helper = new Db(c.getApplicationContext());
    }

    public static synchronized Repo get(Context c) {
        if (instance == null) instance = new Repo(c);
        return instance;
    }

    private SQLiteDatabase r() {
        return helper.getReadableDatabase();
    }

    private SQLiteDatabase w() {
        return helper.getWritableDatabase();
    }

    // =========================================================== KOLEKSI

    public List<Collection> collections() {
        List<Collection> out = new ArrayList<>();
        try (Cursor c = r().rawQuery("SELECT * FROM collections ORDER BY position, _id", null)) {
            while (c.moveToNext()) {
                Collection col = readCollection(c);
                col.fields = fieldsOf(col.id);
                out.add(col);
            }
        }
        return out;
    }

    public Collection collection(long id) {
        try (Cursor c = r().rawQuery("SELECT * FROM collections WHERE _id=?", new String[]{"" + id})) {
            if (!c.moveToFirst()) return null;
            Collection col = readCollection(c);
            col.fields = fieldsOf(id);
            return col;
        }
    }

    private Collection readCollection(Cursor c) {
        Collection col = new Collection();
        col.id = c.getLong(c.getColumnIndexOrThrow("_id"));
        col.name = c.getString(c.getColumnIndexOrThrow("name"));
        col.color = c.getInt(c.getColumnIndexOrThrow("color"));
        col.template = c.getString(c.getColumnIndexOrThrow("template"));
        col.prefix = c.getString(c.getColumnIndexOrThrow("prefix"));
        col.position = c.getInt(c.getColumnIndexOrThrow("position"));
        return col;
    }

    public List<FieldDef> fieldsOf(long collectionId) {
        List<FieldDef> out = new ArrayList<>();
        try (Cursor c = r().rawQuery("SELECT * FROM field_defs WHERE collection_id=? ORDER BY position, _id",
                new String[]{"" + collectionId})) {
            while (c.moveToNext()) {
                FieldDef f = new FieldDef();
                f.id = c.getLong(c.getColumnIndexOrThrow("_id"));
                f.collectionId = collectionId;
                f.name = c.getString(c.getColumnIndexOrThrow("name"));
                f.type = c.getString(c.getColumnIndexOrThrow("type"));
                f.options = c.getString(c.getColumnIndexOrThrow("options"));
                f.position = c.getInt(c.getColumnIndexOrThrow("position"));
                out.add(f);
            }
        }
        return out;
    }

    /** Simpan koleksi beserta field-nya. Field yang tidak ada lagi di daftar akan dihapus. */
    public long saveCollection(Collection col) {
        SQLiteDatabase d = w();
        d.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("name", col.name);
            v.put("color", col.color);
            v.put("template", col.template);
            v.put("prefix", col.prefix == null || col.prefix.isEmpty() ? "ITM" : col.prefix);
            if (col.id == 0) {
                v.put("position", countRows("collections"));
                col.id = d.insertOrThrow("collections", null, v);
            } else {
                d.update("collections", v, "_id=?", new String[]{"" + col.id});
            }
            Set<Long> keep = new HashSet<>();
            for (FieldDef f : col.fields) if (f.id != 0) keep.add(f.id);
            for (FieldDef old : fieldsOf(col.id)) {
                if (!keep.contains(old.id)) d.delete("field_defs", "_id=?", new String[]{"" + old.id});
            }
            int pos = 0;
            for (FieldDef f : col.fields) {
                ContentValues fv = new ContentValues();
                fv.put("collection_id", col.id);
                fv.put("name", f.name);
                fv.put("type", f.type);
                fv.put("options", f.options);
                fv.put("position", pos++);
                if (f.id == 0) f.id = d.insertOrThrow("field_defs", null, fv);
                else d.update("field_defs", fv, "_id=?", new String[]{"" + f.id});
            }
            d.setTransactionSuccessful();
        } finally {
            d.endTransaction();
        }
        return col.id;
    }

    public void deleteCollection(long id) {
        w().delete("collections", "_id=?", new String[]{"" + id});
    }

    public int itemCount(long collectionId) {
        try (Cursor c = r().rawQuery("SELECT COUNT(*) FROM items WHERE collection_id=? AND status<>'WISHLIST'",
                new String[]{"" + collectionId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private int countRows(String table) {
        try (Cursor c = r().rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    // =========================================================== ITEM

    private static final String ITEM_SELECT = "SELECT i.*, c.name AS cname, c.color AS ccolor, "
            + "(SELECT COUNT(*) FROM loans l WHERE l.item_id=i._id AND l.returned_at=0) AS loaned "
            + "FROM items i JOIN collections c ON c._id=i.collection_id";

    /**
     * @param collectionId 0 = semua koleksi
     * @param status       null = semua, "NOT_WISHLIST" = pemilik + terjual, atau status tertentu
     * @param query        pencarian nama, kode, catatan, atau detail item; boleh kosong
     */
    public List<Item> items(long collectionId, String status, String query) {
        return items(collectionId, status, query, SORT_NEWEST);
    }

    // Pilihan urutan daftar item
    public static final int SORT_NEWEST = 0, SORT_SCORE = 1, SORT_PRICE = 2, SORT_NAME = 3;

    private static String orderBy(int sort) {
        switch (sort) {
            case SORT_SCORE: return "i.score DESC, i.name COLLATE NOCASE ASC";
            case SORT_PRICE: return "i.value DESC, i.name COLLATE NOCASE ASC";
            case SORT_NAME: return "i.name COLLATE NOCASE ASC";
            default: return "i.created_at DESC, i._id DESC";
        }
    }

    /** Sama seperti items(...) tetapi urutan bisa dipilih (SORT_*). */
    public List<Item> items(long collectionId, String status, String query, int sort) {
        StringBuilder sql = new StringBuilder(ITEM_SELECT).append(" WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (collectionId > 0) {
            sql.append(" AND i.collection_id=?");
            args.add("" + collectionId);
        }
        if ("NOT_WISHLIST".equals(status)) sql.append(" AND i.status<>'WISHLIST'");
        else if (status != null) {
            sql.append(" AND i.status=?");
            args.add(status);
        }
        if (query != null && !query.trim().isEmpty()) {
            // cari di nama, kode, catatan, dan detail buatan pengguna (kecuali tanggal, yang tersimpan sebagai angka)
            sql.append(" AND (i.name LIKE ? OR i.code LIKE ? OR i.note LIKE ? OR EXISTS ("
                    + "SELECT 1 FROM item_values v JOIN field_defs f ON f._id=v.field_id "
                    + "WHERE v.item_id=i._id AND f.type<>'DATE' AND v.value LIKE ?))");
            String q = "%" + query.trim() + "%";
            args.add(q);
            args.add(q);
            args.add(q);
            args.add(q);
        }
        sql.append(" ORDER BY ").append(orderBy(sort));
        List<Item> out = new ArrayList<>();
        try (Cursor c = r().rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (c.moveToNext()) out.add(readItem(c));
        }
        return out;
    }

    public Item item(long id) {
        try (Cursor c = r().rawQuery(ITEM_SELECT + " WHERE i._id=?", new String[]{"" + id})) {
            if (!c.moveToFirst()) return null;
            Item it = readItem(c);
            try (Cursor v = r().rawQuery("SELECT field_id, value FROM item_values WHERE item_id=?",
                    new String[]{"" + id})) {
                while (v.moveToNext()) it.values.put(v.getLong(0), v.getString(1));
            }
            return it;
        }
    }

    public Item itemByCode(String code) {
        try (Cursor c = r().rawQuery("SELECT _id FROM items WHERE code=?", new String[]{code})) {
            return c.moveToFirst() ? item(c.getLong(0)) : null;
        }
    }

    private Item readItem(Cursor c) {
        Item it = new Item();
        it.id = c.getLong(c.getColumnIndexOrThrow("_id"));
        it.collectionId = c.getLong(c.getColumnIndexOrThrow("collection_id"));
        it.code = c.getString(c.getColumnIndexOrThrow("code"));
        it.name = c.getString(c.getColumnIndexOrThrow("name"));
        it.photo = c.getString(c.getColumnIndexOrThrow("photo"));
        it.price = c.getLong(c.getColumnIndexOrThrow("price"));
        it.value = c.getLong(c.getColumnIndexOrThrow("value"));
        it.condition = c.getString(c.getColumnIndexOrThrow("condition"));
        it.acquiredAt = c.getLong(c.getColumnIndexOrThrow("acquired_at"));
        it.status = c.getString(c.getColumnIndexOrThrow("status"));
        it.targetPrice = c.getLong(c.getColumnIndexOrThrow("target_price"));
        it.note = c.getString(c.getColumnIndexOrThrow("note"));
        it.score = c.getInt(c.getColumnIndexOrThrow("score"));
        it.setId = c.getLong(c.getColumnIndexOrThrow("set_id"));
        it.store = c.getString(c.getColumnIndexOrThrow("store"));
        it.receiptPhoto = c.getString(c.getColumnIndexOrThrow("receipt_photo"));
        it.warrantyUntil = c.getLong(c.getColumnIndexOrThrow("warranty_until"));
        it.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        it.soldPrice = c.getLong(c.getColumnIndexOrThrow("sold_price"));
        it.tierOverride = c.getInt(c.getColumnIndexOrThrow("tier_override"));
        it.collectionName = c.getString(c.getColumnIndexOrThrow("cname"));
        it.collectionColor = c.getInt(c.getColumnIndexOrThrow("ccolor"));
        it.loaned = c.getInt(c.getColumnIndexOrThrow("loaned")) > 0;
        return it;
    }

    /** Simpan item baru atau perbarui. Skor dan kode dihitung otomatis. */
    public long saveItem(Item it) {
        SQLiteDatabase d = w();
        d.beginTransaction();
        try {
            if (Models.S_WISHLIST.equals(it.status) || it.value <= 0) it.value = it.price;
            it.score = Rarity.withOverride(Rarity.score(it.value, it.condition, it.acquiredAt), it.tierOverride);
            // harga jual hanya disimpan untuk item yang berstatus Terjual
            if (!Models.S_SOLD.equals(it.status)) it.soldPrice = 0;

            ContentValues v = new ContentValues();
            v.put("collection_id", it.collectionId);
            v.put("name", it.name);
            v.put("photo", it.photo);
            v.put("price", it.price);
            v.put("value", it.value);
            v.put("condition", it.condition);
            v.put("acquired_at", it.acquiredAt);
            v.put("status", it.status);
            v.put("target_price", it.targetPrice);
            v.put("note", it.note);
            v.put("score", it.score);
            v.put("set_id", it.setId);
            v.put("store", it.store);
            v.put("receipt_photo", it.receiptPhoto);
            v.put("warranty_until", it.warrantyUntil);
            v.put("sold_price", it.soldPrice);
            v.put("tier_override", it.tierOverride);

            if (it.id == 0) {
                it.code = nextCode(it.collectionId);
                it.createdAt = System.currentTimeMillis();
                v.put("code", it.code);
                v.put("created_at", it.createdAt);
                it.id = d.insertOrThrow("items", null, v);
                if (!Models.S_WISHLIST.equals(it.status) && it.value > 0) {
                    insertValue(d, it.id, it.value, it.acquiredAt > 0 ? it.acquiredAt : it.createdAt);
                }
            } else {
                d.update("items", v, "_id=?", new String[]{"" + it.id});
                // item yang baru pindah dari wishlist belum punya catatan nilai pertama
                if (!Models.S_WISHLIST.equals(it.status) && it.value > 0 && countValues(d, it.id) == 0) {
                    insertValue(d, it.id, it.value, it.acquiredAt > 0 ? it.acquiredAt : System.currentTimeMillis());
                }
            }

            d.delete("item_values", "item_id=?", new String[]{"" + it.id});
            for (Map.Entry<Long, String> e : it.values.entrySet()) {
                if (e.getValue() == null || e.getValue().trim().isEmpty()) continue;
                ContentValues cv = new ContentValues();
                cv.put("item_id", it.id);
                cv.put("field_id", e.getKey());
                cv.put("value", e.getValue().trim());
                d.insert("item_values", null, cv);
            }
            d.setTransactionSuccessful();
        } finally {
            d.endTransaction();
        }
        return it.id;
    }

    private int countValues(SQLiteDatabase d, long itemId) {
        try (Cursor c = d.rawQuery("SELECT COUNT(*) FROM value_history WHERE item_id=?", new String[]{"" + itemId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public void deleteItem(long id) {
        w().delete("items", "_id=?", new String[]{"" + id});
    }

    private String nextCode(long collectionId) {
        Collection col = collection(collectionId);
        String prefix = col == null ? "ITM" : col.prefix;
        int max = 0;
        try (Cursor c = r().rawQuery("SELECT code FROM items WHERE collection_id=?", new String[]{"" + collectionId})) {
            while (c.moveToNext()) {
                String code = c.getString(0);
                int dash = code.lastIndexOf('-');
                if (dash >= 0) {
                    try {
                        max = Math.max(max, Integer.parseInt(code.substring(dash + 1)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return String.format(java.util.Locale.ROOT, "%s-%03d", prefix, max + 1);
    }

    // =========================================================== PEMINJAMAN

    public Loan activeLoan(long itemId) {
        try (Cursor c = r().rawQuery("SELECT * FROM loans WHERE item_id=? AND returned_at=0 ORDER BY _id DESC LIMIT 1",
                new String[]{"" + itemId})) {
            return c.moveToFirst() ? readLoan(c) : null;
        }
    }

    public long startLoan(long itemId, String borrower, long dueAt) {
        ContentValues v = new ContentValues();
        v.put("item_id", itemId);
        v.put("borrower", borrower);
        v.put("loaned_at", System.currentTimeMillis());
        v.put("due_at", dueAt);
        v.put("returned_at", 0);
        return w().insertOrThrow("loans", null, v);
    }

    public void returnLoan(long loanId) {
        ContentValues v = new ContentValues();
        v.put("returned_at", System.currentTimeMillis());
        w().update("loans", v, "_id=?", new String[]{"" + loanId});
    }

    /** Pinjaman yang belum kembali dan jatuh tempo sebelum waktu tertentu. */
    public List<Loan> loansDueBefore(long ms) {
        List<Loan> out = new ArrayList<>();
        try (Cursor c = r().rawQuery("SELECT l.*, i.name AS iname, i.code AS icode FROM loans l "
                + "JOIN items i ON i._id=l.item_id WHERE l.returned_at=0 AND l.due_at<=? ORDER BY l.due_at",
                new String[]{"" + ms})) {
            while (c.moveToNext()) {
                Loan l = readLoan(c);
                l.itemName = c.getString(c.getColumnIndexOrThrow("iname"));
                l.itemCode = c.getString(c.getColumnIndexOrThrow("icode"));
                out.add(l);
            }
        }
        return out;
    }

    public List<Loan> loanHistory(long itemId) {
        List<Loan> out = new ArrayList<>();
        try (Cursor c = r().rawQuery("SELECT * FROM loans WHERE item_id=? ORDER BY _id DESC", new String[]{"" + itemId})) {
            while (c.moveToNext()) out.add(readLoan(c));
        }
        return out;
    }

    private Loan readLoan(Cursor c) {
        Loan l = new Loan();
        l.id = c.getLong(c.getColumnIndexOrThrow("_id"));
        l.itemId = c.getLong(c.getColumnIndexOrThrow("item_id"));
        l.borrower = c.getString(c.getColumnIndexOrThrow("borrower"));
        l.loanedAt = c.getLong(c.getColumnIndexOrThrow("loaned_at"));
        l.dueAt = c.getLong(c.getColumnIndexOrThrow("due_at"));
        l.returnedAt = c.getLong(c.getColumnIndexOrThrow("returned_at"));
        return l;
    }

    // =========================================================== RIWAYAT NILAI

    private void insertValue(SQLiteDatabase d, long itemId, long value, long at) {
        ContentValues v = new ContentValues();
        v.put("item_id", itemId);
        v.put("value", value);
        v.put("at", at);
        d.insert("value_history", null, v);
    }

    /** Catat nilai terbaru. Nilai item dan skornya ikut diperbarui. */
    public void addValue(long itemId, long value, long at) {
        SQLiteDatabase d = w();
        d.beginTransaction();
        try {
            insertValue(d, itemId, value, at);
            Item it = item(itemId);
            if (it != null) {
                ContentValues v = new ContentValues();
                v.put("value", value);
                v.put("score", Rarity.withOverride(Rarity.score(value, it.condition, it.acquiredAt), it.tierOverride));
                d.update("items", v, "_id=?", new String[]{"" + itemId});
            }
            d.setTransactionSuccessful();
        } finally {
            d.endTransaction();
        }
    }

    public List<ValueRecord> values(long itemId) {
        List<ValueRecord> out = new ArrayList<>();
        try (Cursor c = r().rawQuery("SELECT * FROM value_history WHERE item_id=? ORDER BY at, _id",
                new String[]{"" + itemId})) {
            while (c.moveToNext()) {
                ValueRecord v = new ValueRecord();
                v.id = c.getLong(c.getColumnIndexOrThrow("_id"));
                v.itemId = itemId;
                v.value = c.getLong(c.getColumnIndexOrThrow("value"));
                v.at = c.getLong(c.getColumnIndexOrThrow("at"));
                out.add(v);
            }
        }
        return out;
    }

    // =========================================================== SET

    public List<SetDef> sets() {
        List<SetDef> out = new ArrayList<>();
        try (Cursor c = r().rawQuery("SELECT s.*, (SELECT COUNT(*) FROM items i WHERE i.set_id=s._id) AS cnt "
                + "FROM sets s ORDER BY s._id", null)) {
            while (c.moveToNext()) {
                SetDef s = new SetDef();
                s.id = c.getLong(c.getColumnIndexOrThrow("_id"));
                s.name = c.getString(c.getColumnIndexOrThrow("name"));
                s.targetCount = c.getInt(c.getColumnIndexOrThrow("target_count"));
                s.count = c.getInt(c.getColumnIndexOrThrow("cnt"));
                out.add(s);
            }
        }
        return out;
    }

    public long saveSet(SetDef s) {
        ContentValues v = new ContentValues();
        v.put("name", s.name);
        v.put("target_count", s.targetCount);
        if (s.id == 0) s.id = w().insertOrThrow("sets", null, v);
        else w().update("sets", v, "_id=?", new String[]{"" + s.id});
        return s.id;
    }

    public void deleteSet(long id) {
        SQLiteDatabase d = w();
        ContentValues v = new ContentValues();
        v.put("set_id", 0);
        d.update("items", v, "set_id=?", new String[]{"" + id});
        d.delete("sets", "_id=?", new String[]{"" + id});
    }

    // =========================================================== STATISTIK

    public long totalSpend() {
        return sum("SELECT COALESCE(SUM(price),0) FROM items WHERE status<>'WISHLIST'");
    }

    /** Nilai koleksi saat ini. Barang yang sudah terjual tidak dihitung lagi. */
    public long totalValue() {
        return sum("SELECT COALESCE(SUM(value),0) FROM items WHERE status='OWNED'");
    }

    /** Jumlah item berstatus Terjual. */
    public int soldCount() {
        return (int) sum("SELECT COUNT(*) FROM items WHERE status='SOLD'");
    }

    /** Jumlah item terjual yang harga jualnya sudah dicatat. */
    public int soldPricedCount() {
        return (int) sum("SELECT COUNT(*) FROM items WHERE status='SOLD' AND sold_price>0");
    }

    /** Untung (positif) atau rugi (negatif) dari item terjual yang harga jualnya sudah dicatat. */
    public long soldProfit() {
        return sum("SELECT COALESCE(SUM(sold_price - price),0) FROM items WHERE status='SOLD' AND sold_price>0");
    }

    /** Modal barang yang masih dimiliki, pembanding yang adil untuk totalValue(). */
    public long ownedSpend() {
        return sum("SELECT COALESCE(SUM(price),0) FROM items WHERE status='OWNED'");
    }

    public int ownedCount() {
        return (int) sum("SELECT COUNT(*) FROM items WHERE status<>'WISHLIST'");
    }

    public int wishlistCount() {
        return (int) sum("SELECT COUNT(*) FROM items WHERE status='WISHLIST'");
    }

    public long wishlistTarget() {
        return sum("SELECT COALESCE(SUM(CASE WHEN target_price>0 THEN target_price ELSE price END),0) "
                + "FROM items WHERE status='WISHLIST'");
    }

    private long sum(String sql) {
        try (Cursor c = r().rawQuery(sql, null)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

    public List<Stat> spendByCollection() {
        List<Stat> out = new ArrayList<>();
        try (Cursor c = r().rawQuery("SELECT c.name, c.color, COALESCE(SUM(i.price),0) AS total, COUNT(i._id) AS cnt "
                + "FROM collections c LEFT JOIN items i ON i.collection_id=c._id AND i.status<>'WISHLIST' "
                + "GROUP BY c._id ORDER BY total DESC", null)) {
            while (c.moveToNext()) {
                Stat s = new Stat();
                s.label = c.getString(0);
                s.color = c.getInt(1);
                s.amount = c.getLong(2);
                s.count = c.getInt(3);
                out.add(s);
            }
        }
        return out;
    }

    /** Pengeluaran per bulan untuk [months] bulan terakhir, indeks terakhir = bulan ini. */
    public long[] monthlySpend(int months) {
        long[] out = new long[months];
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        start.add(Calendar.MONTH, -(months - 1));
        long from = start.getTimeInMillis();
        try (Cursor c = r().rawQuery("SELECT acquired_at, price FROM items WHERE status<>'WISHLIST' AND acquired_at>=?",
                new String[]{"" + from})) {
            Calendar cal = Calendar.getInstance();
            while (c.moveToNext()) {
                cal.setTimeInMillis(c.getLong(0));
                int idx = (cal.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12
                        + cal.get(Calendar.MONTH) - start.get(Calendar.MONTH);
                if (idx >= 0 && idx < months) out[idx] += c.getLong(1);
            }
        }
        return out;
    }

    public long spendThisMonth() {
        long[] m = monthlySpend(1);
        return m[0];
    }

    /** Jumlah item per tier: indeks sesuai urutan enum Rarity. */
    public int[] tierCounts() {
        int[] out = new int[Rarity.values().length];
        try (Cursor c = r().rawQuery("SELECT score FROM items WHERE status<>'WISHLIST'", null)) {
            while (c.moveToNext()) out[Rarity.of(c.getInt(0)).ordinal()]++;
        }
        return out;
    }

    /** Total XP: 10 per item, 5 per foto, 2 per catatan nilai, 3 per peminjaman. */
    public int xp() {
        long items = sum("SELECT COUNT(*) FROM items WHERE status<>'WISHLIST'");
        long photos = sum("SELECT COUNT(*) FROM items WHERE status<>'WISHLIST' AND photo<>''");
        long values = sum("SELECT COUNT(*) FROM value_history");
        long loans = sum("SELECT COUNT(*) FROM loans");
        return (int) (items * 10 + photos * 5 + values * 2 + loans * 3);
    }

    public boolean isEmpty() {
        return countRows("items") == 0;
    }

    // =========================================================== CADANGAN

    private static final String[] TABLES = {"collections", "field_defs", "sets", "items",
            "item_values", "value_history", "loans"};

    /** Path semua foto yang dipakai item (foto item dan foto nota), tanpa duplikat. */
    public java.util.Set<String> photoPaths() {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        try (Cursor c = r().rawQuery("SELECT photo, receipt_photo FROM items", null)) {
            while (c.moveToNext()) {
                for (int i = 0; i < 2; i++) {
                    String p = c.getString(i);
                    if (p != null && !p.isEmpty()) out.add(p);
                }
            }
        }
        return out;
    }

    /** Seluruh isi database sebagai JSON. Foto hanya berupa path, berkasnya dibawa oleh util.Backup. */
    public org.json.JSONObject dump() throws org.json.JSONException {
        org.json.JSONObject root = new org.json.JSONObject();
        root.put("app", "kurasi");
        root.put("version", 1);
        for (String t : TABLES) {
            org.json.JSONArray arr = new org.json.JSONArray();
            try (Cursor c = r().rawQuery("SELECT * FROM " + t, null)) {
                String[] names = c.getColumnNames();
                while (c.moveToNext()) {
                    org.json.JSONObject o = new org.json.JSONObject();
                    for (int i = 0; i < names.length; i++) {
                        switch (c.getType(i)) {
                            case Cursor.FIELD_TYPE_INTEGER: o.put(names[i], c.getLong(i)); break;
                            case Cursor.FIELD_TYPE_FLOAT: o.put(names[i], c.getDouble(i)); break;
                            case Cursor.FIELD_TYPE_NULL: break;
                            default: o.put(names[i], c.getString(i));
                        }
                    }
                    arr.put(o);
                }
            }
            root.put(t, arr);
        }
        return root;
    }

    /** Ganti seluruh isi database dengan isi cadangan. Semua data lama dihapus. */
    public void restore(org.json.JSONObject root) throws org.json.JSONException {
        restore(root, null);
    }

    /** Path foto di cadangan menunjuk HP lama. Cocokkan lewat nama berkas ke foto yang sudah dipulihkan. */
    private static String remapPhoto(String path, java.util.Map<String, String> photoMap) {
        if (path == null || path.isEmpty()) return "";
        if (photoMap != null) {
            String mapped = photoMap.get(path.substring(path.lastIndexOf('/') + 1));
            if (mapped != null) return mapped;
        }
        return new java.io.File(path).exists() ? path : "";
    }

    /**
     * Seperti restore(root), tetapi foto dicocokkan dengan [photoMap] (nama berkas lama ke path baru).
     * Foto yang berkasnya tidak ditemukan dikosongkan supaya tidak menunjuk berkas yang hilang.
     */
    public void restore(org.json.JSONObject root, java.util.Map<String, String> photoMap)
            throws org.json.JSONException {
        if (!"kurasi".equals(root.optString("app"))) throw new org.json.JSONException("Bukan file cadangan Kurasi");
        SQLiteDatabase d = w();
        d.beginTransaction();
        try {
            for (int i = TABLES.length - 1; i >= 0; i--) d.delete(TABLES[i], null, null);
            for (String t : TABLES) {
                org.json.JSONArray arr = root.optJSONArray(t);
                if (arr == null) continue;
                for (int k = 0; k < arr.length(); k++) {
                    org.json.JSONObject o = arr.getJSONObject(k);
                    ContentValues v = new ContentValues();
                    java.util.Iterator<String> keys = o.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object val = o.get(key);
                        if (val instanceof Long || val instanceof Integer) v.put(key, ((Number) val).longValue());
                        else if (val instanceof Double) v.put(key, (Double) val);
                        else v.put(key, String.valueOf(val));
                    }
                    if ("items".equals(t)) {
                        v.put("photo", remapPhoto(v.getAsString("photo"), photoMap));
                        v.put("receipt_photo", remapPhoto(v.getAsString("receipt_photo"), photoMap));
                    }
                    d.insertOrThrow(t, null, v);
                }
            }
            d.setTransactionSuccessful();
        } finally {
            d.endTransaction();
        }
    }
}
