package id.kurasi.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import id.kurasi.app.R;
import id.kurasi.app.data.Models.Collection;
import id.kurasi.app.data.Models.Item;
import id.kurasi.app.data.Repo;
import id.kurasi.app.data.SampleData;
import id.kurasi.app.util.Prefs;
import id.kurasi.app.util.Rarity;

/** Layar utama: grid kartu semua item, difilter per koleksi, bisa dicari dan diurutkan. */
public class BinderFragment extends ScreenFragment {
    private static final String[] SORT_LABELS = {"Terbaru", "Skor tertinggi", "Harga tertinggi", "Nama A-Z"};

    private Repo repo;
    private CardAdapter adapter;
    private long filterCollection = 0;
    private String query = "";
    private int sort = Repo.SORT_NEWEST;

    private LinearLayout chips;
    private TextView countPill, tierLine;
    private View empty, noResult, dock;
    private EditText etSearch;
    private RecyclerView grid;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_binder, container, false);
        repo = Repo.get(ctx());
        SampleData.ensureDefaults(ctx());

        chips = root.findViewById(R.id.chips);
        countPill = root.findViewById(R.id.countPill);
        tierLine = root.findViewById(R.id.tierLine);
        empty = root.findViewById(R.id.empty);
        noResult = root.findViewById(R.id.noResult);
        dock = requireActivity().findViewById(R.id.dock);
        etSearch = root.findViewById(R.id.etSearch);
        grid = root.findViewById(R.id.grid);

        adapter = new CardAdapter(item -> startActivity(
                new Intent(ctx(), CardActivity.class).putExtra("id", item.id)));
        grid.setLayoutManager(new GridLayoutManager(ctx(), 2));
        grid.setAdapter(adapter);
        int gap = dp(7);
        grid.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect out, View v, RecyclerView p, RecyclerView.State s) {
                out.set(gap, gap, gap, gap);
            }
        });

        root.findViewById(R.id.btnMenu).setOnClickListener(v -> showMenu());
        root.findViewById(R.id.btnSort).setOnClickListener(v -> showSort());
        root.findViewById(R.id.btnSample).setOnClickListener(v -> {
            boolean added = SampleData.load(ctx());
            refreshAll();
            toast(added ? "Data contoh dimuat" : "Data contoh sudah ada di binder");
        });

        // pencarian: daftar diperbarui setiap kali teks berubah
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                refreshGrid();
            }
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        // dock disembunyikan saat mengetik supaya tidak menutupi hasil
        etSearch.setOnFocusChangeListener((v, focused) -> dock.setVisibility(focused ? View.GONE : View.VISIBLE));

        // blur dock di belakang daftar kartu dihitung ulang saat daftar di-scroll
        if (dock instanceof id.kurasi.app.ui.view.GlassDock) ((id.kurasi.app.ui.view.GlassDock) dock).watch(grid);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAll();
        id.kurasi.app.widget.KurasiWidget.refresh(ctx());
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) ctx().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        etSearch.clearFocus();
    }

    private void refreshAll() {
        refreshChips();
        refreshGrid();
        countPill.setText(repo.ownedCount() + " KARTU");
        int[] t = repo.tierCounts();
        tierLine.setText(t[Rarity.LEGENDARY.ordinal()] + " LEGENDARY · "
                + t[Rarity.EPIC.ordinal()] + " EPIC · " + t[Rarity.RARE.ordinal()] + " RARE");
    }

    private void refreshChips() {
        List<Collection> cols = repo.collections();
        if (filterCollection != 0) {
            boolean exists = false;
            for (Collection c : cols) if (c.id == filterCollection) exists = true;
            if (!exists) filterCollection = 0;
        }
        chips.removeAllViews();
        chips.addView(Ui.chip(ctx(), "Semua", filterCollection == 0, v -> {
            filterCollection = 0;
            refreshAll();
        }));
        for (Collection c : cols) {
            chips.addView(Ui.chip(ctx(), c.name, filterCollection == c.id, v -> {
                filterCollection = c.id;
                refreshAll();
            }));
        }
    }

    private void refreshGrid() {
        List<Item> items = repo.items(filterCollection, "NOT_WISHLIST", query, sort);
        adapter.submit(items);
        boolean noData = repo.ownedCount() == 0;
        boolean showEmpty = noData && filterCollection == 0 && query.isEmpty();
        empty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        // dua pesan kosong tidak boleh tampil bersamaan
        noResult.setVisibility(items.isEmpty() && !showEmpty ? View.VISIBLE : View.GONE);
    }

    private void showSort() {
        hideKeyboard();
        NeoSheet s = new NeoSheet(ctx(), "Urutkan");
        for (int i = 0; i < SORT_LABELS.length; i++) {
            final int mode = i;
            s.addButton((sort == i ? "• " : "") + SORT_LABELS[i], sort == i ? 0 : 1, () -> {
                sort = mode;
                refreshGrid();
            });
        }
        s.show();
    }

    private void showMenu() {
        NeoSheet s = new NeoSheet(ctx(), "Menu");
        s.addButton("Wishlist", 1, () -> startActivity(new Intent().setClassName(ctx(), "id.kurasi.app.ui.WishlistActivity")));
        s.addButton("Pencapaian", 1, () -> startActivity(new Intent().setClassName(ctx(), "id.kurasi.app.ui.AchievementsActivity")));
        s.addButton("Kelola koleksi", 1, () -> startActivity(new Intent().setClassName(ctx(), "id.kurasi.app.ui.CollectionsActivity")));
        s.addButton("Pengaturan", 1, () -> startActivity(new Intent().setClassName(ctx(), "id.kurasi.app.ui.SettingsActivity")));
        s.show();
    }
}
