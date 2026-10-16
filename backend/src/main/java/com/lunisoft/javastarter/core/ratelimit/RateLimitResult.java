package com.lunisoft.javastarter.core.ratelimit;

/**
 * Outcome of one hit on a rate-limit counter.
 *
 * @param count             number of hits recorded in the current window, this one included
 * @param limit             maximum number of hits allowed per window
 * @param resetAfterSeconds seconds until the window expires and the counter restarts from zero
 */
public record RateLimitResult(long count, long limit, long resetAfterSeconds) {

    /**
     * True once the window holds more hits than allowed — the current request must be rejected.
     */
    public boolean isExceeded() {
        return count > limit;
    }

    /**
     * Hits still allowed in the current window (never negative).
     */
    public long remaining() {
        return Math.max(0, limit - count);
    }
}
