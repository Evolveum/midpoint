/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PlusMinusZeroType;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Carries out and holds assignment evaluation:
 * 1) evaluation of the condition
 * 2) evaluation of payload (delegates to {@link PayloadEvaluation})
 * 3) evaluation of targets (delegates to {@link TargetsEvaluation}).
 */
class PathSegmentEvaluation<AH extends AssignmentHolderType> extends AbstractEvaluation<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(PathSegmentEvaluation.class);

    private static final String OP_EVALUATE = PathSegmentEvaluation.class.getName()+".evaluate";

    /**
     * The segment with the assignment that is being evaluated.
     */
    final AssignmentPathSegmentImpl segment;

    /**
     * Operation result related to this segment evaluation.
     *
     * We are using it in quite unorthodox way: it is created in constructor (to make it final,
     * along with the trace), and closed in execute() method. This might be changed in the future.
     */
    private final OperationResult result;

    /**
     * Trace of the segment evaluation.
     */
    private final AssignmentSegmentEvaluationTraceType trace;

    /**
     * Record of targets evaluation, to be used after the evaluation.
     */
    private TargetsEvaluation<AH> targetsEvaluation;

    PathSegmentEvaluation(AssignmentPathSegmentImpl segment, EvaluationContext<AH> ctx, OperationResult parentResult) {
        super(segment, ctx);
        this.segment = segment;
        this.result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .addParam("segment", segment.shortDump())
                .addContext("segmentSourceName", PolyString.getOrig(segment.source.getName()))
                .build();
        this.trace = recordStart();
    }

    /**
     * Execution of the evaluation. Can be called only once for this object.
     */
    void evaluate() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        try {
            checkIfAlreadyEvaluated();
            checkSchema();

            ctx.assignmentPath.add(segment);
            try {
                LOGGER.trace("*** Path (with current segment added):\n{}", ctx.assignmentPath.debugDumpLazily());

                computeValidity();
                computeMode();
                segment.freeze();

                if (segment.getAssignmentRelativityMode() != null) {
                    // Note that we evaluate payload and targets for both valid and invalid assignments.
                    // 1. Validity and relativity mode is recorded to several kinds of payload items (e.g. constructions).
                    // 2. Other ones (e.g. policy rules) are very important also for invalid assignments.
                    evaluateSegmentPayloadAndTargets();
                }
            } finally {
                ctx.assignmentPath.removeLast(segment);
            }
            recordEnd();

        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void evaluateSegmentPayloadAndTargets() throws CommunicationException, ConfigurationException, SchemaException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException, ObjectNotFoundException {

        new PayloadEvaluation<>(segment, ctx).evaluate();

        if (segment.isAssignmentValid() || segment.direct) {
            targetsEvaluation = new TargetsEvaluation<>(segment, ctx, result);
            targetsEvaluation.evaluate();
        }
    }

    private void computeValidity() {
        boolean validity;
        if (segment.isMatchingOrder) {
            // Validity of segment that is sourced at focus (either directly or indirectly i.e. through inducements)
            // is determined using focus lifecycle state model.
            AH focusNew = ctx.ae.focusOdo.getNewObject().asObjectable();
            validity = LensUtil.isAssignmentValid(focusNew, segment.assignment, ctx.ae.now, ctx.ae.activationComputer, ctx.ae.focusStateModel);
        } else {
            // But for other assignments/inducements we consider their validity by regarding their actual source.
            // So, even if (e.g.) focus is in "draft" state, only validity of direct assignments should be influenced by this fact.
            // Other assignments (e.g. from roles to metaroles) should be considered valid, provided these roles are
            // in active lifecycle states. See also MID-6113.
            //
            // TODO We should consider the assignment source's state model! But we are ignoring it for now.
            validity = LensUtil.isAssignmentValid(segment.source, segment.assignment, ctx.ae.now, ctx.ae.activationComputer, null);
        }
        segment.setAssignmentValid(validity);
        LOGGER.trace("Determined validity of segment {} to be {}", segment, validity);
    }

    private void computeMode() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        segment.setAssignmentRelativityMode(determineRelativityMode());
    }

    private AssignmentSegmentEvaluationTraceType recordStart() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("*** Evaluate segment: {}", segment);
            LOGGER.trace("*** Evaluation order - standard:   {}, matching: {}", segment.getEvaluationOrder(), segment.isMatchingOrder);
            LOGGER.trace("*** Evaluation order - for target: {}, matching: {}", segment.getEvaluationOrderForTarget(), segment.isMatchingOrderForTarget);
            LOGGER.trace("*** path to source valid: {}", segment.pathToSourceValid);
        }
        if (result.isTracingNormal(AssignmentSegmentEvaluationTraceType.class)) {
            AssignmentSegmentEvaluationTraceType trace = new AssignmentSegmentEvaluationTraceType(ctx.ae.prismContext)
                    .segment(segment.toAssignmentPathSegmentType(true));
            result.addTrace(trace);
            return trace;
        } else {
            return null;
        }
    }

    private void recordEnd() {
        if (segment.target != null) { // always null here
            result.addContext("segmentTargetName", PolyString.getOrig(segment.getTarget().getName()));
        }
        result.addReturn("assignmentValid", segment.isAssignmentValid());
        result.addArbitraryObjectAsReturn("mode", segment.getAssignmentRelativityMode());
        //result.addReturn("isValid", valid);
        if (trace != null) {
            trace.setMode(PlusMinusZeroType.fromValue(segment.getAssignmentRelativityMode()));
            trace.setTextResult(segment.debugDump());
        }
    }

    private void checkSchema() throws SchemaException {
        if (segment.source == null) {
            throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+ ctx.evalAssignment +")");
        }

        AssignmentType assignment = segment.assignment;
        PrismContainerValue<?> assignmentContainerValue = assignment.asPrismContainerValue();
        PrismContainerable<?> assignmentContainer = assignmentContainerValue.getParent();
        if (assignmentContainer == null) {
            throw new SchemaException("The assignment "+assignment+" does not have a parent in "+segment.sourceDescription);
        }
        if (assignmentContainer.getDefinition() == null) {
            throw new SchemaException("The assignment "+assignment+" does not have definition in "+segment.sourceDescription);
        }
        PrismContainer<Containerable> extensionContainer = assignmentContainerValue.findContainer(AssignmentType.F_EXTENSION);
        if (extensionContainer != null) {
            if (extensionContainer.getDefinition() == null) {
                throw new SchemaException("Extension does not have a definition in assignment "+assignment+" in "+segment.sourceDescription);
            }

            if (extensionContainer.getValue().getItems() == null) {
                throw new SchemaException("Extension without items in assignment " + assignment + " in " + segment.sourceDescription + ", empty extension tag?");
            }

            for (Item<?,?> item: extensionContainer.getValue().getItems()) {
                if (item == null) {
                    throw new SchemaException("Null item in extension in assignment "+assignment+" in "+segment.sourceDescription);
                }
                if (item.getDefinition() == null) {
                    throw new SchemaException("Item "+item+" has no definition in extension in assignment "+assignment+" in "+segment.sourceDescription);
                }
            }
        }
    }

    public List<PrismObject<? extends ObjectType>> getTargets() {
        return targetsEvaluation != null ? targetsEvaluation.targets : emptyList();
    }

    private PlusMinusZero determineRelativityMode()
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        return ctx.conditionEvaluator.computeModeFromCondition(
                segment.sourceRelativityMode,
                segment.assignment.getCondition(),
                segment.source,
                "condition in assignment in " + segment.sourceDescription,
                FocusTypeUtil.dumpAssignmentLazily(segment.assignment),
                result);
    }
}

/*
 *
 * TODO TODO TODO
 * This may happen, e.g. if a condition in an existing assignment turns from false to true.
 * In that case the primary assignment mode is ZERO, but the relative mode is PLUS.
 * The relative mode always starts at ZERO, even for added or removed assignments.
 *
 * This depends on the status of conditions. E.g. if condition evaluates 'false -> true' (i.e. in old
 * state the value is false, and in new state the value is true), then the mode is PLUS.
 *
 * This "triples algebra" is based on the following two methods:
 *
 * @see ExpressionUtil#computeConditionResultMode(boolean, boolean) - Based on condition values "old+new" determines
 * into what set (PLUS/MINUS/ZERO/none) should the result be placed. Irrespective of what is the current mode. So,
 * in order to determine "real" place where to put it (i.e. the new mode) the following method is used.
 *c
 * @see PlusMinusZero#compute(PlusMinusZero, PlusMinusZero) - Takes original mode and the mode from recent condition
 * and determines the new mode:
 *
 * PLUS + PLUS/ZERO = PLUS
 * MINUS + MINUS/ZERO = MINUS
 * ZERO + ZERO = ZERO
 * PLUS + MINUS = none
 *
 * This is quite straightforward, although the last rule deserves a note. If we have an assignment that was originally
 * disabled and becomes enabled by the current delta (i.e. PLUS), and that assignment contains an inducement that was originally
 * enabled and becomes disabled (i.e. MINUS), the result is that the (e.g.) constructions within the inducement were not
 * present in the old state (because assignment was disabled) and are not present in the new state (because inducement is disabled).
 *
 */
