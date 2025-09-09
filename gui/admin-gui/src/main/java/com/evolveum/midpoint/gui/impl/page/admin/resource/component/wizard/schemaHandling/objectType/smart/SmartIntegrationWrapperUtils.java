/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AttributeMappingValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ItemWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SmartIntegrationWrapperUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationWrapperUtils.class);

    public static @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> createResourceObjectTypeDefinitionWrapper(
            @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> prismContainerValue,
            @NotNull LoadableModel<PrismObjectWrapper<ResourceType>> objectWrapperModel) {
        WebPrismUtil.cleanupEmptyContainerValue(prismContainerValue);
        IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel = createObjectTypeContainerModel(objectWrapperModel);
        prismContainerValue.setParent(containerModel.getObject().getItem());
        return containerModel;
    }

    public static <C extends Containerable> @NotNull IModel<PrismContainerWrapper<C>> createObjectTypeContainerModel(
            @NotNull LoadableModel<PrismObjectWrapper<ResourceType>> objectWrapperModel) {
        ItemPath itemPath = ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE);
        return PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel, itemPath);
    }

    /**
     * Resolves the attribute definition's single inbound mapping, if available.
     * Purpose: smart integration support only one mapping for inbound direction.
     * Returns {@code null} if the complex type definition cannot be resolved.
     */
    public static InboundMappingType getSingleInboundMapping(@Nullable ResourceAttributeDefinitionType attributeDefinition) {
        if (attributeDefinition == null || attributeDefinition.getInbound() == null) {
            return null;
        }
        List<InboundMappingType> inboundMappings = attributeDefinition.getInbound();
        if (!inboundMappings.isEmpty()) {
            return inboundMappings.get(0);
        }
        return null;
    }

    @Contract("_, _ -> new")
    public static <C extends Containerable> @NotNull IModel<PrismContainerWrapper<C>> createContainerModel(
            @NotNull IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> model, @Nullable ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    public static <C extends Containerable> PrismContainerValueWrapper<C> createNewItemContainerValueWrapper(
            @NotNull PageBase pageBase, @NotNull PrismContainerValue<?> newItem,
            @NotNull PrismContainerWrapper<C> model, @NotNull AjaxRequestTarget target) {
        return WebPrismUtil.createNewValueWrapper(model, newItem, pageBase, target);
    }

    public static @NotNull PrismContainerValueWrapper<ItemsSubCorrelatorType> createNewItemsSubCorrelatorValue(
            @NotNull PageBase pageBase,
            @NotNull IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> model,
            @Nullable PrismContainerValue<ItemsSubCorrelatorType> value,
            @NotNull AjaxRequestTarget target) {

        IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> containerModel = createContainerModel(
                model,
                ItemPath.create(
                        ResourceObjectTypeDefinitionType.F_CORRELATION,
                        CorrelationDefinitionType.F_CORRELATORS,
                        CompositeCorrelatorType.F_ITEMS));

        PrismContainerWrapper<ItemsSubCorrelatorType> containerWrapper = containerModel.getObject();
        PrismContainer<ItemsSubCorrelatorType> container = containerWrapper.getItem();

        PrismContainerValue<ItemsSubCorrelatorType> newValue;

        if (value == null) {
            newValue = container.createNewValue();
        } else {
            try {
                value.setParent(container);
                if (!container.getValues().contains(value)) {
                    container.add(value);
                }
            } catch (SchemaException e) {
                throw new RuntimeException("Couldn't add new value to the container.", e);
            }
            newValue = value;
        }

        PrismContainerValueWrapper<ItemsSubCorrelatorType> wrapper =
                createNewItemContainerValueWrapper(pageBase, newValue, containerWrapper, target);

        if (wrapper.getStatus() == null) {
            wrapper.setStatus(ValueStatus.ADDED);
        }

        return wrapper;
    }

    //TODO
    @SuppressWarnings("unchecked")
    public static void createMappingsValueIfRequired(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
            @Nullable List<ResourceAttributeDefinitionType> attributes) {
        if (attributes != null && !attributes.isEmpty()) {
            try {
                PrismContainerWrapper<ResourceAttributeDefinitionType> container =
                        resourceObjectTypeDefinition.getObject()
                                .findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

                for (ResourceAttributeDefinitionType attr : attributes) {
                    PrismContainerValue<ResourceAttributeDefinitionType> pcv =
                            (PrismContainerValue<ResourceAttributeDefinitionType>)
                                    attr.clone().asPrismContainerValue(); // or keep cloneWithoutId()

                    pcv.setId(null);
                    pcv.setParent(container.getItem());
                    WebPrismUtil.cleanupEmptyContainerValue(pcv);

                    WebPrismUtil.createNewValueWrapper(container, pcv, pageBase, target); // will create ADD without ID
                }
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //TODO check it
    public static void transformAndAddMappingIntoCorrelationItemContainer(
            @NotNull PageBase pageBase,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            @NotNull PrismContainerValueWrapper<MappingType> mappingWrapper,
            @NotNull AjaxRequestTarget target) {
        MappingType realValueMapping = mappingWrapper.getRealValue();

        IModel<PrismContainerWrapper<CorrelationItemType>> containerModel = createCorrelationItemContainerModel(valueModel, ItemPath.create(
                ItemsSubCorrelatorType.F_ITEM));

        PrismContainerWrapper<CorrelationItemType> container = containerModel.getObject();

        var newValue = container.getItem().createNewValue();
        CorrelationItemType bean = newValue.asContainerable();
        bean.setName(realValueMapping.getName());
        bean.setRef(realValueMapping.getTarget().getPath());
        bean.setDescription(realValueMapping.getDescription());

        WebPrismUtil.createNewValueWrapper(container, newValue, pageBase, target);
    }

    @Contract("_, _ -> new")
    public static <C extends Containerable> @NotNull IModel<PrismContainerWrapper<C>> createCorrelationItemContainerModel(
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> model, @Nullable ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    public static @NotNull PrismContainerValueWrapper<MappingType> createNewMappingValue(
            @NotNull PageBase pageBase,
            @Nullable PrismContainerValue<MappingType> value,
            @NotNull PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> containerValueWrapper,
            @NotNull AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<ResourceObjectTypeDefinitionType> mappingAttributeContainer =
                    containerValueWrapper.findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

            PrismContainerValue<ResourceObjectTypeDefinitionType> newMapping
                    = mappingAttributeContainer.getItem().createNewValue();

            AttributeMappingValueWrapper<?> newAttributeMappingWrapper =
                    WebPrismUtil.createNewValueWrapper(mappingAttributeContainer, newMapping, pageBase, target);
            newAttributeMappingWrapper.addAttributeMappingType(MappingDirection.INBOUND);

            PrismContainerWrapper<MappingType> wrapper =
                    newAttributeMappingWrapper.findContainer(MappingDirection.INBOUND.getContainerName());
            PrismContainerValueWrapper<MappingType> newValueWrapper;
            if (!wrapper.getValues().isEmpty()) {
                if (value == null) {
                    newValueWrapper = wrapper.getValue();
                } else {
                    wrapper.getValues().clear();
                    newValueWrapper = WebPrismUtil.createNewValueWrapper(wrapper, value, pageBase, target);
                }
            } else {
                PrismContainerValue<MappingType> newValue = value;
                if (newValue == null) {
                    newValue = wrapper.getItem().createNewValue();
                }
                newValueWrapper = WebPrismUtil.createNewValueWrapper(wrapper, newValue, pageBase, target);
            }

            newValueWrapper.findProperty(MappingType.F_STRENGTH).getValue().setRealValue(MappingStrengthType.STRONG);

            PrismPropertyDefinition<Object> propertyDef = containerValueWrapper.getDefinition().findPropertyDefinition(
                    ItemPath.create(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF));

            createVirtualItemInMapping(pageBase, newValueWrapper, null, propertyDef);

            return newValueWrapper;

        } catch (SchemaException e) {
            LOGGER.error("Couldn't create new attribute mapping");
            throw new IllegalStateException("Couldn't create new attribute mapping", e);
        }
    }

    protected static void createVirtualItemInMapping(
            @NotNull PageBase pageBase,
            @NotNull PrismContainerValueWrapper<?> mapping,
            @Nullable PrismContainerValueWrapper<ResourceAttributeDefinitionType> value,
            @NotNull PrismPropertyDefinition<Object> propertyRefDef) {
        try {
            if (mapping.findProperty(ItemRefinedDefinitionType.F_REF) == null) {

                Task task = pageBase.createSimpleTask("Create virtual item");
                OperationResult result = task.getResult();

                @NotNull PrismProperty<Object> refValue = propertyRefDef.instantiate();

                ItemWrapper<?, ?> refItemWrapper = pageBase.createItemWrapper(
                        refValue,
                        mapping,
                        ItemStatus.ADDED,
                        new WrapperContext(task, result));

                ((ItemWrapperImpl<?, ?>) refItemWrapper).setDisplayName(
                        pageBase.getString(MappingDirection.INBOUND.name() + "." + ItemRefinedDefinitionType.F_REF));
                ((ItemWrapperImpl<?, ?>) refItemWrapper).setDisplayOrder(1);

                if (value != null && value.getRealValue() != null && value.getRealValue().getRef() != null) {
                    //noinspection unchecked
                    refItemWrapper.getValue().setRealValue(value.getRealValue().getRef().clone());
                }

                refItemWrapper.setVisibleOverwrite(UserInterfaceElementVisibilityType.HIDDEN);
                mapping.addItem(refItemWrapper);
                mapping.getNonContainers().clear();
            }
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't create virtual item in mapping.", e);
        }
    }

    public static <C extends MappingType> @Nullable PrismContainerValueWrapper<C> findRelatedInboundMapping(
            @NotNull PageBase pageBase,
            @NotNull PrismContainerValueWrapper<CorrelationItemType> correlationItemWrapper) {
        ItemPathType correlationItemRef = correlationItemWrapper.getRealValue().getRef();
        List<PrismContainerValueWrapper<C>> allInboundMappings = new ArrayList<>();

        try {
            PrismContainerValueWrapper<CorrelationSuggestionType> suggestionWrapper = correlationItemWrapper
                    .getParentContainerValue(CorrelationSuggestionType.class);

            PrismContainerWrapper<ResourceAttributeDefinitionType> container;
            if (suggestionWrapper != null) {
                container = suggestionWrapper.findContainer(CorrelationSuggestionType.F_ATTRIBUTES);
            } else {
                container = correlationItemWrapper
                        .getParentContainerValue(ResourceObjectTypeDefinitionType.class)
                        .findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);
            }

            if (container == null) {
                LOGGER.warn("Couldn't find related resource attribute definition.");
                return null;
            }

            for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> value : container.getValues()) {
                ResourceAttributeDefinitionType realValue = value.getRealValue();
                List<InboundMappingType> inbound = realValue.getInbound();
                if (inbound != null && !inbound.isEmpty()) {
                    PrismContainerWrapper<C> inboundContainer = value.findContainer(ResourceAttributeDefinitionType.F_INBOUND);
                    allInboundMappings.addAll(inboundContainer.getValues());
                }
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find related resource attribute definition.", e);
            return null;
        }

        return allInboundMappings.stream()
                .filter(inboundMapping -> {
                    VariableBindingDefinitionType target = inboundMapping.getRealValue().getTarget();
                    PrismContainerValueWrapper<ResourceAttributeDefinitionType> parentContainerValue = inboundMapping
                            .getParentContainerValue(ResourceAttributeDefinitionType.class);

                    PrismPropertyDefinition<Object> propertyRefDef = parentContainerValue.getDefinition()
                            .findPropertyDefinition(ResourceAttributeDefinitionType.F_REF);
                    createVirtualItemInMapping(pageBase, inboundMapping, parentContainerValue, propertyRefDef);

                    return correlationItemRef.equals(target.getPath());
                })
                .findFirst()
                .orElse(null);
    }

    public static void discardDraftMapping(
            @NotNull PageBase pageBase,
            @NotNull PrismContainerValueWrapper<MappingType> draftMappingVw) {
        try {
            @SuppressWarnings("unchecked")
            PrismContainerWrapper<MappingType> mappingsCw =
                    (PrismContainerWrapper<MappingType>) draftMappingVw.getParent();
            if (mappingsCw != null) {
                mappingsCw.remove(draftMappingVw, pageBase);
            }

            PrismContainerValueWrapper<ResourceAttributeDefinitionType> attrCvw =
                    draftMappingVw.getParentContainerValue(ResourceAttributeDefinitionType.class);

            if (attrCvw != null && ValueStatus.ADDED.equals(attrCvw.getStatus())) {
                @SuppressWarnings("unchecked")
                PrismContainerWrapper<ResourceAttributeDefinitionType> attrCw =
                        (PrismContainerWrapper<ResourceAttributeDefinitionType>) attrCvw.getParent();
                if (attrCw != null) {
                    attrCw.remove(attrCvw, pageBase);
                }
            }

        } catch (SchemaException e) {
            LOGGER.warn("Failed to discard draft correlation mapping on cancel.", e);
        }
    }
}
