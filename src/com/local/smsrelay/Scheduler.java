package com.local.smsrelay;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.concurrent.atomic.AtomicLong;

final class Scheduler {
    private static final String TAG = "SmsRelay";
    private static final String EXTRA_PERIODIC = "periodic";
    private static final String EXTRA_DUE_AT = "due_at";
    static final int JOB_DELIVERY = 41021;
    static final int JOB_PERIODIC = 41022;
    private static final long PERIOD_MS = 6L * 60 * 60 * 1000;
    private static final long FLEX_MS = 60L * 60 * 1000;
    private static final AtomicLong WORK_GENERATION = new AtomicLong();

    private Scheduler() {}

    static void scheduleImmediate(Context context) {
        WORK_GENERATION.incrementAndGet();
        if (!new Config(context).enabled()) {
            cancelDelivery(context);
            return;
        }
        scheduleAtInternal(context, 0L, false);
    }

    static void scheduleAt(Context context, long delayMs) {
        scheduleAtInternal(context, delayMs, false);
    }

    static void scheduleNext(Context context) {
        if (!new Config(context).enabled()) {
            cancelDelivery(context);
            return;
        }
        long earliest = SmsStore.get(context).earliestDue();
        if (earliest >= 0L) {
            scheduleAtInternal(context,
                    Math.max(0L, earliest - System.currentTimeMillis()), false);
        } else {
            cancelDelivery(context);
        }
    }

    static void cancelDelivery(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler != null) {
            scheduler.cancel(JOB_DELIVERY);
        }
    }

    static void scheduleNextAfterRun(Context context) {
        if (!new Config(context).enabled()) {
            return;
        }
        long earliest = SmsStore.get(context).earliestDue();
        if (earliest >= 0L) {
            scheduleAtInternal(context,
                    Math.max(0L, earliest - System.currentTimeMillis()), true);
        }
    }

    static void scheduleAfterRun(Context context, long delayMs) {
        if (!new Config(context).enabled()) {
            return;
        }
        scheduleAtInternal(context, delayMs, true);
    }

    static long workGeneration() {
        return WORK_GENERATION.get();
    }

    private static void scheduleAtInternal(Context context, long delayMs, boolean force) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) {
            return;
        }
        long safeDelay = Math.max(0L, delayMs);
        long now = System.currentTimeMillis();
        long dueAt = safeDelay > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + safeDelay;

        JobInfo existing = scheduler.getPendingJob(JOB_DELIVERY);
        if (!force && existing != null && hasWifiRequirement(existing)
                && !existing.getExtras().getBoolean(EXTRA_PERIODIC, false)) {
            long existingDueAt = existing.getExtras().getLong(EXTRA_DUE_AT, 0L);
            if (!SchedulePolicy.shouldReplace(existingDueAt, dueAt)) {
                return;
            }
        }

        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean(EXTRA_PERIODIC, false);
        extras.putLong(EXTRA_DUE_AT, dueAt);
        JobInfo.Builder builder = new JobInfo.Builder(
                JOB_DELIVERY, new ComponentName(context, DeliveryJobService.class))
                .setMinimumLatency(safeDelay)
                .setPersisted(true)
                .setExtras(extras);
        requireWifi(builder);
        if (scheduler.schedule(builder.build()) != JobScheduler.RESULT_SUCCESS) {
            Log.e(TAG, "Unable to schedule delivery job");
        }
    }

    static void ensurePeriodic(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) {
            return;
        }
        JobInfo existing = scheduler.getPendingJob(JOB_PERIODIC);
        if (existing != null && hasWifiRequirement(existing)) {
            return;
        }
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean(EXTRA_PERIODIC, true);
        JobInfo.Builder builder = new JobInfo.Builder(
                JOB_PERIODIC, new ComponentName(context, DeliveryJobService.class))
                .setPeriodic(PERIOD_MS, FLEX_MS)
                .setPersisted(true)
                .setExtras(extras);
        requireWifi(builder);
        if (scheduler.schedule(builder.build()) != JobScheduler.RESULT_SUCCESS) {
            Log.e(TAG, "Unable to schedule periodic job");
        }
    }

    private static void requireWifi(JobInfo.Builder builder) {
        if (Build.VERSION.SDK_INT >= 28) {
            builder.setRequiredNetwork(new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build());
        } else {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        }
    }

    private static boolean hasWifiRequirement(JobInfo info) {
        if (Build.VERSION.SDK_INT >= 28) {
            NetworkRequest request = info.getRequiredNetwork();
            return request != null
                    && request.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return info.getNetworkType() == JobInfo.NETWORK_TYPE_UNMETERED;
    }
}
