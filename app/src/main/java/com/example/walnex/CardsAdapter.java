package com.example.walnex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the stacked card list on CardsActivity.
 * Each card gets one of four background drawables in rotation.
 */
public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.CardViewHolder> {

    // ── Background drawables in the rotation order (green → waves → circles → triangles) ──
    private static final int[] CARD_BACKGROUNDS = {
            R.drawable.bg_card_circles,
            R.drawable.bg_card_waves,
            R.drawable.bg_card_green,
            R.drawable.bg_card_triangles
    };

    public interface OnCardClickListener {
        void onCardClick(CardsActivity.CardItem card, int backgroundRes);
    }

    private final List<CardsActivity.CardItem> cards;
    private final OnCardClickListener listener;

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
        int bgRes = CARD_BACKGROUNDS[position % CARD_BACKGROUNDS.length];

        holder.imageCardBg.setImageResource(bgRes);
        holder.textName.setText(card.holderName);
        holder.textNumber.setText(card.maskedNumber);

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        holder.textBalance.setText(card.currency + " " + nf.format((long) card.balance));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClick(card, bgRes);
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

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCardBg  = itemView.findViewById(R.id.imageCardBg);
            textName     = itemView.findViewById(R.id.textCardHolderName);
            textNumber   = itemView.findViewById(R.id.textCardNumber);
            textBalance  = itemView.findViewById(R.id.textCardBalance);
        }
    }
}