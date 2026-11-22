package id.kurasi.app.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.kurasi.app.data.Models.Collection;
import id.kurasi.app.data.Models.FieldDef;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Repo;

/** Ekspor seluruh item ke format CSV (bisa dibuka di Excel / Google Sheets). */
public final class CsvExport {
    private CsvExport() {}

    public static String build(Repo repo) {
        StringBuilder sb = new StringBuilder();
        sb.append("kode,koleksi,nama,status,kondisi,harga_beli,nilai_sekarang,harga_jual,tanggal_beli,skor,tier,detail,catatan\n");
        Map<Long, Collection> cols = new HashMap<>();
        for (Collection c : repo.collections()) cols.put(c.id, c);
        List<Item> items = repo.items(0, null, null);
        for (Item list : items) {
            Item it = repo.item(list.id);
            if (it == null) continue;
            Collection col = cols.get(it.collectionId);
            StringBuilder detail = new StringBuilder();
            if (col != null) {
                for (FieldDef f : col.fields) {
                    String v = it.values.get(f.id);
                    if (v == null || v.isEmpty()) continue;
                    if (detail.length() > 0) detail.append("; ");
                    detail.append(f.name).append("=").append(v);
                }
            }
            row(sb, it.code, it.collectionName, it.name, it.status, it.condition,
                    String.valueOf(it.price), String.valueOf(it.value),
                    it.soldPrice > 0 ? String.valueOf(it.soldPrice) : "",
                    it.acquiredAt > 0 ? Fmt.date(it.acquiredAt) : "",
                    String.valueOf(it.score), Rarity.of(it.score).label, detail.toString(), it.note);
        }
        return sb.toString();
    }

    private static void row(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(cells[i] == null ? "" : cells[i].replace("\"", "\"\"")).append('"');
        }
        sb.append('\n');
    }
}
