package id.kurasi.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Models.Loan;
import id.kurasi.app.data.Repo;
import id.kurasi.app.util.Fmt;
import id.kurasi.app.util.Neo;
import id.kurasi.app.util.QrUtil;
import id.kurasi.app.util.Rarity;

/** Pindai QR di barang: buka kartunya atau catat peminjaman/pengembalian. */
public class ScanFragment extends ScreenFragment {
    private DecoratedBarcodeView scanner;
    private Repo repo;
    private boolean busy;
    private boolean keepBusy;
    private long lastWarn;

    private final ActivityResultLauncher<String> askCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) scanner.resume();
                else {
                    toast("Scan QR butuh izin kamera. Aktifkan lewat Pengaturan HP.");
                    goHome();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_scan, container, false);
        repo = Repo.get(ctx());
        scanner = root.findViewById(R.id.scanner);
        scanner.setStatusText("");
        root.findViewById(R.id.btnClose).setOnClickListener(v -> goHome());

        // senter untuk memindai di tempat gelap, hanya tampil kalau HP punya lampu kilat
        android.widget.ImageView torch = root.findViewById(R.id.btnTorch);
        if (ctx().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            torch.setVisibility(android.view.View.VISIBLE);
            final boolean[] on = {false};
            torch.setOnClickListener(v -> {
                on[0] = !on[0];
                if (on[0]) scanner.setTorchOn();
                else scanner.setTorchOff();
                torch.setBackground(on[0] ? Neo.card(ctx(), Neo.LIME, 99, 3) : Neo.card(ctx(), Neo.PAPER, 99, 3));
                torch.setImageTintList(android.content.res.ColorStateList.valueOf(on[0] ? Neo.ON_FILL : Neo.INK));
                torch.setContentDescription(on[0] ? "Matikan senter" : "Nyalakan senter");
            });
        }
        BarcodeCallback cb = result -> {
            if (busy || result.getText() == null) return;
            String code = QrUtil.parse(result.getText());
            if (code == null) {
                warn("QR ini bukan dari Kurasi. Arahkan ke label QR yang dicetak dari aplikasi ini.");
                return;
            }
            Item it = repo.itemByCode(code);
            if (it == null) {
                warn("Kartu " + code + " tidak ada di binder ini, mungkin sudah dihapus.");
                return;
            }
            busy = true;
            scanner.pause();
            showFound(it);
        };
        scanner.decodeContinuous(cb);
        return root;
    }

    /** Kembali ke halaman Binder di pager. */
    private void goHome() {
        if (requireActivity() instanceof BinderActivity) ((BinderActivity) requireActivity()).goToPage(0);
    }

    private void warn(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastWarn > 2500) {
            lastWarn = now;
            toast(msg);
        }
    }

    private void showFound(Item it) {
        Loan active = repo.activeLoan(it.id);
        NeoSheet s = new NeoSheet(ctx(), it.name);
        LinearLayout body = s.body();
        TextView t = new TextView(ctx(), null, 0, R.style.Neo_Mono);
        String status = active != null ? "DIPINJAM " + active.borrower.toUpperCase(java.util.Locale.ROOT) + " · KEMBALI " + Fmt.date(active.dueAt).toUpperCase(java.util.Locale.ROOT)
                : "DIMILIKI · DI RAK";
        t.setText(it.code + " · " + Rarity.of(it.score).label + "\n" + status);
        body.addView(t);
        keepBusy = false;
        s.addButton(active != null ? "Tandai sudah kembali" : "Pinjamkan", 0, () -> {
            // pemindai tetap berhenti sampai sheet peminjaman ditutup, supaya QR yang sama
            // tidak terbaca lagi dan membuka sheet kedua
            keepBusy = true;
            LoanSheet.show(requireActivity(), it, null).onDismiss(() -> {
                busy = false;
                if (isAdded() && isResumed()) scanner.resume();
            });
        });
        s.addButton("Buka kartu", 1, () ->
                startActivity(new Intent(ctx(), CardActivity.class).putExtra("id", it.id)));
        s.onDismiss(() -> {
            if (keepBusy) return;
            busy = false;
            if (isAdded() && isResumed()) scanner.resume();
        });
        s.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(ctx(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scanner.resume();
        } else {
            askCamera.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        scanner.pause();
    }
}
