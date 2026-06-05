/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.util.DebugUtil.*;

import com.evolveum.midpoint.repo.common.policy.BaseEvaluatedPolicyRuleImpl;

import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;

import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRule;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * An implementation of {@link EvaluatedPolicyRule} specific for activity rules (dealing with execution time, restarts,
 * errors, etc).
 *
 * @see ActivityPolicyRule
 */
public class EvaluatedActivityPolicyRuleImpl
        extends BaseEvaluatedPolicyRuleImpl
        implements EvaluatedPolicyRule, DebugDumpable {

    /** TODO document this [pavol] */
    private final ActivityPolicyRule policyRule;

    EvaluatedActivityPolicyRuleImpl(@NotNull ActivityPolicyRule policyRule) {
        super(policyRule.getPolicyRuleConfigItem(), policyRule.getRuleIdentifier());
        this.policyRule = policyRule;
    }

    @Override
    public Integer getCount() {
        return policyRule.getTotalCount();
    }

    /** Activity for which this rule was defined. */
    @NotNull
    public ActivityPath getActivityPath() {
        return policyRule.getPath();
    }

    @Override
    public Collection<? extends PolicyActionConfigItem<?>> getEnabledActions() {
        // For activity policies, we consider all actions as enabled
        return getPolicyRuleConfigItem().getAllActions();
    }

    @Override
    public void setCount(Integer localValue, Integer totalValue) {
        policyRule.setCount(localValue, totalValue);
    }

    @Override
    public @NotNull Collection<EvaluatedPolicyRuleType> toEvaluatedPolicyRuleBeans(
            @NotNull PolicyRuleExternalizationOptions options,
            @Nullable Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector) {
        return List.of(); // We don't need this functionality here now; we are OK with serializing triggers
    }

    public void setCurrentState(ActivityPolicyStateType currentState) {
        policyRule.setCurrentState(currentState);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDumpLabelLn(sb, "EvaluatedActivityPolicyRule " + (getName() != null ? getName() + " " : "") + "(triggers: " + getTriggers().size() + ")", indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, getPolicyRuleBean(), PolicyRuleType.COMPLEX_TYPE, PrismContext.LANG_XML);
        sb.append('\n');
        debugDumpWithLabelLn(sb, "triggers", getTriggers(), indent + 1);
        return sb.toString();
    }

    public boolean isTriggered() {
        if (super.isTriggered()) {
            return true;
        }
        // The idea here is that trigger could occur during previous activity runs.
        ActivityPolicyStateType currentState = policyRule.getCurrentState();
        return currentState != null && !currentState.getTrigger().isEmpty();
    }

    @Override
    public String toString() {
        return "EvaluatedActivityPolicyRule{" +
                "policy=" + getName() +
                ", triggers=" + getTriggers().size() +
                '}';
    }
}
