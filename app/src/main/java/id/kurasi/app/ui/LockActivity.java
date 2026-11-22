package id.kurasi.app.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import id.kurasi.app.R;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.Prefs;

/** Layar PIN 4 angka, dengan opsi sidik jari atau wajah. */
public class LockActivity extends BaseActivity {
    private final StringBuilder entry = new StringBuilder();
    private LinearLayout dots;
    private TextView hint;

    @Override
    protected boolean requiresLock() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Neo.BG);
        root.setPadding(dp(32), dp(48), dp(32), dp(24));
        setContentView(root);

        TextView brand = new TextView(this, null, 0, R.style.Neo_Display);
        brand.setText("KURASI");
        brand.setTextSize(56);
        root.addView(brand, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        hint = new TextView(this, null, 0, R.style.Neo_Mono);
        hint.setText("MASUKKAN PIN");
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dl.topMargin = dp(28);
        dl.bottomMargin = dp(28);
        root.addView(dots, dl);

        GridLayout pad = new GridLayout(this);
        pad.setColumnCount(3);
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫"};
        for (String k : keys) {
            TextView b = new TextView(this, null, 0, R.style.Neo_Display);
            b.setText(k);
            b.setTextSize(34);
            b.setGravity(Gravity.CENTER);
            if (!k.isEmpty()) {
                b.setBackgroundResource(R.drawable.bg_icon_btn);
                b.setPadding(0, 0, 0, dp(3));
                b.setOnClickListener(v -> press(k));
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(76);
            lp.height = dp(76);
            lp.setMargins(dp(10), dp(8), dp(10), dp(8));
            pad.addView(b, lp);
        }
        root.addView(pad, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        drawDots();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        if (Prefs.useBiometric(this) && BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            unlock();
                        }
                    });
            prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Buka Kurasi")
                    .setNegativeButtonText("Pakai PIN")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build());
        }
    }

    private void press(String k) {
        if ("⌫".equals(k)) {
            if (entry.length() > 0) entry.deleteCharAt(entry.length() - 1);
        } else if (entry.length() < 4) {
            entry.append(k);
        }
        drawDots();
        if (entry.length() == 4) {
            if (Prefs.checkPin(this, entry.toString())) {
                unlock();
            } else {
                entry.setLength(0);
                hint.setText("PIN SALAH, COBA LAGI");
                drawDots();
            }
        }
    }

    private void drawDots() {
        dots.removeAllViews();
        for (int i = 0; i < 4; i++) {
            View d = new View(this);
            d.setBackground(Neo.flat(this, i < entry.length() ? Neo.INK : Neo.PAPER, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(24), dp(24));
            lp.setMargins(dp(8), 0, dp(8), 0);
            dots.addView(d, lp);
        }
    }

    private void unlock() {
        Lock.unlocked = true;
        Lock.showing = false;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Lock.showing = false;
    }
}
