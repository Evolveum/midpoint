/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.getPathBaseOnMappingType;

/**
 * Utility methods for smart integration configuration in resource schema handling.
 *
 * <p>Provides helper logic for correlation items, suggested correlation rules,
 * mapping wrappers, and draft mapping cleanup used by the schema handling wizard.</p>
 */
public class SmartIntegrationWrapperUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationWrapperUtils.class);

    public static <C extends Containerable> @NotNull PrismContainerValue<C> processSuggestedContainerValue(
            @NotNull PrismContainerValue<C> container) {
        return PrismValueCollectionsUtil.cloneCollectionComplex(
                        CloneStrategy.REUSE,
                        Collections.singletonList(container))
                .iterator().next();
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
    public static <C extends Containerable> @NotNull IModel<PrismContainerWrapper<C>> createcContainerModel(
            @NotNull IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> model, @Nullable ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    public static <C extends Containerable> PrismContainerValueWrapper<C> createNewItemContainerValueWrapper(
            @NotNull PageBase pageBase, @NotNull PrismContainerValue<?> newItem,
            @NotNull PrismContainerWrapper<C> model, @NotNull AjaxRequestTarget target) {
        return WebPrismUtil.createNewValueWrapper(model, newItem, pageBase, target);
    }

    public static @Nullable PrismContainerValueWrapper<ItemsSubCorrelatorType> createNewItemsSubCorrelatorValue(
            @NotNull PageBase pageBase,
            @NotNull WebMarkupContainer feedback,
            @NotNull IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> model,
            @Nullable PrismContainerValue<ItemsSubCorrelatorType> value,
            @NotNull AjaxRequestTarget target) {

        IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> containerModel = createcContainerModel(
                model,
                ItemPath.create(
                        CorrelationDefinitionType.F_CORRELATORS,
                        CompositeCorrelatorType.F_ITEMS));

        PrismContainerWrapper<ItemsSubCorrelatorType> containerWrapper = containerModel.getObject();
        PrismContainer<ItemsSubCorrelatorType> container = containerWrapper.getItem();
        PrismContainerValue<ItemsSubCorrelatorType> newValue;
        if (value == null) {
            newValue = container.createNewValue();
        } else {
            boolean correlationRulePresent = isCorrelationRulePresent(container, value);
            if (correlationRulePresent) {
                pageBase.warn(pageBase.getString("CorrelationDefinitionType.correlationRuleAlreadyPresent"));
                target.add(feedback);
                return null;
            }
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

    protected static boolean isCorrelationRulePresent(
            @NotNull PrismContainer<ItemsSubCorrelatorType> container,
            PrismContainerValue<ItemsSubCorrelatorType> newValue) {
        List<PrismContainerValue<ItemsSubCorrelatorType>> values = container.getValues();
        Set<ItemsSubCorrelatorType> valueSet = values.stream().<ItemsSubCorrelatorType>map(PrismValue::getRealValue)
                .collect(Collectors.toSet());
        ItemsSubCorrelatorType v = newValue.getRealValue();
        return valueSet.contains(v);
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
        VariableBindingDefinitionType valueMappingTarget = realValueMapping.getTarget();

        if (valueMappingTarget != null) {
            bean.setRef(valueMappingTarget.getPath());
        } else {
            LOGGER.warn("Mapping target is null. Cannot set reference for mapping '{}'.", realValueMapping.getName());
        }

        bean.setDescription(realValueMapping.getDescription());

        WebPrismUtil.createNewValueWrapper(container, newValue, pageBase, target);
    }

    @Contract("_, _ -> new")
    public static <C extends Containerable> @NotNull IModel<PrismContainerWrapper<C>> createCorrelationItemContainerModel(
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> model, @Nullable ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    /**
     * Finds a mapping related to the provided correlation item.
     *
     * <p>The method searches both newly created and existing mappings and
     * matches them using the correlation item's target path reference.</p>
     */
    public static @Nullable PrismContainerValueWrapper<MappingType> findRelatedMapping(
            @NotNull PageBase pageBase,
            @NotNull PrismContainerValueWrapper<CorrelationItemType> correlationItemWrapper,
            @Nullable PrismContainerWrapper<?> mappings,
            @NotNull ItemPath parentPath,
            @NotNull MappingDirection mappingDirection) {

        ItemPathType correlationItemRef = correlationItemWrapper.getRealValue().getRef();

        if (correlationItemRef == null) {
            LOGGER.error("Correlation item reference is null. Processed mapping likely has no target configured.");
            return null;
        }

        List<PrismContainerValueWrapper<MappingType>> allMappings = new ArrayList<>();

        try {
            PrismContainerWrapper<?> container = findMappingsSourceContainer(
                    correlationItemWrapper, mappings, parentPath);

            if (container == null || container.getValues() == null || container.getValues().isEmpty()) {
                LOGGER.warn("Couldn't find related resource attribute definition.");
                return null;
            }

            ItemPath mappingsPath = getPathBaseOnMappingType(mappingDirection);

            for (PrismContainerValueWrapper<?> value : container.getValues()) {
                PrismContainerWrapper<MappingType> mappingContainer = value.findContainer(mappingsPath);

                if (mappingContainer == null || mappingContainer.getValues() == null) {
                    continue;
                }

                allMappings.addAll(mappingContainer.getValues());
            }

        } catch (SchemaException e) {
            LOGGER.error("Couldn't find related resource attribute definition.", e);
            return null;
        }

        return allMappings.stream()
                .filter(mapping -> isRelatedMapping(mapping, correlationItemRef, pageBase, mappingDirection))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the container holding mappings for the processed correlation item.
     * <p>Falls back to the supplied mappings container when the original
     * container is missing or empty.</p>
     */
    private static @Nullable PrismContainerWrapper<?> findMappingsSourceContainer(
            @NotNull PrismContainerValueWrapper<CorrelationItemType> correlationItemWrapper,
            @Nullable PrismContainerWrapper<?> mappings,
            @NotNull ItemPath parentPath)
            throws SchemaException {

        PrismContainerValueWrapper<CorrelationSuggestionType> suggestionWrapper =
                correlationItemWrapper.getParentContainerValue(CorrelationSuggestionType.class);

        PrismContainerWrapper<?> container;

        if (suggestionWrapper != null) {
            container = suggestionWrapper.findContainer(CorrelationSuggestionType.F_ATTRIBUTES);
        } else {
            PrismContainerValueWrapper<?> parentContainerValue =
                    correlationItemWrapper.getParentContainerValue(ResourceObjectTypeDefinitionType.class);

            if (parentContainerValue == null) {
                parentContainerValue = correlationItemWrapper
                        .getParentContainerValue(AssociationSynchronizationExpressionEvaluatorType.class);
            }

            container = parentContainerValue != null
                    ? parentContainerValue.findContainer(parentPath)
                    : null;
        }

        if (container == null || WebPrismUtil.isEmptyContainer(container.getItem())) {
            return mappings;
        }

        return container;
    }

    /**
     * Determines whether the mapping corresponds to the provided
     * correlation item reference.
     * <p>Ensures required virtual items are available before comparing
     * mapping target paths.</p>
     */
    private static boolean isRelatedMapping(
            @NotNull PrismContainerValueWrapper<MappingType> inboundMapping,
            @NotNull ItemPathType correlationItemRef,
            @NotNull PageBase pageBase,
            @NotNull MappingDirection mappingDirection) {

        VariableBindingDefinitionType target = inboundMapping.getRealValue().getTarget();

        if (target == null || target.getPath() == null) {
            return false;
        }

        PrismContainerValueWrapper<?> parentContainerValue = findMappingParentContainer(inboundMapping);

        if (parentContainerValue == null) {
            return false;
        }

        PrismPropertyDefinition<Object> propertyRefDef = resolveRefPropertyDefinition(parentContainerValue);

        if (propertyRefDef == null) {
            LOGGER.warn("Couldn't resolve ref property definition for related mapping.");
            return false;
        }

        MappingUtils.createVirtualItemInMapping(
                inboundMapping,
                parentContainerValue,
                propertyRefDef,
                pageBase,
                AbstractAttributeMappingsDefinitionType.F_REF,
                mappingDirection);

        return correlationItemRef.equals(target.getPath());
    }

    /**
     * Returns the parent container that owns the mapping definition.
     */
    private static @Nullable PrismContainerValueWrapper<?> findMappingParentContainer(
            @NotNull PrismContainerValueWrapper<MappingType> inboundMapping) {

        PrismContainerValueWrapper<?> parentContainerValue =
                inboundMapping.getParentContainerValue(ResourceAttributeDefinitionType.class);

        if (parentContainerValue != null) {
            return parentContainerValue;
        }

        return inboundMapping.getParentContainerValue(AttributeInboundMappingsDefinitionType.class);
    }

    /**
     * Resolves the definition of the reference property used by mappings.
     * <p>Attempts to obtain the definition directly from the container
     * definition and falls back to the existing property wrapper when
     * no container definition is available.</p>
     */
    private static @Nullable PrismPropertyDefinition<Object> resolveRefPropertyDefinition(
            @NotNull PrismContainerValueWrapper<?> parentContainerValue) {

        PrismContainerDefinition<?> definition = parentContainerValue.getDefinition();

        if (definition != null) {
            return definition.findPropertyDefinition(AbstractAttributeMappingsDefinitionType.F_REF);
        }

        try {
            PrismPropertyWrapper<Object> refProperty =
                    parentContainerValue.findProperty(AbstractAttributeMappingsDefinitionType.F_REF);

            if (refProperty == null || refProperty.getItem() == null) {
                return null;
            }

            return refProperty.getItem().getDefinition();

        } catch (SchemaException e) {
            LOGGER.warn("Couldn't find ref property definition for related mapping.", e);
            return null;
        }
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

    public static List<PrismContainerValueWrapper<CorrelationItemType>> extractCorrelationItemListWrapper(
            @NotNull PrismContainerValueWrapper<ItemsSubCorrelatorType> object) {
        try {
            PrismContainerWrapper<CorrelationItemType> container = object.findContainer(ItemsSubCorrelatorType.F_ITEM);
            return container.getValues();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't extract CorrelationItemType wrappers: {}", e.getMessage(), e);
        }

        return Collections.emptyList();
    }
}
