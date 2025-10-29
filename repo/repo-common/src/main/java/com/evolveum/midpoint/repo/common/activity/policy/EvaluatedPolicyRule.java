/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

public interface EvaluatedPolicyRule {

    String getName();

    @NotNull ActivityPolicyRuleIdentifier getRuleIdentifier();

    Integer getOrder();

    /**
     * Type of the threshold value used for reaction threshold evaluation.
     */
    @NotNull ThresholdValueType getThresholdValueType();

    void setThresholdTypeAndValues(@NotNull ThresholdValueType thresholdValueType, Object localValue, Object totalValue);

    /**
     * Current value that should be used for reaction threshold evaluation.
     * This is the result of policy constraint evaluation.
     *
     * Type of this value is defined by {@link #getThresholdValueType()}.
     *
     * For activity trees, this value is the total value for the activity tree, i.e., it contains the relevant data from
     * all activities that were already finished.
     *
     * For example, if we are dealing with a policy that counts failed operations, this value contains the number of failed
     * operations in the activity tree so far. If we are dealing with execution time, the value contains the total execution
     * time of all finished activities in the tree.
     */
    Object getTotalValue();

    /**
     * Local value of the rule, i.e., the value that was computed for the current activity only.
     * It is a part of {@link #getTotalValue()}.
     *
     * @see #getTotalValue()
     */
    Object getLocalValue();

    boolean isTriggered();

    boolean hasThreshold();
}
