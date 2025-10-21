/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.StatusRowRecord;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.resumeSuggestionTask;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.suspendSuggestionTask;

public class SmartIntegrationStatusInfoUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationStatusInfoUtils.class);

    public record SuggestionProviderResult<C extends Containerable, T>(
            @NotNull List<PrismContainerValueWrapper<C>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<C>, StatusInfo<T>> suggestionByWrapper) {
    }

    /**
     * Loads the current status of a resource, initializing it via smart integration services.
     * Returns a real status or an error status in case of failure.
     */
    public static @Nullable List<StatusInfo<ObjectTypesSuggestionType>> loadObjectTypeSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        var smart = pageBase.getSmartIntegrationService();

        try {
            return smart.listSuggestObjectTypesOperationStatuses(resourceOid, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Couldn't load object type suggestions status for {}", t, resourceOid);
            return null;
        } finally {
            result.close();
        }
    }

    /**
     * Loads the current status of association suggestions for a resource,
     * initializing it via smart integration services.
     * Returns a real status or an error status in case of failure.
     */
    public static @Nullable List<StatusInfo<AssociationsSuggestionType>> loadAssociationSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        var smart = pageBase.getSmartIntegrationService();

        try {
            return smart.listSuggestAssociationsOperationStatuses(resourceOid, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Couldn't load association suggestions status for {}", t, resourceOid);
            return null;
        } finally {
            result.close();
        }
    }

    /**
     * Retrieves the latest available object types suggestion for a given object class.
     * Returns {@code null} if there are no suggestions.
     */
    public static @Nullable StatusInfo<ObjectTypesSuggestionType> loadObjectClassObjectTypeSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull QName objectClassName,
            @NotNull Task task,
            @NotNull OperationResult result) {

        List<StatusInfo<ObjectTypesSuggestionType>> statuses = loadObjectTypeSuggestions(pageBase, resourceOid, task, result);

        if (statuses == null) {
            LOGGER.warn("No object type suggestions found for resource {} and class {}", resourceOid, objectClassName);
            return null;
        }

        List<StatusInfo<ObjectTypesSuggestionType>> suggestionsPerObjectClass = statuses.stream()
                .filter(s -> s.getObjectClassName() != null
                        && s.getObjectClassName().equals(objectClassName))
                .toList();

        return findLatestSuggestion(suggestionsPerObjectClass);
    }

    /**
     * Finds the most recent suggestion entry based on the 'started' timestamp.
     * Returns {@code null} if the list is empty.
     */
    private static StatusInfo<ObjectTypesSuggestionType> findLatestSuggestion(
            @NotNull List<StatusInfo<ObjectTypesSuggestionType>> suggestions) {
        return suggestions.stream()
                .max(Comparator.comparing(
                        StatusInfo::getRealizationStartTimestamp,
                        Comparator.nullsLast(XMLGregorianCalendar::compare)))
                .orElse(null);
    }

    public static @Nullable List<StatusInfo<CorrelationSuggestionsType>> loadCorrelationTypeSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        var smart = pageBase.getSmartIntegrationService();

        try {
            return smart.listSuggestCorrelationOperationStatuses(resourceOid, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Couldn't load correlation status for {}", t, resourceOid);
            return null;
        } finally {
            result.close();
        }
    }

    public static @NotNull SuggestionProviderResult<ItemsSubCorrelatorType, CorrelationSuggestionsType> loadCorrelationSuggestionWrappers(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeDefinitionType rotDef,
            boolean includeNonCompletedSuggestions,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<PrismContainerValueWrapper<ItemsSubCorrelatorType>, StatusInfo<CorrelationSuggestionsType>> byWrapper = new HashMap<>();
        List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> wrappers = new ArrayList<>();

        try {
            List<StatusInfo<CorrelationSuggestionsType>> statuses =
                    loadCorrelationTypeSuggestions(pageBase, resourceOid, task, result);
            if (statuses == null || statuses.isEmpty()) {
                return new SuggestionProviderResult<>(Collections.emptyList(), byWrapper);
            }

            for (StatusInfo<CorrelationSuggestionsType> suggestionStatusInfo : statuses) {
                if (!isCorrelationSuggestionEligible(suggestionStatusInfo, rotDef)) {continue;}

                if (!includeNonCompletedSuggestions && isNotCompletedSuggestion(suggestionStatusInfo)) {
                    continue;
                }

                CorrelationSuggestionsType suggestionParent = ensureCorrelationSuggestionsPresent(suggestionStatusInfo);

                @SuppressWarnings("unchecked")
                PrismContainer<CorrelationSuggestionType> container = suggestionParent.asPrismContainerValue()
                        .findContainer(CorrelationSuggestionsType.F_SUGGESTION);

                if (container == null) {
                    continue;
                }

                PrismContainerWrapper<CorrelationSuggestionType> corrSuggestionW =
                        pageBase.createItemWrapper(container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

                for (PrismContainerValueWrapper<CorrelationSuggestionType> suggestion : corrSuggestionW.getValues()) {
                    try {
                        PrismContainerWrapper<CorrelationDefinitionType> corrDefW = suggestion.findContainer(
                                CorrelationSuggestionType.F_CORRELATION);
                        if (corrDefW == null) {
                            result.recordWarning("Suggestion without correlator definition was skipped.");
                            continue;
                        }

                        PrismContainerWrapper<ItemsSubCorrelatorType> itemsW = corrDefW.findContainer(ItemPath.create(
                                CorrelationDefinitionType.F_CORRELATORS, CompositeCorrelatorType.F_ITEMS));
                        if (itemsW == null || itemsW.getValues() == null) {
                            result.recordWarning("Suggestion without correlator items was skipped.");
                            continue;
                        }

                        for (PrismContainerValueWrapper<ItemsSubCorrelatorType> correlatorItemWrapper : itemsW.getValues()) {
                            PrismContainerWrapper<CorrelationItemType> correlationItemWrapper = correlatorItemWrapper
                                    .findContainer(ItemsSubCorrelatorType.F_ITEM);
                            setupStatusIfRequiredNewMapping(correlationItemWrapper, suggestion, rotDef);
                            wrappers.add(correlatorItemWrapper);
                            byWrapper.put(correlatorItemWrapper, suggestionStatusInfo);
                        }
                    } catch (SchemaException e) {
                        result.recordPartialError("Failed to wrap correlator items.", e);
                    }
                }
            }
            return new SuggestionProviderResult<>(wrappers, byWrapper);

        } catch (SchemaException e) {
            throw new IllegalStateException("Failed to wrap correlation suggestions", e);
        } finally {
            result.close();
        }
    }

    public static @Nullable List<StatusInfo<MappingsSuggestionType>> loadMappingTypeSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        var smart = pageBase.getSmartIntegrationService();

        try {
            return smart.listSuggestMappingsOperationStatuses(resourceOid, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Couldn't load correlation status for {}", t, resourceOid);
            return null;
        } finally {
            result.close();
        }
    }

    /**
     * Loads the mapping type suggestion status for the given resource.
     * <p>
     * This method queries the {@code SmartIntegrationService} for mapping suggestion operation
     * statuses related to the specified {@code resourceOid}. Although the service may return
     * multiple statuses, only the first one is relevant and returned here.
     */
    public static @Nullable StatusInfo<MappingsSuggestionType> loadMappingTypeSuggestion(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        var smart = pageBase.getSmartIntegrationService();

        try {
            List<StatusInfo<MappingsSuggestionType>> statusInfos = smart.listSuggestMappingsOperationStatuses(
                    resourceOid, task, result);
            if (statusInfos == null || statusInfos.isEmpty()) {
                return null;
            }
            return statusInfos.get(0);
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Couldn't load Mapping status for {}", t, resourceOid);
            return null;
        } finally {
            result.close();
        }
    }

    public static @Nullable StatusInfo<CorrelationSuggestionsType> loadCorrelationTypeSuggestion(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        var smart = pageBase.getSmartIntegrationService();

        try {
            List<StatusInfo<CorrelationSuggestionsType>> statusInfos = smart.listSuggestCorrelationOperationStatuses(
                    resourceOid, task, result);
            if (statusInfos == null || statusInfos.isEmpty()) {
                return null;
            }
            return statusInfos.get(0);
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Couldn't load Correlation status for {}", t, resourceOid);
            return null;
        } finally {
            result.close();
        }
    }

    public static @NotNull SuggestionProviderResult<MappingType, MappingsSuggestionType> loadMappingSuggestionWrappers(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeDefinitionType rotDef,
            @NotNull MappingDirection mappingDirection,
            boolean includeNonCompletedSuggestions,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<PrismContainerValueWrapper<MappingType>, StatusInfo<MappingsSuggestionType>> byWrapper = new HashMap<>();
        List<PrismContainerValueWrapper<MappingType>> wrappers = new ArrayList<>();

        try {
            List<StatusInfo<MappingsSuggestionType>> statuses =
                    loadMappingTypeSuggestions(pageBase, resourceOid, task, result);
            if (statuses == null || statuses.isEmpty()) {
                return new SuggestionProviderResult<>(Collections.emptyList(), byWrapper);
            }

            for (StatusInfo<MappingsSuggestionType> suggestionStatusInfo : statuses) {
                if (!isMappingSuggestionEligible(suggestionStatusInfo, rotDef)) {
                    continue;
                }

                if (!includeNonCompletedSuggestions && isNotCompletedSuggestion(suggestionStatusInfo)) {
                    continue;
                }

                MappingsSuggestionType suggestionParent = ensureMappingsSuggestionsPresent(suggestionStatusInfo, mappingDirection);

                @SuppressWarnings("unchecked") PrismContainer<AttributeMappingsSuggestionType> attrMappingsContainer =
                        (PrismContainer<AttributeMappingsSuggestionType>) suggestionParent.asPrismContainerValue()
                                .findContainer(MappingsSuggestionType.F_ATTRIBUTE_MAPPINGS);
                if (attrMappingsContainer == null) {
                    continue;
                }

                PrismContainerWrapper<AttributeMappingsSuggestionType> attrMappingsWrapper =
                        pageBase.createItemWrapper(attrMappingsContainer, ItemStatus.NOT_CHANGED,
                                new WrapperContext(task, result));

                for (PrismContainerValueWrapper<AttributeMappingsSuggestionType> suggestion : attrMappingsWrapper.getValues()) {
                    try {
                        PrismContainerWrapper<ResourceAttributeDefinitionType> defWrapper =
                                suggestion.findContainer(AttributeMappingsSuggestionType.F_DEFINITION);
                        if (defWrapper == null || defWrapper.getValues().isEmpty()) {
                            result.recordWarning("Suggestion without resource attribute definition skipped");
                            continue;
                        }

                        for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> defItemWrapper : defWrapper.getValues()) {
                            PrismContainerWrapper<MappingType> inboundWrapper =
                                    defItemWrapper.findContainer(mappingDirection.getContainerName());
                            if (inboundWrapper == null || inboundWrapper.getValues().isEmpty()) {
                                result.recordWarning("Suggestion without inbound mappings skipped");
                                continue;
                            }

                            PrismPropertyDefinition<Object> refDef =
                                    defItemWrapper.getDefinition()
                                            .findPropertyDefinition(ResourceAttributeDefinitionType.F_REF);

                            for (PrismContainerValueWrapper<MappingType> mappingVw : inboundWrapper.getValues()) {
                                MappingUtils.createVirtualItemInMapping(
                                        mappingVw,
                                        defItemWrapper,
                                        refDef,
                                        pageBase,
                                        ResourceAttributeDefinitionType.F_REF,
                                        mappingDirection);

                                wrappers.add(mappingVw);
                                byWrapper.put(mappingVw, suggestionStatusInfo);
                            }
                        }
                    } catch (SchemaException e) {
                        result.recordPartialError("Failed to wrap mapping items.", e);
                    }
                }
            }

            return new SuggestionProviderResult<>(wrappers, byWrapper);

        } catch (SchemaException e) {
            throw new IllegalStateException("Failed to wrap mapping suggestions", e);
        } finally {
            result.close();
        }
    }

    public static boolean isNotCompletedSuggestion(@Nullable StatusInfo<?> suggestionStatusInfo) {
        if (suggestionStatusInfo == null) {
            return false;
        }
        return suggestionStatusInfo.isExecuting() ||
                (suggestionStatusInfo.getStatus() == OperationResultStatusType.IN_PROGRESS
                        || suggestionStatusInfo.getStatus() == OperationResultStatusType.FATAL_ERROR);
    }

    private static boolean isCorrelationSuggestionEligible(
            @Nullable StatusInfo<CorrelationSuggestionsType> si,
            @NotNull ResourceObjectTypeDefinitionType rotDef) {
        return si != null
                && si.getRequest() != null
                && si.getStatus() != OperationResultStatusType.NOT_APPLICABLE
                && matchKindAndIntent(rotDef, si.getRequest());
    }

    private static boolean isMappingSuggestionEligible(
            StatusInfo<MappingsSuggestionType> si,
            @NotNull ResourceObjectTypeDefinitionType rotDef) {
        return si != null
                && si.getRequest() != null
                && si.getStatus() != OperationResultStatusType.NOT_APPLICABLE
                && matchKindAndIntent(rotDef, si.getRequest());
    }

    /** Ensure the top-level container exists (when result is null). */
    private static @NotNull CorrelationSuggestionsType ensureCorrelationSuggestionsPresent(
            @NotNull StatusInfo<CorrelationSuggestionsType> si) {
        CorrelationSuggestionsType out = si.getResult();
        if (out == null) {
            var def = new CorrelationDefinitionType();
            var comp = new CompositeCorrelatorType();
            comp.getItems().add(new ItemsSubCorrelatorType());
            def.setCorrelators(comp);

            var s = new CorrelationSuggestionType();
            s.setCorrelation(def);

            out = new CorrelationSuggestionsType();
            out.getSuggestion().add(s);
        }
        return out;
    }

    /** Ensure the top-level container exists (when result is null). */
    private static @NotNull MappingsSuggestionType ensureMappingsSuggestionsPresent(
            @NotNull StatusInfo<MappingsSuggestionType> si,
            @NotNull MappingDirection mappingDirection) {
        MappingsSuggestionType out = si.getResult();
        if (out == null) {
            var def = new AttributeMappingsSuggestionType();
            ResourceAttributeDefinitionType attrDef = new ResourceAttributeDefinitionType();

            if (mappingDirection == MappingDirection.INBOUND) {
                attrDef.getInbound().add(new InboundMappingType());
            } else if (mappingDirection == MappingDirection.OUTBOUND) {
                attrDef.setOutbound(new MappingType());
            }

            def.setDefinition(attrDef);

            out = new MappingsSuggestionType();
            out.getAttributeMappings().add(def);
        }
        return out;
    }

    /**
     * Checks if the kind and intent of the resource object type definition match those of the request.
     */
    private static boolean matchKindAndIntent(
            @NotNull ResourceObjectTypeDefinitionType resourceObjectTypeDefinition,
            @NotNull BasicResourceObjectSetType request) {
        return request.getKind().equals(resourceObjectTypeDefinition.getKind())
                && request.getIntent().equals(resourceObjectTypeDefinition.getIntent());
    }

    /**
     * Sets the status of correlation items to ADDED if they correspond to any of the suggested attributes'
     *
     * @param container the container wrapper holding correlation items
     * @param suggestion the correlation suggestion containing suggested attributes
     * @param rotDef the resource object type definition to check existing mappings
     */
    private static void setupStatusIfRequiredNewMapping(
            @NotNull PrismContainerWrapper<CorrelationItemType> container,
            PrismContainerValueWrapper<CorrelationSuggestionType> suggestion,
            @NotNull ResourceObjectTypeDefinitionType rotDef) {

        List<PrismContainerValueWrapper<CorrelationItemType>> correlationItems = container.getValues();
        if (correlationItems == null) {
            return;
        }

        PrismContainerWrapper<ResourceAttributeDefinitionType> attributeDefW;
        try {
            attributeDefW = suggestion.findContainer(
                    CorrelationSuggestionType.F_ATTRIBUTES);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't find attributes in correlation suggestion {}", e, suggestion);
            return;
        }

        List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> suggestedAttributesW = attributeDefW.getValues();

        List<ResourceAttributeDefinitionType> suggestedAttributes = suggestedAttributesW.stream()
                .map(PrismContainerValueWrapper::getRealValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (suggestedAttributes.isEmpty()) {
            return;
        }

        Set<ItemPath> rotDefAttributePaths = collectMappingTargets(rotDef.getAttribute());
        Set<ItemPath> suggestionMappingTargetPaths = collectMappingTargets(suggestedAttributes);
        // We are interested only in those paths that are not already mapped in the object type definition
        suggestionMappingTargetPaths.removeIf(
                s -> rotDefAttributePaths.stream().anyMatch(s::equivalent)
        );

        // Mark all suggested attributes that correspond to the new mapping targets as ADDED (at least one mapping should be new)
        suggestedAttributesW.forEach(valueWrapper -> {
            ResourceAttributeDefinitionType realValue = valueWrapper.getRealValue();
            Set<ItemPath> itemPaths = collectMappingTargets(Collections.singletonList(realValue));
            if (itemPaths.isEmpty()) {
                return;
            }
            if (itemPaths.stream().anyMatch(suggestionMappingTargetPaths::contains)) {
                valueWrapper.setStatus(ValueStatus.ADDED);
            }
        });

        if (suggestionMappingTargetPaths.isEmpty()) {
            return;
        }

        correlationItems.stream()
                .filter(Objects::nonNull)
                .forEach(item -> {
                    ItemPath refPath = Optional.ofNullable(item.getRealValue())
                            .map(CorrelationItemType::getRef)
                            .map(ItemPathType::getItemPath)
                            .orElse(null);

                    if (refPath != null && suggestionMappingTargetPaths.stream().anyMatch(p -> p.equivalent(refPath))) {
                        item.setStatus(ValueStatus.ADDED);
                    }
                });
    }

    public static @NotNull List<ResourceAttributeDefinitionType> collectRequiredResourceAttributeDefs(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull PrismContainerValueWrapper<CorrelationSuggestionType> parentSuggestionW) {
        List<ResourceAttributeDefinitionType> attributes = new ArrayList<>();
        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> wrapper =
                    parentSuggestionW.findContainer(CorrelationSuggestionType.F_ATTRIBUTES);

            if (wrapper != null && wrapper.getValues() != null && !wrapper.getValues().isEmpty()) {
                for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> val : wrapper.getValues()) {
                    if (val != null && val.getRealValue() != null && val.getStatus() == ValueStatus.ADDED) {
                        attributes.add(val.getRealValue());
                    }
                }
            }

        } catch (SchemaException e) {
            LOGGER.error("Couldn't find attributes container in {}", parentSuggestionW, e);
            pageBase.error("Couldn't process correlation suggestion.");
            target.add(pageBase.getFeedbackPanel().getParent());
        }
        return attributes;
    }

    private static @NotNull Set<ItemPath> collectMappingTargets(@Nullable List<ResourceAttributeDefinitionType> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptySet();
        }

        return attributes.stream()
                .filter(Objects::nonNull)
                .flatMap(attr -> {
                    List<InboundMappingType> inbound = attr.getInbound();
                    return inbound != null ? inbound.stream() : Stream.empty();
                })
                .filter(Objects::nonNull)
                .map(MappingType::getTarget)
                .filter(Objects::nonNull)
                .map(VariableBindingDefinitionType::getPath)
                .filter(Objects::nonNull)
                .map(ItemPathType::getItemPath)
                .collect(Collectors.toSet());
    }

    /** Creates value wrappers for each suggested object type. */
    public static @NotNull SuggestionProviderResult<ResourceObjectTypeDefinitionType, ObjectTypesSuggestionType> loadObjectTypeSuggestionWrappers(
            PageBase pageBase,
            String resourceOid,
            Task task,
            OperationResult result) {
        Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, StatusInfo<ObjectTypesSuggestionType>> suggestionByWrapper = new HashMap<>();

        final List<StatusInfo<ObjectTypesSuggestionType>> suggestions = loadObjectTypeSuggestions(
                pageBase, resourceOid, task, result);
        if (suggestions == null || suggestions.isEmpty()) {
            return new SuggestionProviderResult<>(Collections.emptyList(), suggestionByWrapper);
        }

        final List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> wrappers = new ArrayList<>();
        suggestions.stream().filter(Objects::nonNull).forEach(si -> {
            ObjectTypesSuggestionType suggestion = si.getResult();
            if (si.getStatus() == OperationResultStatusType.NOT_APPLICABLE) {
                return;
            }

            if (si.getResult() == null) {
                ObjectTypesSuggestionType tmp = new ObjectTypesSuggestionType();
                tmp.getObjectType().add(new ResourceObjectTypeDefinitionType());
                suggestion = tmp;
            }

            try {
                @SuppressWarnings("unchecked")
                PrismContainer<ResourceObjectTypeDefinitionType> container =
                        suggestion.asPrismContainerValue().findContainer(ObjectTypesSuggestionType.F_OBJECT_TYPE);

                PrismContainerWrapper<ResourceObjectTypeDefinitionType> wrapper = pageBase.createItemWrapper(
                        container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

                wrapper.getValues().forEach(value -> {
                    suggestionByWrapper.put(value, si);
                    wrappers.add(value);
                });
            } catch (SchemaException e) {
                throw new IllegalStateException("Failed to wrap object type suggestions", e);
            }
        });

        return new SuggestionProviderResult<>(wrappers, suggestionByWrapper);
    }

    /** Creates value wrappers for each suggested association type. */
    public static @NotNull SuggestionProviderResult<ShadowAssociationTypeDefinitionType, AssociationsSuggestionType> loadAssociationTypeSuggestionWrappers(
            PageBase pageBase,
            String resourceOid,
            Task task,
            OperationResult result) {
        Map<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, StatusInfo<AssociationsSuggestionType>> suggestionByWrapper = new HashMap<>();

        final @Nullable List<StatusInfo<AssociationsSuggestionType>> suggestions = loadAssociationSuggestions(
                pageBase, resourceOid, task, result);
        if (suggestions == null || suggestions.isEmpty()) {
            return new SuggestionProviderResult<>(Collections.emptyList(), suggestionByWrapper);
        }

        final List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> wrappers = new ArrayList<>();
        suggestions.stream().filter(Objects::nonNull).forEach(si -> {
            AssociationsSuggestionType suggestion = si.getResult();
            if (si.getStatus() == OperationResultStatusType.NOT_APPLICABLE) {
                return;
            }

            if (si.getResult() == null || si.getResult().getAssociation().isEmpty()) {
                AssociationsSuggestionType tmp = new AssociationsSuggestionType();
                tmp.getAssociation().add(new AssociationSuggestionType());
                suggestion = tmp;
            }

            try {
                @SuppressWarnings("unchecked")
                PrismContainer<AssociationSuggestionType> container =
                        suggestion.asPrismContainerValue().findContainer(AssociationsSuggestionType.F_ASSOCIATION);

                PrismContainerWrapper<AssociationSuggestionType> wrapper = pageBase.createItemWrapper(
                        container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

                List<PrismContainerValueWrapper<AssociationSuggestionType>> values = wrapper.getValues();
                for (PrismContainerValueWrapper<AssociationSuggestionType> value : values) {
                    PrismContainerWrapper<ShadowAssociationTypeDefinitionType> assocDefW =
                            value.findContainer(AssociationSuggestionType.F_DEFINITION);
                    if (assocDefW == null || assocDefW.getValues() == null || assocDefW.getValues().isEmpty()) {
                        result.recordWarning("Association suggestion without definition was skipped.");
                        continue;
                    }
                    if (assocDefW.getValues().size() > 1) {
                        result.recordWarning("Association suggestion with multiple definitions was skipped.");
                        continue;
                    }

                    List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> shadowAssociationValueWrappers = assocDefW.getValues();
                    shadowAssociationValueWrappers.forEach(v -> {
                        suggestionByWrapper.put(v, si);
                        wrappers.add(v);
                    });

                }
            } catch (SchemaException e) {
                throw new IllegalStateException("Failed to wrap object type suggestions", e);
            }
        });

        return new SuggestionProviderResult<>(wrappers, suggestionByWrapper);
    }

    /** Builds display rows depending on the suggestion status. */
    public static @NotNull List<StatusRowRecord> buildStatusRows(PageBase pageBase, StatusInfo<?> suggestion, boolean rejectEmptyProgress) {
        List<StatusRowRecord> rows = new ArrayList<>();
        if (suggestion != null && suggestion.getStatus() == OperationResultStatusType.FATAL_ERROR) {
            rows.add(new StatusRowRecord(pageBase.createStringResource(
                    "SmartGeneratingDto.status.failed"),
                    ActivityProgressInformation.RealizationState.UNKNOWN,
                    suggestion));
            return rows;
        }

        if (rejectEmptyProgress && (suggestion == null
                || suggestion.getProgressInformation() == null)) {
            rows.add(new StatusRowRecord(pageBase.createStringResource(
                    "SmartGeneratingDto.no.suggestion"),
                    ActivityProgressInformation.RealizationState.UNKNOWN,
                    suggestion));
            return rows;
        }

        if (suggestion == null) {
            return rows;
        }

        ActivityProgressInformation progressInformation = suggestion.getProgressInformation();
        if (progressInformation == null) {
            return rows;
        }

        List<ActivityProgressInformation> children = progressInformation.getChildren();

        if (!rejectEmptyProgress && progressInformation.getChildren().isEmpty()) {
            return rows;
        }

        if (children.isEmpty()) {
            rows.add(new StatusRowRecord(pageBase.createStringResource(
                    "SmartGeneratingDto." + progressInformation.getRealizationState()),
                    progressInformation.getRealizationState(),
                    suggestion));
            return rows;
        }
        for (ActivityProgressInformation child : children) {
            String activityIdentifier = child.getActivityIdentifier();
            ActivityProgressInformation.RealizationState realizationState = child.getRealizationState();
            rows.add(new StatusRowRecord(
                    buildProgressMessageModel(pageBase, activityIdentifier), realizationState, suggestion));
        }
        return rows;
    }

    protected static IModel<String> buildProgressMessageModel(@NotNull PageBase pageBase, String operationKey) {
        return pageBase.createStringResource("Activity.explanation." + operationKey);
    }

    public static @NotNull String extractEfficiencyFromSuggestedCorrelationItemWrapper(
            @NotNull PrismContainerValueWrapper<ItemsSubCorrelatorType> valueWrapper) {
        PrismContainerValueWrapper<CorrelationSuggestionType> parentContainerValue = valueWrapper.getParentContainerValue(
                CorrelationSuggestionType.class);
        if (parentContainerValue != null && parentContainerValue.getRealValue() != null) {
            CorrelationSuggestionType suggestionValue = parentContainerValue.getRealValue();

            Double quality = suggestionValue.getQuality();
            return (quality != null && quality != -1) ? String.valueOf((suggestionValue.getQuality() * 100)) : "-";
        }
        return "-";
    }

    public static @Nullable Float extractEfficiencyFromSuggestedMappingItemWrapper(
            @NotNull PrismContainerValueWrapper<MappingType> valueWrapper) {
        PrismContainerValueWrapper<AttributeMappingsSuggestionType> parentContainerValue = valueWrapper.getParentContainerValue(
                AttributeMappingsSuggestionType.class);
        if (parentContainerValue != null && parentContainerValue.getRealValue() != null) {
            AttributeMappingsSuggestionType suggestionValue = parentContainerValue.getRealValue();

            return suggestionValue.getExpectedQuality() != null ? (suggestionValue.getExpectedQuality() * 100) : null;
        }
        return null;
    }

    public static int computeSuggestedObjectsCount(StatusInfo<?> statusInfo) {
        if (statusInfo == null || statusInfo.getResult() == null) {
            return 0;
        }

        if (statusInfo.getResult() instanceof MappingsSuggestionType mappingsSuggestion) {
            return Optional.ofNullable(mappingsSuggestion.getAttributeMappings())
                    .map(List::size).orElse(0);
        } else if (statusInfo.getResult() instanceof ObjectTypesSuggestionType objectTypesSuggestion) {
            return Optional.ofNullable(objectTypesSuggestion.getObjectType())
                    .map(List::size).orElse(0);
        } else if (statusInfo.getResult() instanceof CorrelationSuggestionsType correlationSuggestions) {
            return Optional.ofNullable(correlationSuggestions.getSuggestion())
                    .map(List::size).orElse(0);
        } else if (statusInfo.getResult() instanceof AssociationsSuggestionType associationsSuggestion) {
            return Optional.ofNullable(associationsSuggestion.getAssociation())
                    .map(List::size).orElse(0);
        }

        return 0;
    }

    public static void showSuggestionInfoPanelPopup(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @Nullable StatusInfo<?> statusInfo) {
        HelpInfoPanel helpInfoPanel = new HelpInfoPanel(
                pageBase.getMainPopupBodyId(),
                statusInfo != null ? statusInfo::getLocalizedMessage : null) {
            @Override
            public StringResourceModel getTitle() {
                return createStringResource("ResourceObjectTypesPanel.suggestion.details.title");
            }

            @Override
            protected @NotNull Label initLabel(IModel<String> messageModel) {
                Label label = super.initLabel(messageModel);
                label.add(AttributeModifier.append("class", "alert alert-danger"));
                return label;
            }

            @Override
            public @NotNull Component getFooter() {
                Component footer = super.getFooter();
                footer.add(new VisibleBehaviour(() -> false));
                return footer;
            }
        };

        target.add(pageBase.getMainPopup());

        pageBase.showMainPopup(
                helpInfoPanel, target);
    }

    public static void handleSuggestionSuspendResumeOperation(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<?> statusInfo,
            @NotNull Task task, @NotNull OperationResult result) {
        if (statusInfo.isSuspended() && statusInfo.getStatus() != OperationResultStatusType.FATAL_ERROR) {
            resumeSuggestionTask(pageBase, statusInfo, task, result);
        } else if (!statusInfo.isSuspended() && statusInfo.getStatus() != OperationResultStatusType.FATAL_ERROR) {
            suspendSuggestionTask(
                    pageBase, statusInfo, task, result);
        }
    }
}
