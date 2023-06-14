/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.model.impl.lens.assignments.Util.isAllowedByLimitations;

/**
 * Evaluates an inducement of a target (abstract role): basically, creates a new assignment path
 * segment and requests its evaluation. The tricky part is computing evaluation order (focus/target)
 * of the new segment.
 */
class TargetInducementEvaluation<AH extends AssignmentHolderType> extends AbstractEvaluation<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(TargetInducementEvaluation.class);

    @NotNull private final ConditionState targetOverallConditionState;
    @NotNull private final TargetEvaluation.TargetActivity targetActivity;
    private final OperationResult result;
    private final AssignmentType inducement;
    private final boolean archetypeHierarchy;

    TargetInducementEvaluation(AssignmentPathSegmentImpl segment,
            @NotNull ConditionState targetOverallConditionState, @NotNull TargetEvaluation.TargetActivity targetActivity,
            EvaluationContext<AH> ctx, OperationResult result, AssignmentType inducement, boolean archetypeHierarchy) {
        super(segment, ctx);
        this.targetOverallConditionState = targetOverallConditionState;
        this.targetActivity = targetActivity;
        this.result = result;
        this.inducement = inducement;
        this.archetypeHierarchy = archetypeHierarchy;
    }

    void evaluate() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        assert ctx.assignmentPath.last() == segment;
        assert segment.isAssignmentActive() || segment.direct;
        assert targetOverallConditionState.isNotAllFalse();
        assert targetActivity.targetActive;

        checkIfAlreadyEvaluated();

        QName focusType = inducement.getFocusType();
        if (!isInducementApplicableToFocusType(focusType)) {
            LOGGER.trace("Skipping application of inducement {} because the focusType does not match (specified: {}, actual: {})",
                    FocusTypeUtil.dumpAssignmentLazily(inducement), focusType, ctx.ae.lensContext.getFocusClass());
            return;
        }

        if (!isAllowedByLimitations(segment, inducement, ctx)) {
            LOGGER.trace("Skipping application of inducement {} because it is limited", FocusTypeUtil.dumpAssignmentLazily(inducement));
            return;
        }

        boolean nextIsMatchingOrder = segment.getEvaluationOrder().matches(inducement.getOrder(), inducement.getOrderConstraint());
        boolean nextIsMatchingOrderForTarget = segment.getEvaluationOrderForTarget().matches(inducement.getOrder(), inducement.getOrderConstraint());

        OrderAdjustment adjustment = computeOrderAdjustment();

        String nextSourceDescription = segment.target+" in "+segment.sourceDescription;
        AssignmentPathSegmentImpl nextSegment = new AssignmentPathSegmentImpl.Builder()
                .source((AssignmentHolderType) segment.target)
                .sourceDescription(nextSourceDescription)
                .assignment(inducement)
                .isInducement()
                .isHierarchy(archetypeHierarchy)
                .pathToSourceValid(targetActivity.pathAndTargetActive)
                .pathToSourceConditionState(targetOverallConditionState)
                .evaluationOrder(adjustment.evaluationOrder)
                .evaluationOrderForTarget(adjustment.targetEvaluationOrder)
                .isMatchingOrder(nextIsMatchingOrder)
                .isMatchingOrderForTarget(nextIsMatchingOrderForTarget)
                .lastEqualOrderSegmentIndex(adjustment.lastEqualOrderSegmentIndex)
                .build();

        // Originally we executed the following only if isMatchingOrder. However, sometimes we have to look even into
        // inducements with non-matching order: for example because we need to extract target-related policy rules
        // (these are stored with order of one less than orders for focus-related policy rules).
        //
        // We need to make sure NOT to extract anything other from such inducements. That's why we set e.g.
        // processMembership attribute to false for these inducements.
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("orig EO({}): evaluate {} inducement({}) {}; new EO({})",
                    segment.getEvaluationOrder().shortDump(), segment.target, FocusTypeUtil.dumpInducementConstraints(inducement),
                    FocusTypeUtil.dumpAssignment(inducement), adjustment.evaluationOrder.shortDump());
        }

        new PathSegmentEvaluation<>(nextSegment, ctx, result).evaluate();
    }

    private boolean isInducementApplicableToFocusType(QName inducementFocusType) throws SchemaException {
        if (inducementFocusType == null) {
            return true;
        }
        Class<?> inducementFocusClass = ctx.ae.prismContext.getSchemaRegistry().determineCompileTimeClass(inducementFocusType);
        if (inducementFocusClass == null) {
            throw new SchemaException("Could not determine class for " + inducementFocusType);
        }
        if (ctx.ae.lensContext.getFocusClass() == null) {
            // should not occur; it would be probably safe to throw an exception here
            LOGGER.error("No focus class in lens context; inducement targeted at focus type {} will not be applied:\n{}",
                    inducementFocusType, ctx.ae.lensContext.debugDump());
            return false;
        }
        return inducementFocusClass.isAssignableFrom(ctx.ae.lensContext.getFocusClass());
    }

    private static class OrderAdjustment {
        private final EvaluationOrder evaluationOrder;
        private final EvaluationOrder targetEvaluationOrder;
        private final Integer lastEqualOrderSegmentIndex;

        private OrderAdjustment(EvaluationOrder evaluationOrder, EvaluationOrder targetEvaluationOrder, Integer lastEqualOrderSegmentIndex) {
            this.evaluationOrder = evaluationOrder;
            this.targetEvaluationOrder = targetEvaluationOrder;
            this.lastEqualOrderSegmentIndex = lastEqualOrderSegmentIndex;
        }

        private OrderAdjustment(EvaluationOrder evaluationOrder, EvaluationOrder targetEvaluationOrder) {
            this.evaluationOrder = evaluationOrder;
            this.targetEvaluationOrder = targetEvaluationOrder;
            this.lastEqualOrderSegmentIndex = null;
        }

        private static OrderAdjustment undefined() {
            return new OrderAdjustment(EvaluationOrderImpl.UNDEFINED, EvaluationOrderImpl.UNDEFINED);
        }
    }

    /**
     * Tries to compute correct evaluation order after application of an inducement. Terrible method.
     * For some background please see comments at the end of {@link AssignmentPathSegmentImpl} class.
     */
    private OrderAdjustment computeOrderAdjustment() {
        EvaluationOrder currentOrder = segment.getEvaluationOrder();
        EvaluationOrder currentTargetOrder = segment.getEvaluationOrderForTarget();
        List<OrderConstraintsType> constraints = new ArrayList<>(inducement.getOrderConstraint()); // will be modified
        Integer order = inducement.getOrder();

        if (constraints.isEmpty()) {
            if (order == null || order == 1) {
                return new OrderAdjustment(currentOrder, currentTargetOrder);
            } else if (order <= 0) {
                throw new IllegalStateException("Wrong inducement order: it must be positive but it is " + order + " instead");
            }
            // converting legacy -> new specification (with resetTo)
            int currentSummary = currentOrder.getSummaryOrder();
            if (order > currentSummary) {
                LOGGER.trace("order of the inducement ({}) is greater than the current evaluation order ({}), marking as undefined",
                        order, currentOrder);
                return OrderAdjustment.undefined();
            } else {
                // i.e. currentOrder >= order, i.e. currentOrder > order-1
                int newOrder = currentSummary - (order - 1);
                assert newOrder > 0;
                constraints.add(new OrderConstraintsType()
                        .order(order)
                        .resetOrder(newOrder));
            }
        }

        OrderAdjustment adjustment = applyResetTo(currentOrder, currentTargetOrder, constraints);

        if (!adjustment.evaluationOrder.isDefined()) {
            return adjustment;
        }

        if (adjustment.evaluationOrder.getSummaryOrder() <= 0) {
            return OrderAdjustment.undefined();
        }

        if (!adjustment.evaluationOrder.isValid()) {
            throw new AssertionError("Resulting evaluation order path is invalid: " + adjustment.evaluationOrder);
        }

        if (adjustment.targetEvaluationOrder.isValid()) {
            return adjustment;
        } else {
            // some extreme cases like the one described in TestAssignmentProcessor2.test520
            return new OrderAdjustment(adjustment.evaluationOrder, EvaluationOrderImpl.UNDEFINED, adjustment.lastEqualOrderSegmentIndex);
        }
    }

    private @NotNull OrderAdjustment applyResetTo(
            EvaluationOrder currentOrder, EvaluationOrder currentTargetOrder, List<OrderConstraintsType> constraints) {
        Integer resetSummaryTo = getResetSummaryTo(constraints);
        if (resetSummaryTo != null) {
            return applyResetSummary(currentOrder, currentTargetOrder, constraints, resetSummaryTo);
        } else {
            return applyResetForRelations(currentOrder, currentTargetOrder, constraints);
        }
    }

    private @Nullable Integer getResetSummaryTo(List<OrderConstraintsType> constraints) {
        OrderConstraintsType summaryConstraints = getConstraintWithoutRelation(constraints);
        return summaryConstraints != null && summaryConstraints.getResetOrder() != null ?
                summaryConstraints.getResetOrder() : null;
    }

    private OrderAdjustment applyResetForRelations(
            EvaluationOrder currentOrder, EvaluationOrder currentTargetOrder, List<OrderConstraintsType> constraints) {
        EvaluationOrder updatedOrder = currentOrder;
        for (OrderConstraintsType constraint : constraints) {
            if (constraint.getResetOrder() != null) {
                assert constraint.getRelation() != null; // already processed above
                int currentOrderForRelation = updatedOrder.getMatchingRelationOrder(constraint.getRelation());
                int newOrderForRelation = constraint.getResetOrder();
                if (newOrderForRelation > currentOrderForRelation) {
                    LOGGER.warn("Cannot increase evaluation order for {} from {} to {}: {}", constraint.getRelation(),
                            currentOrderForRelation, newOrderForRelation, constraint);
                } else if (newOrderForRelation < currentOrderForRelation) {
                    updatedOrder = updatedOrder.resetOrder(constraint.getRelation(), newOrderForRelation);
                    LOGGER.trace("Reset order for {} from {} to {} -> {}", constraint.getRelation(), currentOrderForRelation, newOrderForRelation, updatedOrder);
                } else {
                    LOGGER.trace("Keeping order for {} at {} -> {}", constraint.getRelation(), currentOrderForRelation, updatedOrder);
                }
            }
        }
        Map<QName, Integer> difference = currentOrder.diff(updatedOrder);
        EvaluationOrder updatedTargetOrder = currentTargetOrder.applyDifference(difference);
        return new OrderAdjustment(updatedOrder, updatedTargetOrder);
    }

    private OrderAdjustment applyResetSummary(EvaluationOrder currentOrder, EvaluationOrder currentTargetOrder,
            List<OrderConstraintsType> constraints, Integer resetSummaryTo) {
        OrderAdjustment adjustment;
        int summaryBackwards = currentOrder.getSummaryOrder() - resetSummaryTo;
        if (summaryBackwards < 0) {
            // or should we throw an exception?
            LOGGER.warn("Cannot move summary order backwards to a negative value ({}). Current order: {}, requested order: {}",
                    summaryBackwards, currentOrder.getSummaryOrder(), resetSummaryTo);
            return OrderAdjustment.undefined();
        } else if (summaryBackwards > 0) {
            int assignmentsSeen = 0;
            int i = ctx.assignmentPath.size()-1;
            while (assignmentsSeen < summaryBackwards) {
                if (i < 0) {
                    LOGGER.trace("Cannot move summary order backwards by {}; only {} assignments segment seen: {}",
                            summaryBackwards, assignmentsSeen, ctx.assignmentPath);
                    return OrderAdjustment.undefined();
                }
                AssignmentPathSegmentImpl segment = ctx.assignmentPath.getSegments().get(i);
                if (segment.isAssignment()) {
                    if (!ctx.ae.relationRegistry.isDelegation(segment.relation)) {
                        assignmentsSeen++;
                        LOGGER.trace("Going back {}: relation at assignment -{} (position -{}): {}", summaryBackwards,
                                assignmentsSeen, ctx.assignmentPath.size() - i, segment.relation);
                    }
                } else {
                    AssignmentType inducement = segment.getAssignment(ctx.evaluateOld); // for i>0 returns value regardless of evaluateOld
                    for (OrderConstraintsType constraint : inducement.getOrderConstraint()) {
                        if (constraint.getResetOrder() != null && constraint.getRelation() != null) {
                            LOGGER.debug("Going back {}: an inducement with non-summary resetting constraint found"
                                            + " in the chain (at position -{}): {} in {}", summaryBackwards, ctx.assignmentPath.size()-i,
                                    constraint, segment);
                            return OrderAdjustment.undefined();
                        }
                    }
                    if (segment.getLastEqualOrderSegmentIndex() != null) {
                        i = segment.getLastEqualOrderSegmentIndex();
                        continue;
                    }
                }
                i--;
            }
            adjustment = new OrderAdjustment(
                    ctx.assignmentPath.getSegments().get(i).getEvaluationOrder(),
                    ctx.assignmentPath.getSegments().get(i).getEvaluationOrderForTarget(),
                    i);
        } else {
            // summaryBackwards is 0 - nothing to change
            adjustment = new OrderAdjustment(currentOrder, currentTargetOrder);
        }
        if (adjustment.evaluationOrder.isDefined()) {
            for (OrderConstraintsType constraint : constraints) {
                if (constraint.getRelation() != null && constraint.getResetOrder() != null) {
                    LOGGER.warn("Ignoring resetOrder (with a value of {} for {}) because summary order was already moved backwards by {} to {}: {}",
                            constraint.getResetOrder(), constraint.getRelation(), summaryBackwards,
                            adjustment.evaluationOrder.getSummaryOrder(), constraint);
                }
            }
        }
        return adjustment;
    }

    private OrderConstraintsType getConstraintWithoutRelation(List<OrderConstraintsType> constraints) {
        return CollectionUtils.emptyIfNull(constraints).stream()
                .filter(c -> c.getRelation() == null)
                .findFirst().orElse(null);
    }
}
