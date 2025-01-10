/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_PASSWORD_VALUE;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedModificationTrigger.EvaluatedObjectModificationTrigger;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Component
public class ObjectModificationConstraintEvaluator
        extends ModificationConstraintEvaluator<ModificationPolicyConstraintType, EvaluatedObjectModificationTrigger> {

    private static final String OP_EVALUATE = ObjectModificationConstraintEvaluator.class.getName() + ".evaluate";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectModificationConstraintEvaluator.class);

    private static final String CONSTRAINT_KEY_PREFIX = "objectModification.";

    @Override
    public @NotNull <O extends ObjectType> Collection<EvaluatedObjectModificationTrigger> evaluate(
            @NotNull JAXBElement<ModificationPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            if (!(rctx instanceof ObjectPolicyRuleEvaluationContext<O> ctx)) {
                LOGGER.trace(
                        "Policy rule evaluation context is not of type ObjectPolicyRuleEvaluationContext. Skipping processing.");
                return List.of();
            }

            if (modificationConstraintMatches(constraint, ctx, result)) {
                LocalizableMessage message = createMessage(constraint, rctx, result);
                LocalizableMessage shortMessage = createShortMessage(constraint, rctx, result);
                return List.of(
                        new EvaluatedObjectModificationTrigger(
                                PolicyConstraintKindType.OBJECT_MODIFICATION, constraint.getValue(),
                                null, message, shortMessage));
            } else {
                LOGGER.trace("No operation matches.");
                return List.of();
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
        if (ObjectDelta.isEmpty(summaryDelta) && !ctx.elementContext.isAdd() && !ctx.elementContext.isDelete()) {
            LOGGER.trace("Element context has no delta (primary nor secondary) nor there is ADD/DELETE intention");
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
            if (ObjectDelta.isModify(summaryDelta)) {
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
        if (delta == null) {
            return false;
        } else if (delta.isAdd()) {
            return delta.getObjectToAdd().containsItem(path, false);
        } else if (delta.isDelete()) {
            PrismObject<?> objectOld = ctx.elementContext.getObjectOld();
            return objectOld != null && objectOld.containsItem(path, false);
        } else if (exactPathMatch) {
            return pathMatchesExactly(
                    emptyIfNull(delta.getModifications()), path, 0);
        } else {
            ItemPath nameOnlyPath = path.namedSegmentsOnly();
            PrismObject<?> oldObject = ctx.elementContext.getObjectOld();
            PrismObject<?> newObject = ctx.elementContext.getObjectNew();
            stateCheck(oldObject != null, "No 'old' object in %s", ctx);
            stateCheck(newObject != null, "No 'new' object in %s", ctx);
            if (!valuesChanged(oldObject.getValue(), newObject.getValue(), nameOnlyPath)) {
                return false;
            }
            if (PATH_PASSWORD_VALUE.equivalent(nameOnlyPath) && oldObject.asObjectable() instanceof ShadowType) {
                // Special treatment for shadow password values: Their comparison can produce false (phantom) changes, as
                // the cached value (currently, always hashed) can get compared e.g. with an empty value (if the shadow is
                // fetched from the resource), or an encrypted value, or even encrypted hashed value (in the case of LDAP-side
                // password hashing).
                //
                // So, if there is a difference between old and new values, we use the delta comparison to check
                // if the change is genuine. This may be imprecise, if there is e.g. ADD delta for the whole container,
                // but that would be resolved in the future, if necessary.
                return pathMatchesExactly(
                        emptyIfNull(delta.getModifications()), path, 0);
            } else {
                return true;
            }
        }
    }

    private boolean specialItemMatches(
            ObjectDelta<?> delta, ObjectPolicyRuleEvaluationContext<?> ctx, SpecialItemSpecificationType specialItem)
            throws SchemaException, ConfigurationException {
        assert delta.isModify();
        ResourceObjectDefinition objectDefinition = getObjectDefinition(ctx);
        if (objectDefinition == null) {
            LOGGER.trace("No object definition -> no special item {} evaluation", specialItem);
            return false;
        }
        return switch (specialItem) {
            case RESOURCE_OBJECT_IDENTIFIER -> pathBasedSpecialItemMatches(
                    delta, specialItem, getResourceObjectIdentifierPaths(objectDefinition));
            case RESOURCE_OBJECT_NAMING_ATTRIBUTE -> pathBasedSpecialItemMatches(
                    delta, specialItem, getResourceObjectNamingAttributePath(objectDefinition, specialItem));
            case RESOURCE_OBJECT_ENTITLEMENT -> isEntitlementChange(delta, objectDefinition);
            case RESOURCE_OBJECT_ITEM -> ShadowUtil.hasResourceModifications(delta.getModifications());
        };
    }

    /**
     * In order to check whether an entitlement was changed, we have to know which associations were changed -> to see
     * if these are real entitlements, or not.
     */
    private boolean isEntitlementChange(ObjectDelta<?> delta, ResourceObjectDefinition objectDefinition) {
        for (ItemDelta<?, ?> modification : delta.getModifications()) {
            if (modification.getParentPath().equivalent(ShadowType.F_ASSOCIATIONS)) {
                if (isEntitlementChange(modification.getElementName(), objectDefinition)) {
                    return true;
                }
            } else if (modification.getPath().equivalent(ShadowType.F_ASSOCIATIONS)) {
                // We are touching the whole container (strange, but possible)
                Collection<?> valuesToReplace = modification.getValuesToReplace();
                if (valuesToReplace != null) {
                    // This is a direct replacement of the whole container. Ugly! Consider disallowing this operation.
                    var newItemsNames = getItemNames(valuesToReplace);
                    if (isEntitlementChange(newItemsNames, objectDefinition)) {
                        return true; // The presence of the entitlement here is enough to conclude it MAY HAVE changed.
                    }
                    Collection<?> estOldValues = modification.getEstimatedOldValues();
                    if (estOldValues != null) {
                        // Let us check all entitlements that were deleted.
                        Collection<QName> oldItemsNames = estOldValues.isEmpty() ? List.of() : getItemNames(estOldValues);
                        var deletedItemsNames = CollectionUtils.subtract(oldItemsNames, newItemsNames);
                        return isEntitlementChange(deletedItemsNames, objectDefinition);
                    } else {
                        LOGGER.warn("Replacement delta for associations, not knowing old values -> we cannot evaluate whether"
                                + " there are any entitlement changes. Delta: {}, modification: {}", delta, modification);
                        return false;
                    }
                } else {
                    if (isEntitlementChange(getItemNames(emptyIfNull(modification.getValuesToAdd())), objectDefinition)) {
                        return true;
                    }
                    if (isEntitlementChange(getItemNames(emptyIfNull(modification.getValuesToDelete())), objectDefinition)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static @NotNull Collection<QName> getItemNames(Collection<?> values) {
        if (values.isEmpty()) {
            return List.of();
        }
        stateCheck(values.size() == 1,
                "Wrong number of values to replace: %s", values);
        var newAssociations = (PrismContainerValue<?>) values.iterator().next();
        return newAssociations.getItemNames();
    }

    private boolean isEntitlementChange(QName assocName, ResourceObjectDefinition objectDefinition) {
        var assocDef = objectDefinition.findAssociationDefinition(assocName);
        return assocDef != null && assocDef.isEntitlement();
    }

    private boolean isEntitlementChange(Collection<? extends QName> assocNames, ResourceObjectDefinition objectDefinition) {
        return assocNames.stream()
                .anyMatch(itemName -> isEntitlementChange(itemName, objectDefinition));
    }

    private ResourceObjectDefinition getObjectDefinition(ObjectPolicyRuleEvaluationContext<?> ctx)
            throws SchemaException, ConfigurationException {
        if (ctx.elementContext instanceof LensProjectionContext lensProjectionContext) {
            return lensProjectionContext.getCompositeObjectDefinition();
        } else {
            return null;
        }
    }

    private Collection<ItemPath> getResourceObjectIdentifierPaths(
            ResourceObjectDefinition objectDefinition) {
        return objectDefinition.getAllIdentifiers().stream()
                .map(def -> ItemPath.create(ShadowType.F_ATTRIBUTES, def.getItemName()))
                .collect(Collectors.toList());
    }

    private Collection<ItemPath> getResourceObjectNamingAttributePath(
            ResourceObjectDefinition objectDefinition, SpecialItemSpecificationType specialItem) {
        ShadowSimpleAttributeDefinition<?> namingAttributeDef = objectDefinition.getNamingAttribute();
        if (namingAttributeDef == null) {
            LOGGER.trace("No naming attribute for {} -> no special item {} evaluation", objectDefinition, specialItem);
            return null;
        } else {
            return List.of(ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttributeDef.getItemName()));
        }
    }

    private boolean pathBasedSpecialItemMatches(
            ObjectDelta<?> delta, SpecialItemSpecificationType specialItem, Collection<ItemPath> paths) {
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
