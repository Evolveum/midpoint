/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;

public class EvaluatedActivityPolicyRule implements EvaluatedPolicyRule, DebugDumpable {

    private final @NotNull ActivityPolicyType policy;

    private final @NotNull ActivityPath path;

    private final List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    private final List<EvaluatedPolicyReaction> reactions = new ArrayList<>();

    /**
     * Whether the rule was enforced (i.e., the action was taken).
     */
    private boolean enforced;

    private ActivityPolicyStateType currentState;

    private ThresholdValueType thresholdValueType;

    private Object thresholdValue;

    public EvaluatedActivityPolicyRule(@NotNull ActivityPolicyType policy, @NotNull ActivityPath path) {
        this.policy = policy;
        this.path = path;
    }

    @Override
    public String getRuleIdentifier() {
        return ActivityPolicyUtils.createIdentifier(path, policy);
    }

    @Override
    public String getName() {
        return policy.getName();
    }

    @Override
    public Integer getOrder() {
        return policy.getOrder();
    }

    @Override
    public Object getThresholdValue() {
        return thresholdValue;
    }

    @Override
    public @NotNull ThresholdValueType getThresholdValueType() {
        return thresholdValueType != null ? thresholdValueType : ThresholdValueType.COUNTER;
    }

    @Override
    public void setThresholdValueType(@NotNull ThresholdValueType thresholdValueType, Object value) {
        if (this.thresholdValueType != null && this.thresholdValueType != thresholdValueType) {
            throw new IllegalStateException(
                    "Cannot change threshold value type from " + this.thresholdValueType + " to " + thresholdValueType);
        }

        this.thresholdValueType = thresholdValueType;
        this.thresholdValue = value;
    }

    @NotNull
    public ActivityPolicyType getPolicy() {
        return policy;
    }

    @Override
    public boolean hasThreshold() {
        return policy.getPolicyReaction().stream()
                .anyMatch(r -> r.getThreshold() != null);
    }

    public List<EvaluatedPolicyReaction> getApplicableReactions() {
        return policy.getPolicyReaction().stream()
                .map(r -> new EvaluatedPolicyReaction(this, r))
                .filter(r -> !r.hasThreshold() || r.isWithinThreshold())
                .toList();
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

    public ActivityPolicyStateType getCurrentState() {
        return currentState;
    }

    public void setCurrentState(ActivityPolicyStateType currentState) {
        this.currentState = currentState;
    }

    @Override
    public boolean isTriggered() {
        return !triggers.isEmpty() || (currentState != null && !currentState.getTrigger().isEmpty());
    }

    public boolean isEnforced() {
        return enforced || (currentState != null && currentState.isEnforced());
    }

    public void enforced() {
        enforced = true;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDumpLabelLn(sb, "EvaluatedActivityPolicyRule " + (getName() != null ? getName() + " " : "") + "(triggers: " + triggers.size() + ")", indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, policy, ActivityPolicyType.COMPLEX_TYPE, PrismContext.LANG_XML);
        sb.append('\n');
        debugDumpWithLabelLn(sb, "triggers", triggers, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedActivityPolicyRule{" +
                "policy=" + policy.getName() +
                ", triggers=" + triggers.size() +
                '}';
    }
}
