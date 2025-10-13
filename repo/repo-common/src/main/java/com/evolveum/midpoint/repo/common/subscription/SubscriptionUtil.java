/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
