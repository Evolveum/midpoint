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
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

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

    public static @Nullable List<StatusInfo<CorrelationSuggestionType>> loadCorrelationTypeSuggestions(
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

    public static @Nullable StatusInfo<CorrelationSuggestionType> loadCorrelationTypeSuggestion(
            @NotNull PageBase pageBase,
            @NotNull String token,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<StatusInfo<CorrelationSuggestionType>> statusInfos = loadCorrelationTypeSuggestions(pageBase, resourceOid, task, result);
        if (statusInfos == null) {
            LOGGER.warn("No correlation suggestions found for resource {}", resourceOid);
            return null;
        }

        StatusInfo<CorrelationSuggestionType> statusInfo = statusInfos.stream()
                .filter(s -> token.equals(s.getToken()))
                .findFirst()
                .orElse(null);

        if (statusInfo == null) {
            LOGGER.warn("No correlation suggestion found for resource {} and token {}", resourceOid, token);
            return null;
        }

        return statusInfo;
    }

    public record CorrelationSuggestionProviderResult(
            @NotNull List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> wrappers,
            @NotNull Map<ItemsSubCorrelatorType, StatusInfo<CorrelationSuggestionType>> suggestionByWrapper) {

    }

    public static @NotNull CorrelationSuggestionProviderResult loadCorrelationSuggestionWrappers(
            PageBase pageBase,
            String resourceOid,
            Task task,
            OperationResult result) {
        Map<ItemsSubCorrelatorType, StatusInfo<CorrelationSuggestionType>> suggestionByWrapper = new HashMap<>();

        try {
            List<StatusInfo<CorrelationSuggestionType>> suggestions =
                    loadCorrelationTypeSuggestions(pageBase, resourceOid, task, result);

            if (suggestions == null || suggestions.isEmpty()) {
                return new CorrelationSuggestionProviderResult(Collections.emptyList(), suggestionByWrapper);
            }

            List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueWrappers = new ArrayList<>();
            for (StatusInfo<CorrelationSuggestionType> si : suggestions) {

                if (si == null || si.getStatus() == OperationResultStatusType.NOT_APPLICABLE) {
                    continue;
                }

                CorrelationSuggestionType suggestion = si.getResult();
                if (suggestion == null) {
                    CorrelationDefinitionType definition = new CorrelationDefinitionType();

                    CompositeCorrelatorType composite = new CompositeCorrelatorType();
                    composite.getItems().add(new ItemsSubCorrelatorType());
                    definition.setCorrelators(composite);

                    suggestion = new CorrelationSuggestionType();
                    suggestion.setCorrelation(definition);
                }

                try {
                    @SuppressWarnings("unchecked")
                    PrismContainer<CorrelationDefinitionType> correlationDef =
                            suggestion.asPrismContainerValue()
                                    .findContainer(CorrelationSuggestionType.F_CORRELATION);

                    if (correlationDef == null) {
                        result.recordWarning("Suggestion without CorrelationDefinition container was skipped.");
                        continue;
                    }

                    PrismContainerWrapper<CorrelationDefinitionType> defWrapper =
                            pageBase.createItemWrapper(
                                    correlationDef,
                                    ItemStatus.NOT_CHANGED,
                                    new WrapperContext(task, result)
                            );

                    for (PrismContainerValueWrapper<CorrelationDefinitionType> defValue : defWrapper.getValues()) {
                        try {
                            PrismContainerWrapper<ItemsSubCorrelatorType> itemsWrapper =
                                    defValue.findContainer(ItemPath.create(
                                            CorrelationDefinitionType.F_CORRELATORS,
                                            CompositeCorrelatorType.F_ITEMS));

                            if (itemsWrapper == null) {
                                continue;
                            }

                            List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> values = itemsWrapper.getValues();
                            if (values != null && !values.isEmpty()) {
                                for (PrismContainerValueWrapper<ItemsSubCorrelatorType> value : values) {
                                    valueWrappers.add(value);
                                    suggestionByWrapper.put(value.getRealValue(), si);
                                }
                            }

                        } catch (SchemaException e) {
                            result.recordPartialError("Failed to read correlator items from a definition.", e);
                        }
                    }

                } catch (SchemaException e) {
                    result.recordPartialError("Failed to wrap correlation suggestion.", e);
                }
            }

            return new CorrelationSuggestionProviderResult(valueWrappers, suggestionByWrapper);

        } finally {
            result.close();
        }
    }

    public record ObjectTypeSuggestionProviderResult(
            @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, StatusInfo<ObjectTypesSuggestionType>> suggestionByWrapper) {

    }

    /** Creates value wrappers for each suggested object type. */
    public static @NotNull ObjectTypeSuggestionProviderResult loadSuggestionWrappers(
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
