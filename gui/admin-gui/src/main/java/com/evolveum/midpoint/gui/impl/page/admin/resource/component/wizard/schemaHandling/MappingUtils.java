/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AttributeMappingValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ItemWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Utility methods for working with mapping wrappers in the resource schema handling wizard.
 * <p>
 * Provides helpers to:
 * <ul>
 *   <li>Create new virtual {@link MappingType} values and their UI wrappers.</li>
 *   <li>Assemble synthetic containers of mappings for table components.</li>
 *   <li>Insert hidden virtual <code>ref</code> properties to support UI editing.</li>
 *   <li>Clone and initialize attribute definitions with inbound mappings if required.</li>
 *   <li>Extract <code>ref</code> values from various attribute container types.</li>
 * </ul>
 *
 * These utilities are intended to keep the UI model in sync with the underlying
 * Prism structures while providing a consistent user experience in forms and tables.
 */
public class MappingUtils {

    private static final Trace LOGGER = TraceManager.getTrace(MappingUtils.class);

    /**
     * Creates a new virtual {@link MappingType} value for the given container model.
     * <p>
     * A new attribute mapping container value is created, wrapped for UI,
     * marked with the specified {@link MappingDirection}, and initialized with
     * a strong mapping strength. A hidden virtual <code>ref</code> property is
     * also created so that the UI can properly display the mapping.
     *
     * @param value optional mapping value to reuse, otherwise a new one is created
     * @param valueModel model of the parent container value wrapper
     * @param mappingDirection direction of the mapping (e.g. inbound/outbound)
     * @param itemNameOfContainerWithMappings item name of the container holding mappings
     * @param itemNameOfRefAttribute item name of the reference attribute
     * @param pageBase current page context
     * @param target Ajax target for UI updates
     * @return created mapping value wrapper, or {@code null} if creation failed
     */
    public static <P extends Containerable, AD extends Containerable>
    @Nullable PrismContainerValueWrapper<MappingType> createNewVirtualMappingValue(
            @Nullable PrismContainerValue<MappingType> value,
            @NotNull IModel<PrismContainerValueWrapper<P>> valueModel,
            @NotNull MappingDirection mappingDirection,
            @NotNull ItemName itemNameOfContainerWithMappings,
            @NotNull ItemName itemNameOfRefAttribute,
            @NotNull PageBase pageBase,
            AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<AD> mappingAttrCw =
                    valueModel.getObject().findContainer(itemNameOfContainerWithMappings);
            PrismContainerValue<AD> newAttrPcv = mappingAttrCw.getItem().createNewValue();

            AttributeMappingValueWrapper<?> attrVw =
                    WebPrismUtil.createNewValueWrapper(mappingAttrCw, newAttrPcv, pageBase, target);
            attrVw.addAttributeMappingType(mappingDirection);

            PrismContainerWrapper<MappingType> mappingsCw =
                    attrVw.findContainer(getPathBaseOnMappingType(mappingDirection));

            PrismContainerValue<MappingType> mappingPcv;
            if (mappingsCw.getValues().isEmpty()) {
                if (value == null) {
                    mappingPcv = mappingsCw.getItem().createNewValue();
                } else {
                    mappingPcv = value.clone();
                    mappingPcv.setId(null);
                    mappingPcv.setParent(mappingsCw.getItem());
                    WebPrismUtil.cleanupEmptyContainerValue(mappingPcv);
                    if (!mappingsCw.getItem().contains(mappingPcv)) {
                        mappingsCw.getItem().add(mappingPcv);
                    }
                }
                PrismContainerValueWrapper<MappingType> mappingVw =
                        WebPrismUtil.createNewValueWrapper(mappingsCw, mappingPcv, pageBase, target);

                PrismContainerValueWrapper<P> root = valueModel.getObject();

                PrismPropertyDefinition<Object> refDef = root.getDefinition()
                        .findPropertyDefinition(ItemPath.create(itemNameOfContainerWithMappings, itemNameOfRefAttribute));

                // value parent can hold ref definition too (e.g. in case of suggested mapping)
                if (value != null
                        && value.getParentContainerValue() != null
                        && value.getParentContainerValue().getRealValue() instanceof ResourceAttributeDefinitionType def) {
                    ItemPathType ref = def.getRef();
                    createDirectRefVirtualItemInMapping(mappingVw, ref, refDef, pageBase, itemNameOfRefAttribute, mappingDirection);
                } else {
                    createVirtualItemInMapping(mappingVw, null, refDef, pageBase, itemNameOfRefAttribute, mappingDirection);
                }

                return mappingVw;

            } else {
                PrismContainerValueWrapper<MappingType> mappingVw;
                if (value == null) {
                    mappingVw = mappingsCw.getValue();
                } else {
                    mappingsCw.getValues().clear();
                    mappingPcv = value.clone();
                    mappingPcv.setId(null);
                    mappingPcv.setParent(mappingsCw.getItem());
                    WebPrismUtil.cleanupEmptyContainerValue(mappingPcv);
                    mappingsCw.getItem().add(mappingPcv);
                    mappingVw = WebPrismUtil.createNewValueWrapper(mappingsCw, mappingPcv, pageBase, target);
                }

                mappingVw.findProperty(MappingType.F_STRENGTH)
                        .getValue().setRealValue(MappingStrengthType.STRONG);

                PrismContainerValueWrapper<P> root = valueModel.getObject();
                PrismPropertyDefinition<Object> refDef = root.getDefinition()
                        .findPropertyDefinition(ItemPath.create(itemNameOfContainerWithMappings, itemNameOfRefAttribute));
                createVirtualItemInMapping(mappingVw, null, refDef, pageBase, itemNameOfRefAttribute, mappingDirection);

                return mappingVw;
            }

        } catch (SchemaException e) {
            LOGGER.error("Couldn't create new attribute mapping", e);
            return null;
        }
    }

    /**
     * Builds a virtual container model for {@link MappingType} values.
     * <p>
     * The returned model collects all mappings of the specified direction
     * from the given parent value, ensures each one has a hidden virtual
     * <code>ref</code> property, and presents them as a synthetic container
     * suitable for use in tables or forms.
     *
     * @param pageBase current page context
     * @param valueModel model of the parent container value wrapper
     * @param itemNameOfContainerWithMappings item name of the container holding mappings
     * @param itemNameOfRefAttribute item name of the reference attribute
     * @param mappingDirection direction of mappings to collect
     * @return model of a virtual mapping container
     */
    public static <P extends Containerable, AD extends Containerable>
    @NotNull IModel<PrismContainerWrapper<MappingType>> createVirtualMappingContainerModel(
            @NotNull PageBase pageBase,
            @NotNull IModel<PrismContainerValueWrapper<P>> valueModel,
            @NotNull ItemName itemNameOfContainerWithMappings,
            @NotNull ItemName itemNameOfRefAttribute,
            @NotNull MappingDirection mappingDirection) {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerWrapper<MappingType> load() {
                PrismContainerValueWrapper<P> container = valueModel.getObject();
                ItemDefinition<?> def = container.getDefinition().findContainerDefinition(
                        ItemPath.create(itemNameOfContainerWithMappings, getPathBaseOnMappingType(mappingDirection)));
                try {
                    Task task = pageBase.createSimpleTask("Create virtual item");
                    OperationResult result = task.getResult();
                    PrismContainerWrapper<MappingType> virtualMappingContainer =
                            pageBase.createItemWrapper(def.instantiate(), ItemStatus.ADDED, new WrapperContext(task, result));
                    virtualMappingContainer.getValues().clear();

                    PrismContainerWrapper<AD> mappingAttributeContainer =
                            valueModel.getObject().findContainer(itemNameOfContainerWithMappings);

                    PrismPropertyDefinition<Object> propertyDef = container.getDefinition().findPropertyDefinition(
                            ItemPath.create(itemNameOfContainerWithMappings, itemNameOfRefAttribute));

                    for (PrismContainerValueWrapper<AD> value : mappingAttributeContainer.getValues()) {
                        PrismContainerWrapper<MappingType> mappingContainer = value.findContainer(
                                getPathBaseOnMappingType(mappingDirection));

                        for (PrismContainerValueWrapper<MappingType> mapping : mappingContainer.getValues()) {
                            if (mapping.getParent() != null
                                    && mapping.getParent().getParent() != null
                                    && mapping.getParent().getParent() instanceof AttributeMappingValueWrapper
                                    && ((AttributeMappingValueWrapper<?>) mapping.getParent().getParent())
                                    .getAttributeMappingTypes().contains(mappingDirection)) {

                                createVirtualItemInMapping(mapping, value, propertyDef, pageBase,
                                        itemNameOfRefAttribute, mappingDirection);

                                virtualMappingContainer.getValues().add(mapping);
                            }
                        }
                    }
                    return virtualMappingContainer;

                } catch (SchemaException e) {
                    LOGGER.error("Couldn't instantiate virtual container for mappings", e);
                }
                return null;
            }
        };
    }

    /**
     * Creates a virtual <code>ref</code> property inside the given mapping,
     * if it does not already exist.
     * <p>
     * The virtual item is initialized from the provided attribute value (if present),
     * wrapped for UI, hidden from the user, and inserted into the mapping container
     * to ensure consistent editing in forms and tables.
     *
     * @param mapping mapping container value wrapper where the virtual item is created
     * @param value optional parent attribute value to initialize the ref from
     * @param propertyDef definition of the reference property
     * @param pageBase current page context
     * @param itemNameOfRefAttribute item name of the reference attribute
     * @param mappingDirection mapping direction (used for display name and context)
     */
    public static <AD extends Containerable> void createVirtualItemInMapping(
            @NotNull PrismContainerValueWrapper<MappingType> mapping,
            @Nullable PrismContainerValueWrapper<AD> value,
            @NotNull PrismPropertyDefinition<Object> propertyDef,
            @NotNull PageBase pageBase,
            @NotNull ItemName itemNameOfRefAttribute,
            @NotNull MappingDirection mappingDirection) {

        try {
            if (mapping.findProperty(itemNameOfRefAttribute) == null) {

                Task task = pageBase.createSimpleTask("Create virtual item");
                OperationResult result = task.getResult();

                ItemPathType attributeRefAttributeValue = getAttributeRefAttributeValue(value);
                @NotNull PrismProperty<Object> refvalue = propertyDef.instantiate();
                if (value != null && !ValueStatus.ADDED.equals(value.getStatus())) {
                    refvalue.addRealValue(attributeRefAttributeValue);
                }

                ItemWrapper<?, ?> refItemWrapper = pageBase.createItemWrapper(
                        refvalue,
                        mapping,
                        ItemStatus.ADDED,
                        new WrapperContext(task, result));

                ((ItemWrapperImpl<?, ?>) refItemWrapper).setDisplayName(
                        pageBase.getString(mappingDirection.name() + "." + itemNameOfRefAttribute));
                ((ItemWrapperImpl<?, ?>) refItemWrapper).setDisplayOrder(1);

                if (value != null && value.getRealValue() != null && attributeRefAttributeValue != null) {
                    //noinspection unchecked
                    refItemWrapper.getValue().setRealValue(attributeRefAttributeValue.clone());
                }

                refItemWrapper.setVisibleOverwrite(UserInterfaceElementVisibilityType.HIDDEN);
                mapping.addItem(refItemWrapper);
                mapping.getNonContainers().clear();
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create virtual item in mapping", e);
            throw new IllegalStateException("Couldn't create virtual item in mapping", e);
        }
    }

    public static void createDirectRefVirtualItemInMapping(
            @NotNull PrismContainerValueWrapper<MappingType> mapping,
            @Nullable ItemPathType attributeRefAttributeValue,
            @NotNull PrismPropertyDefinition<Object> propertyDef,
            @NotNull PageBase pageBase,
            @NotNull ItemName itemNameOfRefAttribute,
            @NotNull MappingDirection mappingDirection) {

        try {
            if (mapping.findProperty(itemNameOfRefAttribute) == null) {

                Task task = pageBase.createSimpleTask("Create virtual item");
                OperationResult result = task.getResult();

                @NotNull PrismProperty<Object> refvalue = propertyDef.instantiate();
                if (attributeRefAttributeValue != null) {
                    refvalue.addRealValue(attributeRefAttributeValue);
                }

                ItemWrapper<?, ?> refItemWrapper = pageBase.createItemWrapper(
                        refvalue,
                        mapping,
                        ItemStatus.ADDED,
                        new WrapperContext(task, result));

                ((ItemWrapperImpl<?, ?>) refItemWrapper).setDisplayName(
                        pageBase.getString(mappingDirection.name() + "." + itemNameOfRefAttribute));
                ((ItemWrapperImpl<?, ?>) refItemWrapper).setDisplayOrder(1);

                if (attributeRefAttributeValue != null) {
                    //noinspection unchecked
                    refItemWrapper.getValue().setRealValue(attributeRefAttributeValue.clone());
                }

                refItemWrapper.setVisibleOverwrite(UserInterfaceElementVisibilityType.HIDDEN);
                mapping.addItem(refItemWrapper);
                mapping.getNonContainers().clear();
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create virtual item in mapping", e);
            throw new IllegalStateException("Couldn't create virtual item in mapping", e);
        }
    }

    /**
     * Extracts the <code>ref</code> value from the given attribute container wrapper.
     * <p>
     * Supports multiple container types (e.g. {@link ResourceAttributeDefinitionType},
     * {@link AttributeInboundMappingsDefinitionType}, {@link AttributeOutboundMappingsDefinitionType},
     * {@link ShadowAssociationDefinitionType}). Returns {@code null} if the value is
     * missing or not of a supported type.
     *
     * @param value wrapper of an attribute container
     * @param <AD> concrete attribute container type
     * @return extracted {@link ItemPathType} reference, or {@code null} if unavailable
     */
    public static <AD extends Containerable> @Nullable ItemPathType
    getAttributeRefAttributeValue(@Nullable PrismContainerValueWrapper<AD> value) {
        if (value == null || value.getRealValue() == null) {
            return null;
        }

        AD realValue = value.getRealValue();

        if (realValue instanceof ResourceAttributeDefinitionType resourceAttributeDef) {
            return resourceAttributeDef.getRef();
        }

        if (realValue instanceof AttributeInboundMappingsDefinitionType inboundMappingsDef) {
            return inboundMappingsDef.getRef();
        }

        if (realValue instanceof AttributeOutboundMappingsDefinitionType outboundMappingsDef) {
            return outboundMappingsDef.getRef();
        }

        if (realValue instanceof ShadowAssociationDefinitionType associationDef) {
            return associationDef.getRef();
        }

        return null;
    }

    /**
     * Resolves the container item name corresponding to the given mapping direction.
     *
     * @param mappingDirection direction of the mapping (may be {@code null})
     * @return item name of the container for this direction, or {@code null} if direction is not provided
     */
    private static ItemName getPathBaseOnMappingType(MappingDirection mappingDirection) {
        if (mappingDirection != null) {
            return mappingDirection.getContainerName();
        }
        return null;
    }

    /**
     * Ensures that inbound mapping values exist for the given candidate attributes.
     * <p>
     * Each provided {@link ResourceAttributeDefinitionType} is cloned, added into the
     * attribute container of the given resource object type definition, wrapped for UI,
     * and initialized with a hidden virtual <code>ref</code> property. The visible ref
     * is cleared so the user can edit it in the form.
     *
     * @param pageBase current page context
     * @param resourceObjectTypeDefinition model of the resource object type definition wrapper
     * @param candidateAttributes attributes to be added (may-be {@code null} or empty)
     * @param assignmentHolderDetailsModel context used when creating wrappers
     */
    @SuppressWarnings("unchecked")
    public static void createMappingsValueIfRequired(
            @NotNull PageBase pageBase,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
            @Nullable List<ResourceAttributeDefinitionType> candidateAttributes,
            @NotNull ResourceDetailsModel assignmentHolderDetailsModel) {
        if (candidateAttributes == null || candidateAttributes.isEmpty()) {
            return;
        }
        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> container =
                    resourceObjectTypeDefinition.getObject()
                            .findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

            for (ResourceAttributeDefinitionType newAttr : candidateAttributes) {
                addNewMappingValue(pageBase, resourceObjectTypeDefinition, assignmentHolderDetailsModel, newAttr);
            }
        } catch (SchemaException e) {
            throw new RuntimeException("Couldn't create mapping value", e);
        }
    }

    public static <P extends Containerable> void addNewMappingValue(
            @NotNull PageBase pageBase,
            @NotNull IModel<PrismContainerValueWrapper<P>> resourceObjectTypeDefinition,
            @NotNull ResourceDetailsModel assignmentHolderDetailsModel,
            @NotNull ResourceAttributeDefinitionType newAttribute) throws SchemaException {

        PrismContainerWrapper<ResourceAttributeDefinitionType> container =
                resourceObjectTypeDefinition.getObject()
                        .findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

        @SuppressWarnings("unchecked") PrismContainerValue<ResourceAttributeDefinitionType> pcv =
                (PrismContainerValue<ResourceAttributeDefinitionType>)
                        newAttribute.clone().asPrismContainerValue();

        pcv.setId(null);
        pcv.setParent(container.getItem());
        WebPrismUtil.cleanupEmptyContainerValue(pcv);

        container.getItem().add(pcv);
        WrapperContext context = assignmentHolderDetailsModel.createWrapperContext();
        context.setAttributeMappingType(MappingDirection.INBOUND);

        PrismPropertyDefinition<Object> propertyDef = container.findPropertyDefinition(
                ItemPath.create(ResourceAttributeDefinitionType.F_REF));

        PrismContainerValueWrapper<ResourceAttributeDefinitionType> newValueWrapper = WebPrismUtil
                .createNewValueWrapper(container, pcv, pageBase, context);

        PrismContainerWrapper<MappingType> mappings = newValueWrapper.findContainer(
                ResourceAttributeDefinitionType.F_INBOUND);

        List<PrismContainerValueWrapper<MappingType>> values = mappings.getValues();
        for (PrismContainerValueWrapper<MappingType> value : values) {
            MappingUtils.createVirtualItemInMapping(value, newValueWrapper, propertyDef, pageBase,
                    ResourceAttributeDefinitionType.F_REF, MappingDirection.INBOUND);
        }

        container.getValues().add(newValueWrapper);
        newValueWrapper.findProperty(ResourceAttributeDefinitionType.F_REF)
                .getValue().setRealValue(null);
        pcv.findProperty(ResourceAttributeDefinitionType.F_REF).getValue().setValue(null);
        pcv.getValue().setRef(null);
    }

}
