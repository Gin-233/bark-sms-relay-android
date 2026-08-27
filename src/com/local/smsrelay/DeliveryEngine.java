package com.local.smsrelay;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BooleanSupplier;

final class DeliveryEngine {
    private static final String TAG = "SmsRelay";
    private static final long NOT_CONFIGURED_DELAY_MS = 6L * 60 * 60 * 1000;
    private static final long HEARTBEAT_INTERVAL_MS = 24L * 60 * 60 * 1000;

    private final Context context;
    private final Config config;
    private final SmsStore store;
    private final Network wifiNetwork;

    DeliveryEngine(Context context, Network wifiNetwork) {
        if (wifiNetwork == null) {
            throw new IllegalArgumentException("Validated Wi-Fi network is required");
        }
        this.context = context.getApplicationContext();
        this.config = new Config(context);
        this.store = SmsStore.get(context);
        this.wifiNetwork = wifiNetwork;
    }

    void processQueue(BooleanSupplier cancelled) {
        if (!config.enabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<SmsRecord> records = store.due(now, 20);
        for (SmsRecord record : records) {
            if (cancelled.getAsBoolean()) {
                return;
            }
            String text = DeliveryText.smsBody(record);
            if (!record.barkSent) {
                deliverBark(record, text, now);
            }
        }
    }

    void sendHeartbeatIfDue(BooleanSupplier cancelled) {
        if (!config.enabled() || cancelled.getAsBoolean()) {
            return;
        }
        long now = System.currentTimeMillis();
        String body = heartbeatBody();
        String dailyId = "heartbeat-" + LocalDate.now();

        if (config.barkReady()
                && now - config.lastBarkHeartbeatAt() >= HEARTBEAT_INTERVAL_MS) {
            try {
                new BarkClient(config).sendOnNetwork(wifiNetwork,
                        "短信安全转发 · 每日心跳", body,
                        "转发状态", "passive", dailyId);
                config.setLastBarkHeartbeatAt(now);
            } catch (Exception e) {
                Log.w(TAG, "Bark heartbeat failed: " + safeError(e));
            }
        }
    }

    private void deliverBark(SmsRecord record, String text, long now) {
        if (!config.barkReady()) {
            store.deferBark(record.id, now + NOT_CONFIGURED_DELAY_MS);
            return;
        }
        try {
            new BarkClient(config).sendOnNetwork(wifiNetwork,
                    DeliveryText.smsTitle(record), text,
                    "短信", "timeSensitive", record.id);
            store.markBarkSuccess(record.id);
        } catch (Exception e) {
            int attempts = record.barkAttempts + 1;
            store.markBarkFailure(record.id, attempts,
                    now + RetryPolicy.delayAfterFailure(attempts), safeError(e));
        }
    }

    private String heartbeatBody() {
        BatteryManager battery = context.getSystemService(BatteryManager.class);
        int percent = battery == null ? -1
                : battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return DeliveryText.heartbeatBody(
                store.pendingCount(), percent, networkDescription(),
                SystemClock.elapsedRealtime());
    }

    private String networkDescription() {
        ConnectivityManager connectivity = context.getSystemService(ConnectivityManager.class);
        if (connectivity == null) {
            return "未知";
        }
        NetworkCapabilities caps = connectivity.getNetworkCapabilities(wifiNetwork);
        if (caps == null) {
            return "未连接";
        }
        String type;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            type = "Wi-Fi";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            type = "移动网络";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            type = "以太网";
        } else {
            type = "其他网络";
        }
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                ? type + "（非计费）" : type + "（计费）";
    }

    static String safeError(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = current.getClass().getSimpleName();
        }
        message = message.replace('\r', ' ').replace('\n', ' ');
        return message.length() <= 220 ? message : message.substring(0, 220);
    }
}
