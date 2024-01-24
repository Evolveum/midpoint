/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import java.time.LocalDate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The state of the subscription with regards to the current situation
 * (subscription ID presence, time, system features, and so on).
 *
 * See e.g.
 *
 * - {@link #isActive()}
 * - {@link #isDemo()}
 * - {@link #isInGracePeriod()}
 * - {@link #isProductionEnvironment()}
 * - ...
 */
public class SubscriptionState {

    /** The parsed subscription ID. */
    @NotNull private final Subscription subscription;

    private final boolean productionEnvironment;

    /** The resulting validity of the subscription with regards to the current time and system features. */
    @NotNull private final Validity validity;

    private SubscriptionState(
            @NotNull Subscription subscription,
            boolean productionEnvironment,
            @NotNull Validity validity) {
        this.subscription = subscription;
        this.productionEnvironment = productionEnvironment;
        this.validity = validity;
    }

    public static @NotNull SubscriptionState determine(
            @NotNull Subscription subscription, @Nullable SystemFeatures systemFeatures) {

        if (systemFeatures == null) {
            return error(subscription);
        }

        boolean productionEnvironment = SubscriptionPolicies.isProductionEnvironment(systemFeatures);
        try {
            if (subscription.isNone()) {
                return new SubscriptionState(subscription, productionEnvironment, Validity.NONE);
            }
            if (subscription.isMalformed()) {
                return new SubscriptionState(subscription, productionEnvironment, Validity.MALFORMED);
            }
            int monthsAfter = subscription.computeMonthsAfter(LocalDate.now());
            if (monthsAfter == 0) {
                return new SubscriptionState(subscription, productionEnvironment, Validity.FULLY_ACTIVE);
            }
            if (!SubscriptionPolicies.isGracePeriodAvailable(subscription, systemFeatures)) {
                return new SubscriptionState(subscription, productionEnvironment, Validity.EXPIRED);
            }
            return new SubscriptionState(subscription, productionEnvironment, Validity.fromMonthsAfter(monthsAfter));

        } catch (Exception e) {
            return new SubscriptionState(subscription, productionEnvironment, Validity.EXPIRED);
        }
    }

    public static SubscriptionState error(@NotNull Subscription subscription) {
        // Very strange case, but let us handle it gracefully. Let us expect the worst (production environment).
        return new SubscriptionState(subscription, true, Validity.EXPIRED);
    }

    /**
     * To be called in situations when there is either no information about the subscription,
     * or when we - for the simplicity - pretend there's none. We are invalid anyway, so it's not
     * that much different.
     */
    public static SubscriptionState error() {
        return error(Subscription.none());
    }

    /**
     * Is the subscription active? The subscription ID must be present, well-formed, and within its time validity period.
     * (The grace period - if available - counts also as the validity.) The expired or malformed subscriptions
     * give `false` here.
     */
    public boolean isActive() {
        return validity.isActive();
    }

    /** Is the (valid) subscription in the grace period? */
    public boolean isInGracePeriod() {
        return validity.isInGracePeriod();
    }

    /** Is this a demo subscription? Note that the subscription itself may be expired. */
    public boolean isDemo() {
        return subscription.isDemo();
    }

    /** Just a convenience method. */
    public boolean isInactiveOrDemo() {
        return !isActive() || isDemo();
    }

    /** Do we think we run in a production environment (regardless of the subscription ID present or not)? */
    public boolean isProductionEnvironment() {
        return productionEnvironment;
    }

    /**
     * Enumeration for the validity of the subscription.
     *
     * If the time has not passed yet, the validity is {@link #FULLY_ACTIVE}. There is an optional grace period after that,
     * consisting of three parts (by default, they correspond to calendar months). Finally, the state goes to {@link #EXPIRED}.
     */
    public enum Validity {

        /** The subscription information is OK, and the time has not passed. */
        FULLY_ACTIVE,

        /** The subscription information is OK, and we are in the grace period part 1 of 3 (if available). */
        IN_GRACE_PERIOD_PART_ONE,

        /** The subscription information is OK, and we are in the grace period part 2 of 3 (if available). */
        IN_GRACE_PERIOD_PART_TWO,

        /** The subscription information is OK, and we are in the grace period part 3 of 3 (if available). */
        IN_GRACE_PERIOD_PART_THREE,

        /** The subscription period has passed; including the (optional) grace period. */
        EXPIRED,

        /** The subscription information is malformed. */
        MALFORMED,

        /** There is no subscription information. */
        NONE;

        /** Returns grace period level or "expired". Assumes not fully active. */
        public static Validity fromMonthsAfter(int monthsAfter) {
            if (monthsAfter <= 0) {
                throw new IllegalArgumentException();
            } else if (monthsAfter == 1) {
                return IN_GRACE_PERIOD_PART_ONE;
            } else if (monthsAfter == 2) {
                return IN_GRACE_PERIOD_PART_TWO;
            } else if (monthsAfter == 3) {
                return IN_GRACE_PERIOD_PART_THREE;
            } else {
                return EXPIRED;
            }
        }

        /**
         * Is this state generally considered "active"?
         *
         * As for the grace period, note that not all subscriptions have it.
         * For those that do, it is considered still active, although with potential limitations.
         */
        public boolean isActive() {
            return this == FULLY_ACTIVE || isInGracePeriod();
        }

        public boolean isInGracePeriod() {
            return this == IN_GRACE_PERIOD_PART_ONE
                    || this == IN_GRACE_PERIOD_PART_TWO
                    || this == IN_GRACE_PERIOD_PART_THREE;
        }
    }
}
