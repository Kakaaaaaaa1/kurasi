package id.kurasi.app.ui;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import id.kurasi.app.data.Models.Item;
import id.kurasi.app.ui.view.CardRenderer;
import id.kurasi.app.ui.view.CardView;

/** Grid kartu mini di Binder. */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.VH> {
    public interface OnClick {
        void onClick(Item item);
    }

    private final List<Item> items = new ArrayList<>();
    private final OnClick click;

    public CardAdapter(OnClick click) {
        this.click = click;
    }

    public void submit(List<Item> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context c = parent.getContext();
        CardView v = new CardView(c);
        v.setCompact(true);
        v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = items.get(position);
        h.view.setData(CardRenderer.Data.from(h.view.getContext(), it, null));
        h.view.setOnClickListener(v -> click.onClick(it));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final CardView view;

        VH(CardView v) {
            super(v);
            view = v;
        }
    }
}
