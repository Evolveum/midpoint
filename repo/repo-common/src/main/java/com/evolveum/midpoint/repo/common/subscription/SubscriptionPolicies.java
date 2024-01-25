/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import org.jetbrains.annotations.NotNull;

import java.time.Period;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Policies related to the handling of subscriptions.
 *
 * *This is the place to be modified when such policies are to be changed.*
 *
 * Implementation notes:
 *
 * . Effectively a utility class. Should not be instantiated.
 * . These methods should not throw any exceptions.
 */
public class SubscriptionPolicies {

    /**
     * How long is the grace period? It may be zero.
     *
     * Assumes that there IS a well-formed subscription ID.
     * (Otherwise, there's no point in asking about grace period.)
     */
    public static @NotNull Period getGracePeriod(@NotNull SubscriptionId subscriptionId) {
        argCheck(subscriptionId.isWellFormed(), "Subscription ID is not well-formed");

        return !subscriptionId.isDemo() ? Period.ofMonths(3) : Period.ZERO;
    }

    /**
     * This is how we estimate we are in a production environment.
     */
    @SuppressWarnings("RedundantIfStatement")
    public static boolean isProductionEnvironment(@NotNull SubscriptionId subscriptionId, @NotNull SystemFeatures features) {
        if (subscriptionId.isWellFormed() && !subscriptionId.isDemo()) {
            return true;
        }
        if (features.areRealNotificationsEnabled()) {
            return true;
        }
        if (features.isPublicHttpsUrlPatternDefined()) {
            return true;
        }
        if (features.isRemoteHostAddressHeaderDefined()) {
            return true;
        }
        if (features.isCustomLoggingDefined()) {
            return true;
        }
        return false;
    }
}
