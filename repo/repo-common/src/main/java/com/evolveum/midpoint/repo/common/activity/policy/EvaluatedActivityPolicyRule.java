/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.util.DebugUtil.*;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.repo.common.policy.GenericEvaluatedPolicyRule;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

public class EvaluatedActivityPolicyRule implements GenericEvaluatedPolicyRule, DebugDumpable {

    @NotNull private final ActivityPolicyRule policyRule;

    @NotNull private final List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    public EvaluatedActivityPolicyRule(@NotNull ActivityPolicyRule policyRule) {
        this.policyRule = policyRule;
    }

    @Override
    public ActivityPolicyRule getActivityPolicyRule() {
        return policyRule;
    }

    @Override
    public Integer getCount() {
        return policyRule.getTotalCount();
    }

    @NotNull
    @Override
    public PolicyRuleType getPolicyRule() {
        return policyRule.getPolicy();
    }

    @Override
    public ActivityPath getPath() {
        return policyRule.getPath();
    }

    @Override
    public String getName() {
        return policyRule.getName();
    }

    @Override
    public Integer getOrder() {
        return policyRule.getOrder();
    }

    @Override
    public @NotNull PolicyRuleIdentifier getRuleIdentifier() {
        return policyRule.getRuleIdentifier();
    }

    @Override
    public void setCount(Integer localValue, Integer totalValue) {
        policyRule.setCount(localValue, totalValue);
    }

    @Override
    public void setCurrentState(ActivityPolicyStateType currentState) {
        policyRule.setCurrentState(currentState);
    }

    @NotNull
    @Override
    public List<PolicyActionType> getActions() {
        return policyRule.getActions();
    }

    @Override
    public boolean isTriggered() {
        ActivityPolicyStateType currentState = policyRule.getCurrentState();
        return !triggers.isEmpty() || (currentState != null && !currentState.getTrigger().isEmpty());
    }

    @NotNull
    @Override
    public List<EvaluatedPolicyRuleTrigger<?>> getTriggers() {
        return triggers;
    }

    @Override
    public void setTriggers(List<EvaluatedPolicyRuleTrigger<?>> triggers) {
        this.triggers.clear();

        if (triggers != null) {
            this.triggers.addAll(triggers);
        }
    }

    public boolean hasThreshold() {
        return getPolicyRule().getPolicyThreshold() != null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDumpLabelLn(sb, "EvaluatedActivityPolicyRule " + (getName() != null ? getName() + " " : "") + "(triggers: " + triggers.size() + ")", indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, getPolicyRule(), PolicyRuleType.COMPLEX_TYPE, PrismContext.LANG_XML);
        sb.append('\n');
        debugDumpWithLabelLn(sb, "triggers", triggers, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedActivityPolicyRule{" +
                "policy=" + getName() +
                ", triggers=" + triggers.size() +
                '}';
    }
}
