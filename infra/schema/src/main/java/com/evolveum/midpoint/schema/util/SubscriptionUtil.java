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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;

public class SubscriptionUtil {

    public static boolean isSubscriptionIdCorrect(String subscriptionId) {
        if (StringUtils.isEmpty(subscriptionId)) {
            return false;
        }
        if (!NumberUtils.isDigits(subscriptionId)) {
            return false;
        }
        if (subscriptionId.length() < 11) {
            return false;
        }
        String subscriptionType = subscriptionId.substring(0, 2);
        boolean isTypeCorrect = false;
        for (SubscriptionType type : SubscriptionType.values()) {
            if (type.getSubscriptionType().equals(subscriptionType)) {
                isTypeCorrect = true;
                break;
            }
        }
        if (!isTypeCorrect) {
            return false;
        }
        String substring1 = subscriptionId.substring(2, 4);
        String substring2 = subscriptionId.substring(4, 6);
        try {
            if (Integer.parseInt(substring1) < 1 || Integer.parseInt(substring1) > 12) {
                return false;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yy");
            String currentYear = dateFormat.format(Calendar.getInstance().getTime());
            if (Integer.parseInt(substring2) < Integer.parseInt(currentYear)) {
                return false;
            }

            String expDateStr = subscriptionId.substring(2, 6);
            dateFormat = new SimpleDateFormat("MMyy");
            Date expDate = dateFormat.parse(expDateStr);
            Calendar expireCalendarValue = Calendar.getInstance();
            expireCalendarValue.setTime(expDate);
            expireCalendarValue.add(Calendar.MONTH, 1);
            Date currentDate = new Date(System.currentTimeMillis());
            if (expireCalendarValue.getTime().before(currentDate) || expireCalendarValue.getTime().equals(currentDate)) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        VerhoeffCheckDigit checkDigit = new VerhoeffCheckDigit();
        return checkDigit.isValid(subscriptionId);
    }

    /**
     * Enumeration for the type of subscription.
     */
    public enum SubscriptionType {

        ANNUAL_SUBSCRIPTION("01"),
        PLATFORM_SUBSCRIPTION("02"),
        DEPLOYMENT_SUBSCRIPTION("03"),
        DEVELOPMENT_SUBSCRIPTION("04"),
        DEMO_SUBSCRIPTION("05");

        private final String subscriptionType;

        SubscriptionType(String subscriptionType) {
            this.subscriptionType = subscriptionType;
        }

        public String getSubscriptionType() {
            return subscriptionType;
        }
    }
}
