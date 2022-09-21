/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedModificationTrigger;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModificationPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

@Component
public class ObjectModificationConstraintEvaluator extends ModificationConstraintEvaluator<ModificationPolicyConstraintType> {

    private static final String OP_EVALUATE = ObjectModificationConstraintEvaluator.class.getName() + ".evaluate";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectModificationConstraintEvaluator.class);

    private static final String CONSTRAINT_KEY_PREFIX = "objectModification.";

    @Override
    public <AH extends AssignmentHolderType> EvaluatedModificationTrigger evaluate(@NotNull JAXBElement<ModificationPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<AH> rctx, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            if (!(rctx instanceof ObjectPolicyRuleEvaluationContext)) {
                LOGGER.trace(
                        "Policy rule evaluation context is not of type ObjectPolicyRuleEvaluationContext. Skipping processing.");
                return null;
            }
            ObjectPolicyRuleEvaluationContext<AH> ctx = (ObjectPolicyRuleEvaluationContext<AH>) rctx;

            if (modificationConstraintMatches(constraint, ctx, result)) {
                LocalizableMessage message = createMessage(constraint, rctx, result);
                LocalizableMessage shortMessage = createShortMessage(constraint, rctx, result);
                return new EvaluatedModificationTrigger(PolicyConstraintKindType.OBJECT_MODIFICATION, constraint.getValue(),
                        null, message, shortMessage);
            } else {
                LOGGER.trace("No operation matches.");
                return null;
            }
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createMessage(JAXBElement<ModificationPolicyConstraintType> constraint,
            PolicyRuleEvaluationContext<AH> rctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        String keyPostfix = createStateKey(rctx) + createOperationKey(rctx);
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                .args(ObjectTypeUtil.createDisplayInformation(rctx.focusContext.getObjectAny(), true))
                .build();
        return evaluatorHelper.createLocalizableMessage(constraint, rctx, builtInMessage, result);
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createShortMessage(JAXBElement<ModificationPolicyConstraintType> constraint,
            PolicyRuleEvaluationContext<AH> rctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        String keyPostfix = createStateKey(rctx) + createOperationKey(rctx);
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                .args(ObjectTypeUtil.createDisplayInformation(rctx.focusContext.getObjectAny(), false))
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraint, rctx, builtInMessage, result);
    }

    @NotNull
    private <AH extends AssignmentHolderType> String createOperationKey(PolicyRuleEvaluationContext<AH> rctx) {
        if (rctx.focusContext.isAdd()) {
            return "Added";
        } else if (rctx.focusContext.isDelete()) {
            return "Deleted";
        } else {
            return "Modified";
        }
    }

    // TODO discriminate between primary and secondary changes (perhaps make it configurable)
    // Primary changes are "approvable", secondary ones are not.
    private <AH extends AssignmentHolderType> boolean modificationConstraintMatches(JAXBElement<ModificationPolicyConstraintType> constraintElement,
            ObjectPolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        ModificationPolicyConstraintType constraint = constraintElement.getValue();
        if (!operationMatches(ctx.focusContext, constraint.getOperation())) {
            LOGGER.trace("Rule {} operation not applicable", ctx.policyRule.getName());
            return false;
        }
        ObjectDelta<?> summaryDelta = ctx.focusContext.getSummaryDelta();
        if (ObjectDelta.isEmpty(summaryDelta)) {
            LOGGER.trace("Focus context has no delta (primary nor secondary)");
            return false;
        }
        if (!constraint.getItem().isEmpty()) {
            boolean exactPathMatch = isTrue(constraint.isExactPathMatch());
            for (ItemPathType path : constraint.getItem()) {
                if (!pathMatches(summaryDelta, ctx, prismContext.toPath(path), exactPathMatch)) {
                    LOGGER.trace("Path {} does not match the constraint (no modification there)", path);
                    return false;
                }
            }
        }
        return expressionPasses(constraintElement, ctx, result);
    }

    private <AH extends AssignmentHolderType> boolean pathMatches(
            ObjectDelta<?> delta, ObjectPolicyRuleEvaluationContext<AH> ctx, ItemPath path, boolean exactPathMatch)
            throws SchemaException {
        if (delta.isAdd()) {
            return delta.getObjectToAdd().containsItem(path, false);
        } else if (delta.isDelete()) {
            PrismObject<AH> objectOld = ctx.focusContext.getObjectOld();
            return objectOld != null && objectOld.containsItem(path, false);
        } else {
            if (exactPathMatch) {
                return pathMatchesExactly(
                        emptyIfNull(delta.getModifications()), path, 0);
            } else {
                ItemPath nameOnlyPath = path.namedSegmentsOnly();
                PrismObject<AH> oldObject = ctx.focusContext.getObjectOld();
                PrismObject<AH> newObject = ctx.focusContext.getObjectNew();
                stateCheck(oldObject != null, "No 'old' object in %s", ctx);
                stateCheck(newObject != null, "No 'new' object in %s", ctx);
                return valuesChanged(oldObject.getValue(), newObject.getValue(), nameOnlyPath);
            }
        }
    }

    private <AH extends AssignmentHolderType> boolean operationMatches(LensFocusContext<AH> focusContext, List<ChangeTypeType> operations) {
        if (operations.isEmpty()) {
            return true;
        }
        for (ChangeTypeType operation: operations) {
            if (focusContext.operationMatches(operation)) {
                return true;
            }
        }
        return false;
    }
}
