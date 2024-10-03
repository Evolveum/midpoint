/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import static com.evolveum.midpoint.repo.common.subscription.SubscriptionState.TimeValidityStatus.FULLY_ACTIVE;
import static com.evolveum.midpoint.repo.common.subscription.SubscriptionState.TimeValidityStatus.IN_GRACE_PERIOD;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
public class SubscriptionState implements DebugDumpable, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(SubscriptionState.class);

    /** The parsed subscription ID. */
    @NotNull private final SubscriptionId subscriptionId;

    /** The system features. */
    @NotNull private final SystemFeatures systemFeatures;

    /** Are we running in a production environment (estimated)? */
    private final boolean productionEnvironment;

    /** Effective time validity of the subscription. Non-null if and only if {@link #subscriptionId} is well-formed. */
    private final TimeValidity timeValidity;

    private SubscriptionState(
            @NotNull SubscriptionId subscriptionId,
            @NotNull SystemFeatures systemFeatures,
            boolean productionEnvironment,
            TimeValidity timeValidity) {
        this.subscriptionId = subscriptionId;
        this.systemFeatures = systemFeatures;
        this.productionEnvironment = productionEnvironment;
        this.timeValidity = timeValidity;
    }

    public static @NotNull SubscriptionState determine(
            @NotNull SubscriptionId subscriptionId, @NotNull SystemFeatures systemFeatures) {
        var state = new SubscriptionState(
                subscriptionId,
                systemFeatures,
                SubscriptionPolicies.isProductionEnvironment(subscriptionId, systemFeatures),
                TimeValidity.determine(subscriptionId));
        LOGGER.trace("Subscription state was determined to be:\n{}", state.debugDumpLazily(1));
        return state;
    }

    /**
     * To be called in rare situations when there is either no information about the subscription and/or we cannot retrieve
     * the system features. This can happen e.g. if the system configuration cannot be completely retrieved because of
     * a schema exception (that should be quite rare, as the "normal" issues of e.g. missing extension schema should
     * be handled gracefully by the repository).
     */
    public static SubscriptionState error() {
        return new SubscriptionState(SubscriptionId.none(), SystemFeatures.error(), true, null);
    }

    /**
     * Returns the # of months after the subscription period ended.
     *
     * Call only with well-formed subscriptions!
     */
    public int getMonthsAfter() {
        if (timeValidity != null) {
            return timeValidity.getMonthsAfter(LocalDate.now());
        } else {
            throw new IllegalStateException("Subscription ID is not well-formed: " + subscriptionId);
        }
    }

    /**
     * How many days remains to the end of the grace period (if there's one for this kind of subscription)?
     *
     * For example, if the subscription ends 05/24, then the grace period ends on August 31st, 2024
     * (assuming 3-months period), and the "no subscription" period starts on September 1st, 2024.
     *
     * So, on September 1st (or later), the returned value is 0; on August 31st, the returned value is 1; and so on.
     *
     * Call only with well-formed subscriptions!
     */
    public int getDaysToGracePeriodGone() {
        if (timeValidity != null) {
            return timeValidity.daysToGracePeriodGone(LocalDate.now());
        } else {
            throw new IllegalStateException("Subscription ID is not well-formed: " + subscriptionId);
        }
    }

    /**
     * Is the subscription active? The subscription ID must be well-formed, and within its time validity period.
     * (The grace period - if available - counts also as the validity.) The expired or malformed subscriptions
     * give `false` here.
     */
    public boolean isActive() {
        return timeValidity != null && timeValidity.isActive();
    }

    /** Is the (well-formed) subscription in the grace period? */
    public boolean isInGracePeriod() {
        return timeValidity != null && timeValidity.isInGracePeriod();
    }

    /** Is this a demo subscription? Note that the subscription itself may be expired. */
    public boolean isDemo() {
        return subscriptionId.isDemo();
    }

    /** Just a convenience method. */
    public boolean isInactiveOrDemo() {
        return !isActive() || isDemo();
    }

    /** Is used generic repo (other than H2) and we detect */
    public boolean isGenericRepoWithoutSubscription() {
        return getSystemFeatures().isGenericNonH2DatabaseUsed()
                && isProductionEnvironment() && !isActive();
    }

    /** Do we think we run in a production environment (regardless of the subscription ID present or not)? */
    public boolean isProductionEnvironment() {
        return productionEnvironment;
    }

    /** Should we allow clustering? In production environments ONLY with an active subscription. */
    public boolean isClusteringAvailable() {
        return isActive() || !isProductionEnvironment();
    }

    public boolean isFooterVisible() {
        return isInactiveOrDemo() || isInGracePeriod();
    }

    public @NotNull SystemFeatures getSystemFeatures() {
        return systemFeatures;
    }

    /** Representation of the subscription validity interval. */
    @VisibleForTesting
    public static class TimeValidity implements Serializable {

        /** The first day after the declared time validity. E.g., June 1st, 2024 for `0524` subscription. */
        @NotNull private final LocalDate firstDayAfter;

        /** The grace period for the current subscription. */
        @NotNull private final Period gracePeriod;

        /** The resulting validity status of the subscription with regards to the current time. */
        @NotNull private final TimeValidityStatus status;

        public TimeValidity(@NotNull LocalDate firstDayAfter, @NotNull Period gracePeriod, @NotNull TimeValidityStatus status) {
            this.firstDayAfter = firstDayAfter;
            this.gracePeriod = gracePeriod;
            this.status = status;
        }

        @VisibleForTesting
        public static TimeValidity determine(@NotNull SubscriptionId subscriptionId) {
            if (!subscriptionId.isWellFormed()) {
                return null;
            }
            var firstDayAfterFullValidity = subscriptionId.getFirstDayAfter();
            var gracePeriod = SubscriptionPolicies.getGracePeriod(subscriptionId);
            var firstDayAfterGracePeriod = firstDayAfterFullValidity.plus(gracePeriod);
            var now = LocalDate.now();
            TimeValidityStatus status;
            if (now.isBefore(firstDayAfterFullValidity)) {
                status = TimeValidityStatus.FULLY_ACTIVE;
            } else if (now.isBefore(firstDayAfterGracePeriod)) {
                status = TimeValidityStatus.IN_GRACE_PERIOD;
            } else {
                status = TimeValidityStatus.EXPIRED;
            }
            return new TimeValidity(firstDayAfterFullValidity, gracePeriod, status);
        }

        /**
         * How many months passed since the first day after the subscription validity to now.
         * Incomplete months are counted as well.
         *
         * For example, if the subscription ends 05/24, then the first month starts on June 1st, 2024 (inclusive),
         * the second month starts on July 1st, 2024 (inclusive), and so on.
         */
        @VisibleForTesting
        public int getMonthsAfter(@NotNull LocalDate now) {
            if (now.isBefore(firstDayAfter)) {
                return 0;
            } else {
                // We add 1 because the firstDayAfter is already in the "month one".
                return (int) ChronoUnit.MONTHS.between(firstDayAfter, now) + 1;
            }
        }

        @VisibleForTesting
        public int daysToGracePeriodGone(@NotNull LocalDate now) {
            var firstDayOfGracePeriodGone = firstDayAfter.plus(gracePeriod);
            var daysToGracePeriodGone = (int) ChronoUnit.DAYS.between(now, firstDayOfGracePeriodGone);
            return Math.max(0, daysToGracePeriodGone); // negative differences are rounded to zero
        }

        /**
         * Is this state generally considered "active"?
         *
         * As for the grace period, note that not all subscriptions have it.
         * For those that do, it is considered still active, although with potential limitations.
         */
        public boolean isActive() {
            return status == FULLY_ACTIVE || status == IN_GRACE_PERIOD;
        }

        public boolean isInGracePeriod() {
            return status == IN_GRACE_PERIOD;
        }

        @Override
        public String toString() {
            return "TimeValidity{" +
                    "firstDayAfter=" + firstDayAfter +
                    ", gracePeriod=" + gracePeriod +
                    ", status=" + status +
                    '}';
        }
    }

    /** Enumeration for the validity status of the subscription. */
    public enum TimeValidityStatus {

        /** The subscription ID is OK, and the declared time has not passed. E.g. for 0524, the time is <= May 31th, 2024. */
        FULLY_ACTIVE,

        /** The subscription ID is OK, and we are in the grace period (if available). E.g. for 0524, it June-Aug 2024. */
        IN_GRACE_PERIOD,

        /** The subscription period has passed; including the (optional) grace period. */
        EXPIRED
    }

    @Override
    public String toString() {
        return "SubscriptionState{" +
                "subscriptionId=" + subscriptionId +
                ", productionEnvironment=" + productionEnvironment +
                ", timeValidity=" + timeValidity +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "subscriptionId", String.valueOf(subscriptionId), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "systemFeatures", systemFeatures, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "productionEnvironment", productionEnvironment, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "timeValidity", String.valueOf(timeValidity), indent + 1);
        return sb.toString();
    }
}
