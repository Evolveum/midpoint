/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind.ApplicabilityHint;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

import java.util.*;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.ApplicabilityHint.*;
import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.getConstraintNamesForApplicabilityRequirement;

/**
 * Code to determine whether a policy rule is to be applied in a given situation, i.e., when the application
 * to a focus object, an assignment, a projection, or an activity is being considered.
 *
 * The idea is that during processing, we gather policy rules from various sources (from system configuration,
 * from object marks, from assignments, from tasks/activities) and we have to know which of them should be applied e.g.
 * when a specific assignment is being processed.
 *
 * General algorithm for applicability determination is:
 *
 * . If `evaluationTarget` (`assignment`, `object`, `projection`, `activity`) for the rule is set, it is used as authoritative
 * information.
 *
 * . If it's not (which is good for simplicity), we apply some heuristics based on the constraints used in the rule.
 * See specific methods for details.
 */

public class PolicyRuleApplicabilityUtil {

    /** Names of constraints suggesting {@link ApplicabilityHint#ASSIGNMENT} use. */
    private static final Collection<String> ASSIGNMENT_RELATED_CONSTRAINT_NAMES;

    /** Names of constraints suggesting {@link ApplicabilityHint#FOCUS_OBJECT} use. */
    private static final Collection<String> FOCUS_OBJECT_RELATED_CONSTRAINT_NAMES;

    /** Names of constraints suggesting {@link ApplicabilityHint#ACTIVITY} use. */
    private static final Collection<String> ACTIVITY_RELATED_CONSTRAINT_NAMES;

    static {
        ASSIGNMENT_RELATED_CONSTRAINT_NAMES = getConstraintNamesForApplicabilityRequirement(ASSIGNMENT);
        FOCUS_OBJECT_RELATED_CONSTRAINT_NAMES = getConstraintNamesForApplicabilityRequirement(FOCUS_OBJECT);
        ACTIVITY_RELATED_CONSTRAINT_NAMES = getConstraintNamesForApplicabilityRequirement(ACTIVITY);
    }

    /**
     * Returns {@code true} if this policy rule can be applied to an assignment.
     */
    public static boolean isApplicableToAssignment(PolicyRuleType rule) {
        PolicyRuleEvaluationTargetType evaluationTarget = rule.getEvaluationTarget();
        if (evaluationTarget != null) {
            return evaluationTarget == PolicyRuleEvaluationTargetType.ASSIGNMENT;
        } else {
            // 1. An activity-requiring constraint prevent application to a focus object.
            // 2. An Assignment-requiring constraint indicates that the whole rule was meant for an assignment.
            // 3. Even if there is no constraint specific to assignments, we may evaluate the rule - if it does not contain
            // anything that is specific to objects. So "objectState" will not pass, but "true" or "situation" will.
            return !hasActivityRelatedConstraint(rule)
                    && (hasAssignmentRelatedConstraint(rule) || !hasObjectRelatedConstraint(rule));
        }
    }

    /**
     * Returns {@code true} if this policy rule can be applied to a focus object as a whole.
     */
    public static boolean isApplicableToFocusObject(PolicyRuleType rule) {
        PolicyRuleEvaluationTargetType evaluationTarget = rule.getEvaluationTarget();
        if (evaluationTarget != null) {
            return evaluationTarget == PolicyRuleEvaluationTargetType.OBJECT;
        } else {
            // These kinds of constraints simply prevent application to a focus object.
            return !hasAssignmentRelatedConstraint(rule) && !hasActivityRelatedConstraint(rule);
        }
    }

    /**
     * Returns {@code true} if this policy rule can be applied to a projection.
     *
     * Currently, we don't provide heuristics here. These rules must be explicitly marked by `evaluationTarget`.
     */
    public static boolean isApplicableToProjection(PolicyRuleType rule) {
        return rule.getEvaluationTarget() == PolicyRuleEvaluationTargetType.PROJECTION;
    }

    /**
     * Returns {@code true} if this policy rule can be applied to a projection.
     *
     * Currently, we don't support `evaluationTarget` setting here, because the distinction can be based solely on the presence
     * of activity-specific constraints.
     */
    public static boolean isApplicableToActivity(PolicyRuleType rule) {
        return hasActivityRelatedConstraint(rule);
    }

    @FunctionalInterface
    private interface ConstraintPredicate {
        boolean test(QName name);
    }

    /**
     * Returns {@code true} if the rule has a constraint matching given predicate.
     * We ignore roots and references.
     */
    private static boolean hasSpecificConstraint(PolicyRuleType rule, ConstraintPredicate predicate) {
        // 'accept' continues until predicate returns 'true' (so the visitor returns false to the 'accept' method).
        // Then, 'accept' returns 'false' to us. So we know that the predicate matched for some constraint.
        return !PolicyRuleTypeUtil.accept(
                rule.getPolicyConstraints(),
                (name, c) -> !predicate.test(name),
                true,
                false,
                null,
                false);
    }

    /**
     * Returns {@code true} if the rule contains a constraint that is related to assignments
     * (like {@link PolicyConstraintKind#ASSIGNMENT_MODIFICATION}).
     */
    private static boolean hasAssignmentRelatedConstraint(PolicyRuleType rule) {
        return hasSpecificConstraint(rule, name -> ASSIGNMENT_RELATED_CONSTRAINT_NAMES.contains(name.getLocalPart()));
    }

    /**
     * Returns {@code true} if the rule contains a constraint that is related to focus objects
     * (like {@link PolicyConstraintKind#OBJECT_STATE}).
     *
     * Notable exception: {@link PolicyConstraintKind#MIN_ASSIGNEES} and {@link PolicyConstraintKind#MAX_ASSIGNEES} can be
     * evaluated on both objects and assignments. They are considered to be assignment related.
     */
    private static boolean hasObjectRelatedConstraint(PolicyRuleType rule) {
        return hasSpecificConstraint(rule, name -> FOCUS_OBJECT_RELATED_CONSTRAINT_NAMES.contains(name.getLocalPart()));
    }

    /**
     * Returns {@code true} if the rule contains a constraint that is specific to activities
     * (like {@link PolicyConstraintKind#EXECUTION_TIME}).
     *
     * Currently, such rules are mutually exclusive with rules containing focus, projection, and assignments constraints.
     */
    private static boolean hasActivityRelatedConstraint(PolicyRuleType rule) {
        return hasSpecificConstraint(rule, name -> ACTIVITY_RELATED_CONSTRAINT_NAMES.contains(name.getLocalPart()));
    }
}
