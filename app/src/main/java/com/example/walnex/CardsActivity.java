package com.example.walnex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.firestore.DocumentSnapshot;

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
        public final String cardType;

        public CardItem(String holderName, String maskedNumber,
                        double balance, String currency, String cardType) {
            this.holderName   = holderName;
            this.maskedNumber = maskedNumber;
            this.balance      = balance;
            this.currency     = currency;
            this.cardType     = cardType;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────────────────────────────────

    private ViewPager2   viewPagerCards;
    private View         viewEmptyState;
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCards();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Bind
    // ──────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        viewPagerCards = findViewById(R.id.viewPagerCards);
        viewEmptyState = findViewById(R.id.viewEmptyState);
        navHome        = findViewById(R.id.navHome);
        navHistory     = findViewById(R.id.navHistory);
        navCards       = findViewById(R.id.navCards);
        navMore        = findViewById(R.id.navMore);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Add card button → launches AddNewCardActivity
    // ──────────────────────────────────────────────────────────────────────────

    private void setupAddCard() {
        TextView textAddCard = findViewById(R.id.textAddCard);
        if (textAddCard != null) {
            textAddCard.setOnClickListener(v ->
                    startActivity(new Intent(this, AddNewCardActivity.class)));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Load cards from Firestore
    // ──────────────────────────────────────────────────────────────────────────

    private void loadCards() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        new CardRepository(user.getUid()).getCards(
                snapshot -> {
                    List<CardItem> cards = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        CardModel model = doc.toObject(CardModel.class);
                        if (model == null) continue;

                        String name      = model.getHolderName() != null  ? model.getHolderName()  : "Card Holder";
                        String lastFour  = model.getLastFour()   != null  ? model.getLastFour()    : "0000";
                        String currency  = model.getCurrency()   != null  ? model.getCurrency()    : "PKR";
                        String cardType  = model.getCardType()   != null  ? model.getCardType()    : CardModel.TYPE_COSMIC_PURPLE;

                        cards.add(new CardItem(name, "**** " + lastFour,
                                model.getBalance(), currency, cardType));
                    }

                    showEmptyState(cards.isEmpty());
                    if (!cards.isEmpty()) setupViewPager(cards);
                },
                error -> Toast.makeText(this, getString(R.string.add_card_error_load),
                        Toast.LENGTH_SHORT).show()
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Empty state
    // ──────────────────────────────────────────────────────────────────────────

    private void showEmptyState(boolean empty) {
        if (viewEmptyState != null) {
            viewEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        viewPagerCards.setVisibility(empty ? View.GONE : View.VISIBLE);
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
        viewPagerCards.setOffscreenPageLimit(3);

        viewPagerCards.setPageTransformer((page, position) -> {

            if (position <= -1f || position >= 1f) {
                page.setAlpha(0f);
                return;
            }

            page.setAlpha(1f);
            page.setTranslationZ(0f);

            if (position <= 0f) {
                page.setTranslationY(0f);
                page.setScaleX(1f);
                page.setScaleY(1f);
            } else {
                float pageHeight  = page.getHeight();
                float translationY = -(pageHeight * 0.82f) * position;
                float scale        = 1f - (0.04f * position);

                page.setTranslationY(translationY);
                page.setScaleX(Math.max(scale, 0.88f));
                page.setScaleY(Math.max(scale, 0.88f));
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

        if (navCards != null) navCards.setOnClickListener(v -> { });

        if (navMore != null) navMore.setOnClickListener(v -> {
            startActivity(new Intent(this, MoreActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}