package com.local.smsrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsRelay";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_USER_UNLOCKED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        Config.ensureInitialized(context);
        Scheduler.ensurePeriodic(context);
        PendingResult pending = goAsync();
        EXECUTOR.execute(() -> {
            try {
                if (Intent.ACTION_USER_UNLOCKED.equals(action)
                        || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                    SmsImporter.importRecent(context);
                }
                Scheduler.scheduleNext(context);
            } catch (RuntimeException e) {
                Log.w(TAG, "Deferred boot reconciliation: " + DeliveryEngine.safeError(e));
            } finally {
                pending.finish();
            }
        });
    }
}
