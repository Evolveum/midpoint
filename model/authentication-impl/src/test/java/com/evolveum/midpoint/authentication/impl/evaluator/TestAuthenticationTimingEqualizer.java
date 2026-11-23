/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

/**
 * Unit test for {@link AuthenticationTimingEqualizer}.
 */
public class TestAuthenticationTimingEqualizer {

    @Test
    public void test100DefaultDelayWithoutRecordedLogins() {
        AuthenticationTimingEqualizer equalizer = new AuthenticationTimingEqualizer();

        long delay = equalizer.computeDelayNanos();

        assertWithinTenPercentOf(delay, ms(AuthenticationTimingEqualizer.DEFAULT_LOGIN_DURATION_MILLIS));
    }

    @Test
    public void test110DelayAroundAverageOfRecordedLogins() {
        AuthenticationTimingEqualizer equalizer = new AuthenticationTimingEqualizer();
        equalizer.recordLogin(ms(600));
        equalizer.recordLogin(ms(1000));

        for (int i = 0; i < 1000; i++) {
            assertWithinTenPercentOf(equalizer.computeDelayNanos(), ms(800));
        }
    }

    @Test
    public void test120DelayIsRandomized() {
        AuthenticationTimingEqualizer equalizer = new AuthenticationTimingEqualizer();
        equalizer.recordLogin(ms(800));

        long first = equalizer.computeDelayNanos();
        boolean differs = false;
        for (int i = 0; i < 100 && !differs; i++) {
            differs = equalizer.computeDelayNanos() != first;
        }

        assertThat(differs).as("delay varies between calls").isTrue();
    }

    @Test
    public void test130OnlyLastWindowOfLoginsCounts() {
        AuthenticationTimingEqualizer equalizer = new AuthenticationTimingEqualizer();
        equalizer.recordLogin(ms(10_000)); // will be pushed out of the window
        for (int i = 0; i < AuthenticationTimingEqualizer.WINDOW_SIZE; i++) {
            equalizer.recordLogin(ms(800));
        }

        assertWithinTenPercentOf(equalizer.computeDelayNanos(), ms(800));
    }

    @Test
    public void test140NonPositiveDurationsIgnored() {
        AuthenticationTimingEqualizer equalizer = new AuthenticationTimingEqualizer();
        equalizer.recordLogin(0);
        equalizer.recordLogin(-1);

        assertWithinTenPercentOf(equalizer.computeDelayNanos(), ms(AuthenticationTimingEqualizer.DEFAULT_LOGIN_DURATION_MILLIS));
    }

    @Test
    public void test150InterruptedSleepRestoresInterruptFlag() {
        AuthenticationTimingEqualizer equalizer = new AuthenticationTimingEqualizer();
        Thread.currentThread().interrupt(); // sleep throws immediately

        equalizer.delayFailure();

        assertThat(Thread.interrupted()).as("interrupt flag restored").isTrue();
    }

    private static void assertWithinTenPercentOf(long delay, long expected) {
        assertThat(delay).isBetween(expected * 9 / 10, expected * 11 / 10);
    }

    private static long ms(long millis) {
        return TimeUnit.MILLISECONDS.toNanos(millis);
    }
}
