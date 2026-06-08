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
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.StatusRowRecord;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.schema.util.task.ItemsProgressInformation;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @FunctionalInterface
    private interface StatusLoader<T> {
        List<StatusInfo<T>> load() throws Exception;
    }

    private static <T> @Nullable List<StatusInfo<T>> loadStatuses(
            @NotNull OperationResult result,
            @NotNull String message,
            @NotNull String resourceOid,
            @NotNull StatusLoader<T> loader) {
        try {
            return loader.load();
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, message + " for {}", t, resourceOid);
            return null;
        } finally {
            result.close();
        }
    }

    private static <C extends Containerable, T> @NotNull SuggestionProviderResult<C, T> emptyResult(
            @NotNull Map<PrismContainerValueWrapper<C>, StatusInfo<T>> byWrapper) {
        return new SuggestionProviderResult<>(Collections.emptyList(), byWrapper);
    }

    private static <C extends Containerable, T> void addWrapper(
            @NotNull PrismContainerValueWrapper<C> wrapper,
            @NotNull StatusInfo<T> statusInfo,
            @NotNull List<PrismContainerValueWrapper<C>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<C>, StatusInfo<T>> byWrapper) {
        wrappers.add(wrapper);
        byWrapper.put(wrapper, statusInfo);
    }

    public static @Nullable List<StatusInfo<ObjectTypesSuggestionType>> loadObjectTypeSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        return loadStatuses(
                result,
                "Couldn't load object type suggestions status",
                resourceOid,
                () -> pageBase.getSmartIntegrationService()
                        .listSuggestObjectTypesOperationStatuses(resourceOid, task, result));
    }

    public static @Nullable List<StatusInfo<AssociationsSuggestionType>> loadAssociationSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        return loadStatuses(
                result,
                "Couldn't load association suggestions status",
                resourceOid,
                () -> pageBase.getSmartIntegrationService()
                        .listSuggestAssociationsOperationStatuses(resourceOid, task, result));
    }

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

        return statuses.stream()
                .filter(s -> objectClassName.equals(s.getObjectClassName()))
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
        return loadStatuses(
                result,
                "Couldn't load correlation status",
                resourceOid,
                () -> pageBase.getSmartIntegrationService()
                        .listSuggestCorrelationOperationStatuses(resourceOid, task, result));
    }

    public static @Nullable List<StatusInfo<MappingsSuggestionType>> loadObjectTypeMappingTypeSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull MappingDirection mappingDirection,
            @Nullable ResourceObjectTypeIdentification objectTypeIdentification,
            @NotNull Task task,
            @NotNull OperationResult result) {

        boolean isInbound = mappingDirection == MappingDirection.INBOUND;
        return loadStatuses(
                result,
                "Couldn't load mapping status",
                resourceOid,
                () -> {
                    List<StatusInfo<MappingsSuggestionType>> statuses = pageBase.getSmartIntegrationService()
                            .listSuggestMappingsOperationStatuses(resourceOid, objectTypeIdentification, isInbound, task, result);
                    LOGGER.debug("Loaded mapping suggestions statuses for resource {} and object type identification {}",
                            resourceOid, objectTypeIdentification);
                    return statuses;
                });
    }

    public static @Nullable StatusInfo<MappingsSuggestionType> loadObjectTypeMappingTypeSuggestion(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @Nullable ResourceObjectTypeIdentification objectTypeIdentification,
            @NotNull MappingDirection mappingDirection,
            @NotNull Task task,
            @NotNull OperationResult result) {

        try {
            List<StatusInfo<MappingsSuggestionType>> statuses = loadObjectTypeMappingTypeSuggestions(
                    pageBase, resourceOid, mappingDirection, objectTypeIdentification, task, result);
            return singleStatusOrNull(statuses, resourceOid, result);
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Couldn't load Mapping status for {}", t, resourceOid);
            return null;
        } finally {
            result.close();
        }
    }

    private static <T> @Nullable StatusInfo<T> singleStatusOrNull(
            @Nullable List<StatusInfo<T>> statuses,
            @NotNull String resourceOid,
            @NotNull OperationResult result) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        if (statuses.size() > 1) {
            LOGGER.debug("Expected exactly one suggestion status for resource {}, but found {}.", resourceOid, statuses.size());
            result.recordException(new IllegalStateException(
                    "Expected suggestion status size to be 0 or 1, but found " + statuses.size()));
            return null;
        }
        return statuses.get(0);
    }

    public static @Nullable StatusInfo<AssociationsSuggestionType> loadAssociationTypeSuggestion(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {

        List<StatusInfo<AssociationsSuggestionType>> statuses = loadAssociationSuggestions(pageBase, resourceOid, task, result);
        return statuses == null || statuses.isEmpty() ? null : statuses.get(0);
    }

    public static @Nullable StatusInfo<CorrelationSuggestionsType> loadCorrelationTypeSuggestion(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification identification,
            @NotNull Task task,
            @NotNull OperationResult result) {

        List<StatusInfo<CorrelationSuggestionsType>> statuses = loadCorrelationTypeSuggestions(pageBase, resourceOid, task, result);
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }

        return statuses.stream()
                .filter(s -> s.getRequest() != null
                        && Objects.equals(s.getRequest().getKind(), identification.getKind())
                        && Objects.equals(s.getRequest().getIntent(), identification.getIntent()))
                .findFirst()
                .orElse(null);
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
            List<StatusInfo<CorrelationSuggestionsType>> statuses = loadCorrelationTypeSuggestions(pageBase, resourceOid, task, result);
            if (statuses == null || statuses.isEmpty()) {
                return emptyResult(byWrapper);
            }

            for (StatusInfo<CorrelationSuggestionsType> statusInfo : statuses) {
                if (!isEligible(statusInfo, rotDef) || shouldSkipNonCompleted(statusInfo, includeNonCompletedSuggestions)) {
                    continue;
                }
                addCorrelationWrappers(pageBase, rotDef, task, result, statusInfo, wrappers, byWrapper);
            }
            return new SuggestionProviderResult<>(wrappers, byWrapper);
        } catch (SchemaException e) {
            throw new IllegalStateException("Failed to wrap correlation suggestions", e);
        } finally {
            result.close();
        }
    }

    private static void addCorrelationWrappers(
            @NotNull PageBase pageBase,
            @NotNull ResourceObjectTypeDefinitionType rotDef,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull StatusInfo<CorrelationSuggestionsType> statusInfo,
            @NotNull List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<ItemsSubCorrelatorType>, StatusInfo<CorrelationSuggestionsType>> byWrapper)
            throws SchemaException {

        CorrelationSuggestionsType suggestionParent = ensureCorrelationSuggestionsPresent(statusInfo);
        @SuppressWarnings("unchecked")
        PrismContainer<CorrelationSuggestionType> container = suggestionParent.asPrismContainerValue()
                .findContainer(CorrelationSuggestionsType.F_SUGGESTION);
        if (container == null) {
            return;
        }

        PrismContainerWrapper<CorrelationSuggestionType> corrSuggestionW =
                pageBase.createItemWrapper(container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

        for (PrismContainerValueWrapper<CorrelationSuggestionType> suggestion : corrSuggestionW.getValues()) {
            addCorrelationItemsFromSuggestion(suggestion, rotDef, result, statusInfo, wrappers, byWrapper);
        }
    }

    private static void addCorrelationItemsFromSuggestion(
            @NotNull PrismContainerValueWrapper<CorrelationSuggestionType> suggestion,
            @NotNull ResourceObjectTypeDefinitionType rotDef,
            @NotNull OperationResult result,
            @NotNull StatusInfo<CorrelationSuggestionsType> statusInfo,
            @NotNull List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<ItemsSubCorrelatorType>, StatusInfo<CorrelationSuggestionsType>> byWrapper) {
        try {
            PrismContainerWrapper<CorrelationDefinitionType> corrDefW = suggestion.findContainer(
                    CorrelationSuggestionType.F_CORRELATION);
            if (corrDefW == null) {
                result.recordWarning("Suggestion without correlator definition was skipped.");
                return;
            }

            PrismContainerWrapper<ItemsSubCorrelatorType> itemsW = corrDefW.findContainer(ItemPath.create(
                    CorrelationDefinitionType.F_CORRELATORS, CompositeCorrelatorType.F_ITEMS));
            if (itemsW == null || itemsW.getValues() == null) {
                result.recordWarning("Suggestion without correlator items was skipped.");
                return;
            }

            for (PrismContainerValueWrapper<ItemsSubCorrelatorType> itemWrapper : itemsW.getValues()) {
                PrismContainerWrapper<CorrelationItemType> correlationItemWrapper = itemWrapper.findContainer(
                        ItemsSubCorrelatorType.F_ITEM);
                setupStatusIfRequiredNewMapping(correlationItemWrapper, suggestion, rotDef);
                addWrapper(itemWrapper, statusInfo, wrappers, byWrapper);
            }
        } catch (SchemaException e) {
            result.recordPartialError("Failed to wrap correlator items.", e);
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
            ResourceObjectTypeIdentification identification = ResourceObjectTypeIdentification.of(rotDef);
            StatusInfo<MappingsSuggestionType> statusInfo = loadObjectTypeMappingTypeSuggestion(
                    pageBase, resourceOid, identification, mappingDirection, task, result);

            if (!isEligible(statusInfo, rotDef) || shouldSkipNonCompleted(statusInfo, includeNonCompletedSuggestions)) {
                return emptyResult(byWrapper);
            }

            addMappingWrappers(pageBase, mappingDirection, task, result, statusInfo, wrappers, byWrapper);
            return new SuggestionProviderResult<>(wrappers, byWrapper);
        } catch (SchemaException e) {
            throw new IllegalStateException("Failed to wrap mapping suggestions", e);
        } finally {
            result.close();
        }
    }

    private static void addMappingWrappers(
            @NotNull PageBase pageBase,
            @NotNull MappingDirection mappingDirection,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull StatusInfo<MappingsSuggestionType> statusInfo,
            @NotNull List<PrismContainerValueWrapper<MappingType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<MappingType>, StatusInfo<MappingsSuggestionType>> byWrapper)
            throws SchemaException {

        MappingsSuggestionType suggestionParent = ensureMappingsSuggestionsPresent(statusInfo, mappingDirection);
        @SuppressWarnings("unchecked")
        PrismContainer<AttributeMappingsSuggestionType> container = suggestionParent.asPrismContainerValue()
                .findContainer(MappingsSuggestionType.F_ATTRIBUTE_MAPPINGS);
        if (container == null) {
            return;
        }

        PrismContainerWrapper<AttributeMappingsSuggestionType> attrMappingsWrapper = pageBase.createItemWrapper(
                container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

        for (PrismContainerValueWrapper<AttributeMappingsSuggestionType> suggestion : attrMappingsWrapper.getValues()) {
            addMappingsFromSuggestion(pageBase, mappingDirection, result, statusInfo, suggestion, wrappers, byWrapper);
        }
    }

    private static void addMappingsFromSuggestion(
            @NotNull PageBase pageBase,
            @NotNull MappingDirection mappingDirection,
            @NotNull OperationResult result,
            @NotNull StatusInfo<MappingsSuggestionType> statusInfo,
            @NotNull PrismContainerValueWrapper<AttributeMappingsSuggestionType> suggestion,
            @NotNull List<PrismContainerValueWrapper<MappingType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<MappingType>, StatusInfo<MappingsSuggestionType>> byWrapper) {
        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> defWrapper = suggestion.findContainer(
                    AttributeMappingsSuggestionType.F_DEFINITION);
            if (defWrapper == null || defWrapper.getValues().isEmpty()) {
                result.recordWarning("Suggestion without resource attribute definition skipped");
                return;
            }

            for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> defItemWrapper : defWrapper.getValues()) {
                addMappingsFromAttributeDefinition(pageBase, mappingDirection, statusInfo, defItemWrapper, wrappers, byWrapper, result);
            }
        } catch (SchemaException e) {
            result.recordPartialError("Failed to wrap mapping items.", e);
        }
    }

    private static void addMappingsFromAttributeDefinition(
            @NotNull PageBase pageBase,
            @NotNull MappingDirection mappingDirection,
            @NotNull StatusInfo<MappingsSuggestionType> statusInfo,
            @NotNull PrismContainerValueWrapper<ResourceAttributeDefinitionType> defItemWrapper,
            @NotNull List<PrismContainerValueWrapper<MappingType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<MappingType>, StatusInfo<MappingsSuggestionType>> byWrapper,
            @NotNull OperationResult result)
            throws SchemaException {

        PrismContainerWrapper<MappingType> mappingWrapper = defItemWrapper.findContainer(mappingDirection.getContainerName());
        if (mappingWrapper == null || mappingWrapper.getValues().isEmpty()) {
            result.recordWarning("Suggestion without mappings skipped");
            return;
        }

        PrismPropertyDefinition<Object> refDef = defItemWrapper.getDefinition().findPropertyDefinition(
                ResourceAttributeDefinitionType.F_REF);

        for (PrismContainerValueWrapper<MappingType> mappingVw : mappingWrapper.getValues()) {
            MappingUtils.createVirtualItemInMapping(
                    mappingVw,
                    defItemWrapper,
                    refDef,
                    pageBase,
                    ResourceAttributeDefinitionType.F_REF,
                    mappingDirection);
            addWrapper(mappingVw, statusInfo, wrappers, byWrapper);
        }
    }

    public static boolean isNotCompletedSuggestion(@Nullable StatusInfo<?> statusInfo) {
        if (statusInfo == null) {
            return false;
        }

        OperationResultStatusType operationStatus = statusInfo.getStatus();
        return !statusInfo.isComplete()
                || OperationResultStatusType.IN_PROGRESS.equals(operationStatus)
                || OperationResultStatusType.FATAL_ERROR.equals(operationStatus);
    }

    public static boolean isSuggestionExists(@Nullable StatusInfo<?> statusInfo) {
        return statusInfo != null;
    }

    private static boolean shouldSkipNonCompleted(@Nullable StatusInfo<?> statusInfo, boolean includeNonCompletedSuggestions) {
        return !includeNonCompletedSuggestions && isNotCompletedSuggestion(statusInfo);
    }

    private static boolean isEligible(
            @Nullable StatusInfo<?> si,
            @NotNull ResourceObjectTypeDefinitionType rotDef) {
        return si != null
                && si.getRequest() != null
                && si.getStatus() != OperationResultStatusType.NOT_APPLICABLE
                && matchKindAndIntent(rotDef, si.getRequest());
    }

    private static @NotNull CorrelationSuggestionsType ensureCorrelationSuggestionsPresent(
            @NotNull StatusInfo<CorrelationSuggestionsType> si) {
        CorrelationSuggestionsType out = si.getResult();
        if (out != null) {
            return out;
        }

        CorrelationDefinitionType def = new CorrelationDefinitionType();
        CompositeCorrelatorType comp = new CompositeCorrelatorType();
        comp.getItems().add(new ItemsSubCorrelatorType());
        def.setCorrelators(comp);

        CorrelationSuggestionType suggestion = new CorrelationSuggestionType();
        suggestion.setCorrelation(def);

        out = new CorrelationSuggestionsType();
        out.getSuggestion().add(suggestion);
        return out;
    }

    private static @NotNull MappingsSuggestionType ensureMappingsSuggestionsPresent(
            @NotNull StatusInfo<MappingsSuggestionType> si,
            @NotNull MappingDirection mappingDirection) {
        MappingsSuggestionType out = si.getResult();
        if (out != null) {
            return out;
        }

        ResourceAttributeDefinitionType attrDef = new ResourceAttributeDefinitionType();
        if (mappingDirection == MappingDirection.INBOUND) {
            attrDef.getInbound().add(new InboundMappingType());
        } else if (mappingDirection == MappingDirection.OUTBOUND) {
            attrDef.setOutbound(new MappingType());
        }

        AttributeMappingsSuggestionType def = new AttributeMappingsSuggestionType();
        def.setDefinition(attrDef);

        out = new MappingsSuggestionType();
        out.getAttributeMappings().add(def);
        return out;
    }

    private static boolean matchKindAndIntent(
            @NotNull ResourceObjectTypeDefinitionType resourceObjectTypeDefinition,
            @NotNull BasicResourceObjectSetType request) {
        return Objects.equals(request.getKind(), resourceObjectTypeDefinition.getKind())
                && Objects.equals(request.getIntent(), resourceObjectTypeDefinition.getIntent());
    }

    private static void setupStatusIfRequiredNewMapping(
            @Nullable PrismContainerWrapper<CorrelationItemType> container,
            @NotNull PrismContainerValueWrapper<CorrelationSuggestionType> suggestion,
            @NotNull ResourceObjectTypeDefinitionType rotDef) {

        if (container == null || container.getValues() == null) {
            return;
        }

        PrismContainerWrapper<ResourceAttributeDefinitionType> attributeDefW;
        try {
            attributeDefW = suggestion.findContainer(CorrelationSuggestionType.F_ATTRIBUTES);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't find attributes in correlation suggestion {}", e, suggestion);
            return;
        }

        if (attributeDefW == null || attributeDefW.getValues() == null) {
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

        Set<ItemPath> newTargetPaths = collectMappingTargets(suggestedAttributes).stream()
                .filter(path -> collectMappingTargets(rotDef.getAttribute()).stream().noneMatch(path::equivalent))
                .collect(Collectors.toSet());

        markSuggestedAttributes(suggestedAttributesW, newTargetPaths);
        markCorrelationItems(container.getValues(), newTargetPaths);
    }

    private static void markSuggestedAttributes(
            @NotNull List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> attributes,
            @NotNull Set<ItemPath> newTargetPaths) {
        attributes.forEach(valueWrapper -> {
            Set<ItemPath> itemPaths = collectMappingTargets(Collections.singletonList(valueWrapper.getRealValue()));
            valueWrapper.setStatus(hasEquivalentPath(itemPaths, newTargetPaths) ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED);
        });
    }

    private static void markCorrelationItems(
            @NotNull List<PrismContainerValueWrapper<CorrelationItemType>> correlationItems,
            @NotNull Set<ItemPath> newTargetPaths) {
        correlationItems.stream()
                .filter(Objects::nonNull)
                .forEach(item -> item.setStatus(
                        hasEquivalentPath(Collections.singleton(refPath(item)), newTargetPaths)
                                ? ValueStatus.ADDED
                                : ValueStatus.NOT_CHANGED));
    }

    private static boolean hasEquivalentPath(@NotNull Collection<ItemPath> candidates, @NotNull Set<ItemPath> expected) {
        return !expected.isEmpty()
                && candidates.stream()
                .filter(Objects::nonNull)
                .anyMatch(candidate -> expected.stream().anyMatch(candidate::equivalent));
    }

    private static @Nullable ItemPath refPath(@NotNull PrismContainerValueWrapper<CorrelationItemType> item) {
        return Optional.ofNullable(item.getRealValue())
                .map(CorrelationItemType::getRef)
                .map(ItemPathType::getItemPath)
                .orElse(null);
    }

    public static @NotNull List<ResourceAttributeDefinitionType> collectRequiredResourceAttributeDefs(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull PrismContainerValueWrapper<CorrelationSuggestionType> parentSuggestionW) {
        List<ResourceAttributeDefinitionType> attributes = new ArrayList<>();
        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> wrapper =
                    parentSuggestionW.findContainer(CorrelationSuggestionType.F_ATTRIBUTES);

            if (wrapper != null && wrapper.getValues() != null) {
                wrapper.getValues().stream()
                        .filter(val -> val != null && val.getRealValue() != null && val.getStatus() == ValueStatus.ADDED)
                        .map(PrismContainerValueWrapper::getRealValue)
                        .forEach(attributes::add);
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find attributes container in {}", parentSuggestionW, e);
            pageBase.error("Couldn't process correlation suggestion.");
            target.add(pageBase.getFeedbackPanel().getParent());
        }
        return attributes;
    }

    public static @NotNull Set<AdditionalCorrelationItemMappingType> collectAdditionalMappingsIfSuggestion(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> itemsW) {

        Map<String, AdditionalCorrelationItemMappingType> merged = new LinkedHashMap<>();
        itemsW.stream()
                .filter(Objects::nonNull)
                .map(itemW -> itemW.getParentContainerValue(CorrelationSuggestionType.class))
                .filter(suggestionW -> suggestionW != null && suggestionW.getRealValue() != null)
                .forEach(suggestionW -> mergeAdditionalMappingsFromSuggestion(pageBase, target, suggestionW, merged));

        //noinspection unchecked
        merged.values().forEach(m -> WebPrismUtil.cleanupEmptyContainerValue(m.asPrismContainerValue()));
        return new HashSet<>(merged.values());
    }

    private static void mergeAdditionalMappingsFromSuggestion(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull PrismContainerValueWrapper<CorrelationSuggestionType> suggestionW,
            @NotNull Map<String, AdditionalCorrelationItemMappingType> merged) {

        for (ResourceAttributeDefinitionType attrDef : collectRequiredResourceAttributeDefs(pageBase, target, suggestionW)) {
            if (attrDef == null || attrDef.getRef() == null) {
                continue;
            }

            var ref = attrDef.getRef().clone();
            String key = ref.getItemPath().toString();
            AdditionalCorrelationItemMappingType acc = merged.computeIfAbsent(key, k -> {
                AdditionalCorrelationItemMappingType m = new AdditionalCorrelationItemMappingType();
                m.setRef(ref);
                return m;
            });

            attrDef.getInbound().stream()
                    .filter(Objects::nonNull)
                    .map(InboundMappingType::clone)
                    .forEach(acc.getInbound()::add);
        }
    }

    private static @NotNull Set<ItemPath> collectMappingTargets(@Nullable List<ResourceAttributeDefinitionType> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptySet();
        }

        return attributes.stream()
                .filter(Objects::nonNull)
                .flatMap(attr -> Optional.ofNullable(attr.getInbound()).map(List::stream).orElseGet(Stream::empty))
                .filter(Objects::nonNull)
                .map(MappingType::getTarget)
                .filter(Objects::nonNull)
                .map(VariableBindingDefinitionType::getPath)
                .filter(Objects::nonNull)
                .map(ItemPathType::getItemPath)
                .collect(Collectors.toSet());
    }

    public static @NotNull SuggestionProviderResult<ResourceObjectTypeDefinitionType, ObjectTypesSuggestionType> loadObjectTypeSuggestionWrappers(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, StatusInfo<ObjectTypesSuggestionType>> byWrapper = new HashMap<>();
        List<StatusInfo<ObjectTypesSuggestionType>> suggestions = loadObjectTypeSuggestions(pageBase, resourceOid, task, result);
        if (suggestions == null || suggestions.isEmpty()) {
            return emptyResult(byWrapper);
        }

        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> wrappers = new ArrayList<>();
        suggestions.stream()
                .filter(Objects::nonNull)
                .forEach(si -> addObjectTypeWrappers(pageBase, task, result, si, wrappers, byWrapper));
        return new SuggestionProviderResult<>(wrappers, byWrapper);
    }

    private static void addObjectTypeWrappers(
            @NotNull PageBase pageBase,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull StatusInfo<ObjectTypesSuggestionType> si,
            @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, StatusInfo<ObjectTypesSuggestionType>> byWrapper) {

        ObjectTypesSuggestionType suggestion = objectTypeSuggestionToWrap(si);
        if (suggestion == null) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            PrismContainer<ResourceObjectTypeDefinitionType> container =
                    suggestion.asPrismContainerValue().findContainer(ObjectTypesSuggestionType.F_OBJECT_TYPE);

            PrismContainerWrapper<ResourceObjectTypeDefinitionType> wrapper = pageBase.createItemWrapper(
                    container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));
            wrapper.getValues().forEach(value -> addWrapper(value, si, wrappers, byWrapper));
        } catch (SchemaException e) {
            throw new IllegalStateException("Failed to wrap object type suggestions", e);
        }
    }

    private static @Nullable ObjectTypesSuggestionType objectTypeSuggestionToWrap(
            @NotNull StatusInfo<ObjectTypesSuggestionType> si) {
        if (si.getStatus() == OperationResultStatusType.NOT_APPLICABLE) {
            return null;
        }
        if (si.getStatus() == OperationResultStatusType.SUCCESS
                && (si.getResult() == null || si.getResult().getObjectType().isEmpty())) {
            return null;
        }
        if (si.getResult() == null || si.getStatus() == OperationResultStatusType.IN_PROGRESS) {
            ObjectTypesSuggestionType tmp = new ObjectTypesSuggestionType();
            tmp.getObjectType().add(new ResourceObjectTypeDefinitionType());
            return tmp;
        }
        return si.getResult();
    }

    public static @NotNull SuggestionProviderResult<ShadowAssociationTypeDefinitionType, AssociationsSuggestionType> loadAssociationTypeSuggestionWrappers(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, StatusInfo<AssociationsSuggestionType>> byWrapper = new HashMap<>();
        List<StatusInfo<AssociationsSuggestionType>> suggestions = loadAssociationSuggestions(pageBase, resourceOid, task, result);
        if (suggestions == null || suggestions.isEmpty() || suggestions.get(0).getResult() == null) {
            return emptyResult(byWrapper);
        }

        List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> wrappers = new ArrayList<>();
        suggestions.stream()
                .filter(Objects::nonNull)
                .forEach(si -> addAssociationWrappers(pageBase, task, result, si, wrappers, byWrapper));
        return new SuggestionProviderResult<>(wrappers, byWrapper);
    }

    private static void addAssociationWrappers(
            @NotNull PageBase pageBase,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull StatusInfo<AssociationsSuggestionType> si,
            @NotNull List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, StatusInfo<AssociationsSuggestionType>> byWrapper) {

        AssociationsSuggestionType suggestion = associationSuggestionToWrap(si);
        if (suggestion == null) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            PrismContainer<AssociationSuggestionType> container =
                    suggestion.asPrismContainerValue().findContainer(AssociationsSuggestionType.F_ASSOCIATION);

            PrismContainerWrapper<AssociationSuggestionType> wrapper = pageBase.createItemWrapper(
                    container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

            for (PrismContainerValueWrapper<AssociationSuggestionType> value : wrapper.getValues()) {
                addAssociationDefinitions(value, si, result, wrappers, byWrapper);
            }
        } catch (SchemaException e) {
            throw new IllegalStateException("Failed to wrap object type suggestions", e);
        }
    }

    private static @Nullable AssociationsSuggestionType associationSuggestionToWrap(
            @NotNull StatusInfo<AssociationsSuggestionType> si) {
        if (si.getStatus() == OperationResultStatusType.NOT_APPLICABLE) {
            return null;
        }
        if (si.getStatus() == OperationResultStatusType.SUCCESS
                && (si.getResult() == null || si.getResult().getAssociation().isEmpty())) {
            return null;
        }
        if (si.getResult() == null || si.getResult().getAssociation().isEmpty()
                || si.getStatus() == OperationResultStatusType.IN_PROGRESS) {
            AssociationsSuggestionType tmp = new AssociationsSuggestionType();
            tmp.getAssociation().add(new AssociationSuggestionType());
            return tmp;
        }
        return si.getResult();
    }

    private static void addAssociationDefinitions(
            @NotNull PrismContainerValueWrapper<AssociationSuggestionType> value,
            @NotNull StatusInfo<AssociationsSuggestionType> si,
            @NotNull OperationResult result,
            @NotNull List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> wrappers,
            @NotNull Map<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, StatusInfo<AssociationsSuggestionType>> byWrapper)
            throws SchemaException {

        PrismContainerWrapper<ShadowAssociationTypeDefinitionType> assocDefW = value.findContainer(
                AssociationSuggestionType.F_DEFINITION);
        if (assocDefW == null || assocDefW.getValues() == null || assocDefW.getValues().isEmpty()) {
            result.recordWarning("Association suggestion without definition was skipped.");
            return;
        }
        if (assocDefW.getValues().size() > 1) {
            result.recordWarning("Association suggestion with multiple definitions was skipped.");
            return;
        }

        assocDefW.getValues().forEach(v -> addWrapper(v, si, wrappers, byWrapper));
    }

    public static @NotNull List<StatusRowRecord> buildStatusRows(
            @NotNull PageBase pageBase,
            @Nullable StatusInfo<?> suggestion,
            boolean rejectEmptyProgress) {

        if (suggestion != null && suggestion.getStatus() == OperationResultStatusType.FATAL_ERROR) {
            return List.of(new StatusRowRecord(
                    pageBase.createStringResource("SmartGeneratingDto.status.failed"),
                    ActivityProgressInformation.RealizationState.UNKNOWN,
                    suggestion));
        }

        if (rejectEmptyProgress && (suggestion == null || suggestion.getProgressInformation() == null)) {
            return List.of(new StatusRowRecord(
                    pageBase.createStringResource("SmartGeneratingDto.null"),
                    ActivityProgressInformation.RealizationState.UNKNOWN,
                    suggestion));
        }

        if (suggestion == null || suggestion.getProgressInformation() == null) {
            return Collections.emptyList();
        }

        ActivityProgressInformation progressInformation = suggestion.getProgressInformation();
        List<ActivityProgressInformation> children = progressInformation.getChildren();
        if (!rejectEmptyProgress && children.isEmpty()) {
            return Collections.emptyList();
        }

        if (children.isEmpty()) {
            ActivityProgressInformation.RealizationState state = progressInformation.getRealizationState();
            return List.of(new StatusRowRecord(
                    pageBase.createStringResource("SmartGeneratingDto." + state),
                    state,
                    suggestion));
        }

        return children.stream()
                .map(child -> new StatusRowRecord(
                        buildProgressMessageModel(pageBase, child.getActivityIdentifier(), child),
                        child.getRealizationState(),
                        suggestion))
                .collect(Collectors.toList());
    }

    protected static IModel<String> buildProgressMessageModel(
            @NotNull PageBase pageBase,
            @Nullable String operationKey,
            @NotNull ActivityProgressInformation child) {

        if ("mappingsSuggestion".equals(operationKey)) {
            ActivityProgressInformation mappingsSuggestion = child.getChild(operationKey);
            ItemsProgressInformation itemsProgress = mappingsSuggestion != null ? mappingsSuggestion.getItemsProgress() : null;

            if (itemsProgress != null) {
                int progress = itemsProgress.getProgress();
                int expected = itemsProgress.getExpectedProgress();
                String percent = expected == 0 ? "0%" : String.format("%.0f%%", (progress * 100.0) / expected);
                return pageBase.createStringResource("Activity.explanation." + operationKey, percent);
            }

            return pageBase.createStringResource("Activity.explanation.text." + operationKey);
        }

        return pageBase.createStringResource("Activity.explanation." + operationKey);
    }

    public static @NotNull String extractEfficiencyFromSuggestedCorrelationItemWrapper(
            @NotNull PrismContainerValueWrapper<ItemsSubCorrelatorType> valueWrapper) {
        PrismContainerValueWrapper<CorrelationSuggestionType> parent = valueWrapper.getParentContainerValue(
                CorrelationSuggestionType.class);
        if (parent == null || parent.getRealValue() == null) {
            return "-";
        }

        Double quality = parent.getRealValue().getQuality();
        return quality != null && quality != -1
                ? BigDecimal.valueOf(quality).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.FLOOR).toPlainString()
                : "-";
    }

    public static @Nullable Float extractEfficiencyFromSuggestedMappingItemWrapper(
            @NotNull PrismContainerValueWrapper<MappingType> valueWrapper) {
        PrismContainerValueWrapper<AttributeMappingsSuggestionType> parent = valueWrapper.getParentContainerValue(
                AttributeMappingsSuggestionType.class);
        return parent != null && parent.getRealValue() != null && parent.getRealValue().getExpectedQuality() != null
                ? parent.getRealValue().getExpectedQuality() * 100
                : null;
    }

    public static int computeSuggestedObjectsCount(@Nullable StatusInfo<?> statusInfo) {
        if (statusInfo == null || statusInfo.getResult() == null) {
            return 0;
        }

        Object result = statusInfo.getResult();
        if (result instanceof MappingsSuggestionType mappings) {
            return safeSize(mappings.getAttributeMappings());
        } else if (result instanceof ObjectTypesSuggestionType objectTypes) {
            return safeSize(objectTypes.getObjectType());
        } else if (result instanceof CorrelationSuggestionsType correlations) {
            return safeSize(correlations.getSuggestion());
        } else if (result instanceof AssociationsSuggestionType associations) {
            return safeSize(associations.getAssociation());
        }
        return 0;
    }

    private static int safeSize(@Nullable List<?> list) {
        return list != null ? list.size() : 0;
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
        pageBase.showMainPopup(helpInfoPanel, target);
    }

    public static void handleSuggestionSuspendResumeOperation(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<?> statusInfo,
            @NotNull Task task,
            @NotNull OperationResult result) {
        if (statusInfo.getStatus() == OperationResultStatusType.FATAL_ERROR) {
            return;
        }

        if (statusInfo.isSuspended()) {
            resumeSuggestionTask(pageBase, statusInfo, task, result);
        } else {
            suspendSuggestionTask(pageBase, statusInfo, task, result);
        }
    }
}
