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
 * The state of the subscription with regards to the current situation (time, system features, and so on).
 *
 * See e.g.
 *
 * - {@link #isValid()}
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
            if (subscription.isInvalid()) {
                return new SubscriptionState(subscription, productionEnvironment, Validity.INVALID);
            }
            int monthsAfter = subscription.computeMonthsAfter(LocalDate.now());
            if (monthsAfter == 0) {
                return new SubscriptionState(subscription, productionEnvironment, Validity.VALID);
            }
            if (!SubscriptionPolicies.isGracePeriodAvailable(subscription, systemFeatures)) {
                return new SubscriptionState(subscription, productionEnvironment, Validity.INVALID);
            }
            return new SubscriptionState(subscription, productionEnvironment, Validity.fromMonthsAfter(monthsAfter));

        } catch (Exception e) {
            return new SubscriptionState(subscription, productionEnvironment, Validity.INVALID);
        }
    }

    public static SubscriptionState error(@NotNull Subscription subscription) {
        // Very strange case, but let us handle it gracefully. Let us expect the worst (production environment).
        return new SubscriptionState(subscription, true, Validity.INVALID);
    }

    /**
     * To be called in situations when there is either no information about the subscription,
     * or when we - for the simplicity - pretend there's none. We are invalid anyway, so it's not
     * that much different.
     */
    public static SubscriptionState error() {
        return error(Subscription.none());
    }

    public boolean isValid() {
        return validity.isValid();
    }

    public boolean isInGracePeriod() {
        return validity.isInGracePeriod();
    }

    /** Note that the subscription itself may be (e.g.) expired. */
    public boolean isDemo() {
        return subscription.isDemo();
    }

    public boolean isInvalidOrDemo() {
        return !isValid() || isDemo();
    }

    public boolean isProductionEnvironment() {
        return productionEnvironment;
    }

    /**
     * Enumeration for the validity of subscription.
     *
     * If the time has not passed yet, the validity is {@link #VALID}. There is an optional grace period after that,
     * consisting of three parts (by default, they correspond to calendar months). Finally, the state goes to {@link #INVALID}.
     */
    public enum Validity {

        /** The subscription information is OK, and the time has not passed. */
        VALID,

        /** The subscription information is OK, and we are in the grace period part 1 of 3 (if available). */
        IN_GRACE_PERIOD_PART_ONE,

        /** The subscription information is OK, and we are in the grace period part 2 of 3 (if available). */
        IN_GRACE_PERIOD_PART_TWO,

        /** The subscription information is OK, and we are in the grace period part 3 of 3 (if available). */
        IN_GRACE_PERIOD_PART_THREE,

        /** The subscription information is not OK, and/or the time has passed; including the (optional) grace period. */
        INVALID,

        /** There is no subscription information. */
        NONE;

        public static Validity fromMonthsAfter(int monthsAfter) {
            assert monthsAfter > 0;
            if (monthsAfter == 1) {
                return IN_GRACE_PERIOD_PART_ONE;
            } else if (monthsAfter == 2) {
                return IN_GRACE_PERIOD_PART_TWO;
            } else if (monthsAfter == 3) {
                return IN_GRACE_PERIOD_PART_THREE;
            } else {
                return INVALID;
            }
        }

        /**
         * Is this state generally considered "valid"? Note that not all subscriptions have the grace period.
         * For those that have it, it is considered still valid, although with potential limitations.
         */
        public boolean isValid() {
            return this == VALID || isInGracePeriod();
        }

        public boolean isInGracePeriod() {
            return this == IN_GRACE_PERIOD_PART_ONE
                    || this == IN_GRACE_PERIOD_PART_TWO
                    || this == IN_GRACE_PERIOD_PART_THREE;
        }
    }
}
