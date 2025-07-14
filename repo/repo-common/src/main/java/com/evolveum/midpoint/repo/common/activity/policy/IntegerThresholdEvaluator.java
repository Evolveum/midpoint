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

        WaterMarkType lowWaterMark = threshold.getLowWaterMark();
        if (lowWaterMark == null || lowWaterMark.getCount() == null) {
            return true;
        }

        if (lowWaterMark.getCount() == null) {
            return true;
        }

        if (count < lowWaterMark.getCount()) {
            return false;
        }

        WaterMarkType highWaterMark = threshold.getHighWaterMark();
        if (highWaterMark == null || highWaterMark.getCount() == null) {
            return true;
        }

        if (count > highWaterMark.getCount()) {
            return false;
        }

        return true;
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
