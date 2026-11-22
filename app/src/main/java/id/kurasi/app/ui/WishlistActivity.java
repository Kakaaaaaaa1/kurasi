package id.kurasi.app.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.ImageStore;
import id.kurasi.app.util.Neo;

/** Barang incaran dengan target harga. Notifikasi dikirim saat harga di bawah target. */
public class WishlistActivity extends ScrollScreenActivity {

    @Override
    protected String screenTitle() {
        return "Wishlist";
    }

    @Override
    protected void build() {
        Repo repo = Repo.get(this);
        List<Item> items = repo.items(0, Models.S_WISHLIST, null);

        LinearLayout sum = new LinearLayout(this);
        sum.setOrientation(LinearLayout.VERTICAL);
        sum.setBackground(Neo.card(this, Neo.PINK, 22, 5));
        sum.setPadding(dp(16), dp(12), dp(21), dp(19));
        TextView l = new TextView(this, null, 0, R.style.Neo_Mono);
        l.setText("TOTAL TARGET · " + items.size() + " ITEM");
        sum.addView(l);
        LinearLayout big = new LinearLayout(this);
        big.setGravity(Gravity.BOTTOM);
        TextView n = new TextView(this, null, 0, R.style.Neo_Display);
        n.setText(Fmt.juta(repo.wishlistTarget()));
        n.setTextSize(68);
        big.addView(n);
        TextView u = new TextView(this, null, 0, R.style.Neo_Display);
        u.setText(" JUTA RP");
        u.setTextSize(28);
        u.setPadding(dp(8), 0, 0, dp(6));
        big.addView(u);
        sum.addView(big);
        int hit = 0;
        for (Item it : items) if (it.targetPrice > 0 && it.price <= it.targetPrice) hit++;
        TextView sub = new TextView(this, null, 0, R.style.Neo_Mono);
        sub.setText(hit + " ITEM SUDAH DI BAWAH TARGET");
        sum.addView(sub);
        Neo.onFill(sum);
        content.addView(sum, wrapTop(18));

        if (items.isEmpty()) {
            TextView e = new TextView(this, null, 0, R.style.Neo_Body);
            e.setText("Wishlist kosong. Tambahkan barang incaran beserta target harganya.");
            content.addView(e, wrapTop(20));
        }

        for (Item it : items) content.addView(row(it), wrapTop(16));

        content.addView(Ui.button(this, "+ Barang incaran", 0, v ->
                startActivity(new Intent(this, ItemFormActivity.class).putExtra("wishlist", true))), Ui.fullWidth(this, 58, 24));
    }

    private LinearLayout row(Item it) {
        boolean below = it.targetPrice > 0 && it.price <= it.targetPrice;
        LinearLayout box = new LinearLayout(this);
        box.setBackground(Neo.card(this, Neo.PAPER, 18, 4));
        box.setPadding(dp(10), dp(10), dp(14), dp(14));
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setOnClickListener(v -> actions(it));

        ImageView img = new ImageView(this);
        img.setBackground(Neo.flat(this, Neo.BG, 12));
        img.setPadding(dp(2), dp(2), dp(2), dp(2));
        Bitmap b = ImageStore.load(it.photo, 300);
        if (b != null) {
            img.setImageBitmap(b);
            img.setImageTintList(null);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            img.setImageResource(R.drawable.ic_image);
            img.setImageTintList(android.content.res.ColorStateList.valueOf(Neo.INK));
            img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        box.addView(img, new LinearLayout.LayoutParams(dp(72), dp(72)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(this, null, 0, R.style.Neo_Display);
        name.setText(it.name.toUpperCase(java.util.Locale.ROOT));
        name.setTextSize(23);
        name.setMaxLines(2);
        top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView tag = new TextView(this, null, 0, R.style.Neo_Mono);
        tag.setTextSize(10);
        tag.setText(below ? "TERCAPAI" : it.targetPrice > 0 ? "BELUM" : "TANPA TARGET");
        tag.setBackground(Neo.flat(this, below ? Neo.MINT : Neo.COMMON, 99));
        tag.setTextColor(Neo.ON_FILL);
        tag.setPadding(dp(8), 0, dp(8), 0);
        LinearLayout.LayoutParams tg = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tg.setMarginStart(dp(6));
        top.addView(tag, tg);
        info.addView(top);

        TextView price = new TextView(this, null, 0, R.style.Neo_Label);
        price.setText("SEKARANG " + Fmt.jt(it.price).toUpperCase(java.util.Locale.ROOT)
                + (it.targetPrice > 0 ? " · TARGET " + Fmt.jt(it.targetPrice).toUpperCase(java.util.Locale.ROOT) : ""));
        info.addView(price);

        if (it.targetPrice > 0) {
            int pct = below ? 100 : (int) Math.max(5, it.targetPrice * 100 / Math.max(1, it.price));
            LinearLayout track = new LinearLayout(this);
            track.setBackground(Neo.flat(this, Neo.BG, 6));
            track.setPadding(dp(2), dp(2), dp(2), dp(2));
            android.view.View fill = new android.view.View(this);
            fill.setBackground(Neo.flat(this, below ? Neo.MINT : Neo.ORANGE, 4));
            track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct));
            track.addView(new android.view.View(this), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - pct));
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12));
            pl.topMargin = dp(6);
            info.addView(track, pl);
        }
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        il.setMarginStart(dp(12));
        box.addView(info, il);
        return box;
    }

    private void actions(Item it) {
        new NeoSheet(this, it.name)
                .addButton("Ubah harga atau target", 1, () ->
                        startActivity(new Intent(this, ItemFormActivity.class).putExtra("id", it.id)))
                .addButton("Tandai sudah dibeli", 0, () -> {
                    Repo repo = Repo.get(this);
                    Item full = repo.item(it.id);
                    if (full == null) return;
                    full.status = Models.S_OWNED;
                    full.acquiredAt = System.currentTimeMillis();
                    full.value = 0;
                    repo.saveItem(full);
                    toast("Masuk ke binder: " + full.code);
                    onResume();
                })
                .addButton("Hapus dari wishlist", 2, () -> new NeoSheet(this, "Hapus " + it.name + "?")
                        .addButton("Ya, hapus dari wishlist", 2, () -> {
                            Repo.get(this).deleteItem(it.id);
                            toast("Dihapus dari wishlist");
                            onResume();
                        })
                        .addButton("Batal", 1, null)
                        .show())
                .show();
    }
}
