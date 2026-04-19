package com.example.walnex.ui;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import androidx.core.content.ContextCompat;

import com.example.walnex.R;

public final class BrandingUtils {

    private static final ArgbEvaluator COLOR_EVALUATOR = new ArgbEvaluator();

    private BrandingUtils() {
        // Utility class.
    }

    public static CharSequence alternatingWordmark(Context context, String source, int visibleChars) {
        return signatureWordmark(context, source, visibleChars);
    }

    public static CharSequence signatureWordmark(Context context, String source, int visibleChars) {
        if (source == null || source.isEmpty()) {
            return "";
        }

        int safeLength = Math.max(0, Math.min(visibleChars, source.length()));
        if (safeLength == 0) {
            return "";
        }

        String visibleText = source.substring(0, safeLength);
        SpannableString styledText = new SpannableString(visibleText);

        int gradientStart = ContextCompat.getColor(context, R.color.white);
        int gradientEnd = ContextCompat.getColor(context, R.color.splash_text_secondary);
        int signatureAccent = ContextCompat.getColor(context, R.color.action_link);

        int length = visibleText.length();

        for (int index = 0; index < length; index++) {
            float fraction = (length == 1) ? 0f : (float) index / (float) (length - 1);
            int color = (int) COLOR_EVALUATOR.evaluate(fraction, gradientStart, gradientEnd);
            styledText.setSpan(
                new ForegroundColorSpan(color),
                index,
                index + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        int signatureIndex = visibleText.indexOf('N');
        if (signatureIndex < 0) {
            signatureIndex = visibleText.indexOf('n');
        }

        if (signatureIndex >= 0) {
            styledText.setSpan(
                new ForegroundColorSpan(signatureAccent),
                signatureIndex,
                signatureIndex + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            styledText.setSpan(
                new StyleSpan(Typeface.BOLD),
                signatureIndex,
                signatureIndex + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            styledText.setSpan(
                new RelativeSizeSpan(1.08f),
                signatureIndex,
                signatureIndex + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        return styledText;
    }

    public static CharSequence alternatingWordmark(Context context, String source) {
        return signatureWordmark(context, source, source != null ? source.length() : 0);
    }

    public static CharSequence signatureWordmark(Context context, String source) {
        return signatureWordmark(context, source, source != null ? source.length() : 0);
    }
}
