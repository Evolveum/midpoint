/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleExternalizationOptions;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleIdentifier;

import com.evolveum.midpoint.repo.common.policy.TriggerFilter;
import com.evolveum.midpoint.schema.config.AbstractPolicyRuleConfigItem;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.impl.lens.DirectlyEvaluatedClockworkPolicyRuleImpl;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleType;

/**
 * A direct implementation of {@link ForeignEvaluatedClockworkPolicyRule} that simply delegates to the original policy rule.
 */
public class ForeignEvaluatedClockworkPolicyRuleImpl implements ForeignEvaluatedClockworkPolicyRule {

    /**
     * The original policy rule. Beware, it is seen from the point of view of the original assignment on which it was evaluated!
     */
    @NotNull private final DirectlyEvaluatedClockworkPolicyRuleImpl originalPolicyRule;

    /** The new owner - i.e. assignment onto which the rule is "transplanted". */
    @NotNull private final EvaluatedAssignmentImpl<?> assignmentOverride;

    private ForeignEvaluatedClockworkPolicyRuleImpl(
            @NotNull DirectlyEvaluatedClockworkPolicyRuleImpl originalPolicyRule,
            @NotNull EvaluatedAssignmentImpl<?> assignmentOverride) {
        this.originalPolicyRule = originalPolicyRule;
        this.assignmentOverride = assignmentOverride;
    }

    public static ForeignEvaluatedClockworkPolicyRuleImpl of(
            @NotNull DirectlyEvaluatedClockworkPolicyRuleImpl rule, @NotNull EvaluatedAssignmentImpl<?> newOwner) {
        return new ForeignEvaluatedClockworkPolicyRuleImpl(rule, newOwner);
    }

    @Override
    public ActivityPath getActivityPath() {
        return originalPolicyRule.getActivityPath();
    }

    @Override
    public @Nullable EvaluatedAssignment getOriginatingAssignment() {
        return originalPolicyRule.getOriginatingAssignment();
    }

    @Override
    public @NotNull PolicyRuleIdentifier getRuleIdentifier() {
        return originalPolicyRule.getRuleIdentifier();
    }

    @Override
    public boolean isGlobal() {
        return originalPolicyRule.isGlobal();
    }

    @Override
    public Collection<? extends PolicyActionConfigItem<?>> getEnabledActions() {
        return originalPolicyRule.getEnabledActions();
    }

    @Override
    public Integer getCount() {
        return originalPolicyRule.getCount(); // TODO reconsider [pavol]
    }

    @Override
    public void setCount(Integer localValue, Integer totalValue) {
        originalPolicyRule.setCount(localValue, totalValue); // TODO reconsider [pavol]
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "original policy rule", originalPolicyRule, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "current owner", assignmentOverride.toString(), indent + 1);
        return sb.toString();
    }

    @Override
    public String toShortString() {
        return getAssignmentOverrideShortDump() + " " + originalPolicyRule.toShortString();
    }

    @Override
    public boolean isTriggered() {
        return originalPolicyRule.isTriggered();
    }

    @Override
    public boolean isEvaluated() {
        return originalPolicyRule.isEvaluated();
    }

    @Override
    public @NotNull Collection<EvaluatedExclusionTrigger> getRelevantExclusionTriggers() {
        return originalPolicyRule.getAllTriggers(EvaluatedExclusionTrigger.class).stream()
                .filter(t -> t.getConflictingAssignment().equals(assignmentOverride))
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> getTriggers() {
        return originalPolicyRule.getTriggers();
    }

    @Override
    public @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers() {
        return originalPolicyRule.getAllTriggers();
    }

    @Override
    public <T extends EvaluatedPolicyRuleTrigger<?>> Collection<T> getAllTriggers(Class<T> type) {
        return originalPolicyRule.getAllTriggers(type);
    }

    @Override
    public void trigger(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        originalPolicyRule.trigger(triggers);
    }

    @Override
    public List<TreeNode<LocalizableMessage>> extractMessages() {
        return originalPolicyRule.extractMessages();
    }

    @Override
    public List<TreeNode<LocalizableMessage>> extractShortMessages() {
        return originalPolicyRule.extractShortMessages();
    }

    @Override
    public @NotNull EvaluatedAssignmentImpl<?> getAssignmentOverride() {
        return assignmentOverride;
    }

    @Override
    public @NotNull TriggerFilter getRelevantTriggersFilter() {
        return trigger -> trigger instanceof EvaluatedClockworkPolicyRuleTrigger<?> clockworkTrigger
                && clockworkTrigger.isRelevantForAssignmentOverride(assignmentOverride);
    }

    @Override
    public @Nullable String getPolicySituation() {
        return originalPolicyRule.getPolicySituation();
    }

    @Override
    public @NotNull AbstractPolicyRuleConfigItem<?> getPolicyRuleConfigItem() {
        return originalPolicyRule.getPolicyRuleConfigItem();
    }

    @Override
    public @NotNull Collection<EvaluatedPolicyRuleType> toEvaluatedPolicyRuleBeans(
            @NotNull PolicyRuleExternalizationOptions options,
            @Nullable Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector) {
        return originalPolicyRule.toEvaluatedPolicyRuleBeans(options, triggerSelector);
    }
}
