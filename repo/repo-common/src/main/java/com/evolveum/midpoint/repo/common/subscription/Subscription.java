/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * Parsed form of the subscription ID string.
 *
 * It is independent of the current system time and/or the system features used (like the database, clustering, and so on).
 * Hence, the subscription can be e.g. expired. Please see {@link SubscriptionState} to obtain the "real" validity
 * of this subscription in given time point and system feature set.
 *
 * @see SubscriptionState
 */
public class Subscription implements Serializable {

    private static final Subscription NONE = new Subscription(Type.NONE, null);
    private static final Subscription INVALID = new Subscription(Type.INVALID, null);

    @NotNull private final Type type;

    /** Always non-null if {@link #isValid()} is `true`. */
    @Nullable private final TimeValidity timeValidity;

    private Subscription(@NotNull Type type, @Nullable TimeValidity timeValidity) {
        this.type = type;
        this.timeValidity = timeValidity;
    }

    public static Subscription none() {
        return NONE;
    }

    public static Subscription invalid() {
        return INVALID;
    }

    public static Subscription parse(String stringValue) {
        if (StringUtils.isEmpty(stringValue)) {
            return none();
        }
        if (!NumberUtils.isDigits(stringValue)) {
            return invalid();
        }
        if (stringValue.length() < 11) {
            return invalid();
        }

        try {
            // Let us check the correctness first.
            VerhoeffCheckDigit checkDigit = new VerhoeffCheckDigit();
            if (!checkDigit.isValid(stringValue)) {
                return invalid();
            }

            return new Subscription(
                    Type.parse(stringValue.substring(0, 2)),
                    TimeValidity.parse(stringValue.substring(2, 6)));

        } catch (Exception ex) {
            return invalid();
        }
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "type=" + type +
                ", timeValidity=" + timeValidity +
                '}';
    }

    public boolean isDemo() {
        return type == Type.DEMO;
    }

    public boolean isNone() {
        return type == Type.NONE;
    }

    public boolean isInvalid() {
        return type == Type.INVALID;
    }

    public boolean isValid() {
        return !isNone() && !isInvalid();
    }

    /** Call only on valid subscriptions. */
    public int computeMonthsAfter(@NotNull LocalDate now) {
        stateCheck(isValid(), "Invalid subscription: %s", this);
        assert timeValidity != null;
        return timeValidity.computeMonthsAfter(now);
    }

    @TestOnly
    public static int testTimeValidity(@NotNull LocalDate now, @NotNull String monthYearSpec) {
        return TimeValidity.parse(monthYearSpec)
                .computeMonthsAfter(now);
    }

    /**
     * Enumeration for the type of subscription.
     */
    public enum Type {

        /** No information about the subscription exists. */
        NONE(null),

        /** The subscription string is not valid. Note that this does NOT include time-invalid strings that were valid before. */
        INVALID(null),

        ANNUAL("01"),
        PLATFORM("02"),
        DEPLOYMENT("03"),
        DEVELOPMENT("04"),
        DEMO("05");

        private final String code;

        Type(String code) {
            this.code = code;
        }

        public static @NotNull Subscription.Type parse(@NotNull String code) {
            for (Type value : values()) {
                if (code.equals(value.code)) {
                    return value;
                }
            }
            return INVALID;
        }
    }

    private record TimeValidity(int month, int year) implements Serializable {

        /** The value has 4 characters in the form of `mmyy` (month + year). */
        private static TimeValidity parse(String stringValue) {
            int month = Integer.parseInt(stringValue.substring(0, 2));
            int year = Integer.parseInt(stringValue.substring(2, 4));
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException();
            }
            return new TimeValidity(month, 2000 + year);
        }

        /**
         * How many months passed since the first day after the subscription validity to now.
         * Incomplete months are counted as well.
         */
        int computeMonthsAfter(@NotNull LocalDate now) {
            var firstDayOff = LocalDate
                    .of(year, month, 1)
                    .plus(Period.ofMonths(1));
            var diff = Period.between(firstDayOff, now);
            if (diff.isNegative()) {
                return 0;
            } else {
                return (int) diff.toTotalMonths() + 1;
            }
        }
    }
}
