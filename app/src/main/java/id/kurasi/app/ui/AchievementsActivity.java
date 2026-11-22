package id.kurasi.app.ui;

import android.content.res.ColorStateList;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.SetDef;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.Rarity;

/** Level kolektor (XP), lencana, dan progres set. */
public class AchievementsActivity extends ScrollScreenActivity {

    @Override
    protected String screenTitle() {
        return "Pencapaian";
    }

    @Override
    protected void build() {
        Repo repo = Repo.get(this);
        int xp = repo.xp();

        // level: level n butuh 100 + (n-1)*50 XP untuk naik ke level berikutnya
        int level = 1, need = 100, left = xp;
        while (left >= need) {
            left -= need;
            level++;
            need = 100 + (level - 1) * 50;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Neo.card(this, Neo.LIME, 22, 5));
        box.setPadding(dp(16), dp(12), dp(21), dp(19));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.BOTTOM);
        TextView lv = new TextView(this, null, 0, R.style.Neo_Display);
        lv.setText("KOLEKTOR LV. " + level);
        lv.setTextSize(44);
        row.addView(lv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView xpText = new TextView(this, null, 0, R.style.Neo_Mono);
        xpText.setText(left + " / " + need + " XP");
        xpText.setPadding(0, 0, 0, dp(8));
        row.addView(xpText);
        box.addView(row);
        int pct = Math.max(1, left * 100 / need);
        LinearLayout track = new LinearLayout(this);
        track.setBackground(Neo.flatOnFill(this, Neo.WHITE, 9));
        track.setPadding(dp(2), dp(2), dp(2), dp(2));
        View fill = new View(this);
        fill.setBackground(Neo.flatOnFill(this, Neo.ON_FILL, 7));
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct));
        track.addView(new View(this), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - pct));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18));
        tl.topMargin = dp(8);
        box.addView(track, tl);
        TextView how = new TextView(this, null, 0, R.style.Neo_Mono);
        how.setTextSize(10);
        how.setText("DAPAT XP SAAT TAMBAH ITEM, PASANG FOTO, CATAT NILAI, DAN MEMINJAMKAN");
        how.setPadding(0, dp(8), 0, 0);
        box.addView(how);
        Neo.onFill(box);
        content.addView(box, wrapTop(18));

        // lencana
        int items = repo.ownedCount();
        int[] tiers = repo.tierCounts();
        int leg = tiers[Rarity.LEGENDARY.ordinal()];
        List<SetDef> sets = repo.sets();
        int complete = 0;
        for (SetDef s : sets) if (s.targetCount > 0 && s.count >= s.targetCount) complete++;

        Object[][] badges = {
                {"10", "ITEM", Neo.CYAN, items, 10},
                {"50", "ITEM", Neo.LAVENDER, items, 50},
                {"100", "ITEM", Neo.ORANGE, items, 100},
                {"1", "LEGENDARY", Neo.LIME, leg, 1},
                {"5", "LEGENDARY", Neo.YELLOW, leg, 5},
                {"SET", "LENGKAP", Neo.PINK, complete, 1}
        };
        int got = 0;
        for (Object[] b : badges) if ((int) b[3] >= (int) b[4]) got++;
        content.addView(Ui.label(this, "Lencana · " + got + " dari " + badges.length));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout badgeRow = null;
        for (int bi = 0; bi < badges.length; bi++) {
            Object[] b = badges[bi];
            if (bi % 3 == 0) {
                badgeRow = new LinearLayout(this);
                grid.addView(badgeRow, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            boolean ok = (int) b[3] >= (int) b[4];
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            if (ok) {
                cell.setBackground(Neo.card(this, (int) b[2], 18, 3));
            } else {
                cell.setBackground(Neo.flat(this, Neo.COMMON, 18));
            }
            TextView big = new TextView(this, null, 0, R.style.Neo_Display);
            big.setText((String) b[0]);
            big.setTextSize(((String) b[0]).length() > 2 ? 30 : 40);
            TextView small = new TextView(this, null, 0, R.style.Neo_Mono);
            small.setTextSize(10);
            small.setText((String) b[1]);
            cell.addView(big);
            cell.addView(small);
            if (!ok) {
                TextView prog = new TextView(this, null, 0, R.style.Neo_Mono);
                prog.setTextSize(10);
                prog.setText(Math.min((int) b[3], (int) b[4]) + " / " + b[4]);
                cell.addView(prog);
            }
            // semua kotak lencana berisi warna tetap, jadi teksnya selalu gelap dan rata tengah
            Neo.onFill(cell);
            for (int k = 0; k < cell.getChildCount(); k++) {
                ((TextView) cell.getChildAt(k)).setGravity(Gravity.CENTER);
            }
            if (!ok) {
                big.setTextColor(Neo.MUTED_ON_FILL);
                small.setTextColor(Neo.MUTED_ON_FILL);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(104), 1);
            lp.setMargins(0, dp(6), dp(10), dp(6));
            badgeRow.addView(cell, lp);
        }
        content.addView(grid, wrapTop(2));

        // progres set
        content.addView(Ui.label(this, "Progres set"));
        for (SetDef s : sets) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Neo.flat(this, Neo.PAPER, 16));
            card.setPadding(dp(14), dp(10), dp(14), dp(12));
            card.setOnClickListener(v -> setActions(s));
            LinearLayout line = new LinearLayout(this);
            TextView name = new TextView(this, null, 0, R.style.Neo_Display);
            name.setText(s.name.toUpperCase(java.util.Locale.ROOT));
            name.setTextSize(22);
            line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView cnt = new TextView(this, null, 0, R.style.Neo_Mono);
            cnt.setText(s.count + " / " + s.targetCount);
            line.addView(cnt);
            card.addView(line);
            int p = s.targetCount <= 0 ? 0 : Math.min(100, s.count * 100 / s.targetCount);
            LinearLayout tr = new LinearLayout(this);
            tr.setBackground(Neo.flat(this, Neo.BG, 7));
            tr.setPadding(dp(2), dp(2), dp(2), dp(2));
            View f = new View(this);
            f.setBackground(Neo.flat(this, p >= 100 ? Neo.MINT : Neo.LAVENDER, 5));
            tr.addView(f, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, p)));
            tr.addView(new View(this), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - Math.max(1, p)));
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(14));
            pl.topMargin = dp(6);
            card.addView(tr, pl);
            content.addView(card, wrapTop(10));
        }
        if (sets.isEmpty()) {
            TextView e = new TextView(this, null, 0, R.style.Neo_Body);
            e.setText("Set adalah kumpulan item yang ingin kamu lengkapi, misalnya satu seri buku. Buat set, lalu pilih set itu saat menambah item.");
            e.setTextSize(13);
            content.addView(e, wrapTop(6));
        }
        content.addView(Ui.button(this, "+ Set baru", 1, v -> setSheet(null)), Ui.fullWidth(this, 54, 16));
    }

    private void setActions(SetDef s) {
        new NeoSheet(this, s.name)
                .addButton("Ubah set", 1, () -> setSheet(s))
                .addButton("Hapus set", 2, () -> new NeoSheet(this, "Hapus set " + s.name + "?")
                        .addButton("Ya, hapus set (item tetap ada)", 2, () -> {
                            Repo.get(this).deleteSet(s.id);
                            toast("Set dihapus. Item di dalamnya tetap ada di binder.");
                            onResume();
                        })
                        .addButton("Batal", 1, null)
                        .show())
                .show();
    }

    private void setSheet(SetDef edit) {
        SetDef s = edit == null ? new SetDef() : edit;
        NeoSheet sheet = new NeoSheet(this, edit == null ? "Set baru" : "Ubah set");
        sheet.body().addView(Ui.label(this, "Nama set"));
        EditText name = new EditText(this, null, 0, R.style.Neo_Input);
        name.setHint("Contoh: Seri Dune");
        name.setText(s.name);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        sheet.body().addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        sheet.body().addView(Ui.label(this, "Jumlah item dalam set lengkap"));
        EditText target = new EditText(this, null, 0, R.style.Neo_Input);
        target.setHint("6");
        target.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (s.targetCount > 0) target.setText(String.valueOf(s.targetCount));
        sheet.body().addView(target, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        sheet.addButton("Simpan set", 0, null);
        sheet.body().getChildAt(sheet.body().getChildCount() - 1).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            int t;
            try {
                t = Integer.parseInt(target.getText().toString().trim());
            } catch (NumberFormatException e) {
                t = 0;
            }
            if (n.isEmpty()) {
                name.setError("Nama setnya belum diisi");
                return;
            }
            if (t <= 0) {
                target.setError("Jumlah itemnya belum diisi");
                return;
            }
            s.name = n;
            s.targetCount = t;
            Repo.get(this).saveSet(s);
            sheet.dismiss();
            onResume();
        });
        sheet.show();
    }
}
