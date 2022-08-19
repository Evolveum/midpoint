/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

public class SubscriptionUtil {

    @NotNull
    public static SubscriptionType getSubscriptionType(@Nullable SystemConfigurationType systemConfigurationType) {
        if (systemConfigurationType == null) {
            return SubscriptionType.NONE;
        }

        DeploymentInformationType deploymentInformation = systemConfigurationType.getDeploymentInformation();
        if (deploymentInformation == null) {
            return SubscriptionType.NONE;
        }

        return getSubscriptionType(deploymentInformation.getSubscriptionIdentifier());
    }

    @NotNull
    public static SubscriptionType getSubscriptionType(String subscriptionId) {
        if (StringUtils.isEmpty(subscriptionId)) {
            return SubscriptionType.NONE;
        }
        if (!NumberUtils.isDigits(subscriptionId)) {
            return SubscriptionType.INVALID;
        }
        if (subscriptionId.length() < 11) {
            return SubscriptionType.INVALID;
        }

        SubscriptionType type = SubscriptionType.resolveType(subscriptionId.substring(0, 2));
        if (type == null || !type.isCorrect()) {
            return SubscriptionType.INVALID;
        }

        String substring1 = subscriptionId.substring(2, 4);
        String substring2 = subscriptionId.substring(4, 6);
        try {
            if (Integer.parseInt(substring1) < 1 || Integer.parseInt(substring1) > 12) {
                return SubscriptionType.INVALID;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yy");
            String currentYear = dateFormat.format(Calendar.getInstance().getTime());
            if (Integer.parseInt(substring2) < Integer.parseInt(currentYear)) {
                return SubscriptionType.INVALID;
            }

            String expDateStr = subscriptionId.substring(2, 6);
            dateFormat = new SimpleDateFormat("MMyy");
            Date expDate = dateFormat.parse(expDateStr);
            Calendar expireCalendarValue = Calendar.getInstance();
            expireCalendarValue.setTime(expDate);
            expireCalendarValue.add(Calendar.MONTH, 1);
            Date currentDate = new Date(System.currentTimeMillis());
            if (expireCalendarValue.getTime().before(currentDate) || expireCalendarValue.getTime().equals(currentDate)) {
                return SubscriptionType.INVALID;
            }
        } catch (Exception ex) {
            return SubscriptionType.INVALID;
        }
        VerhoeffCheckDigit checkDigit = new VerhoeffCheckDigit();
        if (!checkDigit.isValid(subscriptionId)) {
            return SubscriptionType.INVALID;
        }

        return type;
    }

    /**
     * Enumeration for the type of subscription.
     */
    public enum SubscriptionType {

        NONE(null),
        INVALID(null),
        ANNUAL_SUBSCRIPTION("01"),
        PLATFORM_SUBSCRIPTION("02"),
        DEPLOYMENT_SUBSCRIPTION("03"),
        DEVELOPMENT_SUBSCRIPTION("04"),
        DEMO_SUBSCRIPTION("05");

        private final String subscriptionType;

        SubscriptionType(String subscriptionType) {
            this.subscriptionType = subscriptionType;
        }

        public boolean isCorrect() {
            return subscriptionType != null;
        }

        private static final Map<String, SubscriptionType> codeToType = new HashMap<>();

        static {
            for (SubscriptionType value : values()) {
                if (value.subscriptionType != null) {
                    codeToType.put(value.subscriptionType, value);
                }
            }
        }

        @Nullable
        public static SubscriptionType resolveType(String code) {
            return codeToType.get(code);
        }
    }
}
