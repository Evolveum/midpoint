/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;

/**
 * Evaluates whether the current value meets the specified threshold criteria.
 * Implementations should define how to interpret the threshold and current value.
 *
 * Threshold evaluators must be stateless and thread-safe, as they may be
 * used in a multi-threaded environment.
 */
public interface ThresholdEvaluator {

    /**
     * Evaluates whether the current value meets the threshold criteria defined in the given threshold.
     *
     * @param threshold
     * @param currentValue
     */
    boolean evaluate(@NotNull PolicyThresholdType threshold, Object currentValue);
}
