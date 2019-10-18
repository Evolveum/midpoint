/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedModificationTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentModificationPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author semancik
 * @author mederly
 */
@Component
public class AssignmentModificationConstraintEvaluator extends ModificationConstraintEvaluator<AssignmentModificationPolicyConstraintType> {

    private static final String CONSTRAINT_KEY_PREFIX = "assignmentModification.";

    private static final String OP_EVALUATE = AssignmentModificationConstraintEvaluator.class.getName() + ".evaluate";

    @Override
    public <AH extends AssignmentHolderType> EvaluatedPolicyRuleTrigger evaluate(JAXBElement<AssignmentModificationPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> rctx, OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            AssignmentModificationPolicyConstraintType constraint = constraintElement.getValue();
            if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
                return null;
            }
            AssignmentPolicyRuleEvaluationContext<AH> ctx = (AssignmentPolicyRuleEvaluationContext<AH>) rctx;
            if (!ctx.isDirect ||
                    !operationMatches(constraint, ctx.inPlus, ctx.inZero, ctx.inMinus) ||
                    !relationMatches(constraint, ctx) ||
                    !pathsMatch(constraint, ctx) ||
                    !expressionPasses(constraintElement, ctx, result)) {
                return null;
            }
            // TODO check modifications
            EvaluatedModificationTrigger rv = new EvaluatedModificationTrigger(PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, constraint,
                    createMessage(constraintElement, ctx, result),
                    createShortMessage(constraintElement, ctx, result));
            result.addReturn("trigger", rv.toDiagShortcut());
            return rv;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createMessage(JAXBElement<AssignmentModificationPolicyConstraintType> constraint,
            AssignmentPolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        String keyPostfix = createStateKey(ctx) + createOperationKey(ctx);
        QName relation = ctx.evaluatedAssignment.getNormalizedRelation(relationRegistry);
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                .arg(ObjectTypeUtil.createDisplayInformation(ctx.evaluatedAssignment.getTarget(), false))
                .arg(relation != null ? relation.getLocalPart() : null)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraint, ctx, builtInMessage, result);
    }

    @NotNull
    private <AH extends AssignmentHolderType> String createOperationKey(AssignmentPolicyRuleEvaluationContext<AH> ctx) {
        if (ctx.inPlus) {
            return "Added";
        } else if (ctx.inMinus) {
            return "Deleted";
        } else {
            return "Modified";
        }
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createShortMessage(JAXBElement<AssignmentModificationPolicyConstraintType> constraint,
            AssignmentPolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        String keyPostfix = createStateKey(ctx) + createOperationKey(ctx);
        QName relation = ctx.evaluatedAssignment.getNormalizedRelation(relationRegistry);
        LocalizableMessage builtInMessage;

        if (relation == null || relation == prismContext.getDefaultRelation()) {
            builtInMessage = new LocalizableMessageBuilder()
                    .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                    .arg(ObjectTypeUtil.createDisplayInformation(ctx.evaluatedAssignment.getTarget(), false))
                    .arg(ObjectTypeUtil.createDisplayInformation(ctx.getObject(), false))
                    .build();
        } else { //non-default relation = print relation to short message
            builtInMessage = new LocalizableMessageBuilder()
                    .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_REL_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                    .arg(ObjectTypeUtil.createDisplayInformation(ctx.evaluatedAssignment.getTarget(), false))
                    .arg(ObjectTypeUtil.createDisplayInformation(ctx.getObject(), false))
                    .arg(relation.getLocalPart())
                    .build();
        }
        return evaluatorHelper.createLocalizableShortMessage(constraint, ctx, builtInMessage, result);
    }

    private <AH extends AssignmentHolderType> boolean relationMatches(AssignmentModificationPolicyConstraintType constraint,
            AssignmentPolicyRuleEvaluationContext<AH> ctx) {
        List<QName> relationsToCheck = constraint.getRelation().isEmpty() ?
                singletonList(null) : constraint.getRelation();
        for (QName constraintRelation : relationsToCheck) {
            if (prismContext.relationMatches(constraintRelation, ctx.evaluatedAssignment.getNormalizedRelation(relationRegistry))) {
                return true;
            }
        }
        return false;
    }

    private boolean operationMatches(AssignmentModificationPolicyConstraintType constraint, boolean inPlus, boolean inZero, boolean inMinus) {
        List<ChangeTypeType> operations = constraint.getOperation();
        return operations.isEmpty() ||
                inPlus && operations.contains(ChangeTypeType.ADD) ||
                inZero && operations.contains(ChangeTypeType.MODIFY) ||
                inMinus && operations.contains(ChangeTypeType.DELETE);
    }

    // TODO discriminate between primary and secondary changes (perhaps make it configurable)
    // Primary changes are "approvable", secondary ones are not.
    private <AH extends AssignmentHolderType> boolean pathsMatch(AssignmentModificationPolicyConstraintType constraint,
            AssignmentPolicyRuleEvaluationContext<AH> ctx) throws SchemaException {

        boolean exactMatch = isTrue(constraint.isExactPathMatch());

        // hope this is correctly filled in
        if (constraint.getItem().isEmpty()) {
            if (ctx.inPlus || ctx.inMinus) {
                return true;
            } else {
                Collection<? extends ItemDelta<?, ?>> subItemDeltas = ctx.evaluatedAssignment.getAssignmentIdi().getSubItemDeltas();
                return subItemDeltas != null && !subItemDeltas.isEmpty();
            }
        }
        for (ItemPathType path : constraint.getItem()) {
            ItemPath itemPath = prismContext.toPath(path);
            if (ctx.inPlus && !pathMatches(ctx.evaluatedAssignment.getAssignmentType(false), itemPath) ||
                    ctx.inMinus && !pathMatches(ctx.evaluatedAssignment.getAssignmentType(true), itemPath) ||
                    ctx.inZero && !pathMatches(ctx.evaluatedAssignment.getAssignmentIdi().getSubItemDeltas(), itemPath, exactMatch)) {
                return false;
            }
        }
        return true;
    }

    private boolean pathMatches(AssignmentType assignment, ItemPath path) throws SchemaException {
        return assignment.asPrismContainerValue().containsItem(path, false);
    }

    private boolean pathMatches(Collection<? extends ItemDelta<?, ?>> deltas, ItemPath path, boolean exactMatch) {
        return ItemDeltaCollectionsUtil.pathMatches(emptyIfNull(deltas), path, 2, exactMatch);
    }
}
