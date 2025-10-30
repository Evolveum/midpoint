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

    void setCount(Integer localValue, Integer totalValue);

    /**
     * Current count that should be used for policy threshold evaluation.
     * This is the result of policy constraint evaluation.
     *
     * For activity trees, this value is the total count for the activity tree, i.e., it contains the relevant data from
     * all activities that were already finished.
     */
    Integer getTotalCount();

    /**
     * Local count of the rule, i.e., the count that was computed for the current activity only.
     * It is a part of {@link #getTotalCount()}.
     *
     * @see #getTotalCount()
     */
    Integer getLocalCount();

    boolean isTriggered();

    boolean hasThreshold();

    boolean isOverThreshold();
}
