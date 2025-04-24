/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedModificationTrigger.EvaluatedAssignmentModificationTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Component
public class AssignmentModificationConstraintEvaluator
        extends ModificationConstraintEvaluator
        <AssignmentModificationPolicyConstraintType, EvaluatedAssignmentModificationTrigger> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentModificationConstraintEvaluator.class);

    private static final String CONSTRAINT_KEY_PREFIX = "assignmentModification.";

    private static final String OP_EVALUATE = AssignmentModificationConstraintEvaluator.class.getName() + ".evaluate";

    @Override
    public @NotNull <O extends ObjectType> Collection<EvaluatedAssignmentModificationTrigger> evaluate(
            @NotNull JAXBElement<AssignmentModificationPolicyConstraintType> constraintElement,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
                LOGGER.trace("Not an AssignmentPolicyRuleEvaluationContext: {}", rctx.getClass());
                return List.of();
            }
            AssignmentPolicyRuleEvaluationContext<?> ctx = (AssignmentPolicyRuleEvaluationContext<?>) rctx;
            AssignmentModificationPolicyConstraintType constraint = constraintElement.getValue();
            if(!isConstraintApplicable(ctx, constraint)) {
                LOGGER.trace("Assignment directness does not match constraint scope ({}) => not triggering", constraint.getScope());
                return List.of();
            }

            if (!operationMatches(constraint, ctx.isAdded, ctx.isKept, ctx.isDeleted) ||
                    !relationMatches(constraint, ctx) ||
                    !pathsMatch(constraint, ctx) ||
                    !expressionPasses(constraintElement, ctx, result)) {
                // Logging is done inside matcher methods
                return List.of();
            }

            // TODO check modifications
            EvaluatedAssignmentModificationTrigger rv = new EvaluatedAssignmentModificationTrigger(
                    PolicyConstraintKindType.ASSIGNMENT_MODIFICATION,
                    constraint, ctx.evaluatedAssignment.getTarget(),
                    createMessage(constraintElement, ctx, result),
                    createShortMessage(constraintElement, ctx, result));
            result.addReturn("trigger", rv.toDiagShortcut());
            return List.of(rv);
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private boolean isConstraintApplicable(AssignmentPolicyRuleEvaluationContext<?> ctx, AssignmentModificationPolicyConstraintType constraint) {
        AssignmentModificationPolicyConstraintScopeType scope = constraint.getScope();
        if (scope == null) {
            return ctx.isDirect();
        }
        return switch (scope) {
            case DIRECT -> ctx.isDirect();
            case INDIRECT -> !ctx.isDirect();
            case ANY -> true;
        };
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createMessage(
            JAXBElement<AssignmentModificationPolicyConstraintType> constraint,
            AssignmentPolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        String keyPostfix = createStateKey(ctx) + createOperationKey(ctx);
        QName relation = ctx.evaluatedAssignment.getNormalizedRelation();
        LocalizableMessage relationMessage = relation != null ?
                new LocalizableMessageBuilder()
                .key("relation." + relation.getLocalPart())
                .build()
                : null;

        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                .arg(ObjectTypeUtil.createDisplayInformation(ctx.evaluatedAssignment.getTarget(), false))
                .arg(relationMessage)
                .build();

        return evaluatorHelper.createLocalizableMessage(constraint, ctx, builtInMessage, result);
    }

    @NotNull
    private <AH extends AssignmentHolderType> String createOperationKey(AssignmentPolicyRuleEvaluationContext<AH> ctx) {
        if (ctx.isAdded) {
            return "Added";
        } else if (ctx.isDeleted) {
            return "Deleted";
        } else {
            return "Modified";
        }
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createShortMessage(JAXBElement<AssignmentModificationPolicyConstraintType> constraint,
            AssignmentPolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        String keyPostfix = createStateKey(ctx) + createOperationKey(ctx);
        QName relation = ctx.evaluatedAssignment.getNormalizedRelation();
        LocalizableMessage builtInMessage;

        if (prismContext.isDefaultRelation(relation)) {
            builtInMessage = new LocalizableMessageBuilder()
                    .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                    .arg(ObjectTypeUtil.createDisplayInformation(ctx.evaluatedAssignment.getTarget(), false))
                    .arg(ObjectTypeUtil.createDisplayInformation(ctx.getObject(), false))
                    .build();
        } else { //non-default relation = print relation to short message
            LocalizableMessage relationMessage = new LocalizableMessageBuilder()
                            .key("relation." + relation.getLocalPart())
                            .build();

            builtInMessage = new LocalizableMessageBuilder()
                    .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_REL_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                    .arg(ObjectTypeUtil.createDisplayInformation(ctx.evaluatedAssignment.getTarget(), false))
                    .arg(ObjectTypeUtil.createDisplayInformation(ctx.getObject(), false))
                    .arg(relationMessage)
                    .build();
        }
        return evaluatorHelper.createLocalizableShortMessage(constraint, ctx, builtInMessage, result);
    }

    private <AH extends AssignmentHolderType> boolean relationMatches(AssignmentModificationPolicyConstraintType constraint,
            AssignmentPolicyRuleEvaluationContext<AH> ctx) {
        QName normalizedAssignmentRelation = ctx.evaluatedAssignment.getNormalizedRelation();
        List<QName> relationsToCheck = constraint.getRelation().isEmpty() ?
                singletonList(null) : constraint.getRelation();
        for (QName constraintRelation : relationsToCheck) {
            if (prismContext.relationMatches(constraintRelation, normalizedAssignmentRelation)) {
                return true;
            }
        }
        LOGGER.trace("Relation does not match. Relations to check: {}, real (normalized) relation: {}", relationsToCheck,
                normalizedAssignmentRelation);
        return false;
    }

    private boolean operationMatches(
            AssignmentModificationPolicyConstraintType constraint, boolean inPlus, boolean inZero, boolean inMinus) {
        List<ChangeTypeType> operations = constraint.getOperation();
        if (operations.isEmpty()
                || inPlus && operations.contains(ChangeTypeType.ADD)
                || inZero && operations.contains(ChangeTypeType.MODIFY)
                || inMinus && operations.contains(ChangeTypeType.DELETE)) {
            return true;
        } else {
            LOGGER.trace("Operation does not match. Configured operations: {}, real inPlus={}, inMinus={}, inZero={}.",
                    operations, inPlus, inMinus, inZero);
            return false;
        }
    }

    // TODO discriminate between primary and secondary changes (perhaps make it configurable)
    // Primary changes are "approvable", secondary ones are not.
    private <AH extends AssignmentHolderType> boolean pathsMatch(
            AssignmentModificationPolicyConstraintType constraint, AssignmentPolicyRuleEvaluationContext<AH> ctx)
            throws SchemaException {

        boolean exactMatch = isTrue(constraint.isExactPathMatch());

        // hope this is correctly filled in
        if (constraint.getItem().isEmpty()) {
            if (ctx.isAdded || ctx.isDeleted) {
                LOGGER.trace("pathsMatch: returns true because no items are configured and isAdded||isDeleted");
                return true;
            } else {
                Collection<? extends ItemDelta<?, ?>> subItemDeltas = ctx.evaluatedAssignment.getAssignmentIdi().getSubItemDeltas();
                boolean subItemDeltasPresent = subItemDeltas != null && !subItemDeltas.isEmpty();
                LOGGER.trace("pathsMatch: returns subItemsDeltasPresent ({}) because not in plus nor minus", subItemDeltasPresent);
                return subItemDeltasPresent;
            }
        } else {
            for (ItemPathType path : constraint.getItem()) {
                ItemPath itemPath = prismContext.toPath(path);
                if (ctx.isAdded && pathDoesNotMatch(ctx.evaluatedAssignment.getAssignment(false), itemPath)) {
                    LOGGER.trace("pathsMatch: returns 'false' because isAdded and path {} does not match new assignment", itemPath);
                    return false;
                } else if (ctx.isDeleted && pathDoesNotMatch(ctx.evaluatedAssignment.getAssignment(true), itemPath)) {
                    LOGGER.trace("pathsMatch: returns 'false' because isDeleted and path {} does not match old assignment", itemPath);
                    return false;
                } else if (ctx.isKept) {
                    if (exactMatch) {
                        Collection<? extends ItemDelta<?, ?>> subItemDeltas = ctx.evaluatedAssignment.getAssignmentIdi().getSubItemDeltas();
                        if (!pathMatchesExactly(emptyIfNull(subItemDeltas), itemPath, 2)) {
                            LOGGER.trace("pathsMatch: returns false because isKept and path {} does not match new assignment; "
                                    + "exact=true; sub item deltas={}", itemPath, subItemDeltas);
                            return false;
                        }
                    } else {
                        AssignmentType oldValue = ctx.evaluatedAssignment.getAssignment(true);
                        AssignmentType newValue = ctx.evaluatedAssignment.getAssignment(false);
                        stateCheck(oldValue != null, "No 'old' assignment value in %s", ctx);
                        stateCheck(newValue != null, "No 'new' assignment value in %s", ctx);
                        ItemPath nameOnlyPath = itemPath.namedSegmentsOnly();
                        if (!valuesChanged(oldValue.asPrismContainerValue(), newValue.asPrismContainerValue(), nameOnlyPath)) {
                            LOGGER.trace(
                                    "pathsMatch: returns 'false' because isKept and value(s) for path {} have not been changed",
                                    nameOnlyPath);
                            return false;
                        }
                    }
                }
            }
            LOGGER.trace("pathsMatch: returns 'true' because all paths ({}) match", constraint.getItem());
            return true;
        }
    }

    private boolean pathDoesNotMatch(AssignmentType assignment, ItemPath path) throws SchemaException {
        return !assignment.asPrismContainerValue().containsItem(path, false);
    }
}
