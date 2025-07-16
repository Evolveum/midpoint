/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WaterMarkType;

public class IntegerThresholdEvaluator implements ThresholdEvaluator {

    @Override
    public boolean evaluate(PolicyThresholdType threshold, Object currentValue) {
        Integer count = getCount(currentValue);
        if (count == null) {
            count = 0;
        }

        Integer low = getWaterMarkValue(threshold.getLowWaterMark());
        Integer high = getWaterMarkValue(threshold.getHighWaterMark());

        if (low != null) {
            if (count < low) {
                return false; // below low water mark
            }
        }

        if (high != null) {
            if (count > high) {
                return false; // above high water mark
            }
        }

        // either marks are not set, or the count is within the range
        return true;
    }

    private Integer getWaterMarkValue(WaterMarkType waterMark) {
        if (waterMark == null || waterMark.getDuration() == null) {
            return null;
        }

        return waterMark.getCount();
    }

    private Integer getCount(Object currentValue) {
        if (currentValue == null) {
            return null;
        }

        if (currentValue instanceof Integer i) {
            return i;
        }

        throw new IllegalStateException(
                "Cannot evaluate " + currentValue.getClass() + " with " + IntegerThresholdEvaluator.class.getName());
    }
}
