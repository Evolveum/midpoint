/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Date;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WaterMarkType;

public class DurationThresholdEvaluator implements ThresholdEvaluator {

    @Override
    public boolean evaluate(PolicyThresholdType threshold, Object currentValue) {
        Duration currentDuration = getDuration(currentValue);
        if (currentDuration == null) {
            return false; // no duration means, no threshold
        }

        Date date = new Date();
        long currentDurationMs = currentDuration.getTimeInMillis(date);

        Long lowDurationMs = getDurationInMillis(threshold.getLowWaterMark(), date);
        Long highDurationMs = getDurationInMillis(threshold.getHighWaterMark(), date);

        if (lowDurationMs != null) {
            if (currentDurationMs < lowDurationMs) {
                return false; // below low water mark
            }
        }

        if (highDurationMs != null) {
            if (currentDurationMs > highDurationMs) {
                return false; // above high water mark
            }
        }

        // either marks are not set, or the count is within the range
        return true;
    }

    private Long getDurationInMillis(WaterMarkType waterMark, Date date) {
        if (waterMark == null || waterMark.getDuration() == null) {
            return null;
        }

        return waterMark.getDuration().getTimeInMillis(date);
    }

    private Duration getDuration(Object currentValue) {
        if (currentValue == null) {
            return null;
        }

        if (currentValue instanceof Duration duration) {
            return duration;
        }

        throw new IllegalStateException(
                "Cannot evaluate " + currentValue.getClass() + " with " + DurationThresholdEvaluator.class.getName());
    }
}
