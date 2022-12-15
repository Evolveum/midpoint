/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.impl.lens.assignments.TargetEvaluation.TargetActivity;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.model.impl.lens.assignments.Util.isAllowedByLimitations;

/**
 * Evaluates an assignment of a target (assignment holder): basically, creates a new assignment path
 * segment and requests its evaluation.
 */
class TargetAssignmentEvaluation<AH extends AssignmentHolderType> extends AbstractEvaluation<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(TargetAssignmentEvaluation.class);

    private final ConditionState targetOverallConditionState;
    private final TargetActivity targetActivity;
    private final OperationResult result;

    private final AssignmentType nextAssignment;

    TargetAssignmentEvaluation(AssignmentPathSegmentImpl segment, ConditionState targetOverallConditionState,
            TargetActivity targetActivity, EvaluationContext<AH> ctx, OperationResult result,
            AssignmentType nextAssignment) {
        super(segment, ctx);
        this.targetOverallConditionState = targetOverallConditionState;
        this.targetActivity = targetActivity;
        this.result = result;
        this.nextAssignment = nextAssignment;
    }

    void evaluate() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        assert ctx.assignmentPath.last() == segment;
        assert segment.isAssignmentActive() || segment.direct;
        assert targetActivity.targetActive || segment.direct;
        assert targetOverallConditionState.isNotAllFalse();
        checkIfAlreadyEvaluated();

        if (ctx.ae.relationRegistry.isDelegation(segment.relation)) {
            // We have to handle assignments as though they were inducements here.
            if (!isAllowedByLimitations(segment, nextAssignment, ctx)) {
                LOGGER.trace("Skipping application of delegated assignment {} because it is limited in the delegation",
                        FocusTypeUtil.dumpAssignmentLazily(nextAssignment));
                return;
            }
        }
        QName nextRelation = Util.getNormalizedRelation(nextAssignment);
        EvaluationOrder nextEvaluationOrder = segment.getEvaluationOrder().advance(nextRelation);
        EvaluationOrder nextEvaluationOrderForTarget = segment.getEvaluationOrderForTarget().advance(nextRelation);
        LOGGER.trace("orig EO({}): follow assignment {} {}; new EO({})", segment.getEvaluationOrder().shortDumpLazily(),
                segment.target, FocusTypeUtil.dumpAssignmentLazily(nextAssignment), nextEvaluationOrder);

        AssignmentPathSegmentImpl nextSegment = new AssignmentPathSegmentImpl.Builder()
                .source((AssignmentHolderType) segment.target)
                .sourceDescription(segment.target+" in "+segment.sourceDescription)
                .assignment(nextAssignment)
                .isAssignment()
                .evaluationOrder(nextEvaluationOrder)
                .evaluationOrderForTarget(nextEvaluationOrderForTarget)
                .pathToSourceValid(targetActivity.pathAndTargetActive)
                .pathToSourceConditionState(targetOverallConditionState)
                .build();
        new PathSegmentEvaluation<>(nextSegment, ctx, result).evaluate();
    }
}
