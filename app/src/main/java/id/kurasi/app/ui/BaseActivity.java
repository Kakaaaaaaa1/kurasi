package id.kurasi.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.function.BooleanSupplier;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Dasar semua layar: tampilan penuh layar dengan padding aman untuk status bar dan navigasi. */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        // warna tema (terang atau gelap) dibaca ulang tiap layar dibuat
        id.kurasi.app.util.Neo.init(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyInsets();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyInsets();
    }

    private void applyInsets() {
        View content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Layar kunci mengembalikan false supaya tidak meminta PIN berulang. */
    protected boolean requiresLock() {
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (requiresLock()) Lock.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (requiresLock()) Lock.onStop();
    }

    /**
     * Pasang penjaga tombol kembali: kalau ada perubahan yang belum disimpan, tanya dulu
     * sebelum layar ditutup supaya isian yang sudah diketik tidak hilang tanpa sengaja.
     */
    protected void guardBack(BooleanSupplier dirty) {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeOrAsk(dirty);
            }
        });
    }

    protected void closeOrAsk(BooleanSupplier dirty) {
        if (!dirty.getAsBoolean()) {
            finish();
            return;
        }
        new NeoSheet(this, "Buang perubahan?")
                .addButton("Lanjut mengisi", 0, null)
                .addButton("Ya, buang", 2, this::finish)
                .show();
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected int dp(float v) {
        return id.kurasi.app.util.Neo.dp(this, v);
    }
}
