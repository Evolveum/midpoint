/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * {@link EvaluatedPolicyRule} that is associated to given {@link EvaluatedAssignment}, either as "own"
 * (i.e. directly evaluated and triggered on) or as "foreign" (triggered on another assignment but connected to this one,
 * typically because of an exclusion constraint).
 *
 * The purpose of this class is to provide the necessary functionality when such rules are returned by
 * {@link EvaluatedAssignment#getAllAssociatedPolicyRules()} method.
 *
 * Important things to consider:
 *
 * . The exclusion constraints on foreign policy rules are just as they are triggered on their original assignments.
 * It means that exclusion triggers on them point to the current assigment as {@link EvaluatedExclusionTrigger#conflictingTarget},
 * and to original one as {@link EvaluatedExclusionTrigger#thisTarget}. See also
 * {@link EvaluatedExclusionTrigger#getRealConflictingAssignment(EvaluatedAssignment)}.
 *
 * . Also note that not all triggers on the original policy rule are relevant to this (new) assignment. For example,
 * if a role `coordinator` excludes all roles of type `worker`, and has appropriate policy rule on it, then when you
 * are assigning `worker-1` and `worker-2`, both get this policy rule (with 2 triggers) as a foreign rule. However, only
 * one trigger is relevant for each of the workers. Hence, use appropriate method to select relevant triggers, e.g.
 * {@link #getRelevantExclusionTriggers()} or {@link EvaluatedPolicyRuleTrigger#isRelevantForNewOwner(EvaluatedAssignment)}.
 */
@Experimental
public interface AssociatedPolicyRule extends DebugDumpable, Serializable, Cloneable {

    /** Automatically generated identifier that - we hope - uniquely identifies the policy rule. */
    @NotNull String getPolicyRuleIdentifier();

    /**
     * Was this rule triggered, i.e. are there any triggers? We do not distinguish between relevant and irrelevant
     * triggers here, as foreign rules should have always some triggers, so this is always `true` for them.
     */
    boolean isTriggered();

    /** TODO */
    boolean isEvaluated();

    /** Are there any enabled actions of given type? */
    boolean containsEnabledAction(Class<? extends PolicyActionType> type);

    /** Returns enabled action of given type, if there's any. Throws an exception if there are more of them. */
    <T extends PolicyActionType> @Nullable PolicyActionConfigItem<T> getEnabledAction(Class<T> type);

    /** Returns all enabled actions of given type. */
    <T extends PolicyActionType> @NotNull List<? extends PolicyActionConfigItem<T>> getEnabledActions(Class<T> type);

    /** Returns exclusion triggers without ones that are not relevant for given "new owner" (see class javadoc). */
    @NotNull Collection<EvaluatedExclusionTrigger> getRelevantExclusionTriggers();

    /** Returns short, (more or less) user-level characterization of this object. */
    String toShortString();

    /** Returns new owner (for foreign rules) or `null` (for original ones). */
    @Nullable EvaluatedAssignment getNewOwner();

    /** Returns the original policy rule. */
    @NotNull EvaluatedPolicyRule getEvaluatedPolicyRule();

    default String getNewOwnerShortString() {
        EvaluatedAssignment newOwner = getNewOwner();
        if (newOwner != null) {
            return String.format("from [%d] (-> %s)", newOwner.getAssignmentId(), newOwner.getTarget());
        } else {
            return "";
        }
    }

    /** Returns the policy situation connected to this rule. Will be replaced by object marks. */
    @Nullable String getPolicySituation();

    /**
     * Serializes the policy rule into bean form ({@link EvaluatedPolicyRuleType}).
     *
     * Currently not very nice contract, should be improved later.
     *
     * @param ruleBeans Collection of beans into which to put the result.
     * @param options Options - how the serialization should take place.
     * @param triggerSelector Which triggers should be processed?
     * @param newOwner If set, we should ignore triggers not relevant for this evaluated assignment.
     */
    void addToEvaluatedPolicyRuleBeans(
            @NotNull Collection<EvaluatedPolicyRuleType> ruleBeans,
            @NotNull PolicyRuleExternalizationOptions options,
            @Nullable Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector,
            @Nullable EvaluatedAssignment newOwner);

    /**
     * Adds a trigger to the policy rule.
     * For internal use only.
     */
    void addTrigger(@NotNull EvaluatedPolicyRuleTrigger<?> trigger);

    static int getTriggeredRulesCount(Collection<? extends AssociatedPolicyRule> policyRules) {
        return (int) policyRules.stream().filter(AssociatedPolicyRule::isTriggered).count();
    }

    static boolean contains(List<? extends AssociatedPolicyRule> rules, AssociatedPolicyRule otherRule) {
        return rules.stream()
                .anyMatch(r -> r.getPolicyRuleIdentifier().equals(otherRule.getPolicyRuleIdentifier()));
    }
}
