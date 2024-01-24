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
    private static final Subscription MALFORMED = new Subscription(Type.MALFORMED, null);

    @NotNull private final Type type;

    /** Always non-null if {@link #isWellFormed()} is `true`. */
    @Nullable private final TimeValidity timeValidity;

    private Subscription(@NotNull Type type, @Nullable TimeValidity timeValidity) {
        this.type = type;
        this.timeValidity = timeValidity;
    }

    /** See {@link Type#NONE}. */
    public static Subscription none() {
        return NONE;
    }

    /** See {@link Type#MALFORMED}. */
    public static Subscription malformed() {
        return MALFORMED;
    }

    public static Subscription parse(String stringValue) {
        if (StringUtils.isEmpty(stringValue)) {
            return none();
        }
        if (!NumberUtils.isDigits(stringValue)) {
            return malformed();
        }
        if (stringValue.length() < 11) {
            return malformed();
        }

        try {
            // Let us check the correctness first.
            VerhoeffCheckDigit checkDigit = new VerhoeffCheckDigit();
            if (!checkDigit.isValid(stringValue)) {
                return malformed();
            }

            return new Subscription(
                    Type.parse(stringValue.substring(0, 2)),
                    TimeValidity.parse(stringValue.substring(2, 6)));

        } catch (Exception ex) {
            return malformed();
        }
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "type=" + type +
                ", timeValidity=" + timeValidity +
                '}';
    }

    /** Is this a 05-style (demo) subscription? */
    public boolean isDemo() {
        return type == Type.DEMO;
    }

    /** Is the subscription information missing? */
    public boolean isNone() {
        return type == Type.NONE;
    }

    /** Is the subscription information malformed (corrupted)? */
    public boolean isMalformed() {
        return type == Type.MALFORMED;
    }

    /** Is the subscription well-formed? Note it may or may not be expired. See also {@link SubscriptionState#isActive()}. */
    public boolean isWellFormed() {
        return !isNone() && !isMalformed();
    }

    /** Call only on well-formed subscriptions. */
    public int computeMonthsAfter(@NotNull LocalDate now) {
        stateCheck(isWellFormed(), "Malformed or no subscription: %s", this);
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

        /** The subscription string is not OK, e.g. it is too short or corrupted. The time aspect is NOT taken into account. */
        MALFORMED(null),

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
            return MALFORMED;
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
