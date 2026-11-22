package id.kurasi.app.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

import java.util.Locale;

import id.kurasi.app.util.Fmt;

/**
 * Memformat isi EditText sebagai rupiah: ketik 2200000, tampil 2.200.000.
 *
 * Catatan: EditText bertipe angka biasa membuang titik saat diisi lewat kode, dan memanggil
 * setSelection() dengan panjang teks yang salah akan crash. Karena itu titik diizinkan lewat
 * DigitsKeyListener dan teks diganti langsung di Editable (kursor ikut ke akhir).
 */
public class MoneyWatcher implements TextWatcher {
    private final Runnable onChange;
    private boolean busy;

    public MoneyWatcher(EditText edit, Runnable onChange) {
        this.onChange = onChange;
        // keyboard tetap angka, tetapi titik pemisah ribuan boleh ada di teks
        edit.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        format(edit.getText());
    }

    private void format(Editable e) {
        long v = Fmt.parseMoney(e);
        String out = v == 0 ? "" : String.format(new Locale("id", "ID"), "%,d", v);
        if (!out.equals(e.toString())) {
            busy = true;
            e.replace(0, e.length(), out);
            busy = false;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable e) {
        if (busy) return;
        format(e);
        if (onChange != null) onChange.run();
    }
}
