package id.kurasi.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Skema SQLite Kurasi. Semua relasi memakai ON DELETE CASCADE. */
public class Db extends SQLiteOpenHelper {
    private static final String NAME = "kurasi.db";
    private static final int VERSION = 2;

    public Db(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE collections ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "color INTEGER NOT NULL,"
                + "template TEXT NOT NULL DEFAULT '',"
                + "prefix TEXT NOT NULL DEFAULT 'ITM',"
                + "position INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE field_defs ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "collection_id INTEGER NOT NULL REFERENCES collections(_id) ON DELETE CASCADE,"
                + "name TEXT NOT NULL,"
                + "type TEXT NOT NULL,"
                + "options TEXT NOT NULL DEFAULT '',"
                + "position INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE sets ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "target_count INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE items ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "collection_id INTEGER NOT NULL REFERENCES collections(_id) ON DELETE CASCADE,"
                + "code TEXT NOT NULL,"
                + "name TEXT NOT NULL,"
                + "photo TEXT NOT NULL DEFAULT '',"
                + "price INTEGER NOT NULL DEFAULT 0,"
                + "value INTEGER NOT NULL DEFAULT 0,"
                + "condition TEXT NOT NULL DEFAULT 'BARU',"
                + "acquired_at INTEGER NOT NULL DEFAULT 0,"
                + "status TEXT NOT NULL DEFAULT 'OWNED',"
                + "target_price INTEGER NOT NULL DEFAULT 0,"
                + "note TEXT NOT NULL DEFAULT '',"
                + "score INTEGER NOT NULL DEFAULT 0,"
                + "set_id INTEGER NOT NULL DEFAULT 0,"
                + "store TEXT NOT NULL DEFAULT '',"
                + "receipt_photo TEXT NOT NULL DEFAULT '',"
                + "warranty_until INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL DEFAULT 0,"
                + "sold_price INTEGER NOT NULL DEFAULT 0,"
                + "tier_override INTEGER NOT NULL DEFAULT -1)");
        db.execSQL("CREATE INDEX idx_items_collection ON items(collection_id)");

        db.execSQL("CREATE TABLE item_values ("
                + "item_id INTEGER NOT NULL REFERENCES items(_id) ON DELETE CASCADE,"
                + "field_id INTEGER NOT NULL REFERENCES field_defs(_id) ON DELETE CASCADE,"
                + "value TEXT NOT NULL,"
                + "PRIMARY KEY (item_id, field_id))");

        db.execSQL("CREATE TABLE value_history ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "item_id INTEGER NOT NULL REFERENCES items(_id) ON DELETE CASCADE,"
                + "value INTEGER NOT NULL,"
                + "at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE loans ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "item_id INTEGER NOT NULL REFERENCES items(_id) ON DELETE CASCADE,"
                + "borrower TEXT NOT NULL,"
                + "loaned_at INTEGER NOT NULL,"
                + "due_at INTEGER NOT NULL,"
                + "returned_at INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Versi 2: harga jual untuk item terjual, dan tier kartu yang bisa diatur manual (-1 = otomatis).
        // Data lama tetap utuh, hanya menambah dua kolom dengan nilai bawaan.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE items ADD COLUMN sold_price INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE items ADD COLUMN tier_override INTEGER NOT NULL DEFAULT -1");
        }
    }
}
