package id.kurasi.app.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import id.kurasi.app.R;
import id.kurasi.app.util.Neo;

/**
 * Dasar untuk layar berisi daftar vertikal: judul + tombol kembali di atas,
 * lalu isi yang dibangun ulang setiap layar muncul.
 */
public abstract class ScrollScreenActivity extends BaseActivity {
    protected LinearLayout content;

    protected abstract String screenTitle();

    protected abstract void build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Neo.BG);
        sv.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(24), dp(32));
        sv.addView(content);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
    }

    /** Bangun ulang seluruh isi layar (header + build()). */
    protected void rebuild() {
        content.removeAllViews();
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = new ImageView(this, null, 0, R.style.Neo_IconButton);
        back.setImageResource(R.drawable.ic_back);
        back.setContentDescription("Kembali");
        back.setOnClickListener(v -> onBackTapped());
        head.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView t = new TextView(this, null, 0, R.style.Neo_Title);
        t.setText(screenTitle().toUpperCase(java.util.Locale.ROOT));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tl.setMarginStart(dp(14));
        head.addView(t, tl);
        content.addView(head);
        build();
    }

    /** Dipanggil saat tombol kembali di header diketuk. Layar dengan isian bisa menimpanya. */
    protected void onBackTapped() {
        finish();
    }

    protected LinearLayout.LayoutParams wrapTop(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }
}
