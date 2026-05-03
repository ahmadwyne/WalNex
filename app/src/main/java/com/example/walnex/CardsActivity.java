package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class CardsActivity extends AppCompatActivity {

    // ──────────────────────────────────────────────────────────────────────────
    //  Data model
    // ──────────────────────────────────────────────────────────────────────────

    public static class CardItem {
        public final String holderName;
        public final String maskedNumber;
        public final double balance;
        public final String currency;

        public CardItem(String holderName, String maskedNumber,
                        double balance, String currency) {
            this.holderName   = holderName;
            this.maskedNumber = maskedNumber;
            this.balance      = balance;
            this.currency     = currency;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────────────────────────────────

    private ViewPager2   viewPagerCards;
    private LinearLayout navHome, navHistory, navCards, navMore;

    // ──────────────────────────────────────────────────────────────────────────
    //  Factory
    // ──────────────────────────────────────────────────────────────────────────

    public static Intent newIntent(Context context) {
        return new Intent(context, CardsActivity.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cards);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.cardsRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, 0);
                    return insets;
                });

        bindViews();
        setupAddCard();
        setupBottomNav();
        loadCards();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Bind
    // ──────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        viewPagerCards = findViewById(R.id.viewPagerCards);
        navHome        = findViewById(R.id.navHome);
        navHistory     = findViewById(R.id.navHistory);
        navCards       = findViewById(R.id.navCards);
        navMore        = findViewById(R.id.navMore);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Add card button
    // ──────────────────────────────────────────────────────────────────────────

    private void setupAddCard() {
        TextView textAddCard = findViewById(R.id.textAddCard);
        if (textAddCard != null) {
            textAddCard.setOnClickListener(v ->
                    Toast.makeText(this, "Add card – coming soon", Toast.LENGTH_SHORT).show());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Load cards
    // ──────────────────────────────────────────────────────────────────────────

    private void loadCards() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String defaultName = (user != null && user.getDisplayName() != null)
                ? user.getDisplayName()
                : "Abdullah Ghatashe";

        // TODO: Replace with real Firestore fetch from users/{uid}/cards
        List<CardItem> cards = buildSampleCards(defaultName);
        setupViewPager(cards);
    }

    private List<CardItem> buildSampleCards(String holderName) {
        List<CardItem> list = new ArrayList<>();
        list.add(new CardItem(holderName, "**** 2312",  2354.00, "PKR"));
        list.add(new CardItem(holderName, "**** 5432", 12800.50, "PKR"));
        list.add(new CardItem(holderName, "**** 3245",  4750.75, "PKR"));
        list.add(new CardItem(holderName, "**** 7891",  9100.00, "PKR"));
        return list;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  ViewPager2 + stacked card PageTransformer
    // ──────────────────────────────────────────────────────────────────────────

    private void setupViewPager(List<CardItem> cards) {

        CardsAdapter adapter = new CardsAdapter(cards, (card, bgRes) -> {
            Intent intent = CardPaymentActivity.newIntent(
                    this,
                    card.holderName,
                    card.maskedNumber,
                    card.balance,
                    card.currency,
                    bgRes);
            startActivity(intent);
        });

        viewPagerCards.setAdapter(adapter);

        // Keep the 2 adjacent pages laid out so PageTransformer can position them
        viewPagerCards.setOffscreenPageLimit(3);

        // ── Stacked-card PageTransformer ──────────────────────────────────────
        //
        //  How it works:
        //  • position == 0  → selected card  : full size, no translation, full alpha
        //  • position == -1 → previous card  : peek above (translated up + scaled down)
        //  • position == +1 → next card      : peek above (translated up more + scaled down)
        //
        //  All cards that are "behind" the selected one are translated upward so
        //  that only their top edge (name + number row) peeks above the selected card.
        //  The selected card swipe reveals the next card beneath it.
        // ─────────────────────────────────────────────────────────────────────

        // ── Stacked-card PageTransformer ──────────────────────────────────────
        //
        //  Pages are match_parent height. Each page's CardView sits at the
        //  bottom of the page frame. For cards that are "behind" (position > 0),
        //  we translate the entire page UP so only its top strip (name + number)
        //  peeks above the active card.
        //
        //  PEEK_DP = how many dp of the behind-card's top strip remain visible.
        //  We translate the page up by (pageHeight - cardHeight - marginBottom - peekPx).
        //  Since we can't know exact px at transformer time, we use a fraction of
        //  the page height as the offset: each behind-card shifts up by ~85% of
        //  the page height, leaving ~15% (the card top strip) visible.
        // ─────────────────────────────────────────────────────────────────────

        viewPagerCards.setPageTransformer((page, position) -> {

            if (position <= -1f || position >= 1f) {
                page.setAlpha(0f);
                return;
            }

            page.setAlpha(1f);
            page.setTranslationZ(0f); // reset

            if (position <= 0f) {
                // Active card (0) and card swiping off to the left (< 0):
                // slide normally, no stacking
                page.setTranslationY(0f);
                page.setScaleX(1f);
                page.setScaleY(1f);

            } else {
                // Behind cards (position > 0): stack them upward.
                // Translate each one up so only ~44dp of its top peeks out.
                // page.getHeight() == pager height (360dp in the layout).
                // CardView is 200dp + 16dp marginBottom = 216dp from bottom.
                // So the card top is at: pageHeight - 216dp from the top of the page.
                // We want to show PEEK_DP (44dp) of the card top above the active card.
                // => translationY = -(pageHeight - 216dp - PEEK_DP) * position
                //
                // In practice, use a simpler relative formula that looks right:
                //   shift each behind-card up by (pageHeight * 0.82 * position)
                // This leaves ~18% of the page at the top = ≈65dp = card top strip visible.
                float pageHeight  = page.getHeight();
                float translationY = -(pageHeight * 0.82f) * position;
                float scale        = 1f - (0.04f * position);

                page.setTranslationY(translationY);
                page.setScaleX(Math.max(scale, 0.88f));
                page.setScaleY(Math.max(scale, 0.88f));
                // Cards further back render behind cards in front
                page.setTranslationZ(-position * 10f);
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Bottom navigation
    // ──────────────────────────────────────────────────────────────────────────

    private void setupBottomNav() {

        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        if (navHistory != null) navHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        // current tab – no-op
        if (navCards != null) navCards.setOnClickListener(v -> { });

        if (navMore != null) navMore.setOnClickListener(v -> {
            startActivity(new Intent(this, MoreActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}