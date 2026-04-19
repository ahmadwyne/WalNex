package com.example.walnex.startup;

import android.content.Context;

import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {

    public StartupState resolveStartupState(Context context) {
        return StartupGate.evaluate(context);
    }

    public LaunchDestination resolveLaunchDestination(boolean isSignedIn, StartupState startupState) {
        if (startupState == StartupState.UPDATE_REQUIRED || startupState == StartupState.MAINTENANCE) {
            return LaunchDestination.STARTUP_BLOCKING;
        }

        return isSignedIn ? LaunchDestination.HOME : LaunchDestination.WELCOME;
    }
}
