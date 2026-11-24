/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.evaluator;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

/**
 * Hides the response-time difference between authentication failures that did the full work
 * (existing user, wrong password) and failures that returned early (unknown, disabled or locked
 * user). Early failures are delayed by roughly the average duration of recent full attempts,
 * so an attacker cannot tell from the timing whether a username exists.
 *
 * The delay is randomized between 90 and 110 percent of the average, so the padded responses
 * do not all take exactly the same time.
 */
@Component
public class AuthenticationTimingEqualizer {

    /** Number of most recent login durations the average is computed from. */
    static final int WINDOW_SIZE = 100;

    /** Assumed login duration until the first real login is recorded. */
    static final long DEFAULT_LOGIN_DURATION_MILLIS = 500;

    private final long[] durationsNanos = new long[WINDOW_SIZE];
    private int nextIndex;
    private int count;

    private final SecureRandom random = new SecureRandom();

    /** Records the duration of an attempt that performed the full work, i.e. reached the password check. */
    public synchronized void recordLogin(long durationNanos) {
        if (durationNanos <= 0) {
            return;
        }
        durationsNanos[nextIndex] = durationNanos;
        nextIndex = (nextIndex + 1) % WINDOW_SIZE;
        if (count < WINDOW_SIZE) {
            count++;
        }
    }

    /** Sleeps for roughly the duration of a full login attempt. */
    public void delayFailure() {
        try {
            TimeUnit.NANOSECONDS.sleep(computeDelayNanos());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    long computeDelayNanos() {
        long average = averageDurationNanos();
        double factor = 0.9 + 0.2 * random.nextDouble();
        return (long) (average * factor);
    }

    private synchronized long averageDurationNanos() {
        if (count == 0) {
            return TimeUnit.MILLISECONDS.toNanos(DEFAULT_LOGIN_DURATION_MILLIS);
        }
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += durationsNanos[i];
        }
        return sum / count;
    }
}
