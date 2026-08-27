package com.local.smsrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsRelay";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }
        PendingResult pendingResult = goAsync();
        EXECUTOR.execute(() -> {
            try {
                SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                if (messages == null || messages.length == 0) {
                    return;
                }
                String sender = messages[0].getDisplayOriginatingAddress();
                StringBuilder body = new StringBuilder();
                for (SmsMessage message : messages) {
                    if (message != null && message.getDisplayMessageBody() != null) {
                        body.append(message.getDisplayMessageBody());
                    }
                }
                long timestamp = messages[0].getTimestampMillis();
                if (timestamp <= 0L) {
                    timestamp = System.currentTimeMillis();
                }
                int simSlot = firstIntExtra(intent, "slot_id", "slot", "phone", "simId");
                if (SmsStore.get(context).insert(sender, body.toString(), timestamp, simSlot)) {
                    Scheduler.scheduleImmediate(context);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "SMS queue insertion failed; inbox reconciliation will retry", e);
            } finally {
                pendingResult.finish();
            }
        });
    }

    private static int firstIntExtra(Intent intent, String... names) {
        for (String name : names) {
            if (intent.hasExtra(name)) {
                return intent.getIntExtra(name, -1);
            }
        }
        return -1;
    }
}
