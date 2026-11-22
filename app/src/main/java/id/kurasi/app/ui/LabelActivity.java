package id.kurasi.app.ui;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.LabelPdf;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.QrUtil;

/** Pratinjau stiker QR, lalu bagikan sebagai PDF A4 atau langsung cetak. */
public class LabelActivity extends BaseActivity {
    private Repo repo;
    private Item item;
    private int sizeIdx = 1;
    private boolean showName = true, showPrice = false, allInCollection = false;

    private TextView previewLabel, stickerName, stickerPrice, pageInfo;
    private PdfDocument keepAlive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label);
        repo = Repo.get(this);
        item = repo.item(getIntent().getLongExtra("id", 0));
        if (item == null) {
            finish();
            return;
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        previewLabel = findViewById(R.id.previewLabel);
        stickerName = findViewById(R.id.stickerName);
        stickerPrice = findViewById(R.id.stickerPrice);
        pageInfo = findViewById(R.id.pageInfo);

        ((TextView) findViewById(R.id.stickerCollection)).setText("KURASI · " + item.collectionName.toUpperCase(java.util.Locale.ROOT));
        ((TextView) findViewById(R.id.stickerCode)).setText(item.code);
        stickerName.setText(item.name.toUpperCase(java.util.Locale.ROOT));
        stickerPrice.setText(Fmt.rpJt(item.price).toUpperCase(java.util.Locale.ROOT));
        Bitmap qr = QrUtil.bitmap(QrUtil.payload(item.code), 512);
        ImageView qrView = findViewById(R.id.qrImage);
        qrView.setImageBitmap(qr);
        // QR selalu hitam di atas putih dengan tepi putih (quiet zone), juga di mode gelap
        qrView.setBackgroundColor(Neo.WHITE);
        qrView.setPadding(dp(6), dp(6), dp(6), dp(6));

        buildOptions();
        refreshPreview();
        findViewById(R.id.btnPdf).setOnClickListener(v -> sharePdf());
        findViewById(R.id.btnPrint).setOnClickListener(v -> print());
    }

    private void buildOptions() {
        LinearLayout o = findViewById(R.id.options);
        o.removeAllViews();

        o.addView(Ui.label(this, "Ukuran label"));
        o.addView(row(new String[]{"Kecil", "Sedang", "Besar"}, sizeIdx, i -> {
            sizeIdx = i;
            refreshPreview();
        }));

        o.addView(Ui.label(this, "Isi label"));
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        TextView n = Ui.chip(this, "Nama item", showName, null);
        n.setOnClickListener(v -> {
            showName = !showName;
            n.setSelected(showName);
            refreshPreview();
        });
        TextView p = Ui.chip(this, "Harga", showPrice, null);
        p.setOnClickListener(v -> {
            showPrice = !showPrice;
            p.setSelected(showPrice);
            refreshPreview();
        });
        r.addView(n);
        r.addView(p);
        o.addView(r);

        o.addView(Ui.label(this, "Cetak untuk"));
        o.addView(row(new String[]{"Item ini saja", "Semua di " + item.collectionName}, allInCollection ? 1 : 0, i -> {
            allInCollection = i == 1;
            refreshPreview();
        }));
    }

    private interface Pick {
        void on(int i);
    }

    private View row(String[] labels, int selected, Pick pick) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        final List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView c = Ui.chip(this, labels[i], i == selected, null);
            c.setOnClickListener(v -> {
                for (int k = 0; k < chips.size(); k++) chips.get(k).setSelected(k == idx);
                pick.on(idx);
            });
            chips.add(c);
            r.addView(c);
        }
        return r;
    }

    private void refreshPreview() {
        int mm = LabelPdf.SIZES_MM[sizeIdx];
        previewLabel.setText("PRATINJAU CETAK · " + mm + " × " + (mm / 2) + " MM");
        stickerName.setVisibility(showName ? View.VISIBLE : View.GONE);
        stickerPrice.setVisibility(showPrice ? View.VISIBLE : View.GONE);
        int per = LabelPdf.perPage(mm);
        pageInfo.setText("1 HALAMAN A4 MUAT " + per + " LABEL");
    }

    private PdfDocument build() {
        int mm = LabelPdf.SIZES_MM[sizeIdx];
        List<Item> list = new ArrayList<>();
        int count;
        if (allInCollection) {
            list = repo.items(item.collectionId, "NOT_WISHLIST", null);
            if (list.isEmpty()) list.add(item);
            count = list.size();
        } else {
            list.add(item);
            count = LabelPdf.perPage(mm);
        }
        return LabelPdf.build(this, list, mm, showName, showPrice, count);
    }

    private void sharePdf() {
        try {
            PdfDocument doc = build();
            File dir = new File(getCacheDir(), "share");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File f = new File(dir, "label_" + item.code + ".pdf");
            try (FileOutputStream out = new FileOutputStream(f)) {
                LabelPdf.write(doc, out);
            }
            doc.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", f);
            Intent send = new Intent(Intent.ACTION_SEND).setType("application/pdf")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Bagikan label"));
        } catch (Exception e) {
            toast("Gagal membuat PDF");
        }
    }

    private void print() {
        if (keepAlive != null) keepAlive.close();
        keepAlive = build();
        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
        pm.print("Label " + item.code, LabelPdf.printAdapter(keepAlive, "Label " + item.code), null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // dokumen cetak dibiarkan terbuka sampai layar ini ditutup
        if (keepAlive != null) keepAlive.close();
    }
}
