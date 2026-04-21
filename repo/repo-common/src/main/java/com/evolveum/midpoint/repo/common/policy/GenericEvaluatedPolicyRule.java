/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.repo.common.activity.policy.PolicyRuleIdentifier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRule;
import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO docs and probably better name [viliam]
 */
public interface GenericEvaluatedPolicyRule extends DebugDumpable {

    @NotNull PolicyRuleType getPolicyRule();

    ActivityPath getPath();

    String getName();

    @NotNull PolicyRuleIdentifier getRuleIdentifier();

    void setCount(Integer localValue, Integer totalValue);

    void setCurrentState(ActivityPolicyStateType currentState);

    @NotNull List<PolicyActionType> getActions();

    @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> getTriggers();

    void setTriggers(List<EvaluatedPolicyRuleTrigger<?>> triggers);

    boolean isTriggered();

    boolean hasThreshold();

    ActivityPolicyRule getActivityPolicyRule();

    Integer getCount();

    default boolean isOverThreshold() {
        if (!hasThreshold()) {
            return true;
        }

        Integer count = getCount();
        if (count == null) {
            count = 0;
        }

        PolicyThresholdType policyThreshold = getPolicyRule().getPolicyThreshold();
        Integer low = getWaterMarkValue(policyThreshold.getLowWaterMark());
        Integer high = getWaterMarkValue(policyThreshold.getHighWaterMark());

        if (low != null && count < low) {
            // below low water-mark
            return false;
        }

        if (high != null && count > high) {
            // above high water-mark
            return false;
        }

        // either marks are not set, or the count is within the range
        return true;
    }

    private Integer getWaterMarkValue(WaterMarkType waterMark) {
        if (waterMark == null) {
            return null;
        }

        return waterMark.getCount();
    }
}
