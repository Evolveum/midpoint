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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalType;

public class DurationThresholdEvaluator implements ThresholdEvaluator {

    @Override
    public boolean evaluate(PolicyThresholdType threshold, Object currentValue) {
        TimeIntervalType timeInterval = threshold.getTimeInterval();
        Duration interval = timeInterval.getInterval();
        if (interval == null) {
            return true; // no interval means, no threshold
        }

        Duration currentDuration = getDuration(currentValue);
        if (currentDuration == null) {
            return false; // no duration means, no threshold
        }

        Date date = new Date();
        long currentDurationMs = currentDuration.getTimeInMillis(date);
        long intervalMs = interval.getTimeInMillis(date);

        if (intervalMs < currentDurationMs) {
            return true;
        }

        return false;
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
