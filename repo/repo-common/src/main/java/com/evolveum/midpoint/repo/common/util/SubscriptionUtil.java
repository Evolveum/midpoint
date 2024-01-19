/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.util.SubscriptionInformation.SubscriptionValidity;
import com.evolveum.midpoint.repo.common.util.SubscriptionInformation.SubscriptionType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.TestOnly;

public class SubscriptionUtil {

    private static final Trace LOGGER = TraceManager.getTrace(SubscriptionUtil.class);

    @NotNull
    public static SubscriptionInformation getSubscriptionInformation(@Nullable SystemConfigurationType systemConfiguration) {
        if (systemConfiguration == null) {
            return createNoneSubscription();
        }

        DeploymentInformationType deploymentInformation = systemConfiguration.getDeploymentInformation();
        if (deploymentInformation == null) {
            return createNoneSubscription();
        }

        return getSubscriptionInformation(deploymentInformation.getSubscriptionIdentifier());
    }

    public static SubscriptionInformation createNoneSubscription() {
        return new SubscriptionInformation(SubscriptionValidity.NONE);
    }

    public static SubscriptionInformation createInvalidSubscription() {
        return new SubscriptionInformation(SubscriptionValidity.INVALID);
    }

    private static @NotNull SubscriptionInformation getSubscriptionInformation(String subscriptionId) {
        if (StringUtils.isEmpty(subscriptionId)) {
            return createNoneSubscription();
        }
        if (!NumberUtils.isDigits(subscriptionId)) {
            return createInvalidSubscription();
        }
        if (subscriptionId.length() < 11) {
            return createInvalidSubscription();
        }

        try {
            // Let us check the correctness first.
            VerhoeffCheckDigit checkDigit = new VerhoeffCheckDigit();
            if (!checkDigit.isValid(subscriptionId)) {
                return createInvalidSubscription();
            }

            SubscriptionType type = SubscriptionType.resolveType(subscriptionId.substring(0, 2));
            if (type == null) {
                return createInvalidSubscription();
            }

            return new SubscriptionInformation(
                    type,
                    determineValidity(subscriptionId, new Date()));

        } catch (Exception ex) {
            return createInvalidSubscription();
        }
    }

    @TestOnly
    public static @NotNull SubscriptionValidity determineValidity(String subscriptionId, Date currentDate)
            throws ParseException {
        String months = subscriptionId.substring(2, 4);
        String years = subscriptionId.substring(4, 6);

        if (Integer.parseInt(months) < 1 || Integer.parseInt(months) > 12) {
            return SubscriptionValidity.INVALID;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yy");
        String currentYear = dateFormat.format(currentDate);
        if (Integer.parseInt(years) < Integer.parseInt(currentYear) && Integer.parseInt(months) < 10) {
            return SubscriptionValidity.INVALID;
        }

        String expDateStr = subscriptionId.substring(2, 6);
        dateFormat = new SimpleDateFormat("MMyy");
        Date expDate = dateFormat.parse(expDateStr);
        Calendar expireCalendarValue = Calendar.getInstance();
        expireCalendarValue.setTime(expDate);
        expireCalendarValue.add(Calendar.MONTH, 1);
        if (expireCalendarValue.getTime().before(currentDate) || expireCalendarValue.getTime().equals(currentDate)) {
            if (expiresIn(expireCalendarValue, currentDate, 1)) {
                return SubscriptionValidity.INVALID_FIRST_MONTH;
            } else if (expiresIn(expireCalendarValue, currentDate, 2)) {
                return SubscriptionValidity.INVALID_SECOND_MONTH;
            } else if (expiresIn(expireCalendarValue, currentDate, 3)) {
                return SubscriptionValidity.INVALID_THIRD_MONTH;
            } else {
                return SubscriptionValidity.INVALID;
            }
        }

        return SubscriptionValidity.VALID;
    }

    private static boolean expiresIn(Calendar expireCalendarValue, Date currentDate, int i) {
        Calendar expireInFuture = (Calendar) expireCalendarValue.clone();
        expireInFuture.add(Calendar.MONTH, i);
        return !expireInFuture.getTime().before(currentDate) && !expireInFuture.getTime().equals(currentDate);
    }

    /**
     * If null is returned, subscription is valid and no action is needed.
     * If non-null message is returned, it can be added where necessary.
     */
    @Nullable
    public static String missingSubscriptionAppeal(
            SystemObjectCache systemObjectCache, LocalizationService localizationService, Locale locale) {
        try {
            SystemConfigurationType config =
                    systemObjectCache.getSystemConfigurationBean(new OperationResult("dummy"));
            if (SubscriptionUtil.getSubscriptionInformation(config).isCorrect()) {
                return null;
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve system configuration", e);
        }

        // Everything else uses Locale.getDefault()
        return localizationService.translate("PageBase.nonActiveSubscriptionMessage", null, locale,
                "No active subscription. Please support midPoint by purchasing a subscription.");
    }

}
