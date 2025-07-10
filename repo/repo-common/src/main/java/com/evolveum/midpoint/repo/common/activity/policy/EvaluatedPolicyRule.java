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

    @NotNull ThresholdValueType getThresholdValueType();

    void setThresholdValueType(@NotNull ThresholdValueType thresholdValueType);

    <T> T getThresholdValue(Class<T> type);

    void setThresholdValue(Object value);

    boolean isTriggered();

    boolean hasThreshold();
}
