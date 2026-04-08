/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRuleIdentifier;
import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedActivityPolicyRuleTrigger;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * TODO docs
 * TODO rename
 */
public interface GenericEvaluatedPolicyRule extends DebugDumpable {

    @NotNull PolicyRuleType getPolicyRule();

    ActivityPath getPath();

    String getName();

    @NotNull ActivityPolicyRuleIdentifier getRuleIdentifier();

    void setCount(Integer localValue, Integer totalValue);

    void setCurrentState(ActivityPolicyStateType currentState);

    @NotNull List<PolicyActionType> getActions();

    @NotNull List<EvaluatedActivityPolicyRuleTrigger<?>> getTriggers();

    void setTriggers(List<EvaluatedActivityPolicyRuleTrigger<?>> triggers);

    boolean isTriggered();

    boolean hasThreshold();

    boolean isOverThreshold();
}
