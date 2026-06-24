/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.schema.policy.PolicyRuleApplicabilityUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

import org.jetbrains.annotations.NotNull;

/**
 * An {@link EvaluatedClockworkPolicyRule} that was directly evaluated against an object or an assignment.
 *
 * Such a rule can then be co-attached to a related assignment (e.g. in the case of exclusion violation defined on
 * a single side only) by wrapping in {@link ForeignEvaluatedClockworkPolicyRule}.
 *
 * @see EvaluatedAssignment#getAllAssociatedPolicyRules()
 * @see EvaluatedClockworkPolicyRule
 */
public interface DirectlyEvaluatedClockworkPolicyRule extends EvaluatedClockworkPolicyRule {

    /**
     * Information about exact place where the rule was found. This can be important for rules that are
     * indirectly attached to an assignment.
     *
     * An example: Let `Engineer` induce `Employee` which conflicts with `Contractor`. The SoD rule is attached
     * to `Employee`. But let the user have assignments for `Engineer` and `Contractor` only. When evaluating
     * `Engineer` assignment, we find a (indirectly attached) SoD rule. But we need to know it came from `Employee`.
     * This is what `assignmentPath` (`Engineer`->`Employee`->(maybe some metarole)->rule) is for.
     *
     * For global policy rules, `assignmentPath` is the path to the target object that matched global policy rule.
     *
     * See also {@link #getTargetType()}.
     *
     * It can null for artificially-created policy rules e.g. in task validity cases. TODO To be reviewed.
     */
    AssignmentPath getRuleAssignmentPath();

    @Deprecated // for client scripts
    default AssignmentPath getAssignmentPath() {
        return getRuleAssignmentPath();
    }

    /** To which object is the policy rule targeted and how. See {@link TargetType} for detailed explanation. */
    @NotNull TargetType getTargetType();

    /** @see PolicyRuleApplicabilityUtil#isApplicableToFocusObject(PolicyRuleType) */
    default boolean isApplicableToFocusObject() {
        return PolicyRuleApplicabilityUtil.isApplicableToFocusObject(getPolicyRuleBean());
    }

    /** @see PolicyRuleApplicabilityUtil#isApplicableToProjection(PolicyRuleType) */
    default boolean isApplicableToProjection() {
        return PolicyRuleApplicabilityUtil.isApplicableToProjection(getPolicyRuleBean());
    }

    /** @see PolicyRuleApplicabilityUtil#isApplicableToAssignment(PolicyRuleType) */
    default boolean isApplicableToAssignment() {
        return PolicyRuleApplicabilityUtil.isApplicableToAssignment(getPolicyRuleBean());
    }

    /**
     * To which object is the policy rule (that has this target type) targeted, from the point of assignment mechanisms - and how?
     */
    enum TargetType {

        /** The rule targets the focus or one of its projections. It is assigned to the focus object. */
        OBJECT,

        /** The rule targets a specific assignment. It is directly assigned to the target of this assignment. */
        DIRECT_ASSIGNMENT_TARGET,

        /**
         * The rule targets a specific assignment. It is indiretly assigned to the target of this assignment.
         *
         * An example: Let `Engineer` induce `Employee` which conflicts with `Contractor`. An SoD rule is attached
         * to `Employee`. But let the user have assignments for `Engineer` and `Contractor` only. So the target type
         * this such rule is this one: indirect assignment target.
         */
        INDIRECT_ASSIGNMENT_TARGET
    }
}
