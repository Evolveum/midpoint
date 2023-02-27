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
import java.util.Objects;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Clock;

import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.util.PendingOperationTypeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.isCompletedAndOverPeriod;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTION_PENDING;

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

    @Autowired private Clock clock;

    // Please use this method only via ProvisioningContext
    void applyAttributesDefinition(ProvisioningContext ctx, ObjectDelta<ShadowType> delta)
            throws SchemaException, ConfigurationException {
        if (delta.isAdd()) {
            applyAttributesDefinitionInNewContext(ctx, delta.getObjectToAdd());
        } else if (delta.isModify()) {
            applyAttributesDefinition(ctx, delta.getModifications());
        }
    }

    // Please use this method only via ProvisioningContext
    void applyAttributesDefinition(ProvisioningContext ctx, Collection<? extends ItemDelta<?, ?>> modifications)
            throws SchemaException {
        for (ItemDelta<?, ?> itemDelta : modifications) {
            if (SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
                applyAttributeDefinition(ctx, (ItemDelta) itemDelta);
            } else if (SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getPath())) {
                if (itemDelta.isAdd()) {
                    for (PrismValue value : itemDelta.getValuesToAdd()) {
                        applyAttributesDefinition(ctx, value);
                    }
                }
                if (itemDelta.isReplace()) {
                    for (PrismValue value : itemDelta.getValuesToReplace()) {
                        applyAttributesDefinition(ctx, value);
                    }
                }
            }
        }
    }

    // value should be a value of attributes container
    private void applyAttributesDefinition(ProvisioningContext ctx, PrismValue value)
            throws SchemaException  {
        if (!(value instanceof PrismContainerValue)) {
            return; // should never occur
        }
        //noinspection unchecked
        PrismContainerValue<ShadowAttributesType> pcv = (PrismContainerValue<ShadowAttributesType>) value;
        for (Item<?, ?> item : pcv.getItems()) {
            ItemDefinition<?> itemDef = item.getDefinition();
            if (!(itemDef instanceof ResourceAttributeDefinition)) {
                QName attributeName = item.getElementName();
                ResourceAttributeDefinition<?> attributeDefinition = ctx.findAttributeDefinitionRequired(attributeName);
                if (itemDef != null) {
                    // We are going to rewrite the definition anyway. Let's just
                    // do some basic checks first
                    if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
                        throw new SchemaException("The value of type " + itemDef.getTypeName()
                                + " cannot be applied to attribute " + attributeName + " which is of type "
                                + attributeDefinition.getTypeName());
                    }
                }
                //noinspection unchecked,rawtypes
                ((Item) item).applyDefinition(attributeDefinition);
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void applyAttributeDefinition(
            ProvisioningContext ctx, ItemDelta<V, D> itemDelta)
            throws SchemaException {
        if (!SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
            // just to be sure
            return;
        }
        D itemDef = itemDelta.getDefinition();
        if (!(itemDef instanceof ResourceAttributeDefinition)) {
            QName attributeName = itemDelta.getElementName();
            ResourceAttributeDefinition<?> attributeDefinition =
                    ctx.findAttributeDefinitionRequired(attributeName, () -> " in object delta");
            if (itemDef != null) {
                // We are going to rewrite the definition anyway. Let's just do
                // some basic checks first
                if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
                    throw new SchemaException("The value of type " + itemDef.getTypeName()
                            + " cannot be applied to attribute " + attributeName + " which is of type "
                            + attributeDefinition.getTypeName());
                }
            }
            //noinspection unchecked
            itemDelta.applyDefinition((D) attributeDefinition);
        }
    }

    /** See {@link #applyAttributesDefinitionInNewContext(ProvisioningContext, ShadowType)}. */
    ProvisioningContext applyAttributesDefinitionInNewContext(
            @NotNull ProvisioningContext ctx, @NotNull PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException {
        return applyAttributesDefinitionInNewContext(ctx, shadow.asObjectable());
    }

    /**
     * Creates a new sub-context based on the kind/intent/class of the provided shadow, and applies the attribute definitions.
     *
     * TODO better name?
     *
     * @return the new sub-context
     */
    ProvisioningContext applyAttributesDefinitionInNewContext(
            @NotNull ProvisioningContext ctx, @NotNull ShadowType shadow)
            throws SchemaException, ConfigurationException {

        PrismObject<ShadowType> shadowObject = shadow.asPrismObject();
        ProvisioningContext subContext = ctx.spawnForShadow(shadow);
        subContext.assertDefinition();
        ResourceObjectDefinition objectDefinition = subContext.getObjectDefinitionRequired();

        PrismContainer<ShadowAttributesType> attributesContainer = shadowObject.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null) {
            applyAttributesDefinitionToContainer(objectDefinition, attributesContainer, shadowObject.getValue(), shadow);
        }

        // TODO what about associations definitions?

        // We also need to replace the entire object definition to inject
        // correct resource object definition here
        // If we don't do this then the patch (delta.applyTo) will not work
        // correctly because it will not be able to
        // create the attribute container if needed.

        PrismObjectDefinition<ShadowType> prismShadowDefinition = shadowObject.getDefinition();
        PrismContainerDefinition<ShadowAttributesType> origAttrContainerDef = prismShadowDefinition
                .findContainerDefinition(ShadowType.F_ATTRIBUTES);
        if (!(origAttrContainerDef instanceof ResourceAttributeContainerDefinition)) {
            PrismObjectDefinition<ShadowType> clonedDefinition =
                    prismShadowDefinition.cloneWithReplacedDefinition(
                            ShadowType.F_ATTRIBUTES, objectDefinition.toResourceAttributeContainerDefinition());
            shadowObject.setDefinition(clonedDefinition);
            clonedDefinition.freeze();
        }

        return subContext;
    }

    private void applyAttributesDefinitionToContainer(
            ResourceObjectDefinition objectDefinition,
            PrismContainer<ShadowAttributesType> attributesContainer,
            PrismContainerValue<?> parentPcv,
            Object context) throws SchemaException {
        if (attributesContainer instanceof ResourceAttributeContainer) {
            if (attributesContainer.getDefinition() == null) {
                attributesContainer.applyDefinition(objectDefinition.toResourceAttributeContainerDefinition());
            }
        } else {
            try {
                // We need to convert <attributes> to ResourceAttributeContainer
                ResourceAttributeContainer convertedContainer =
                        ResourceAttributeContainer.convertFromContainer(attributesContainer, objectDefinition);
                parentPcv.replace(attributesContainer, convertedContainer);
            } catch (SchemaException e) {
                throw e.wrap("Couldn't apply attributes definitions in " + context);
            }
        }
    }

    /**
     * Reapplies definition to the shadow if needed. The definition needs to be
     * reapplied e.g. if the shadow has auxiliary object classes, if it is of a subclass
     * of the object class that was originally requested, etc.
     */
    public ProvisioningContext reapplyDefinitions(
            ProvisioningContext ctx, ShadowType rawResourceObject) throws SchemaException, ConfigurationException {
        QName objectClassQName = rawResourceObject.getObjectClass();
        List<QName> auxiliaryObjectClassQNames = rawResourceObject.getAuxiliaryObjectClass();
        if (auxiliaryObjectClassQNames.isEmpty()
                && objectClassQName.equals(ctx.getObjectClassNameRequired())) {
            // shortcut, no need to reapply anything
            return ctx;
        }
        ProvisioningContext shadowCtx = ctx.spawnForShadow(rawResourceObject);
        shadowCtx.assertDefinition();
        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(rawResourceObject);
        attributesContainer.applyDefinition(
                shadowCtx.getObjectDefinitionRequired()
                        .toResourceAttributeContainerDefinition());
        return shadowCtx;
    }

    public @NotNull ShadowType applyPendingOperations(
            @NotNull ProvisioningContext shadowCtx,
            @NotNull ShadowType repoShadow,
            ShadowType resourceShadow,
            boolean skipExecutionPendingOperations,
            XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        @NotNull ShadowType resultShadow = Objects.requireNonNullElse(resourceShadow, repoShadow);

        if (ShadowUtil.isDead(resultShadow)) {
            return resultShadow;
        }

        List<PendingOperationType> pendingOperations = repoShadow.getPendingOperation();
        if (pendingOperations.isEmpty()) {
            return resultShadow;
        }
        List<PendingOperationType> sortedOperations = sortPendingOperations(pendingOperations);
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(shadowCtx);
        boolean resourceReadIsCachingOnly = shadowCtx.isReadingCachingOnly();
        for (PendingOperationType pendingOperation: sortedOperations) {
            OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
            PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
            if (resultStatus == NOT_APPLICABLE) {
                // Not applicable means: "no point trying this, will not retry". Therefore it will not change future state.
                continue;
            }
            if (executionStatus == COMPLETED && isCompletedAndOverPeriod(now, gracePeriod, pendingOperation)) {
                // Completed operations over grace period. They have already affected current state. They are already "applied".
                continue;
            }
            // Note: We still want to process errors, even fatal errors. As long as they are in executing state then they
            // are going to be retried and they still may influence future state
            if (skipExecutionPendingOperations && executionStatus == EXECUTION_PENDING) {
                continue;
            }
            if (resourceReadIsCachingOnly) {
                // We are getting the data from our own cache. So we know that all completed operations are already applied
                // in the cache. Re-applying them will mean additional risk of corrupting the data.
                if (resultStatus != null && resultStatus != IN_PROGRESS && resultStatus != UNKNOWN) {
                    continue;
                }
            } else {
                // We want to apply all the deltas, even those that are already completed. They might not be reflected
                // on the resource yet. E.g. they may be not be present in the CSV export until the next export cycle is scheduled
            }
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingOperation.getDelta());
            if (pendingDelta.isAdd()) {
                // In case that we have resourceShadow then do NOT apply ADD operation
                // In that case the object was obviously already created. The data that we have from the
                // resource are going to be more precise than the pending ADD delta (which might not have been applied completely)
                if (resourceShadow == null) {
                    resultShadow = pendingDelta.getObjectToAdd().clone().asObjectable();
                    resultShadow.setOid(repoShadow.getOid());
                    resultShadow.setExists(true);
                    resultShadow.setName(repoShadow.getName());
                    resultShadow.setShadowLifecycleState(repoShadow.getShadowLifecycleState());
                    List<PendingOperationType> newPendingOperations = resultShadow.getPendingOperation();
                    for (PendingOperationType pendingOperation2: repoShadow.getPendingOperation()) {
                        newPendingOperations.add(pendingOperation2.clone());
                    }
                    applyAttributesDefinitionInNewContext(shadowCtx, resultShadow);
                }
            }
            if (pendingDelta.isModify()) {
                // Attribute values get their definitions here (assuming the shadow has the refined definition).
                // Association values do not.
                pendingDelta.applyTo(resultShadow.asPrismObject());
            }
            if (pendingDelta.isDelete()) {
                resultShadow.setDead(true);
                resultShadow.setExists(false);
                resultShadow.setPrimaryIdentifierValue(null);
            }
        }

        if (shadowCtx.hasDefinition()) {
            applyAssociationsDefinitions(shadowCtx, resultShadow);
        }

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

    /** Applies the correct definitions to identifier containers in association values. Assumes known shadow type. */
    private void applyAssociationsDefinitions(ProvisioningContext shadowCtx, ShadowType shadow)
            throws ConfigurationException, SchemaException {
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
            ResourceAssociationDefinition assocDef =
                    shadowCtx.getObjectDefinitionRequired().findAssociationDefinition(associationName);
            if (assocDef == null) {
                continue;
            }

            ProvisioningContext assocCtx = shadowCtx.spawnForKindIntent(assocDef.getKind(), assocDef.getAnyIntent());
            ResourceObjectDefinition assocObjectDef = assocCtx.getObjectDefinition();
            if (assocObjectDef == null) {
                continue;
            }
            applyAttributesDefinitionToContainer(assocObjectDef, identifiersContainer, associationPcv, shadow);
        }
    }

    public List<PendingOperationType> sortPendingOperations(List<PendingOperationType> pendingOperations) {
        // Copy to mutable list that is not bound to the prism
        List<PendingOperationType> sortedList = new ArrayList<>(pendingOperations.size());
        sortedList.addAll(pendingOperations);
        sortedList.sort((o1, o2) -> XmlTypeConverter.compare(o1.getRequestTimestamp(), o2.getRequestTimestamp()));
        return sortedList;
    }

    /** Returns {@link ChangeTypeType#ADD}, {@link ChangeTypeType#DELETE}, or null. */
    public ChangeTypeType findPendingLifecycleOperationInGracePeriod(
            @Nullable ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            @NotNull XMLGregorianCalendar now) {
        List<PendingOperationType> pendingOperations = shadow.getPendingOperation();
        if (pendingOperations.isEmpty()) {
            return null;
        }
        Duration gracePeriod = ctx != null ? ProvisioningUtil.getGracePeriod(ctx) : null;
        ChangeTypeType found = null;
        for (PendingOperationType pendingOperation : pendingOperations) {
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (delta == null) {
                continue;
            }
            ChangeTypeType changeType = delta.getChangeType();
            if (ChangeTypeType.MODIFY.equals(changeType)) {
                continue; // MODIFY is not a lifecycle operation
            }
            if (ProvisioningUtil.isCompletedAndOverPeriod(now, gracePeriod, pendingOperation)) {
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

    private boolean hasPendingOrExecutingAdd(@NotNull ShadowType shadow) {
        return shadow.getPendingOperation().stream()
                .anyMatch(p ->
                        PendingOperationTypeUtil.isAdd(p)
                        && PendingOperationTypeUtil.isPendingOrExecuting(p));
    }

    // NOTE: detection of quantum states (gestation, corpse) might not be precise. E.g. the shadow may already be
    // tombstone because it is not in the snapshot. But as long as the pending operation is in grace we will still
    // detect it as corpse. But that should not cause any big problems.
    public @NotNull ShadowLifecycleStateType determineShadowState(
            @NotNull ProvisioningContext ctx, @NotNull ShadowType shadow) {
        return determineShadowStateInternal(ctx, shadow, clock.currentTimeXMLGregorianCalendar());
   }

    /**
     * Determines the shadow lifecycle state according to https://docs.evolveum.com/midpoint/reference/resources/shadow/dead/.
     *
     * @param ctx Used to know the grace period. In emergency situations it can be null.
     */
    private @NotNull ShadowLifecycleStateType determineShadowStateInternal(
            @Nullable ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            @NotNull XMLGregorianCalendar now) {
        ChangeTypeType pendingLifecycleOperation = findPendingLifecycleOperationInGracePeriod(ctx, shadow, now);
        if (ShadowUtil.isDead(shadow)) {
            if (pendingLifecycleOperation == ChangeTypeType.DELETE) {
                return ShadowLifecycleStateType.CORPSE;
            } else {
                return ShadowLifecycleStateType.TOMBSTONE;
            }
        }
        if (ShadowUtil.isExists(shadow)) {
            if (pendingLifecycleOperation == ChangeTypeType.DELETE) {
                return ShadowLifecycleStateType.REAPING;
            } else if (pendingLifecycleOperation == ChangeTypeType.ADD) {
                return ShadowLifecycleStateType.GESTATING;
            } else {
                return ShadowLifecycleStateType.LIVE;
            }
        }
        if (hasPendingOrExecutingAdd(shadow)) {
            return ShadowLifecycleStateType.CONCEIVED;
        } else {
            return ShadowLifecycleStateType.PROPOSED;
        }
    }

    /** Determines and updates the shadow state. */
    void updateShadowState(ProvisioningContext ctx, ShadowType shadow) {
        shadow.setShadowLifecycleState(
                determineShadowState(ctx, shadow));
    }
}
