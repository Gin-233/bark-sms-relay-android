package com.local.smsrelay;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public final class RelayApp extends Application {
    private static final String TAG = "SmsRelay";

    @Override
    public void onCreate() {
        super.onCreate();
        Config.ensureInitialized(this);
        Scheduler.ensurePeriodic(this);
        try {
            Scheduler.scheduleNext(this);
        } catch (RuntimeException e) {
            Log.w(TAG, "Deferred queue initialization: " + DeliveryEngine.safeError(e));
        }
    }

    static Context deviceContext(Context context) {
        return context.getApplicationContext().createDeviceProtectedStorageContext();
    }
}
