package com.local.smsrelay;

final class RetryPolicy {
    private static final long[] DELAYS_MS = {
            60_000L,
            2 * 60_000L,
            5 * 60_000L,
            15 * 60_000L,
            30 * 60_000L,
            60 * 60_000L,
            3 * 60 * 60_000L,
            6 * 60 * 60_000L
    };

    private RetryPolicy() {}

    static long delayAfterFailure(int attemptsAfterThisFailure) {
        int index = Math.max(0, Math.min(attemptsAfterThisFailure - 1, DELAYS_MS.length - 1));
        return DELAYS_MS[index];
    }
}
