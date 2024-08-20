/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.processor.ShadowAttributesContainer.createEmptyContainer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Applies attributes and/or associations definitions to a shadow, delta, or query.
 *
 * It is instantiated for a specific {@link ResourceObjectDefinition} (see {@link #definition}).
 *
 * Can be used for model-level and resource-level shadows, but *not for repository-level ones*.
 *
 * Generally, we don't replace definitions if they are of {@link ShadowItemDefinition} type;
 * see {@link #requiresAttributeDefinitionApplication(ItemDefinition)} and
 * {@link #requiresAssociationDefinitionApplication(ItemDefinition)}.
 *
 * NOTE: Originally, this functionality was in `ShadowCaretaker` in the `provisioning-impl` module.
 * In the future, maybe it should be part of standard `applyDefinition` mechanism. (But, we need some flexibility
 * about lax mode and error handling.)
 *
 * @author Radovan Semancik
 */
public class ShadowDefinitionApplicator {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDefinitionApplicator.class);

    /** The definition to be applied to a shadow, delta, or query. */
    @NotNull private final ResourceObjectDefinition definition;

    /**
     * If {@code true}, less serious attribute-level errors are logged as errors; otherwise, exceptions are thrown.
     * This is to be used when reading raw shadows from the repository, as the schema could be changed in the meanwhile.
     */
    private final boolean lax;

    private ShadowDefinitionApplicator(
            @NotNull ResourceObjectDefinition definition, boolean lax) {
        this.definition = definition;
        this.lax = lax;
    }

    public static ShadowDefinitionApplicator create(@NotNull ResourceObjectDefinition definition, boolean lax) {
        return new ShadowDefinitionApplicator(definition, lax);
    }

    public static ShadowDefinitionApplicator strict(@NotNull ResourceObjectDefinition definition) {
        return new ShadowDefinitionApplicator(definition, false);
    }

    public static ShadowDefinitionApplicator lax(@NotNull ResourceObjectDefinition definition) {
        return new ShadowDefinitionApplicator(definition, true);
    }

    public void applyToDelta(@NotNull ObjectDelta<ShadowType> delta) throws SchemaException {
        if (delta.isAdd()) {
            applyToShadow(delta.getObjectableToAdd());
        } else if (delta.isModify()) {
            applyToItemDeltas(delta.getModifications());
        }
    }

    public void applyToItemDeltas(@NotNull Collection<? extends ItemDelta<?, ?>> itemDeltas) throws SchemaException {
        for (ItemDelta<?, ?> itemDelta : itemDeltas) {
            if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
                applyToAttributeDelta(itemDelta);
            } else if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getPath())) {
                for (PrismValue value : itemDelta.getNewValues()) {
                    applyToAttributesContainerValue(value);
                }
            } else if (ShadowType.F_ASSOCIATIONS.equivalent(itemDelta.getParentPath())) {
                applyToAssociationDelta(itemDelta);
            } else if (ShadowType.F_ASSOCIATIONS.equivalent(itemDelta.getPath())) {
                for (PrismValue value : itemDelta.getNewValues()) {
                    applyToAssociationsContainerValue(value);
                }
            }
        }
    }

    private void applyToAttributesContainerValue(PrismValue value) throws SchemaException  {
        var pcv = (PrismContainerValue<?>) value;
        for (Item<?, ?> item : List.copyOf(pcv.getItems())) {
            if (requiresAttributeDefinitionApplication(item)) {
                // We do remove & add, because it is usually not sufficient to apply the definition to item in place,
                // as we probably need to change the object identity of the item (e.g. from PrismProperty to a
                // ShadowSimpleAttribute).
                var newItem = (Item<?, ?>) applyToItem(item);
                pcv.remove(item);
                pcv.add(newItem);
            }
        }
    }

    /** Unlike other "applyTo..." methods, this one returns the "new" item. */
    @NotNull ShadowAttribute<?, ?, ?, ?> applyToItem(Item<?, ?> item) throws SchemaException {
        return definition
                .findAttributeDefinitionRequired(item.getElementName())
                .instantiateFrom(item);
    }

    private void applyToAssociationsContainerValue(PrismValue value) throws SchemaException  {
        //noinspection unchecked
        var pcv = (PrismContainerValue<ShadowAssociationsType>) value;
        for (Item<?, ?> item : pcv.getItems()) {
            if (requiresAssociationDefinitionApplication(item)) {
                //noinspection unchecked,rawtypes
                ((Item) item).applyDefinition(
                        definition.findAssociationDefinitionRequired(item.getElementName()));
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void applyToAttributeDelta(ItemDelta<V, D> itemDelta)
            throws SchemaException {
        if (requiresAttributeDefinitionApplication(itemDelta)) {
            //noinspection unchecked
            itemDelta.applyDefinition(
                    (D) definition.findAttributeDefinitionRequired(itemDelta.getElementName(), " in object delta"));
        }
    }

    private void applyToAssociationDelta(ItemDelta<?, ?> itemDelta) throws SchemaException {
        assert ShadowType.F_ASSOCIATIONS.equivalent(itemDelta.getParentPath());
        if (requiresAssociationDefinitionApplication(itemDelta)) {
            //noinspection unchecked,rawtypes
            ((ItemDelta) itemDelta).applyDefinition(
                    definition.findAssociationDefinitionRequired(itemDelta.getElementName(), " in object delta"));
        }
    }

    /** Applies the definition to the model-level or resource-level shadow. */
    public void applyToShadow(@NotNull ShadowType bean) throws SchemaException {

        PrismObject<ShadowType> shadowObject = bean.asPrismObject();

        var attributesContainer = shadowObject.<ShadowAttributesType>findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null) {
            applyAttributesDefinitionToContainer(definition, attributesContainer, shadowObject.getValue(), lax, bean);
        }

        var associationsContainer = shadowObject.<ShadowAssociationsType>findContainer(ShadowType.F_ASSOCIATIONS);
        if (associationsContainer != null) {
            applyAssociationsDefinitionToContainer(definition, associationsContainer, shadowObject.getValue(), bean);
        }

        // We also need to replace the entire object definition to inject correct resource object definition here.
        // If we don't do this then the patch (delta.applyTo) will not work correctly because it will not be able to
        // create the attribute container if needed.

        PrismObjectDefinition<ShadowType> prismShadowDefinition = shadowObject.getDefinition();
        var origAttrContDef = prismShadowDefinition.<ShadowAttributesType>findContainerDefinition(ShadowType.F_ATTRIBUTES);
        boolean wrongAttrContDef = !(origAttrContDef instanceof ShadowAttributesContainerDefinition);
        var origAssocContDef = prismShadowDefinition.<ShadowAssociationsType>findContainerDefinition(ShadowType.F_ASSOCIATIONS);
        boolean wrongAssocContDef = !(origAssocContDef instanceof ShadowAssociationsContainerDefinition);

        if (wrongAttrContDef || wrongAssocContDef) {
            // FIXME eliminate double cloning!
            var clonedDefinition =
                    prismShadowDefinition
                            .cloneWithNewDefinition(
                                    ShadowType.F_ATTRIBUTES, definition.toShadowAttributesContainerDefinition())
                            .cloneWithNewDefinition(
                                    ShadowType.F_ASSOCIATIONS, definition.toShadowAssociationsContainerDefinition());
            shadowObject.setDefinition(clonedDefinition);
            clonedDefinition.freeze();
        }
    }

    /**
     * Applies the correct definitions to objects embedded in association values. Assumes known shadow type.
     *
     * TEMPORARY. Reconsider this method.
     */
    public void applyToAssociationValues(ShadowType shadow) throws SchemaException {
//        for (var association : ShadowUtil.getAssociations(shadow)) {
//            for (var associationValue : association.getValues()) {
//                var shadowRef = associationValue.asContainerable().getShadowRef();
//                ShadowType embeddedShadow = shadowRef != null ? (ShadowType) shadowRef.getObjectable() : null;
//                if (embeddedShadow == null) {
//                    continue;
//                }
//                var embeddedShadowDef = definition
//                        .findAssociationDefinitionRequired(association.getElementName())
//                        .getRepresentativeTargetObjectDefinition();
//                // TODO what if we don't have the correct object definition here?!
//                new ShadowDefinitionApplicator(embeddedShadowDef)
//                        .applyToShadow(embeddedShadow);
//            }
//        }
    }

    /**
     * The container can be a shadow or "identifiers" container for an association value.
     * The definition can be the object definition or the association target (entitlement) definition.
     */
    private static void applyAttributesDefinitionToContainer(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull PrismContainer<ShadowAttributesType> attributesContainer,
            @NotNull PrismContainerValue<?> parentPcv,
            boolean lax,
            Object context) throws SchemaException {
        try {
            if (attributesContainer instanceof ShadowAttributesContainer) {
                // Intentionally forcing the definition. We want to make sure that everything is up to date.
                attributesContainer.applyDefinition(objectDefinition.toShadowAttributesContainerDefinition());
            } else {
                // We need to convert <attributes> to ResourceAttributeContainer
                parentPcv.replace(
                        attributesContainer,
                        convertFromPrismContainer(
                                attributesContainer, objectDefinition, lax, context));
            }
        } catch (SchemaException e) {
            throw e.wrap("Couldn't apply attributes definitions in " + context);
        }
    }

    /** Converts raw {@link PrismContainer} to {@link ShadowAttributesContainer}. */
    private static ShadowAttributesContainer convertFromPrismContainer(
            @NotNull PrismContainer<?> prismContainer,
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            boolean lax,
            Object context) throws SchemaException {
        var attributesContainer = createEmptyContainer(prismContainer.getElementName(), resourceObjectDefinition);
        for (var item : prismContainer.getValue().getItems()) {
            var itemName = item.getElementName();
            try {
                if (item instanceof PrismProperty<?> property) {
                    attributesContainer.add(
                            resourceObjectDefinition
                                    .findAttributeDefinitionRequired(itemName)
                                    .instantiateFrom(property));
                } else {
                    throw new SchemaException(
                            "Cannot use item of type %s as an attribute: attributes can only be properties".formatted(
                                    item.getClass().getSimpleName()));
                }
            } catch (SchemaException e) {
                if (lax) {
                    LoggingUtils.logException(LOGGER, "Couldn't convert attribute {} in {}", e, item.getElementName(), context);
                } else {
                    throw new SchemaException(
                            "Couldn't convert attribute %s in %s: %s".formatted(item.getElementName(), context, e.getMessage()),
                            e);
                }
            }
        }
        return attributesContainer;
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

    private boolean requiresAttributeDefinitionApplication(ItemDefinition<?> currentDefinition) {
        return !(currentDefinition instanceof ShadowAttributeDefinition);
    }

    private boolean requiresAssociationDefinitionApplication(ItemDefinition<?> currentDefinition) {
        return !(currentDefinition instanceof ShadowAssociationDefinition);
    }

    private boolean requiresAttributeDefinitionApplication(Item<?, ?> item) {
        // This assures that the definition is correct and the values are of specialized type (ShadowReferenceAttributeValue).
        return !(item instanceof ShadowAttribute);
    }

    private boolean requiresAssociationDefinitionApplication(Item<?, ?> item) {
        // This assures that the definition is correct and the values are of specialized type (ShadowAssociationValue).
        return !(item instanceof ShadowAssociation);
    }

    private boolean requiresAttributeDefinitionApplication(ItemDelta<?, ?> itemDelta) {
        var currentDefinition = itemDelta.getDefinition();
        if (requiresAttributeDefinitionApplication(currentDefinition)) {
            return true;
        }
        if (currentDefinition instanceof ShadowReferenceAttributeDefinition) {
            // TODO implement in a nicer way
            AtomicBoolean containsRaw = new AtomicBoolean(false);
            itemDelta.foreach(val -> {
                if (!(val instanceof ShadowReferenceAttributeValue)) {
                    containsRaw.set(true);
                }
            });
            return containsRaw.get();
        } else {
            return false;
        }
    }

    private boolean requiresAssociationDefinitionApplication(ItemDelta<?, ?> itemDelta) {
        if (requiresAssociationDefinitionApplication(itemDelta.getDefinition())) {
            return true;
        }
        // TODO implement in a nicer way
        AtomicBoolean containsRaw = new AtomicBoolean(false);
        itemDelta.foreach(val -> {
            if (!(val instanceof ShadowAssociationValue)) {
                containsRaw.set(true);
            }
        });
        return containsRaw.get();
    }
}
