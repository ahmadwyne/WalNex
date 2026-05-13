package com.example.walnex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.CardViewHolder> {

    private static final String HIDDEN_NUMBER = "•••• ••••";

    // Maps cardType string → drawable resource
    private static final Map<String, Integer> CARD_DRAWABLES = new HashMap<>();
    static {
        CARD_DRAWABLES.put(CardModel.TYPE_COSMIC_PURPLE, R.drawable.bg_card_circles);
        CARD_DRAWABLES.put(CardModel.TYPE_EMERALD_GREEN, R.drawable.bg_card_green);
        CARD_DRAWABLES.put(CardModel.TYPE_OCEAN_BLUE,    R.drawable.bg_card_triangles);
        CARD_DRAWABLES.put(CardModel.TYPE_ROSE_WAVE,     R.drawable.bg_card_waves);
    }

    private static final int[] FALLBACK_BACKGROUNDS = {
            R.drawable.bg_card_circles,
            R.drawable.bg_card_waves,
            R.drawable.bg_card_green,
            R.drawable.bg_card_triangles
    };

    public interface OnCardClickListener {
        void onCardClick(CardsActivity.CardItem card, int backgroundRes);
    }

    private final List<CardsActivity.CardItem> cards;
    private final OnCardClickListener          listener;
    // Tracks which card positions currently have their number hidden
    private final Set<Integer> hiddenNumbers = new HashSet<>();

    public CardsAdapter(List<CardsActivity.CardItem> cards, OnCardClickListener listener) {
        this.cards    = cards;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardsActivity.CardItem card = cards.get(position);

        Integer bgRes = CARD_DRAWABLES.get(card.cardType);
        if (bgRes == null) bgRes = FALLBACK_BACKGROUNDS[position % FALLBACK_BACKGROUNDS.length];
        final int resolvedBg = bgRes;

        holder.imageCardBg.setImageResource(resolvedBg);
        holder.textName.setText(card.holderName);

        boolean hidden = hiddenNumbers.contains(position);
        holder.textNumber.setText(hidden ? HIDDEN_NUMBER : card.maskedNumber);
        holder.btnToggleNumber.setImageResource(
                hidden ? R.drawable.ic_eye_off : R.drawable.ic_eye);

        holder.btnToggleNumber.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            boolean nowHidden = !hiddenNumbers.contains(pos);
            if (nowHidden) hiddenNumbers.add(pos); else hiddenNumbers.remove(pos);
            holder.textNumber.setText(nowHidden ? HIDDEN_NUMBER : cards.get(pos).maskedNumber);
            holder.btnToggleNumber.setImageResource(
                    nowHidden ? R.drawable.ic_eye_off : R.drawable.ic_eye);
        });

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        holder.textBalance.setText(card.currency + " " + nf.format((long) card.balance));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClick(card, resolvedBg);
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    // ──────────────────────────────────────────────────────────────────────────

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageCardBg;
        final TextView  textName;
        final TextView  textNumber;
        final TextView  textBalance;
        final ImageView btnToggleNumber;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCardBg      = itemView.findViewById(R.id.imageCardBg);
            textName         = itemView.findViewById(R.id.textCardHolderName);
            textNumber       = itemView.findViewById(R.id.textCardNumber);
            textBalance      = itemView.findViewById(R.id.textCardBalance);
            btnToggleNumber  = itemView.findViewById(R.id.btnToggleCardNumber);
        }
    }
}
