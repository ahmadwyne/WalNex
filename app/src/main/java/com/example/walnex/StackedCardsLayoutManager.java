package com.example.walnex;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A vertical LinearLayoutManager subclass that gives the Cards screen its
 * stacked-deck appearance:
 *
 *  ┌──────────────────┐   ← card 0  (top of stack, fully visible)
 *  │                  │
 *  │                  │
 *  │                  │
 *  └──────────────────┘
 *    ┌──────────────────┐  ← card 1 peek  (PEEK_DP from top of card 0)
 *    ┌──────────────────┐  ← card 2 peek
 *
 * Each card sits PEEK_DP below the previous one.  When the user scrolls up,
 * the next card slides in from below and the previous one scrolls away.
 */
public class StackedCardsLayoutManager extends LinearLayoutManager {

    /** How many dp of each underlying card are visible below the one above it. */
    private static final int PEEK_DP = 44;

    private final int peekPx;

    public StackedCardsLayoutManager(Context context) {
        super(context, VERTICAL, false);
        peekPx = dpToPx(context, PEEK_DP);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Override item height so cards stack with a peek instead of scrolling
    //  fully off-screen before the next card appears.
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    /**
     * After default layout, we shift each child upward so that only PEEK_DP
     * of it remains below the card above, creating the stacked look.
     *
     * The scroll offset is handled by RecyclerView natively; we only adjust
     * how items are positioned relative to each other.
     */
    @Override
    public void layoutDecorated(View child, int left, int top, int right, int bottom) {
        int position = getPosition(child);
        // Each card is shifted up by (cardHeight - peekPx) × position
        int cardHeight = bottom - top;
        int shift = position * (cardHeight - peekPx);
        super.layoutDecorated(child, left, top - shift, right, bottom - shift);
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return getHeight();
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        int count = getItemCount();
        if (count == 0) return 0;
        // Total range = first card height + (N-1) * peekPx
        View first = findViewByPosition(0);
        int cardHeight = (first != null) ? first.getHeight() : dpToPx(getBasicContext(), 200);
        return cardHeight + (count - 1) * peekPx;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Context basicContext;

    public StackedCardsLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        peekPx = dpToPx(context, PEEK_DP);
        basicContext = context;
    }

    private Context getBasicContext() { return basicContext; }

    private static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics()));
    }
}