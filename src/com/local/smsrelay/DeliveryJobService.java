package com.local.smsrelay;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.Network;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DeliveryJobService extends JobService {
    private static final String TAG = "SmsRelay";
    private static final long JOB_FAILURE_RETRY_MS = 5L * 60 * 1000;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<JobParameters, AtomicBoolean> cancellations =
            new ConcurrentHashMap<>();

    @Override
    public boolean onStartJob(JobParameters params) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancellations.put(params, cancelled);
        final int jobId = params.getJobId();
        final Network scheduledNetwork = Build.VERSION.SDK_INT >= 28
                ? params.getNetwork() : null;
        final long startGeneration = Scheduler.workGeneration();
        Log.i(TAG, "Delivery job started: " + jobId);
        EXECUTOR.execute(() -> {
            boolean retryJob = false;
            long observedGeneration = startGeneration;
            try {
                Network wifi = WifiNetwork.findValidated(this, scheduledNetwork);
                if (wifi == null) {
                    retryJob = true;
                    Log.w(TAG, "Delivery job has no validated Wi-Fi network");
                    return;
                }
                boolean periodic = params.getExtras().getBoolean("periodic", false);
                if (periodic) {
                    SmsImporter.importRecent(this);
                }
                observedGeneration = Scheduler.workGeneration();
                DeliveryEngine engine = new DeliveryEngine(this, wifi);
                engine.processQueue(cancelled::get);
                if (periodic && !cancelled.get()) {
                    engine.sendHeartbeatIfDue(cancelled::get);
                }
                SmsStore store = SmsStore.get(this);
                store.cleanup();
            } catch (RuntimeException e) {
                retryJob = true;
                Log.e(TAG, "Delivery job failed: " + DeliveryEngine.safeError(e));
            } finally {
                cancellations.remove(params, cancelled);
                if (!cancelled.get()) {
                    if (Scheduler.workGeneration() != observedGeneration) {
                        Scheduler.scheduleAfterRun(this, 0L);
                    } else if (retryJob) {
                        Scheduler.scheduleAfterRun(this, JOB_FAILURE_RETRY_MS);
                    } else {
                        Scheduler.scheduleNextAfterRun(this);
                    }
                    Log.i(TAG, "Delivery job finished: " + jobId);
                    jobFinished(params, false);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        AtomicBoolean cancelled = cancellations.remove(params);
        if (cancelled != null) {
            cancelled.set(true);
        }
        Log.w(TAG, "Delivery job stopped by system: " + params.getJobId());
        return true;
    }

    @Override
    public void onNetworkChanged(JobParameters params) {
        // Each connection reuses the exact validated Wi-Fi Network selected for this job.
    }
}
