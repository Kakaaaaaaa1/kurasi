package id.kurasi.app.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import id.kurasi.app.R;
import id.kurasi.app.util.Neo;

/** Bottom sheet bergaya Kurasi. Isi dibangun lewat kode. */
public final class NeoSheet {
    private final BottomSheetDialog dialog;
    private final LinearLayout body;
    private final Context ctx;

    public NeoSheet(Context c, String title) {
        ctx = c;
        dialog = new BottomSheetDialog(c);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Neo.dp(c, 20);
        root.setPadding(p, Neo.dp(c, 16), p, Neo.dp(c, 24));

        GradientDrawable bg = new GradientDrawable();
        float r = Neo.dp(c, 28);
        bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        bg.setColor(Neo.BG);
        bg.setStroke(Neo.dp(c, 3), Neo.INK);
        root.setBackground(bg);

        View handle = new View(c);
        GradientDrawable hd = new GradientDrawable();
        hd.setColor(Neo.INK);
        hd.setCornerRadius(Neo.dp(c, 3));
        handle.setBackground(hd);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(Neo.dp(c, 44), Neo.dp(c, 5));
        hl.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(handle, hl);

        TextView t = new TextView(c, null, 0, R.style.Neo_Title);
        t.setText(title.toUpperCase(java.util.Locale.ROOT));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = Neo.dp(c, 12);
        root.addView(t, tl);

        body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(c);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(root);
        // latar bawaan dialog dibuat transparan agar sudut bulat terlihat
        View parent = (View) root.getParent();
        if (parent != null) parent.setBackgroundColor(0);
    }

    public LinearLayout body() {
        return body;
    }

    public NeoSheet addView(View v) {
        body.addView(v);
        return this;
    }

    /** Tambah tombol aksi yang menutup sheet saat diklik. style: 0 lime, 1 putih, 2 pink. */
    public NeoSheet addButton(String text, int style, Runnable action) {
        TextView b = Ui.button(ctx, text, style, v -> {
            dialog.dismiss();
            if (action != null) action.run();
        });
        body.addView(b, Ui.fullWidth(ctx, 58, 12));
        return this;
    }

    public NeoSheet onDismiss(Runnable r) {
        dialog.setOnDismissListener(d -> r.run());
        return this;
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }
}
