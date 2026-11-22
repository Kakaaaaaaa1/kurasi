package id.kurasi.app.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import id.kurasi.app.R;
import id.kurasi.app.util.Neo;

/** Pembuat komponen kecil bergaya Kurasi lewat kode. */
public final class Ui {
    private Ui() {}

    /** Chip berbentuk pil. selected = lime. */
    public static TextView chip(Context c, String text, boolean selected, View.OnClickListener click) {
        TextView t = new TextView(c, null, 0, R.style.Neo_Chip);
        t.setText(text.toUpperCase(java.util.Locale.ROOT));
        t.setSelected(selected);
        t.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(Neo.dp(c, 8));
        t.setLayoutParams(lp);
        return t;
    }

    /** Label kecil huruf kapital abu-abu di atas input. */
    public static TextView label(Context c, String text) {
        TextView t = new TextView(c, null, 0, R.style.Neo_Label);
        t.setText(text.toUpperCase(java.util.Locale.ROOT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Neo.dp(c, 14);
        lp.bottomMargin = Neo.dp(c, 4);
        t.setLayoutParams(lp);
        return t;
    }

    /** Tombol bertepi bawah. style: 0 lime, 1 putih, 2 pink. */
    @android.annotation.SuppressLint("ClickableViewAccessibility") // listener sentuh hanya memberi getar dan mengembalikan false
    public static TextView button(Context c, String text, int style, View.OnClickListener click) {
        int res = style == 1 ? R.style.Neo_Button_White : style == 2 ? R.style.Neo_Button_Pink : R.style.Neo_Button;
        TextView t = new TextView(c, null, 0, res);
        t.setText(text.toUpperCase(java.util.Locale.ROOT));
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(click);
        // getar halus saat disentuh. Mengembalikan false supaya klik tetap berjalan normal
        t.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
            return false;
        });
        return t;
    }

    /** Beri tinggi 58dp dan jarak ke tombol. */
    public static LinearLayout.LayoutParams fullWidth(Context c, int heightDp, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Neo.dp(c, heightDp));
        lp.topMargin = Neo.dp(c, topMarginDp);
        return lp;
    }
}
