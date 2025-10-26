/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

public interface EvaluatedPolicyRule {

    String getName();

    String getRuleIdentifier();

    Integer getOrder();

    /**
     * Type of the threshold value used for reaction threshold evaluation.
     */
    @NotNull ThresholdValueType getThresholdValueType();

    void setThresholdValueType(@NotNull ThresholdValueType thresholdValueType, Object value);

    /**
     * Valute that should be used for reaction threshold evaluation.
     * This is the result of policy constraint evaluation.
     *
     * Type of this value is defined by {@link #getThresholdValueType()}.
     */
    Object getThresholdValue();

    boolean isTriggered();

    boolean hasThreshold();
}
