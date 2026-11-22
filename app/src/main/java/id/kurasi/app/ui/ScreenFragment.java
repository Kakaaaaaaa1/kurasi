package id.kurasi.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.File;

import id.kurasi.app.util.Neo;

/**
 * Dasar untuk tiga halaman utama (Binder, Scan, Statistik) yang hidup di satu pager.
 * Menyediakan pembantu yang dulu ada di Activity supaya kodenya tetap sama.
 */
public abstract class ScreenFragment extends Fragment {
    protected Context ctx() {
        return requireContext();
    }

    protected int dp(float v) {
        return Neo.dp(ctx(), v);
    }

    protected void toast(String msg) {
        Toast.makeText(ctx(), msg, Toast.LENGTH_SHORT).show();
    }

    protected File getCacheDir() {
        return ctx().getCacheDir();
    }

    protected String getPackageName() {
        return ctx().getPackageName();
    }

    protected <T extends View> T find(int id) {
        return requireView().findViewById(id);
    }
}
