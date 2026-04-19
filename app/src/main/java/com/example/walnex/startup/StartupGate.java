package com.example.walnex.startup;

import android.content.Context;
import android.content.SharedPreferences;

public final class StartupGate {

    private static final String PREFS_NAME = "walnex_startup_gate";
    private static final String KEY_OVERRIDE_STATE = "override_state";

    private StartupGate() {
        // Utility class.
    }

    public static StartupState evaluate(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String override = preferences.getString(KEY_OVERRIDE_STATE, StartupState.OK.name());

        // Remote-config or backend health checks can be integrated here later.
        return StartupState.fromName(override);
    }
}
