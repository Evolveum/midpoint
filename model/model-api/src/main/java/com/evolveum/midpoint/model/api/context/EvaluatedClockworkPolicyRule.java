/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRule;

import com.evolveum.midpoint.repo.common.policy.TriggerFilter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A policy rule that was evaluated during clockwork processing. It is either:
 *
 * - {@link DirectlyEvaluatedClockworkPolicyRule}, i.e., rule that was evaluated on an object or an assignment
 * (this is a typical scenario),
 *
 * - or {@link ForeignEvaluatedClockworkPolicyRule}, i.e., a rule that was evaluated and triggered on an assignment,
 * but is relevant also to a different assignment. This is typical for exclusion constraints that may be defined only
 * on one side (e.g. `Judge` having a rule "`Judge` excludes `Pirate`"), but should be applied also to the other side (`Pirate`).
 *
 * The purpose of this class is to provide the necessary functionality when such rules are returned by
 * {@link EvaluatedAssignment#getAllAssociatedPolicyRules()} method.
 *
 * A care must be taken how to interpret data returned from various getters in the case of foreign rules.
 * Please see {@link ForeignEvaluatedClockworkPolicyRule} for details.
 */
public interface EvaluatedClockworkPolicyRule extends EvaluatedPolicyRule {

    /**
     * The assignment that brought this policy rule to the focus or to the assignment target object.
     * May be missing e.g. for object-related global policy rules or object collection related policy rules.
     * (Note that assignment-related global policy rules have a value here.)
     */
    @Nullable EvaluatedAssignment getOriginatingAssignment();

    @Deprecated // for client scripts
    default EvaluatedAssignment getEvaluatedAssignment() {
        return getOriginatingAssignment();
    }

    /** For foreign rules, i.e. those that were "transplanted" to a different {@link EvaluatedAssignment}, here it is. */
    default @Nullable EvaluatedAssignment getAssignmentOverride() {
        return null;
    }

    default String getAssignmentOverrideShortDump() {
        EvaluatedAssignment assignmentOverride = getAssignmentOverride();
        if (assignmentOverride != null) {
            return String.format("from [%d] (-> %s)", assignmentOverride.getAssignmentId(), assignmentOverride.getTarget());
        } else {
            return null;
        }
    }

    /**
     * Returns a filter that keeps only triggers that are relevant for a policy rule after it was "transplanted"
     * to a different assignment. Approximate solution for now.
     *
     * @see ForeignEvaluatedClockworkPolicyRule
     * @see EvaluatedClockworkPolicyRuleTrigger#isRelevantForAssignmentOverride(EvaluatedAssignment)
     */
    default @NotNull TriggerFilter getRelevantTriggersFilter() {
        return trigger -> true; // by default, all triggers are relevant
    }

    /**
     * Was this rule triggered, i.e. are there any triggers? We do not distinguish between relevant and irrelevant
     * triggers here, as foreign rules should have always some triggers, so this is always `true` for them.
     */
    boolean isTriggered(); // the method is here only because of the javadoc

    /** Was the rule already evaluated? */
    boolean isEvaluated();

    /** Is this a global policy rule? */
    boolean isGlobal();

    /**
     * Returns all exclusion triggers that are relevant for the assignment owning this direct/foreign rule.
     *
     * @see DirectlyEvaluatedClockworkPolicyRule
     * @see ForeignEvaluatedClockworkPolicyRule
     */
    @NotNull Collection<EvaluatedExclusionTrigger> getRelevantExclusionTriggers();

    /** Returns short, (more or less) user-level characterization of this object. */
    String toShortString();

    static int getTriggeredRulesCount(Collection<? extends EvaluatedClockworkPolicyRule> policyRules) {
        return (int) policyRules.stream().filter(EvaluatedClockworkPolicyRule::isTriggered).count();
    }
}
