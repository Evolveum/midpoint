/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;
import org.jetbrains.annotations.NotNull;
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
public abstract class SubscriptionId implements Serializable {

    public static SubscriptionId none() {
        return NoSubscriptionId.INSTANCE;
    }

    public static SubscriptionId malformed() {
        return MalformedSubscriptionId.INSTANCE;
    }

    public static SubscriptionId parse(String stringValue) {
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

            return new WellFormedSubscriptionId(
                    Type.parse(stringValue.substring(0, 2)),
                    getFirstDayAfter(stringValue.substring(2, 6)));

        } catch (Exception ex) {
            return malformed();
        }
    }

    @TestOnly
    public static SubscriptionId forTesting(Type type, String timeValidityString) {
        return new WellFormedSubscriptionId(type, getFirstDayAfter(timeValidityString));
    }

    private static LocalDate getFirstDayAfter(String timeValidityString) {
        int month = Integer.parseInt(timeValidityString.substring(0, 2));
        int year = Integer.parseInt(timeValidityString.substring(2, 4)) + 2000;
        return LocalDate
                .of(year, month, 1)
                .plus(Period.ofMonths(1));
    }

    /** Is this a 05-style (demo) subscription? */
    public boolean isDemo() {
        return this instanceof WellFormedSubscriptionId subscription
                && subscription.type == Type.DEMO;
    }

    /** Is the subscription information missing? */
    public boolean isNone() {
        return this instanceof NoSubscriptionId;
    }

    /** Is the subscription information malformed (corrupted)? */
    public boolean isMalformed() {
        return this instanceof MalformedSubscriptionId;
    }

    /** Is the subscription well-formed? Note it may or may not be expired. See also {@link SubscriptionState#isActive()}. */
    public boolean isWellFormed() {
        return this instanceof WellFormedSubscriptionId;
    }

    /** The first day after the declared time validity. E.g., June 1st, 2024 for `0524` subscription. */
    public @NotNull LocalDate getFirstDayAfter() {
        throw new IllegalStateException("Malformed or no subscription: " + this);
    }

    public @NotNull Type getType() {
        throw new IllegalStateException("Malformed or no subscription: " + this);
    }

    /** No information about the subscription exists. */
    private static class NoSubscriptionId extends SubscriptionId {
        private static final NoSubscriptionId INSTANCE = new NoSubscriptionId();

        @Override
        public String toString() {
            return "No subscription ID";
        }
    }

    /** The subscription string is not OK, e.g. it is too short or corrupted. */
    private static class MalformedSubscriptionId extends SubscriptionId {
        private static final MalformedSubscriptionId INSTANCE = new MalformedSubscriptionId();

        @Override
        public String toString() {
            return "Malformed subscription ID";
        }
    }

    /** The subscription string is formally OK. It may or may not be expired. */
    static class WellFormedSubscriptionId extends SubscriptionId {

        @NotNull private final Type type;

        /** See {@link SubscriptionId#getFirstDayAfter()} for the explanation. */
        @NotNull private final LocalDate firstDayAfter;

        WellFormedSubscriptionId(@NotNull Type type, @NotNull LocalDate firstDayAfter) {
            this.type = type;
            this.firstDayAfter = firstDayAfter;
        }

        @Override
        public @NotNull LocalDate getFirstDayAfter() {
            return firstDayAfter;
        }

        @Override
        public @NotNull Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return "WellFormedSubscription{" +
                    "type=" + type +
                    ", firstDayAfter=" + firstDayAfter +
                    '}';
        }
    }

    /**
     * Enumeration for the type of subscription.
     */
    public enum Type {

        ANNUAL("01"),
        PLATFORM("02"),
        DEPLOYMENT("03"),
        DEVELOPMENT("04"),
        DEMO("05"),
        CONSULTING_PLATFORM("06"),
        PRODUCT_SUPPORT_JP_MODEL("07"),
        PRODUCT_SUPPORT_SAAS("08");

        private final String code;

        Type(String code) {
            this.code = code;
        }

        public static @NotNull SubscriptionId.Type parse(@NotNull String code) {
            for (Type value : values()) {
                if (code.equals(value.code)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(code);
        }
    }
}
