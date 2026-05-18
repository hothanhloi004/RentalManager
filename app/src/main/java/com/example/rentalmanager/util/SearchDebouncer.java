package com.example.rentalmanager.util;

import android.os.Handler;
import android.os.Looper;

public final class SearchDebouncer {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long delayMillis;
    private Runnable pendingAction;

    public SearchDebouncer() {
        this(250L);
    }

    public SearchDebouncer(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void submit(Runnable action) {
        cancel();
        pendingAction = action;
        handler.postDelayed(action, delayMillis);
    }

    public void cancel() {
        if (pendingAction != null) {
            handler.removeCallbacks(pendingAction);
            pendingAction = null;
        }
    }
}
