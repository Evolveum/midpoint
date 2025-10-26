/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
