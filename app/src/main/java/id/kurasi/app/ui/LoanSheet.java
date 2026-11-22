package id.kurasi.app.ui;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.Loan;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.Neo;

/** Sheet untuk mencatat peminjaman atau pengembalian sebuah item. */
public final class LoanSheet {
    private LoanSheet() {}

    private static final long DAY = 24L * 3600 * 1000;

    public static NeoSheet show(Activity a, Item item, Runnable onDone) {
        Repo repo = Repo.get(a);
        Loan active = repo.activeLoan(item.id);
        NeoSheet sheet = new NeoSheet(a, active == null ? "Pinjamkan" : "Sedang dipinjam");
        LinearLayout body = sheet.body();

        TextView info = new TextView(a, null, 0, R.style.Neo_Mono);
        info.setText(item.code + " · " + item.name.toUpperCase(java.util.Locale.ROOT));
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        il.topMargin = Neo.dp(a, 6);
        body.addView(info, il);

        if (active != null) {
            TextView t = new TextView(a, null, 0, R.style.Neo_Body);
            t.setText(active.borrower + " meminjam sejak " + Fmt.date(active.loanedAt)
                    + ".\nJatuh tempo " + Fmt.date(active.dueAt)
                    + (active.dueAt < System.currentTimeMillis() ? " (terlambat)" : "") + ".");
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tl.topMargin = Neo.dp(a, 12);
            body.addView(t, tl);
            sheet.addButton("Tandai sudah kembali", 0, () -> {
                repo.returnLoan(active.id);
                if (onDone != null) onDone.run();
            });
            sheet.show();
            return sheet;
        }

        body.addView(Ui.label(a, "Dipinjam oleh"));
        EditText who = new EditText(a, null, 0, R.style.Neo_Input);
        who.setHint("Nama peminjam");
        who.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        body.addView(who, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Neo.dp(a, 48)));

        body.addView(Ui.label(a, "Kembali dalam"));
        final int[] days = {7};
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final int[] opts = {3, 7, 14};
        final String[] names = {"3 hari", "1 minggu", "2 minggu"};
        final List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < opts.length; i++) {
            final int d = opts[i];
            TextView c = Ui.chip(a, names[i], d == days[0], null);
            c.setOnClickListener(v -> {
                days[0] = d;
                for (TextView x : chips) x.setSelected(x == c);
            });
            chips.add(c);
            row.addView(c);
        }
        body.addView(row);

        sheet.addButton("Catat peminjaman", 0, null);
        // tombol terakhir menutup sheet lewat addButton; validasi nama dilakukan sebelum menutup
        View btn = body.getChildAt(body.getChildCount() - 1);
        btn.setOnClickListener(v -> {
            String name = who.getText().toString().trim();
            if (name.isEmpty()) {
                who.setError("Isi nama peminjam");
                return;
            }
            repo.startLoan(item.id, name, System.currentTimeMillis() + days[0] * DAY);
            sheet.dismiss();
            if (onDone != null) onDone.run();
        });
        sheet.show();
        return sheet;
    }
}
