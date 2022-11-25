/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Clock;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Component that takes care of some shadow (or resource object) maintenance, such as applying definitions, applying pending
 * operations and so on.
 *
 * Important: this component should be pretty much stand-alone low-level component. It should NOT have
 * any dependencies on ShadowCache, ShadowManager or any other provisioning components (except util components).
 *
 * @author Radovan Semancik
 */
@Component
public class ShadowCaretaker {

    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;

    public void applyAttributesDefinition(ProvisioningContext ctx, ObjectDelta<ShadowType> delta)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (delta.isAdd()) {
            applyAttributesDefinition(ctx, delta.getObjectToAdd());
        } else if (delta.isModify()) {
            for (ItemDelta<?, ?> itemDelta : delta.getModifications()) {
                if (SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
                    applyAttributeDefinition(ctx, delta, itemDelta);
                } else if (SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getPath())) {
                    if (itemDelta.isAdd()) {
                        for (PrismValue value : itemDelta.getValuesToAdd()) {
                            applyAttributeDefinition(ctx, value);
                        }
                    }
                    if (itemDelta.isReplace()) {
                        for (PrismValue value : itemDelta.getValuesToReplace()) {
                            applyAttributeDefinition(ctx, value);
                        }
                    }
                }
            }
        }
    }

    // value should be a value of attributes container
    private void applyAttributeDefinition(ProvisioningContext ctx, PrismValue value)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (!(value instanceof PrismContainerValue)) {
            return; // should never occur
        }
        PrismContainerValue<ShadowAttributesType> pcv = (PrismContainerValue<ShadowAttributesType>) value;
        for (Item item : pcv.getItems()) {
            ItemDefinition itemDef = item.getDefinition();
            if (!(itemDef instanceof ResourceAttributeDefinition)) {
                QName attributeName = item.getElementName();
                ResourceAttributeDefinition attributeDefinition = ctx.getObjectClassDefinition()
                        .findAttributeDefinition(attributeName);
                if (attributeDefinition == null) {
                    throw new SchemaException("No definition for attribute " + attributeName);
                }
                if (itemDef != null) {
                    // We are going to rewrite the definition anyway. Let's just
                    // do some basic checks first
                    if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
                        throw new SchemaException("The value of type " + itemDef.getTypeName()
                                + " cannot be applied to attribute " + attributeName + " which is of type "
                                + attributeDefinition.getTypeName());
                    }
                }
                item.applyDefinition(attributeDefinition);
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition> void applyAttributeDefinition(
            ProvisioningContext ctx, ObjectDelta<ShadowType> delta, ItemDelta<V, D> itemDelta)
                    throws SchemaException, ConfigurationException, ObjectNotFoundException,
                    CommunicationException, ExpressionEvaluationException {
        if (!SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
            // just to be sure
            return;
        }
        D itemDef = itemDelta.getDefinition();
        if (itemDef == null || !(itemDef instanceof ResourceAttributeDefinition)) {
            QName attributeName = itemDelta.getElementName();
            ResourceAttributeDefinition attributeDefinition = ctx.getObjectClassDefinition()
                    .findAttributeDefinition(attributeName);
            if (attributeDefinition == null) {
                throw new SchemaException(
                        "No definition for attribute " + attributeName + " in object delta " + delta);
            }
            if (itemDef != null) {
                // We are going to rewrite the definition anyway. Let's just do
                // some basic checks first
                if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
                    throw new SchemaException("The value of type " + itemDef.getTypeName()
                            + " cannot be applied to attribute " + attributeName + " which is of type "
                            + attributeDefinition.getTypeName());
                }
            }
            itemDelta.applyDefinition((D) attributeDefinition);
        }
    }

    @NotNull public PrismObject<ShadowType> applyAttributesDefinitionToImmutable(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        PrismObject<ShadowType> mutableShadow = shadow.clone();
        applyAttributesDefinition(ctx, mutableShadow);
        return mutableShadow.createImmutableClone();
    }

    public ProvisioningContext applyAttributesDefinition(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        ProvisioningContext subctx = ctx.spawn(shadow);
        subctx.assertDefinition();
        RefinedObjectClassDefinition objectClassDefinition = subctx.getObjectClassDefinition();

        PrismContainer<ShadowAttributesType> attributesContainer = shadow
                .findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null) {
            if (attributesContainer instanceof ResourceAttributeContainer) {
                if (attributesContainer.getDefinition() == null) {
                    attributesContainer
                            .applyDefinition(objectClassDefinition.toResourceAttributeContainerDefinition());
                }
            } else {
                try {
                    // We need to convert <attributes> to
                    // ResourceAttributeContainer
                    ResourceAttributeContainer convertedContainer = ResourceAttributeContainer
                            .convertFromContainer(attributesContainer, objectClassDefinition);
                    shadow.getValue().replace(attributesContainer, convertedContainer);
                } catch (SchemaException e) {
                    throw new SchemaException(e.getMessage() + " in " + shadow, e);
                }
            }
        }

        // We also need to replace the entire object definition to inject
        // correct object class definition here
        // If we don't do this then the patch (delta.applyTo) will not work
        // correctly because it will not be able to
        // create the attribute container if needed.

        PrismObjectDefinition<ShadowType> objectDefinition = shadow.getDefinition();
        PrismContainerDefinition<ShadowAttributesType> origAttrContainerDef = objectDefinition
                .findContainerDefinition(ShadowType.F_ATTRIBUTES);
        if (!(origAttrContainerDef instanceof ResourceAttributeContainerDefinition)) {
            PrismObjectDefinition<ShadowType> clonedDefinition = objectDefinition.cloneWithReplacedDefinition(
                    ShadowType.F_ATTRIBUTES, objectClassDefinition.toResourceAttributeContainerDefinition());
            shadow.setDefinition(clonedDefinition);
            clonedDefinition.freeze();
        }

        return subctx;
    }

    /**
     * Reapplies definition to the shadow if needed. The definition needs to be
     * reapplied e.g. if the shadow has auxiliary object classes, if it is of a subclass
     * of the object class that was originally requested, etc.
     */
    public ProvisioningContext reapplyDefinitions(ProvisioningContext ctx,
            PrismObject<ShadowType> rawResourceObject) throws SchemaException, ConfigurationException,
                    ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        ShadowType rawResourceObjectBean = rawResourceObject.asObjectable();
        QName objectClassQName = rawResourceObjectBean.getObjectClass();
        List<QName> auxiliaryObjectClassQNames = rawResourceObjectBean.getAuxiliaryObjectClass();
        if (auxiliaryObjectClassQNames.isEmpty()
                && objectClassQName.equals(ctx.getObjectClassDefinition().getTypeName())) {
            // shortcut, no need to reapply anything
            return ctx;
        }
        ProvisioningContext shadowCtx = ctx.spawn(rawResourceObject);
        shadowCtx.assertDefinition();
        RefinedObjectClassDefinition shadowDef = shadowCtx.getObjectClassDefinition();
        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(rawResourceObject);
        attributesContainer.applyDefinition(shadowDef.toResourceAttributeContainerDefinition());
        return shadowCtx;
    }

    public PrismObject<ShadowType> applyPendingOperations(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, PrismObject<ShadowType> resourceShadow,
            boolean skipExecutionPendingOperations,
            XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (repoShadow == null) {
            return resourceShadow;
        }

        PrismObject<ShadowType> resultShadow;
        if (resourceShadow == null) {
            resultShadow = repoShadow;
        } else {
            resultShadow = resourceShadow;
        }

        if (ShadowUtil.isDead(resultShadow)) {
            return resultShadow;
        }

        List<PendingOperationType> pendingOperations = repoShadow.asObjectable().getPendingOperation();
        if (pendingOperations.isEmpty()) {
            return resultShadow;
        }
        ShadowType resultShadowBean = resultShadow.asObjectable();
        List<PendingOperationType> sortedOperations = sortPendingOperations(pendingOperations);
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        boolean resourceReadIsCachingOnly = ProvisioningUtil.resourceReadIsCachingOnly(ctx.getResource());
        for (PendingOperationType pendingOperation: sortedOperations) {
            OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
            PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
            if (OperationResultStatusType.NOT_APPLICABLE.equals(resultStatus)) {
                // Not applicable means: "no point trying this, will not retry". Therefore it will not change future state.
                continue;
            }
            if (PendingOperationExecutionStatusType.COMPLETED.equals(executionStatus) && ProvisioningUtil.isOverPeriod(now, gracePeriod, pendingOperation)) {
                // Completed operations over grace period. They have already affected current state. They are already "applied".
                continue;
            }
            // Note: We still want to process errors, even fatal errors. As long as they are in executing state then they
            // are going to be retried and they still may influence future state
            if (skipExecutionPendingOperations && executionStatus == PendingOperationExecutionStatusType.EXECUTION_PENDING) {
                continue;
            }
            if (resourceReadIsCachingOnly) {
                // We are getting the data from our own cache. So we know that all completed operations are already applied in the cache.
                // Re-applying them will mean additional risk of corrupting the data.
                if (resultStatus != null && resultStatus != OperationResultStatusType.IN_PROGRESS && resultStatus != OperationResultStatusType.UNKNOWN) {
                    continue;
                }
            } else {
                // We want to apply all the deltas, even those that are already completed. They might not be reflected on the resource yet.
                // E.g. they may be not be present in the CSV export until the next export cycle is scheduled
            }
            ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);
            if (pendingDelta.isAdd()) {
                // In case that we have resourceShadow then do NOT apply ADD operation
                // In that case the object was obviously already created. The data that we have from the
                // resource are going to be more precise than the pending ADD delta (which might not have been applied completely)
                if (resourceShadow == null) {
                    ShadowType repoShadowBean = repoShadow.asObjectable();
                    resultShadow = pendingDelta.getObjectToAdd().clone();
                    resultShadow.setOid(repoShadow.getOid());
                    resultShadowBean = resultShadow.asObjectable();
                    resultShadowBean.setExists(true);
                    resultShadowBean.setName(repoShadowBean.getName());
                    resultShadowBean.setShadowLifecycleState(repoShadowBean.getShadowLifecycleState());
                    List<PendingOperationType> newPendingOperations = resultShadowBean.getPendingOperation();
                    for (PendingOperationType pendingOperation2: repoShadowBean.getPendingOperation()) {
                        newPendingOperations.add(pendingOperation2.clone());
                    }
                    applyAttributesDefinition(ctx, resultShadow);
                }
            }
            if (pendingDelta.isModify()) {
                pendingDelta.applyTo(resultShadow);
            }
            if (pendingDelta.isDelete()) {
                resultShadowBean.setDead(true);
                resultShadowBean.setExists(false);
                resultShadowBean.setPrimaryIdentifierValue(null);
            }
        }
        applyAssociationsDefinitions(ctx, resultShadow.asObjectable());
        // TODO: check schema, remove non-readable attributes, activation, password, etc.
//        CredentialsType creds = resultShadowType.getCredentials();
//        if (creds != null) {
//            PasswordType passwd = creds.getPassword();
//            if (passwd != null) {
//                passwd.setValue(null);
//            }
//        }
        return resultShadow;
    }

    /**
     * Applies the correct definitions to identifier containers in association values. Assumes known shadow type.
     * A bit hacked. See 4.7-version for more serious solution.
     */
    private void applyAssociationsDefinitions(ProvisioningContext shadowCtx, ShadowType shadow)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ObjectNotFoundException {
        RefinedObjectClassDefinition objectDef = shadowCtx.getObjectClassDefinition();
        if (objectDef == null) {
            return;
        }
        for (ShadowAssociationType association : shadow.getAssociation()) {
            var associationPcv = association.asPrismContainerValue();
            // Identifiers are ShadowIdentifiersType but to make compiler happy let's pretend it's ShadowAttributesType.
            //noinspection unchecked
            PrismContainer<ShadowAttributesType> identifiersContainer =
                    associationPcv.findContainer(ShadowAssociationType.F_IDENTIFIERS);
            if (identifiersContainer == null) {
                continue;
            }

            QName associationName = association.getName();
            if (associationName == null) {
                continue;
            }
            RefinedAssociationDefinition assocDef = objectDef.findAssociationDefinition(associationName);
            if (assocDef == null) {
                continue;
            }
            Collection<String> intents = assocDef.getIntents();
            if (intents.isEmpty()) {
                continue;
            }

            ProvisioningContext assocCtx = shadowCtx.spawn(assocDef.getKind(), intents.iterator().next());
            RefinedObjectClassDefinition assocObjectDef = assocCtx.getObjectClassDefinition();
            if (assocObjectDef == null) {
                continue;
            }
            applyAttributesDefinitionToContainer(assocObjectDef, identifiersContainer, associationPcv);
        }
    }

    private void applyAttributesDefinitionToContainer(
            RefinedObjectClassDefinition objectDefinition,
            PrismContainer<ShadowAttributesType> attributesContainer,
            PrismContainerValue<?> parentPcv) throws SchemaException {
        if (attributesContainer instanceof ResourceAttributeContainer) {
            if (attributesContainer.getDefinition() == null) {
                attributesContainer.applyDefinition(objectDefinition.toResourceAttributeContainerDefinition());
            }
        } else {
            // We need to convert <attributes> to ResourceAttributeContainer
            ResourceAttributeContainer convertedContainer =
                    ResourceAttributeContainer.convertFromContainer(attributesContainer, objectDefinition);
            parentPcv.replace(attributesContainer, convertedContainer);
        }
    }

    public List<PendingOperationType> sortPendingOperations(List<PendingOperationType> pendingOperations) {
        // Copy to mutable list that is not bound to the prism
        List<PendingOperationType> sortedList = new ArrayList<>(pendingOperations.size());
        sortedList.addAll(pendingOperations);
        sortedList.sort((o1, o2) -> XmlTypeConverter.compare(o1.getRequestTimestamp(), o2.getRequestTimestamp()));
        return sortedList;
    }

    public ChangeTypeType findPreviousPendingLifecycleOperationInGracePeriod(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow, XMLGregorianCalendar now)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
        if (pendingOperations.isEmpty()) {
            return null;
        }
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        ChangeTypeType found = null;
        for (PendingOperationType pendingOperation : pendingOperations) {
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (delta == null) {
                continue;
            }
            ChangeTypeType changeType = delta.getChangeType();
            if (ChangeTypeType.MODIFY.equals(changeType)) {
                continue;
            }
            if (ProvisioningUtil.isOverPeriod(now, gracePeriod, pendingOperation)) {
                continue;
            }
            if (changeType == ChangeTypeType.DELETE) {
                // DELETE always wins
                return changeType;
            } else {
                // If there is an ADD then let's check for delete.
                found = changeType;
            }
        }
        return found;
    }


    // NOTE: detection of quantum states (gestation, corpse) might not be precise. E.g. the shadow may already be
    // tombstone because it is not in the snapshot. But as long as the pending operation is in grace we will still
    // detect it as corpse. But that should not cause any big problems.
    public @NotNull ShadowLifecycleStateType determineShadowState(ProvisioningContext ctx, PrismObject<ShadowType> shadow, XMLGregorianCalendar now)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ShadowType shadowBean = shadow.asObjectable();
        ChangeTypeType pendingLifecycleOperation = findPreviousPendingLifecycleOperationInGracePeriod(ctx, shadow, now);
        if (ShadowUtil.isDead(shadowBean)) {
            if (pendingLifecycleOperation == ChangeTypeType.DELETE) {
                return ShadowLifecycleStateType.CORPSE;
            } else {
                return ShadowLifecycleStateType.TOMBSTONE;
            }
        }
        if (ShadowUtil.isExists(shadowBean)) {
            if (pendingLifecycleOperation == ChangeTypeType.DELETE) {
                return ShadowLifecycleStateType.REAPING;
            } else if (pendingLifecycleOperation == ChangeTypeType.ADD) {
                return ShadowLifecycleStateType.GESTATING;
            } else {
                return ShadowLifecycleStateType.LIVE;
            }
        }
        // FIXME currently we don't work correctly with the object lifecycle for shadows. E.g. some shadows are marked
        //  as proposed, even if they are active in fact. So be careful with this.
        if (SchemaConstants.LIFECYCLE_PROPOSED.equals(shadowBean.getLifecycleState())) {
            return ShadowLifecycleStateType.PROPOSED;
        } else {
            return ShadowLifecycleStateType.CONCEIVED;
        }
    }

    /** Determines and updates the shadow state. */
    public void updateShadowState(ProvisioningContext ctx, PrismObject<ShadowType> shadow)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException {
        ShadowLifecycleStateType state = determineShadowState(ctx, shadow, clock.currentTimeXMLGregorianCalendar());
        shadow.asObjectable().setShadowLifecycleState(state);
    }

    /** Determines and updates (and returns) the shadow state. */
    public @NotNull ShadowLifecycleStateType updateAndReturnShadowState(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow, XMLGregorianCalendar now)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ShadowLifecycleStateType state = determineShadowState(ctx, shadow, now);
        shadow.asObjectable().setShadowLifecycleState(state);
        return state;
    }
}
