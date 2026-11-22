package id.kurasi.app.ui;

import android.content.Intent;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Collection;
import id.kurasi.app.data.Models.FieldDef;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Neo;

/** Daftar koleksi: pilih untuk diubah, atau buat koleksi baru dari template atau kosong. */
public class CollectionsActivity extends ScrollScreenActivity {

    @Override
    protected String screenTitle() {
        return "Koleksi";
    }

    @Override
    protected void build() {
        Repo repo = Repo.get(this);
        List<Collection> cols = repo.collections();
        TextView hint = new TextView(this, null, 0, R.style.Neo_Body);
        hint.setText("Tiap koleksi punya detail sendiri. Pakai template, atau rancang sendiri untuk apa pun yang kamu kumpulkan.");
        hint.setTextSize(14);
        content.addView(hint, wrapTop(14));

        for (Collection c : cols) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Neo.card(this, c.color, 18, 4));
            card.setPadding(dp(16), dp(12), dp(20), dp(18));
            card.setOnClickListener(v -> startActivity(new Intent(this, CollectionEditorActivity.class).putExtra("id", c.id)));

            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = new TextView(this, null, 0, R.style.Neo_Display);
            name.setText(c.name.toUpperCase(java.util.Locale.ROOT));
            name.setTextSize(32);
            name.setTextColor(Neo.ON_FILL);
            top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView code = new TextView(this, null, 0, R.style.Neo_Mono);
            code.setText(c.prefix + " · " + repo.itemCount(c.id) + " ITEM");
            code.setBackground(Neo.flat(this, Neo.INK, 99));
            code.setTextColor(Neo.BG);
            code.setPadding(dp(10), dp(2), dp(10), dp(2));
            top.addView(code);
            card.addView(top);

            StringBuilder sb = new StringBuilder();
            for (FieldDef f : c.fields) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(f.name);
            }
            TextView fields = new TextView(this, null, 0, R.style.Neo_Mono);
            fields.setText(sb.length() == 0 ? "BELUM ADA DETAIL. KETUK UNTUK MENAMBAH" : sb.toString().toUpperCase(java.util.Locale.ROOT));
            fields.setTextColor(Neo.ON_FILL);
            card.addView(fields);
            content.addView(card, wrapTop(16));
        }
        content.addView(Ui.button(this, "+ Koleksi baru", 0, v ->
                startActivity(new Intent(this, CollectionEditorActivity.class))), Ui.fullWidth(this, 58, 24));
    }
}
