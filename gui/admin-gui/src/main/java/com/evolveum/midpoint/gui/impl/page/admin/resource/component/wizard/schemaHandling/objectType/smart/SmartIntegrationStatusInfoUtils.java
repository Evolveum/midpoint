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
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SmartIntegrationStatusInfoUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationStatusInfoUtils.class);

    /*OBJECT_TYPE_SUGGESTIONS*/

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
                .filter(s -> s.getObjectClassName() != null && s.getObjectClassName().equals(objectClassName))
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

    /*CORRELATION_TYPE_SUGGESTIONS*/

    public static List<StatusInfo<CorrelationSuggestionsType>> loadCorrelationTypeSuggestions(
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

    public record CorrelationSuggestionProviderResult(
            @NotNull List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<ItemsSubCorrelatorType>, StatusInfo<CorrelationSuggestionsType>> suggestionByWrapper) {

    }

    public static @NotNull CorrelationSuggestionProviderResult loadCorrelationSuggestionWrappers(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeDefinitionType resourceObjectTypeDefinition,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Map<PrismContainerValueWrapper<ItemsSubCorrelatorType>, StatusInfo<CorrelationSuggestionsType>> suggestionByWrapper = new HashMap<>();

        try {
            List<StatusInfo<CorrelationSuggestionsType>> suggestions =
                    loadCorrelationTypeSuggestions(pageBase, resourceOid, task, result);

            if (suggestions == null || suggestions.isEmpty()) {
                return new CorrelationSuggestionProviderResult(Collections.emptyList(), suggestionByWrapper);
            }

            List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueWrappers = new ArrayList<>();
            for (StatusInfo<CorrelationSuggestionsType> si : suggestions) {

                if (si == null || si.getRequest() == null
                        || si.getStatus() == OperationResultStatusType.NOT_APPLICABLE
                        || !matchKindAndIntent(resourceObjectTypeDefinition, si.getRequest())) {
                    continue;
                }
                @NotNull CorrelationSuggestionsType suggestionsResult = ensureCorrelationSuggestionPresent(si);

                for (CorrelationSuggestionType suggestion : suggestionsResult.getSuggestion()) {
                    try {
                        @SuppressWarnings("unchecked")
                        PrismContainer<CorrelationSuggestionType> suggestionContainer = suggestion.asPrismContainerValue().getContainer();
                        PrismContainerWrapper<CorrelationSuggestionType> suggestionWrapper =
                                pageBase.createItemWrapper(suggestionContainer, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

                        PrismContainerWrapper<CorrelationDefinitionType> defWrapper = suggestionWrapper.findContainer(
                                CorrelationSuggestionType.F_CORRELATION);

                        if (defWrapper == null) {
                            result.recordWarning("Suggestion without CorrelationDefinition container was skipped.");
                            continue;
                        }

                        for (PrismContainerValueWrapper<CorrelationDefinitionType> defValue : defWrapper.getValues()) {
                            PrismContainerWrapper<ItemsSubCorrelatorType> itemsWrapper = defValue.findContainer(
                                    ItemPath.create(CorrelationDefinitionType.F_CORRELATORS, CompositeCorrelatorType.F_ITEMS));

                            if (itemsWrapper == null) {
                                continue;
                            }

                            List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> values = itemsWrapper.getValues();
                            if (values != null && !values.isEmpty()) {
                                for (PrismContainerValueWrapper<ItemsSubCorrelatorType> value : values) {
                                    PrismContainerWrapper<CorrelationItemType> container = value.findContainer(ItemsSubCorrelatorType.F_ITEM);
                                    setupStatusIfRequiredNewMapping(container, suggestion, resourceObjectTypeDefinition);
                                    valueWrappers.add(value);
                                    suggestionByWrapper.put(value, si);
                                }
                            }
                        }

                    } catch (SchemaException e) {
                        result.recordPartialError("Failed to wrap correlation suggestion.", e);
                    }
                }
            }
            return new CorrelationSuggestionProviderResult(valueWrappers, suggestionByWrapper);
        } finally {
            result.close();
        }
    }

    /**
     * Ensures that the correlation suggestion contains a valid CorrelationDefinition.
     * If not present, initializes a new one with default values.
     * Required to avoid NPEs in later processing stages.
     */
    private static @NotNull CorrelationSuggestionsType ensureCorrelationSuggestionPresent(
            @NotNull StatusInfo<CorrelationSuggestionsType> si) {
        CorrelationSuggestionsType suggestionsResult = si.getResult();
        if (suggestionsResult == null) {

            CorrelationDefinitionType definition = new CorrelationDefinitionType();

            CompositeCorrelatorType composite = new CompositeCorrelatorType();
            composite.getItems().add(new ItemsSubCorrelatorType());
            definition.setCorrelators(composite);

            CorrelationSuggestionType suggestion = new CorrelationSuggestionType();
            suggestion.setCorrelation(definition);

            suggestionsResult = new CorrelationSuggestionsType();
            suggestionsResult.getSuggestion().add(suggestion);
        }
        return suggestionsResult;
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
     * Sets the status of correlation items to ADDED if they correspond to a suggested attribute mapping
     * that does not yet exist in the resource object type definition.
     *
     * @param container the container wrapper holding correlation items
     * @param suggestion the correlation suggestion containing suggested attributes
     * @param resourceObjectTypeDefinition the resource object type definition to check existing attributes against
     */
    private static void setupStatusIfRequiredNewMapping(
            @NotNull PrismContainerWrapper<CorrelationItemType> container,
            @NotNull CorrelationSuggestionType suggestion, ResourceObjectTypeDefinitionType resourceObjectTypeDefinition) {

        List<PrismContainerValueWrapper<CorrelationItemType>> correlationItems = container.getValues();
        if (correlationItems == null) {
            return;
        }

//        List<ResourceAttributeDefinitionType> suggestedAttributes = loadNonExistingSuggestedMappings(suggestion,
//                resourceObjectTypeDefinition);
        List<ResourceAttributeDefinitionType> suggestedAttributes = suggestion.getAttributes();

        Set<ItemPath> targetPaths = suggestedAttributes.stream()
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

        if (targetPaths.isEmpty()) {
            return;
        }

        correlationItems.stream()
                .filter(Objects::nonNull)
                .forEach(item -> {
                    ItemPath refPath = Optional.ofNullable(item.getRealValue())
                            .map(CorrelationItemType::getRef)
                            .map(ItemPathType::getItemPath)
                            .orElse(null);

                    if (refPath != null && targetPaths.stream().anyMatch(p -> p.equivalent(refPath))) {
                        item.setStatus(ValueStatus.ADDED);
                    }
                });
    }

    /**
     * Removes suggested attributes that already exist in the resource object type definition.
     *
     * @param suggestion correlation suggestion to load suggested attributes from
     * @param resourceObjectTypeDefinition resource object type definition to check existing attributes against
     * @return list of suggested attributes that do not exist in the resource object type definition
     */
    //TODO: decide whats if mapping with ref/target exist but with different additional properties (e.g. expression, strength, etc.)
    public static @NotNull List<ResourceAttributeDefinitionType> loadNonExistingSuggestedMappings(
            @NotNull CorrelationSuggestionType suggestion,
            @NotNull ResourceObjectTypeDefinitionType resourceObjectTypeDefinition) {
        List<ResourceAttributeDefinitionType> suggestedAttributes = new ArrayList<>(suggestion.getAttributes());

        List<ResourceAttributeDefinitionType> existingAttribute = resourceObjectTypeDefinition.getAttribute();
        if (existingAttribute != null && !existingAttribute.isEmpty()) {
            List<ItemPath> existingMappingRefPaths = existingAttribute.stream()
                    .map(ResourceAttributeDefinitionType::getRef)
                    .filter(Objects::nonNull)
                    .map(ItemPathType::getItemPath)
                    .toList();

            suggestedAttributes.removeIf(attr ->
                    attr.getRef() != null &&
                            existingMappingRefPaths.stream()
                                    .anyMatch(p -> p.equivalent(attr.getRef().getItemPath()))
            );
        }
        return suggestedAttributes;
    }

    public record ObjectTypeSuggestionProviderResult(
            @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, StatusInfo<ObjectTypesSuggestionType>> suggestionByWrapper) {

    }

    /** Creates value wrappers for each suggested object type. */
    public static @NotNull ObjectTypeSuggestionProviderResult loadObjectTypeSuggestionWrappers(
            PageBase pageBase,
            String resourceOid,
            Task task,
            OperationResult result) {
        Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, StatusInfo<ObjectTypesSuggestionType>> suggestionByWrapper = new HashMap<>();

        final List<StatusInfo<ObjectTypesSuggestionType>> suggestions = loadObjectTypeSuggestions(
                pageBase, resourceOid, task, result);
        if (suggestions == null || suggestions.isEmpty()) {
            return new ObjectTypeSuggestionProviderResult(Collections.emptyList(), suggestionByWrapper);
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

        return new ObjectTypeSuggestionProviderResult(wrappers, suggestionByWrapper);
    }

    /** Builds display rows depending on the suggestion status. */
    public static @NotNull List<SmartGeneratingDto.StatusRow> buildStatusRows(PageBase pageBase, StatusInfo<?> suggestion) {
        List<SmartGeneratingDto.StatusRow> rows = new ArrayList<>();
        if (suggestion == null
                || suggestion.getProgressInformation() == null
                || suggestion.getProgressInformation().getChildren().isEmpty()) {
            rows.add(new SmartGeneratingDto.StatusRow(pageBase.createStringResource(
                    "SmartGeneratingDto.no.suggestion"),
                    ActivityProgressInformation.RealizationState.UNKNOWN,
                    suggestion));
            return rows;
        }

        ActivityProgressInformation progressInformation = suggestion.getProgressInformation();

        List<ActivityProgressInformation> children = progressInformation.getChildren();
        for (ActivityProgressInformation child : children) {
            String activityIdentifier = child.getActivityIdentifier();
            ActivityProgressInformation.RealizationState realizationState = child.getRealizationState();
            rows.add(new SmartGeneratingDto.StatusRow(
                    buildProgressMessageModel(pageBase, activityIdentifier), realizationState, suggestion));
        }
        return rows;
    }

    protected static IModel<String> buildProgressMessageModel(@NotNull PageBase pageBase, String operationKey) {
        return pageBase.createStringResource("Activity.explanation." + operationKey);
    }
}
