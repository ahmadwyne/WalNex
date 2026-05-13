package com.example.walnex;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.Arrays;
import java.util.List;

public class CardDesignAdapter extends RecyclerView.Adapter<CardDesignAdapter.DesignViewHolder> {

    public static class CardDesign {
        public final String type;
        public final String name;
        public final int    drawableRes;

        public CardDesign(String type, String name, int drawableRes) {
            this.type        = type;
            this.name        = name;
            this.drawableRes = drawableRes;
        }
    }

    public static final List<CardDesign> DESIGNS = Arrays.asList(
            new CardDesign(CardModel.TYPE_COSMIC_PURPLE, "Cosmic Purple", R.drawable.bg_card_circles),
            new CardDesign(CardModel.TYPE_EMERALD_GREEN, "Emerald Green", R.drawable.bg_card_green),
            new CardDesign(CardModel.TYPE_OCEAN_BLUE,    "Ocean Blue",    R.drawable.bg_card_triangles),
            new CardDesign(CardModel.TYPE_ROSE_WAVE,     "Rose Wave",     R.drawable.bg_card_waves)
    );

    public interface OnDesignSelectedListener {
        void onDesignSelected(CardDesign design);
    }

    private int selectedPosition = 0;
    private final OnDesignSelectedListener listener;

    public CardDesignAdapter(OnDesignSelectedListener listener) {
        this.listener = listener;
    }

    public CardDesign getSelectedDesign() {
        return DESIGNS.get(selectedPosition);
    }

    @NonNull
    @Override
    public DesignViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_design_option, parent, false);
        return new DesignViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DesignViewHolder holder, int position) {
        CardDesign design = DESIGNS.get(position);

        holder.imageDesignBg.setImageResource(design.drawableRes);
        holder.textDesignName.setText(design.name);

        int strokePx = position == selectedPosition
                ? dpToPx(3, holder.itemView.getResources())
                : 0;
        holder.cardOption.setStrokeWidth(strokePx);

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onDesignSelected(DESIGNS.get(selectedPosition));
        });
    }

    @Override
    public int getItemCount() {
        return DESIGNS.size();
    }

    private static int dpToPx(int dp, Resources res) {
        return Math.round(dp * res.getDisplayMetrics().density);
    }

    static class DesignViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardOption;
        final ImageView        imageDesignBg;
        final TextView         textDesignName;

        DesignViewHolder(@NonNull View itemView) {
            super(itemView);
            cardOption     = itemView.findViewById(R.id.cardDesignOption);
            imageDesignBg  = itemView.findViewById(R.id.imageDesignBg);
            textDesignName = itemView.findViewById(R.id.textDesignName);
        }
    }
}