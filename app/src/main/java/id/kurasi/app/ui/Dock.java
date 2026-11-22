package id.kurasi.app.ui;

import android.content.Intent;
import android.widget.ImageView;

import androidx.viewpager2.widget.ViewPager2;

import id.kurasi.app.R;
import id.kurasi.app.ui.view.GlassDock;

/**
 * Dock melayang di bawah: Binder, Scan, Statistik, dan tambah item.
 * Dock dan pager saling mengikuti: geser lensa di dock dan halaman ikut bergeser, geser halaman dan lensa ikut.
 */
public final class Dock {
    private Dock() {}

    private static final int[] ICONS = {R.id.dockBinder, R.id.dockScan, R.id.dockStats};

    public static void bind(BaseActivity a, ViewPager2 pager) {
        GlassDock dock = a.findViewById(R.id.dock);

        // pager -> dock: lensa mengikuti posisi halaman, termasuk posisi pecahan saat sedang digeser
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float offset, int offsetPx) {
                dock.setProgress(position + offset);
                dock.invalidate(); // blur dihitung ulang karena isi di belakang dock bergeser
            }

            @Override
            public void onPageSelected(int position) {
                dock.setProgress(position);
            }
        });

        // dock -> pager: ketuk pindah dengan animasi, geser menggeser halaman secara langsung
        final boolean[] fake = {false};
        dock.setListeners(index -> pager.setCurrentItem(index, true), new GlassDock.DragListener() {
            @Override
            public void onDragStart() {
                fake[0] = pager.beginFakeDrag();
            }

            @Override
            public void onDrag(float deltaIndex) {
                // lensa ke kanan = halaman berikutnya masuk dari kanan, jadi isi halaman bergeser ke kiri
                if (fake[0]) pager.fakeDragBy(-deltaIndex * pager.getWidth());
            }

            @Override
            public void onDragEnd(int targetIndex) {
                if (fake[0]) pager.endFakeDrag();
                fake[0] = false;
                // dipanggil setelah pager selesai memutuskan arah geserannya sendiri; kalau langsung,
                // pemanggilan ini terabaikan dan halaman kembali ke tempat asal
                pager.post(() -> pager.setCurrentItem(targetIndex, true));
            }
        });

        // pendengar klik pada ikon tetap dipasang untuk pembaca layar (TalkBack) dan papan ketik
        for (int i = 0; i < ICONS.length; i++) {
            final int index = i;
            ImageView v = a.findViewById(ICONS[i]);
            v.setOnClickListener(x -> pager.setCurrentItem(index, true));
        }
        a.findViewById(R.id.dockAdd).setOnClickListener(v ->
                a.startActivity(new Intent().setClassName(a, "id.kurasi.app.ui.ItemFormActivity")));
    }
}
