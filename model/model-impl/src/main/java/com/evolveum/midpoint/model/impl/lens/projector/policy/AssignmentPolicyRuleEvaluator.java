/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * Evaluates assignment-related policy rules.
 */
class AssignmentPolicyRuleEvaluator<F extends AssignmentHolderType> extends PolicyRuleEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentPolicyRuleEvaluator.class);

    @NotNull private final LensFocusContext<F> focusContext;
    @NotNull private final DeltaSetTriple<? extends EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple;

    AssignmentPolicyRuleEvaluator(
            @NotNull LensFocusContext<F> focusContext,
            @NotNull Task task) {
        super(focusContext.getLensContext(), task);
        this.focusContext = focusContext;
        //noinspection unchecked,rawtypes
        this.evaluatedAssignmentTriple = (DeltaSetTriple) context.getEvaluatedAssignmentTriple();
    }

    /**
     * Evaluates the policies (policy rules, but also legacy policies). Triggers the rules.
     * But does not enforce anything and does not make any context changes. TODO really? also for legacy policies?
     *
     * Takes into account all policy rules related to assignments in the given evaluatedAssignmentTriple.
     * Focus policy rules are not processed here, even though they might come through these assignments.
     */
    void evaluate(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        collector.initialize(result);
        collector.collectGlobalAssignmentRules(evaluatedAssignmentTriple, result);

        for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.union()) {
            Collection<EvaluatedPolicyRuleImpl> policyRules = evaluatedAssignment.getAllTargetsPolicyRules();
            collector.resolveConstraintReferences(policyRules);

            List<AssignmentPolicyRuleEvaluationContext<F>> contextsToEvaluate = new ArrayList<>();
            for (EvaluatedPolicyRuleImpl policyRule : policyRules) {
                if (policyRule.isApplicableToAssignment()) {
                    contextsToEvaluate.add(
                            new AssignmentPolicyRuleEvaluationContext<>(
                                    policyRule,
                                    evaluatedAssignment,
                                    focusContext,
                                    evaluatedAssignmentTriple,
                                    task));
                } else {
                    LOGGER.trace("Skipping rule {} because it is not applicable to assignment: {}",
                            policyRule.getName(), policyRule);
                }
            }
            evaluateRules(contextsToEvaluate, result);
        }
    }

    void record(OperationResult result) throws SchemaException {
        for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.union()) {
            new PolicyStateRecorder().applyAssignmentState(
                    context,
                    evaluatedAssignment,
                    selectRulesToRecord(evaluatedAssignment.getAllTargetsAndForeignPolicyRules()));
        }
    }
}
