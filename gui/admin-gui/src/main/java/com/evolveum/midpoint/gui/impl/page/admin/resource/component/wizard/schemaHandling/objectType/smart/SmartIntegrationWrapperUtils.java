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
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
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
    public static InboundMappingType getSingleInboundMapping(ResourceAttributeDefinitionType attributeDefinition) {
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
            IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> model, ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    public static <C extends Containerable> PrismContainerValueWrapper<C> createNewItemContainerValueWrapper(
            PageBase pageBase, PrismContainerValue<?> newItem,
            PrismContainerWrapper<C> model, AjaxRequestTarget target) {
        return WebPrismUtil.createNewValueWrapper(model, newItem, pageBase, target);
    }

    public static PrismContainerValueWrapper<ItemsSubCorrelatorType> createNewItemsSubCorrelatorValue(
            IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> model,
            PageBase pageBase, PrismContainerValue<ItemsSubCorrelatorType> value,
            AjaxRequestTarget target) {
        IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> containerModel = createContainerModel(model, ItemPath.create(
                CorrelationDefinitionType.F_CORRELATORS,
                CompositeCorrelatorType.F_ITEMS));
        PrismContainerWrapper<ItemsSubCorrelatorType> container = containerModel.getObject();

        PrismContainerValue<ItemsSubCorrelatorType> newValue = value;
        if (newValue == null) {
            newValue = container.getItem().createNewValue();
        }
        return createNewItemContainerValueWrapper(pageBase, newValue, container, target);
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
                PrismContainerWrapper<ResourceAttributeDefinitionType> container = resourceObjectTypeDefinition
                        .getObject().findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);
                for (ResourceAttributeDefinitionType attribute : attributes) {
                    PrismContainerValue<ResourceAttributeDefinitionType> prismContainerValue =
                            (PrismContainerValue<ResourceAttributeDefinitionType>) attribute.cloneWithoutId().asPrismContainerValue();
                    WebPrismUtil.cleanupEmptyContainerValue(prismContainerValue);
                    WebPrismUtil.createNewValueWrapper(container, prismContainerValue,
                            pageBase, target);
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
            AjaxRequestTarget target) {
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
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> model, ItemPath path) {
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

            createVirtualItemInMapping(pageBase, newValueWrapper, null, null, containerValueWrapper);

            return newValueWrapper;

        } catch (SchemaException e) {
            LOGGER.error("Couldn't create new attribute mapping");
            throw new IllegalStateException("Couldn't create new attribute mapping", e);
        }
    }

    protected static void createVirtualItemInMapping(
            PageBase pageBase,
            @NotNull PrismContainerValueWrapper<MappingType> mapping,
            PrismContainerValueWrapper<ResourceAttributeDefinitionType> value,
            ItemPathType attributeRefValue,
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> containerValueWrapper)
            throws SchemaException {
        if (mapping.findProperty(ItemRefinedDefinitionType.F_REF) == null) {

            PrismPropertyDefinition<Object> propertyDef = containerValueWrapper.getDefinition().findPropertyDefinition(
                    ItemPath.create(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF));

            Task task = pageBase.createSimpleTask("Create virtual item");
            OperationResult result = task.getResult();

            @NotNull PrismProperty<Object> refValue = propertyDef.instantiate();
            if (value != null && !ValueStatus.ADDED.equals(value.getStatus())) {
                refValue.addRealValue(attributeRefValue);
            }

            ItemWrapper<?, ?> refItemWrapper = pageBase.createItemWrapper(
                    refValue,
                    mapping,
                    ItemStatus.ADDED,
                    new WrapperContext(task, result));

            ((ItemWrapperImpl<?, ?>) refItemWrapper).setDisplayName(
                    pageBase.getString(MappingDirection.INBOUND.name() + "." + ItemRefinedDefinitionType.F_REF));
            ((ItemWrapperImpl<?, ?>) refItemWrapper).setDisplayOrder(1);

            if (value != null && value.getRealValue() != null && attributeRefValue != null) {
                //noinspection unchecked
                refItemWrapper.getValue().setRealValue(attributeRefValue.clone());
            }

            refItemWrapper.setVisibleOverwrite(UserInterfaceElementVisibilityType.HIDDEN);
            mapping.addItem(refItemWrapper);
            mapping.getNonContainers().clear();
        }
    }

    public static <C extends MappingType> @Nullable PrismContainerValueWrapper<C> findRelatedInboundMapping(
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
                    ItemPathType path = target.getPath();
                    return correlationItemRef.equals(path);
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
