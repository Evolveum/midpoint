/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;
import java.util.List;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Clock;

import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.util.PendingOperationTypeUtil;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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

    private static final Trace LOGGER = TraceManager.getTrace(ShadowCaretaker.class);

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
                //noinspection rawtypes,unchecked
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
                    // We are going to rewrite the definition anyway. Let's just do some basic checks first
                    if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
                        throw new SchemaException(
                                "The value of type %s cannot be applied to attribute %s which is of type %s".formatted(
                                        itemDef.getTypeName(), attributeName, attributeDefinition.getTypeName()));
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
            @NotNull ProvisioningContext ctx, @NotNull PrismObject<ShadowType> shadow)
            throws SchemaException, ConfigurationException {
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

        ProvisioningContext subContext = ctx.spawnForShadow(shadow);
        subContext.assertDefinition();

        applyAttributesDefinition(subContext.getObjectDefinitionRequired(), shadow);

        return subContext;
    }

    /**
     * Just applies the definition to a bean.
     */
    void applyAttributesDefinition(
            @NotNull ResourceObjectDefinition definition, @NotNull ShadowType bean)
            throws SchemaException {

        PrismObject<ShadowType> shadowObject = bean.asPrismObject();

        PrismContainer<ShadowAttributesType> attributesContainer = shadowObject.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null) {
            applyAttributesDefinitionToContainer(definition, attributesContainer, shadowObject.getValue(), bean);
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
                            ShadowType.F_ATTRIBUTES, definition.toResourceAttributeContainerDefinition());
            shadowObject.setDefinition(clonedDefinition);
            clonedDefinition.freeze();
        }
    }

    public static void applyAttributesDefinitionToContainer(
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

    /** Returns {@link ChangeTypeType#ADD}, {@link ChangeTypeType#DELETE}, or null. */
    public ChangeTypeType findPendingLifecycleOperationInGracePeriod(
            @Nullable ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            @NotNull XMLGregorianCalendar now) {
        List<PendingOperationType> pendingOperations = shadow.getPendingOperation();
        if (pendingOperations.isEmpty()) {
            return null;
        }
        Duration gracePeriod = ctx != null ? ctx.getGracePeriod() : null;
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
        // after the life (dead)
        if (ShadowUtil.isDead(shadow)) {
            if (pendingLifecycleOperation == ChangeTypeType.DELETE) {
                return ShadowLifecycleStateType.CORPSE;
            } else {
                return ShadowLifecycleStateType.TOMBSTONE;
            }
        }
        // before the life (not existing yet)
        if (!ShadowUtil.isExists(shadow)) {
            if (hasPendingOrExecutingAdd(shadow)) {
                return ShadowLifecycleStateType.CONCEIVED;
            } else {
                return ShadowLifecycleStateType.PROPOSED;
            }
        }
        // during the life (existing and not dead)
        if (pendingLifecycleOperation == ChangeTypeType.DELETE) {
            return ShadowLifecycleStateType.REAPING;
        } else if (pendingLifecycleOperation == ChangeTypeType.ADD) {
            return ShadowLifecycleStateType.GESTATING;
        } else {
            return ShadowLifecycleStateType.LIVE;
        }
    }

    /** Determines and updates the shadow state. */
    void updateShadowState(ProvisioningContext ctx, ShadowType shadow) {
        ShadowLifecycleStateType state = determineShadowState(ctx, shadow);
        shadow.setShadowLifecycleState(state);
        LOGGER.trace("shadow state is {}", state);
    }
}
