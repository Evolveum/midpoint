/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleEvaluationTargetType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.ApplicabilityHint.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.*;

/**
 * Characterizes a policy constraint kind from various points of view: applicability, visualization, etc.
 *
 * Terminology:
 *
 * - _policy constraint_ is the specific constraint, e.g. "hasAssignment of Superuser"; i.e. kind + parameters.
 * - _policy constraint kind_ is the kind (type) of the constraint, e.g. "hasAssignment".
 *
 * Later we may split this class into separate classes, but for now, this is a convenient place where to add all
 * static information related to individual policy constraints kinds - to avoid forgetting them when introducing
 * new constraint kinds.
 *
 * @see PolicyConstraintKindType
 */
public enum PolicyConstraintKind {

    OBJECT_STATE(
            F_OBJECT_STATE,
            PolicyConstraintKindType.OBJECT_STATE,
            "o-state",
            FOCUS_OBJECT),

    ASSIGNMENT_STATE(
            F_ASSIGNMENT_STATE,
            PolicyConstraintKindType.ASSIGNMENT_STATE,
            "a-state",
            ASSIGNMENT),

    HAS_ASSIGNMENT(
            F_HAS_ASSIGNMENT,
            PolicyConstraintKindType.HAS_ASSIGNMENT,
            "has-a",
            FOCUS_OBJECT),

    HAS_NO_ASSIGNMENT(
            F_HAS_NO_ASSIGNMENT,
            PolicyConstraintKindType.HAS_NO_ASSIGNMENT,
            "no-a",
            FOCUS_OBJECT),

    REQUIREMENT(
            F_REQUIREMENT,
            PolicyConstraintKindType.REQUIREMENT,
            "req",
            ASSIGNMENT),

    EXCLUSION(
            F_EXCLUSION,
            PolicyConstraintKindType.EXCLUSION,
            "exc",
            ASSIGNMENT),

    /**
     * This and the following constraint are a bit special. They CAN be evaluated against objects, but are typically used for
     * assignments. Hence, we mark them as applicable to assignments.
     *
     * If you need to use them to objects, you must set the `evaluationTarget` to {@link PolicyRuleEvaluationTargetType#OBJECT}
     * for the rule.
     */
    MIN_ASSIGNEES(
            F_MIN_ASSIGNEES,
            PolicyConstraintKindType.MIN_ASSIGNEES_VIOLATION,
            "min-a",
            ASSIGNMENT),

    /** See {@link #MIN_ASSIGNEES}. */
    MAX_ASSIGNEES(
            F_MAX_ASSIGNEES,
            PolicyConstraintKindType.MAX_ASSIGNEES_VIOLATION,
            "max-a",
            ASSIGNMENT),

    // TODO is this a good name for a constraint? It was chosen, as it is similar to "objectState". But its awkward.
    OBJECT_MIN_ASSIGNEES_VIOLATION(
            F_OBJECT_MIN_ASSIGNEES_VIOLATION,
            PolicyConstraintKindType.OBJECT_MIN_ASSIGNEES_VIOLATION,
            "min-a-violation",
            FOCUS_OBJECT),

    // TODO the same as above
    OBJECT_MAX_ASSIGNEES_VIOLATION(
            F_OBJECT_MAX_ASSIGNEES_VIOLATION,
            PolicyConstraintKindType.OBJECT_MAX_ASSIGNEES_VIOLATION,
            "max-a-violation",
            FOCUS_OBJECT),

    OBJECT_MODIFICATION(
            F_MODIFICATION,
            PolicyConstraintKindType.OBJECT_MODIFICATION,
            "o-mod",
            FOCUS_OBJECT),

    ASSIGNMENT_MODIFICATION(
            F_ASSIGNMENT,
            PolicyConstraintKindType.ASSIGNMENT_MODIFICATION,
            "a-mod",
            ASSIGNMENT),

    OBJECT_TIME_VALIDITY(
            F_OBJECT_TIME_VALIDITY,
            PolicyConstraintKindType.OBJECT_TIME_VALIDITY,
            "o-time",
            FOCUS_OBJECT),

    ASSIGNMENT_TIME_VALIDITY(
            F_ASSIGNMENT_TIME_VALIDITY,
            PolicyConstraintKindType.ASSIGNMENT_TIME_VALIDITY,
            "a-time",
            ASSIGNMENT),

    SITUATION(
            F_SITUATION,
            PolicyConstraintKindType.SITUATION,
            "sit",
            GENERAL),

    CUSTOM(
            F_CUSTOM,
            PolicyConstraintKindType.CUSTOM,
            "custom",
            GENERAL),

    COLLECTION_STATS(
            F_COLLECTION_STATS,
            PolicyConstraintKindType.COLLECTION_STATS,
            "coll-stats",
            GENERAL),

    ALWAYS_TRUE(
            F_ALWAYS_TRUE,
            PolicyConstraintKindType.ALWAYS_TRUE,
            "true",
            GENERAL),

    // very experimental
    ORPHANED(
            F_ORPHANED,
            PolicyConstraintKindType.ORPHANED,
            "orphaned",
            FOCUS_OBJECT), // actually acts on task objects

    AND(
            F_AND,
            PolicyConstraintKindType.AND,
            "*",
            GENERAL),

    OR(
            F_OR,
            PolicyConstraintKindType.OR,
            "|",
            GENERAL),

    NOT(
            F_NOT,
            PolicyConstraintKindType.NOT,
            "!",
            GENERAL),

    TRANSITION(
            F_TRANSITION,
            PolicyConstraintKindType.TRANSITION,
            "trans",
            GENERAL),

    EXECUTION_TIME(
            F_EXECUTION_TIME,
            PolicyConstraintKindType.EXECUTION_TIME,
            "exec-time",
            ACTIVITY),

    EXECUTION_ATTEMPTS(
            F_EXECUTION_ATTEMPTS,
            PolicyConstraintKindType.EXECUTION_ATTEMPTS,
            "exec-attempts",
            ACTIVITY),

    ITEM_PROCESSING_RESULT(
            F_ITEM_PROCESSING_RESULT,
            PolicyConstraintKindType.ITEM_PROCESSING_RESULT,
            "result",
            ACTIVITY);

    // Note that "ref" is not a constraint. It is just a reference (pointer) to it.

    /** The local part of the item name in {@link PolicyConstraintsType}. */
    @NotNull private final String itemNameLocalPart;

    /** Corresponding XML/JSON/YAML version of the constraint kind. */
    @NotNull private final PolicyConstraintKindType serializableVersion;

    /** Symbol used for diagnostic and similar outputs. */
    @NotNull private final String symbol;

    /** What this constraint kind requires to be evaluated (applied), like focus object, assignment, activity - or nothing. */
    @NotNull private final ApplicabilityHint applicabilityHint;

    static PolicyConstraintKind findByItemName(@NotNull QName name) {
        var localPart = name.getLocalPart();
        for (var constraint : PolicyConstraintKind.values()) {
            if (constraint.itemNameLocalPart.equals(localPart)) {
                return constraint;
            }
        }
        throw new IllegalStateException("No constraint found for " + name);
    }

    static Set<String> getConstraintNamesForApplicabilityRequirement(ApplicabilityHint applicabilityHint) {
        return Arrays.stream(values())
                .filter(c -> c.applicabilityHint == applicabilityHint)
                .map(c -> c.itemNameLocalPart)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unused")
    public @NotNull String getItemNameLocalPart() {
        return itemNameLocalPart;
    }

    public @NotNull PolicyConstraintKindType getSerializableVersion() {
        return serializableVersion;
    }

    public @NotNull String getSymbol() {
        return symbol;
    }

    @SuppressWarnings("unused")
    public @NotNull ApplicabilityHint getApplicabilityRequirement() {
        return applicabilityHint;
    }

    PolicyConstraintKind(
            @NotNull QName itemName,
            @NotNull PolicyConstraintKindType serializableVersion,
            @NotNull String symbol,
            @NotNull ApplicabilityHint applicabilityHint) {
        this.itemNameLocalPart = itemName.getLocalPart();
        this.serializableVersion = serializableVersion;
        this.symbol = symbol;
        this.applicabilityHint = applicabilityHint;
    }

    @Override
    public String toString() {
        return itemNameLocalPart + " (" + applicabilityHint + ")";
    }

    /**
     * Indication of the target, on which the constraint is to be applied: focus object, assignment, activity.
     *
     * @see PolicyRuleApplicabilityUtil
     */
    public enum ApplicabilityHint {

        /** Indicates a constraint that is to be applied to a focus object. */
        FOCUS_OBJECT,

        /** Indicates a constraint that is to be applied to an assignment. */
        ASSIGNMENT,

        /** Indicates a constraint that is to be applied to an activity. */
        ACTIVITY,

        /** Indicates a general constraint (and, or, not, ...) */
        GENERAL
    }
}
