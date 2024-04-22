/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Applies attributes/associations definitions to a shadow.
 *
 * Originally, this functionality was in `ShadowCaretaker` in the `provisioning-impl` module.
 * In the future, maybe it should be part of standard `applyDefinition` mechanism.
 *
 * @author Radovan Semancik
 */
public class ShadowDefinitionApplicator {

    @NotNull private final ResourceObjectDefinition definition;

    public ShadowDefinitionApplicator(@NotNull ResourceObjectDefinition definition) {
        this.definition = definition;
    }

    public void applyTo(@NotNull ObjectDelta<ShadowType> delta) throws SchemaException {
        if (delta.isAdd()) {
            applyTo(delta.getObjectableToAdd());
        } else if (delta.isModify()) {
            applyTo(delta.getModifications());
        }
    }

    public void applyTo(@NotNull Collection<? extends ItemDelta<?, ?>> modifications)
            throws SchemaException {
        for (ItemDelta<?, ?> itemDelta : modifications) {
            if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
                //noinspection rawtypes,unchecked
                applyToAttributeDelta((ItemDelta) itemDelta);
            } else if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getPath())) {
                if (itemDelta.isAdd()) {
                    for (PrismValue value : itemDelta.getValuesToAdd()) {
                        applyToAttributesContainerValue(value);
                    }
                }
                if (itemDelta.isReplace()) {
                    for (PrismValue value : itemDelta.getValuesToReplace()) {
                        applyToAttributesContainerValue(value);
                    }
                }
            } else if (ShadowType.F_ASSOCIATIONS.equivalent(itemDelta.getParentPath())) {
                applyToAssociationDelta(itemDelta);
            } else if (ShadowType.F_ASSOCIATIONS.equivalent(itemDelta.getPath())) {
                if (itemDelta.isAdd()) {
                    for (PrismValue value : itemDelta.getValuesToAdd()) {
                        applyToAssociationsContainerValue(value);
                    }
                }
                if (itemDelta.isReplace()) { // actually, this should not occur
                    for (PrismValue value : itemDelta.getValuesToReplace()) {
                        applyToAssociationsContainerValue(value);
                    }
                }
            }
        }
    }

    private void applyToAttributesContainerValue(PrismValue value) throws SchemaException  {
        //noinspection unchecked
        PrismContainerValue<ShadowAttributesType> pcv = (PrismContainerValue<ShadowAttributesType>) value;
        for (Item<?, ?> item : List.copyOf(pcv.getItems())) {
            ItemDefinition<?> itemDef = item.getDefinition();
            if (itemDef instanceof ResourceAttributeDefinition) {
                continue; // already ok
            }
            var attributeDefinition = definition.findAttributeDefinitionRequired(item.getElementName());
            pcv.remove(item);
            pcv.add(
                    attributeDefinition.instantiateFrom(
                            MiscUtil.castSafely(item, PrismProperty.class)));
        }
    }

    private void applyToAssociationsContainerValue(PrismValue value) throws SchemaException  {
        //noinspection unchecked
        PrismContainerValue<ShadowAssociationsType> pcv = (PrismContainerValue<ShadowAssociationsType>) value;
        for (Item<?, ?> item : pcv.getItems()) {
            ItemDefinition<?> itemDef = item.getDefinition();
            if (itemDef instanceof ShadowAssociationDefinition) {
                continue;
            }
            //noinspection unchecked,rawtypes
            ((Item) item).applyDefinition(
                    definition.findAssociationDefinitionRequired(
                            item.getElementName()));
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void applyToAttributeDelta(
            ItemDelta<V, D> itemDelta) throws SchemaException {
        D itemDef = itemDelta.getDefinition();
        if (!(itemDef instanceof ResourceAttributeDefinition)) {
            QName attributeName = itemDelta.getElementName();
            ResourceAttributeDefinition<?> attributeDefinition =
                    definition.findAttributeDefinitionRequired(attributeName, () -> " in object delta");
            if (itemDef != null) {
                // We are going to rewrite the definition anyway. Let's just do some basic checks first
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

    private void applyToAssociationDelta(ItemDelta<?, ?> itemDelta)
            throws SchemaException {
        if (!ShadowType.F_ASSOCIATIONS.equivalent(itemDelta.getParentPath())) {
            // just to be sure
            return;
        }
        if (!(itemDelta.getDefinition() instanceof ShadowAssociationDefinition)) {
            //noinspection unchecked,rawtypes
            ((ItemDelta) itemDelta).applyDefinition(
                    definition.findAssociationDefinitionRequired(
                            itemDelta.getElementName()));
        }
    }

    /**
     * Just applies the definition to a bean.
     */
    public void applyTo(@NotNull ShadowType bean) throws SchemaException {

        PrismObject<ShadowType> shadowObject = bean.asPrismObject();

        PrismContainer<ShadowAttributesType> attributesContainer = shadowObject.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null) {
            applyAttributesDefinitionToContainer(definition, attributesContainer, shadowObject.getValue(), bean);
        }

        PrismContainer<ShadowAssociationsType> associationsContainer = shadowObject.findContainer(ShadowType.F_ASSOCIATIONS);
        if (associationsContainer != null) {
            applyAssociationsDefinitionToContainer(definition, associationsContainer, shadowObject.getValue(), bean);
        }

        // We also need to replace the entire object definition to inject
        // correct resource object definition here
        // If we don't do this then the patch (delta.applyTo) will not work
        // correctly because it will not be able to
        // create the attribute container if needed.

        PrismObjectDefinition<ShadowType> prismShadowDefinition = shadowObject.getDefinition();
        PrismContainerDefinition<ShadowAttributesType> origAttrContainerDef =
                prismShadowDefinition.findContainerDefinition(ShadowType.F_ATTRIBUTES);
        boolean wrongAttrDef = !(origAttrContainerDef instanceof ResourceAttributeContainerDefinition);
        PrismContainerDefinition<ShadowAssociationsType> origAssocContainerDef =
                prismShadowDefinition.findContainerDefinition(ShadowType.F_ASSOCIATIONS);
        boolean wrongAssocDef = !(origAssocContainerDef instanceof ShadowAssociationsContainerDefinition);

        if (wrongAttrDef || wrongAssocDef) {
            // FIXME eliminate double cloning!
            var clonedDefinition =
                    prismShadowDefinition
                            .cloneWithNewDefinition(
                                    ShadowType.F_ATTRIBUTES, definition.toResourceAttributeContainerDefinition())
                            .cloneWithNewDefinition(
                                    ShadowType.F_ASSOCIATIONS, definition.toShadowAssociationsContainerDefinition());
            shadowObject.setDefinition(clonedDefinition);
            clonedDefinition.freeze();
        }
    }

    /** Applies the correct definitions to objects embedded in association values. Assumes known shadow type. */
    public void applyToAssociationValues(ShadowType shadow) throws SchemaException {
        for (ShadowAssociation association : ShadowUtil.getAssociations(shadow)) {
            for (var associationValue : association.getValues()) {
                var shadowRef = associationValue.asContainerable().getShadowRef();
                ShadowType embeddedShadow = shadowRef != null ? (ShadowType) shadowRef.getObjectable() : null;
                if (embeddedShadow == null) {
                    continue;
                }
                var embeddedShadowDef = definition
                        .findAssociationDefinitionRequired(association.getElementName())
                        .getTargetObjectDefinition();
                // TODO what if we don't have the correct object definition here?!
                new ShadowDefinitionApplicator(embeddedShadowDef)
                        .applyTo(embeddedShadow);
            }
        }
    }

    /**
     * The container can be a shadow or "identifiers" container for an association value.
     * The definition can be the object definition or the association target (entitlement) definition.
     */
    private static void applyAttributesDefinitionToContainer(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull PrismContainer<ShadowAttributesType> attributesContainer,
            @NotNull PrismContainerValue<?> parentPcv,
            Object context) throws SchemaException {
        try {
            if (attributesContainer instanceof ResourceAttributeContainer) {
                // Intentionally forcing the definition. We want to make sure that everything is up to date.
                attributesContainer.applyDefinition(objectDefinition.toResourceAttributeContainerDefinition());
            } else {
                // We need to convert <attributes> to ResourceAttributeContainer
                parentPcv.replace(
                        attributesContainer,
                        ResourceAttributeContainer.convertFromPrismContainer(attributesContainer, objectDefinition));
            }
        } catch (SchemaException e) {
            throw e.wrap("Couldn't apply attributes definitions in " + context);
        }
    }

    private static void applyAssociationsDefinitionToContainer(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull PrismContainer<ShadowAssociationsType> associationsContainer,
            @NotNull PrismContainerValue<?> parentPcv,
            Object context) throws SchemaException {
        try {
            if (associationsContainer instanceof ShadowAssociationsContainer) {
                // Intentionally forcing the definition. We want to make sure that everything is up to date.
                associationsContainer.applyDefinition(objectDefinition.toShadowAssociationsContainerDefinition());
            } else {
                // We need to convert <attributes> to ResourceAttributeContainer
                parentPcv.replace(
                        associationsContainer,
                        ShadowAssociationsContainer.convertFromPrismContainer(associationsContainer, objectDefinition));
            }
        } catch (SchemaException e) {
            throw e.wrap("Couldn't apply associations definitions in " + context);
        }
    }

}
