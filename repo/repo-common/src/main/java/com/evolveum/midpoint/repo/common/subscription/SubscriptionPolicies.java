/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import org.jetbrains.annotations.NotNull;

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
     * Is the 3-month grace period available?
     *
     * Please adapt the code to match your needs.
     */
    @SuppressWarnings("RedundantIfStatement")
    public static boolean isGracePeriodAvailable(@NotNull Subscription subscription, @NotNull SystemFeatures features) {
        assert subscription.isWellFormed();

        if (subscription.isDemo()) {
            return false;
        }
        if (features.isGenericProductionDatabasePresent()) {
            return false;
        }
        return true;
    }

    /**
     * This is how we estimate we are in a production environment.
     *
     * Please adapt the code to match your needs.
     */
    public static boolean isProductionEnvironment(@NotNull SystemFeatures features) {
        if (features.isClusterPresent()) {
            return true;
        }
        return features.isPublicHttpUrlPatternPresent()
                && features.isCustomLoggingPresent()
                && features.isMailNotificationsPresent();
    }
}
