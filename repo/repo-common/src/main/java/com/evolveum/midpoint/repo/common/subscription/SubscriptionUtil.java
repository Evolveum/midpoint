/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.subscription;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;

public class SubscriptionUtil {

    public static @Nullable String missingSubscriptionAppeal(LocalizationService localizationService, Locale locale) {
        if (LocalBeans.get().subscriptionStateHolder.getSubscriptionState().isActive()) {
            return null;
        } else {
            return localizationService.translate("PageBase.nonActiveSubscriptionMessage", null, locale,
                    "No active subscription. Please support midPoint by purchasing a subscription.");
        }
    }
}
