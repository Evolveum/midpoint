/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * This is to reduce the number of parameters passed between methods in this class.
 * Moreover, it highlights the fact that identity of objects referenced here is fixed for any invocation of the evaluate() method.
 * (There is single EvaluationContext instance for any call to evaluate().)
 */

class EvaluationContext<AH extends AssignmentHolderType> {

    /**
     * Evaluated assignment. Here we collect all relevant information.
     */
    @NotNull final EvaluatedAssignmentImpl<AH> evalAssignment;

    /**
     * Assignment path: initially empty. Segments are being added and removed from here.
     */
    @NotNull final AssignmentPathImpl assignmentPath;

    /**
     * Tells whether the primary assignment was added, removed or it is unchanged.
     *
     * The primary (direct) assignment is the first assignment in the assignment path,
     * i.e. the assignment that is located in the focal object.
     */
    final PlusMinusZero primaryAssignmentMode;

    /**
     * True if we are evaluating old state of the assignment.
     */
    final boolean evaluateOld;

    /**
     * The task.
     */
    @NotNull final Task task;

    /**
     * Assignment evaluator itself. We use this (strangely named) field to access the broader
     * evaluation context via "ctx.ae" reference.
     */
    @NotNull final AssignmentEvaluator<AH> ae;

    /**
     * Used to collect membership information.
     */
    @NotNull final TargetMembershipCollector membershipCollector;

    /**
     * Used to evaluate conditions on assignments and abstract roles (targets).
     */
    @NotNull final ConditionEvaluator conditionEvaluator;

    EvaluationContext(@NotNull EvaluatedAssignmentImpl<AH> evalAssignment,
            @NotNull AssignmentPathImpl assignmentPath,
            PlusMinusZero primaryAssignmentMode, boolean evaluateOld,
            @NotNull Task task,
            @NotNull AssignmentEvaluator<AH> assignmentEvaluator) {
        this.evalAssignment = evalAssignment;
        this.assignmentPath = assignmentPath;
        this.primaryAssignmentMode = primaryAssignmentMode;
        this.evaluateOld = evaluateOld;
        this.task = task;
        this.ae = assignmentEvaluator;
        this.membershipCollector = new TargetMembershipCollector(this);
        this.conditionEvaluator = new ConditionEvaluator(this);
    }
}
