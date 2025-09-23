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
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SmartIntegrationWrapperUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationWrapperUtils.class);

    public static <C extends Containerable> @NotNull PrismContainerValue<C> processSuggestedContainerValue(
            @NotNull PrismContainerValue<C> container,
            @NotNull PrismContainer<C> parent) {
        PrismContainerValue<C> value = PrismValueCollectionsUtil.cloneCollectionComplex(
                        CloneStrategy.REUSE,
                        Collections.singletonList(container))
                .iterator().next();
        value.setParent(parent);
        // TODO: Be careful here! (need refactoring)
// 1. If <schemaHandling/> is not present in the resource, calling parent.getValues().add(value)
//    implicitly creates a new <schemaHandling/> block in Prism.
// 2. This forces MidPoint to generate an ADD delta not only for <schemaHandling/>,
//    but also for the newly added <objectType/>.
// 3. As a result, we can end up with *two* "new" items in the container, which breaks validation
//    (e.g. duplicate check reports value already exists).
// 4. Another side effect: when the user just clicks "Review" and then exits the wizard,
//    these generated deltas are already stored on the object, even though the user
//    never confirmed the change explicitly.

        parent.getValues().add(value);
        return value;
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
                pageBase.warn("Correlation rule already present.");
                target.add(pageBase.getFeedbackPanel().getParent());
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
        bean.setRef(realValueMapping.getTarget().getPath());
        bean.setDescription(realValueMapping.getDescription());

        WebPrismUtil.createNewValueWrapper(container, newValue, pageBase, target);
    }

    @Contract("_, _ -> new")
    public static <C extends Containerable> @NotNull IModel<PrismContainerWrapper<C>> createCorrelationItemContainerModel(
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> model, @Nullable ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    public static  @Nullable PrismContainerValueWrapper<MappingType> findRelatedInboundMapping(
            @NotNull PageBase pageBase,
            @NotNull PrismContainerValueWrapper<CorrelationItemType> correlationItemWrapper) {
        ItemPathType correlationItemRef = correlationItemWrapper.getRealValue().getRef();
        List<PrismContainerValueWrapper<MappingType>> allInboundMappings = new ArrayList<>();

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
                    PrismContainerWrapper<MappingType> inboundContainer = value.findContainer(ResourceAttributeDefinitionType.F_INBOUND);
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
                    MappingUtils.createVirtualItemInMapping(inboundMapping, parentContainerValue, propertyRefDef, pageBase,
                            ResourceAttributeDefinitionType.F_REF, MappingDirection.INBOUND);

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
