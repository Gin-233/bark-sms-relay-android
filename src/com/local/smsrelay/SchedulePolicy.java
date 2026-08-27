package com.local.smsrelay;

final class SchedulePolicy {
    private static final long COALESCE_WINDOW_MS = 1_000L;

    private SchedulePolicy() {}

    static boolean shouldReplace(long existingDueAt, long requestedDueAt) {
        if (existingDueAt <= 0L) {
            return true;
        }
        return requestedDueAt + COALESCE_WINDOW_MS < existingDueAt;
    }
}
