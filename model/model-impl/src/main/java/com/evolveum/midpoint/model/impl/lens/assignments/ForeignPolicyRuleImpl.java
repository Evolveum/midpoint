/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;

import org.jetbrains.annotations.Nullable;

/**
 * A rule that has been "transplanted" onto new owner ({@link EvaluatedAssignment}) - currently, the other side
 * of "one-sided" exclusion constraint.
 */
public class ForeignPolicyRuleImpl implements AssociatedPolicyRule {

    /**
     * The original policy rule. Beware, it is seen from the point of view of the original assignment on which it was evaluated!
     */
    @NotNull private final EvaluatedPolicyRuleImpl evaluatedPolicyRule;

    /** The new owner - i.e. assignment onto which the rule is "transplanted". */
    @NotNull private final EvaluatedAssignmentImpl<?> newOwner;

    private ForeignPolicyRuleImpl(
            @NotNull EvaluatedPolicyRuleImpl evaluatedPolicyRule,
            @NotNull EvaluatedAssignmentImpl<?> newOwner) {
        this.evaluatedPolicyRule = evaluatedPolicyRule;
        this.newOwner = newOwner;
    }

    public static ForeignPolicyRuleImpl of(@NotNull EvaluatedPolicyRuleImpl rule, @NotNull EvaluatedAssignmentImpl<?> newOwner) {
        return new ForeignPolicyRuleImpl(rule, newOwner);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "original policy rule", evaluatedPolicyRule, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "current owner", newOwner.toString(), indent + 1);
        return sb.toString();
    }

    @Override
    public String toShortString() {
        return getNewOwnerShortString() + " " + evaluatedPolicyRule.toShortString();
    }

    @Override
    public @NotNull String getPolicyRuleIdentifier() {
        return evaluatedPolicyRule.getPolicyRuleIdentifier();
    }

    @Override
    public boolean isTriggered() {
        return evaluatedPolicyRule.isTriggered();
    }

    @Override
    public boolean containsEnabledAction(Class<? extends PolicyActionType> type) {
        return evaluatedPolicyRule.containsEnabledAction(type);
    }

    @Override
    public <T extends PolicyActionType> T getEnabledAction(Class<T> type) {
        return evaluatedPolicyRule.getEnabledAction(type);
    }

    @Override
    public @NotNull <T extends PolicyActionType> List<T> getEnabledActions(Class<T> type) {
        return evaluatedPolicyRule.getEnabledActions(type);
    }

    @Override
    public @NotNull Collection<EvaluatedExclusionTrigger> getRelevantExclusionTriggers() {
        return evaluatedPolicyRule.getAllTriggers(EvaluatedExclusionTrigger.class).stream()
                .filter(t -> t.getConflictingAssignment().equals(newOwner))
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull EvaluatedAssignmentImpl<?> getNewOwner() {
        return newOwner;
    }

    @Override
    public @NotNull EvaluatedPolicyRuleImpl getEvaluatedPolicyRule() {
        return evaluatedPolicyRule;
    }

    @Override
    public @Nullable String getPolicySituation() {
        return evaluatedPolicyRule.getPolicySituation();
    }

    @Override
    public void addToEvaluatedPolicyRuleBeans(
            @NotNull Collection<EvaluatedPolicyRuleType> ruleBeans,
            @NotNull PolicyRuleExternalizationOptions options,
            @Nullable Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector,
            @Nullable EvaluatedAssignment newOwner) {
        assert newOwner == null || newOwner == this.newOwner;
        evaluatedPolicyRule.addToEvaluatedPolicyRuleBeansInternal(ruleBeans, options, triggerSelector, this.newOwner);
    }

    @Override
    public void addTrigger(@NotNull EvaluatedPolicyRuleTrigger<?> trigger) {
        evaluatedPolicyRule.addTrigger(trigger);
    }
}
