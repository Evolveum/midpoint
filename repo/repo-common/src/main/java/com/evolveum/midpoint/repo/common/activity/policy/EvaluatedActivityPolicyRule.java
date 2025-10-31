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
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class EvaluatedActivityPolicyRule implements DebugDumpable {

    @NotNull private final ActivityPolicyRule policyRule;

    @NotNull private final List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    public EvaluatedActivityPolicyRule(@NotNull ActivityPolicyRule policyRule) {
        this.policyRule = policyRule;
    }

    public ActivityPolicyType getPolicy() {
        return policyRule.getPolicy();
    }

    public ActivityPath getPath() {
        return policyRule.getPath();
    }

    public String getName() {
        return policyRule.getName();
    }

    public @NotNull ActivityPolicyRuleIdentifier getRuleIdentifier() {
        return policyRule.getRuleIdentifier();
    }

    public void setCount(Integer localValue, Integer totalValue) {
        policyRule.setCount(localValue, totalValue);
    }

    public void setCurrentState(ActivityPolicyStateType currentState) {
        policyRule.setCurrentState(currentState);
    }

    @NotNull
    public List<ActivityPolicyActionType> getActions() {
        return policyRule.getActions();
    }

    public boolean isTriggered() {
        ActivityPolicyStateType currentState = policyRule.getCurrentState();
        return !triggers.isEmpty() || (currentState != null && !currentState.getTrigger().isEmpty());
    }

    @NotNull
    public List<EvaluatedActivityPolicyRuleTrigger<?>> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<EvaluatedActivityPolicyRuleTrigger<?>> triggers) {
        this.triggers.clear();

        if (triggers != null) {
            this.triggers.addAll(triggers);
        }
    }

    public boolean hasThreshold() {
        return getPolicy().getPolicyThreshold() != null;
    }

    public boolean isOverThreshold() {
        if (!hasThreshold()) {
            return true;
        }

        Integer count = policyRule.getTotalCount();
        if (count == null) {
            count = 0;
        }

        PolicyThresholdType policyThreshold = policyRule.getPolicy().getPolicyThreshold();
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

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDumpLabelLn(sb, "EvaluatedActivityPolicyRule " + (getName() != null ? getName() + " " : "") + "(triggers: " + triggers.size() + ")", indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, getPolicy(), ActivityPolicyType.COMPLEX_TYPE, PrismContext.LANG_XML);
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
