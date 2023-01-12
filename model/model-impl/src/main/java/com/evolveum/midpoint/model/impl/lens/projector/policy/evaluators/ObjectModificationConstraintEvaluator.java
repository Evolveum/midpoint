/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.schema.processor.CompositeObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedModificationTrigger;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Component
public class ObjectModificationConstraintEvaluator extends ModificationConstraintEvaluator<ModificationPolicyConstraintType> {

    private static final String OP_EVALUATE = ObjectModificationConstraintEvaluator.class.getName() + ".evaluate";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectModificationConstraintEvaluator.class);

    private static final String CONSTRAINT_KEY_PREFIX = "objectModification.";

    @Override
    public <O extends ObjectType> EvaluatedModificationTrigger evaluate(
            @NotNull JAXBElement<ModificationPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            if (!(rctx instanceof ObjectPolicyRuleEvaluationContext)) {
                LOGGER.trace(
                        "Policy rule evaluation context is not of type ObjectPolicyRuleEvaluationContext. Skipping processing.");
                return null;
            }
            ObjectPolicyRuleEvaluationContext<O> ctx = (ObjectPolicyRuleEvaluationContext<O>) rctx;

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

    private LocalizableMessage createMessage(
            JAXBElement<ModificationPolicyConstraintType> constraint, PolicyRuleEvaluationContext<?> rctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        String keyPostfix = createStateKey(rctx) + createOperationKey(rctx);
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                .args(ObjectTypeUtil.createDisplayInformation(rctx.elementContext.getObjectAny(), true))
                .build();
        return evaluatorHelper.createLocalizableMessage(constraint, rctx, builtInMessage, result);
    }

    private LocalizableMessage createShortMessage(
            JAXBElement<ModificationPolicyConstraintType> constraint,
            PolicyRuleEvaluationContext<?> rctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        String keyPostfix = createStateKey(rctx) + createOperationKey(rctx);
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
                .args(ObjectTypeUtil.createDisplayInformation(rctx.elementContext.getObjectAny(), false))
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraint, rctx, builtInMessage, result);
    }

    @NotNull
    private String createOperationKey(PolicyRuleEvaluationContext<?> rctx) {
        if (rctx.elementContext.isAdd()) {
            return "Added";
        } else if (rctx.elementContext.isDelete()) {
            return "Deleted";
        } else {
            return "Modified";
        }
    }

    // TODO discriminate between primary and secondary changes (perhaps make it configurable)
    // Primary changes are "approvable", secondary ones are not.
    private boolean modificationConstraintMatches(
            JAXBElement<ModificationPolicyConstraintType> constraintElement,
            ObjectPolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        ModificationPolicyConstraintType constraint = constraintElement.getValue();
        if (!operationMatches(ctx.elementContext, constraint.getOperation())) {
            LOGGER.trace("Rule {} operation not applicable", ctx.policyRule.getName());
            return false;
        }
        ObjectDelta<?> summaryDelta = ctx.elementContext.getSummaryDelta();
        if (ObjectDelta.isEmpty(summaryDelta)) {
            LOGGER.trace("Focus context has no delta (primary nor secondary)");
            return false;
        }
        List<ItemPathType> itemPaths = constraint.getItem();
        if (!itemPaths.isEmpty()) {
            boolean exactPathMatch = isTrue(constraint.isExactPathMatch());
            for (ItemPathType itemPath : itemPaths) {
                if (!pathMatches(summaryDelta, ctx, prismContext.toPath(itemPath), exactPathMatch)) {
                    LOGGER.trace("Path {} does not match the delta (no modification there)", itemPath);
                    return false;
                }
            }
        }
        List<SpecialItemSpecificationType> specialItems = constraint.getSpecialItem();
        if (!specialItems.isEmpty()) {
            if (summaryDelta.isModify()) {
                for (SpecialItemSpecificationType specialItem : specialItems) {
                    if (!specialItemMatches(summaryDelta, ctx, specialItem)) {
                        LOGGER.trace("Special item {} does not match the delta (no modification there)", specialItem);
                        return false;
                    }
                }
            } else {
                LOGGER.trace("There are 'special items' specified but the delta is not MODIFY one -> ignoring");
                return false;
            }
        }
        return expressionPasses(constraintElement, ctx, result);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean pathMatches(
            ObjectDelta<?> delta, ObjectPolicyRuleEvaluationContext<?> ctx, ItemPath path, boolean exactPathMatch)
            throws SchemaException {
        if (delta.isAdd()) {
            return delta.getObjectToAdd().containsItem(path, false);
        } else if (delta.isDelete()) {
            PrismObject<?> objectOld = ctx.elementContext.getObjectOld();
            return objectOld != null && objectOld.containsItem(path, false);
        } else {
            if (exactPathMatch) {
                return pathMatchesExactly(
                        emptyIfNull(delta.getModifications()), path, 0);
            } else {
                ItemPath nameOnlyPath = path.namedSegmentsOnly();
                PrismObject<?> oldObject = ctx.elementContext.getObjectOld();
                PrismObject<?> newObject = ctx.elementContext.getObjectNew();
                stateCheck(oldObject != null, "No 'old' object in %s", ctx);
                stateCheck(newObject != null, "No 'new' object in %s", ctx);
                return valuesChanged(oldObject.getValue(), newObject.getValue(), nameOnlyPath);
            }
        }
    }

    private boolean specialItemMatches(
            ObjectDelta<?> delta, ObjectPolicyRuleEvaluationContext<?> ctx, SpecialItemSpecificationType specialItem)
            throws SchemaException, ConfigurationException {
        assert delta.isModify();
        Collection<ItemPath> paths = determineSpecialItemPaths(ctx, specialItem);
        if (paths == null) {
            LOGGER.trace("Special item {} is not applicable here", specialItem);
            return false;
        }
        for (ItemPath path : paths) {
            if (pathMatchesExactly(emptyIfNull(delta.getModifications()), path, 0)) {
                return true;
            }
        }
        return false;
    }

    private Collection<ItemPath> determineSpecialItemPaths(
            ObjectPolicyRuleEvaluationContext<?> ctx, SpecialItemSpecificationType specialItem)
            throws SchemaException, ConfigurationException {
        if (!(ctx.elementContext instanceof LensProjectionContext)) {
            return null;
        }
        LensProjectionContext projectionContext = (LensProjectionContext) ctx.elementContext;
        CompositeObjectDefinition objectDefinition = projectionContext.getCompositeObjectDefinition();
        if (objectDefinition == null) {
            LOGGER.trace("No object definition -> no special item {} evaluation", specialItem);
            return null;
        }

        switch (specialItem) {
            case RESOURCE_OBJECT_IDENTIFIER:
                return objectDefinition.getAllIdentifiers().stream()
                        .map(def -> ItemPath.create(ShadowType.F_ATTRIBUTES, def.getItemName()))
                        .collect(Collectors.toList());
            case RESOURCE_OBJECT_ENTITLEMENT:
                return List.of(ShadowType.F_ASSOCIATION); // for the time being
            case RESOURCE_OBJECT_NAMING_ATTRIBUTE:
                ResourceAttributeDefinition<?> namingAttributeDef = objectDefinition.getNamingAttribute();
                if (namingAttributeDef == null) {
                    LOGGER.trace("No naming attribute for {} -> no special item {} evaluation", objectDefinition, specialItem);
                    return null;
                } else {
                    return List.of(ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttributeDef.getItemName()));
                }
            default:
                throw new IllegalStateException("Item specification " + specialItem + " is not supported");
        }
    }

    private boolean operationMatches(LensElementContext<?> elementContext, List<ChangeTypeType> operations) {
        if (operations.isEmpty()) {
            return true;
        }
        for (ChangeTypeType operation: operations) {
            if (elementContext.operationMatches(operation)) {
                return true;
            }
        }
        return false;
    }
}
