/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.smart.api.ServiceClient.Method.SUGGEST_OBJECT_TYPES;
import static com.evolveum.midpoint.util.MiscUtil.nullIfEmpty;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.smart.impl.scoring.ObjectTypeFiltersValidator;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiSuggestObjectTypesRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiSuggestObjectTypesResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiSuggestedObjectTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiValidationErrorFeedbackEntryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Implements "suggest object types" operation.
 */
class ObjectTypesSuggestionOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypesSuggestionOperation.class);

    private final OperationContext ctx;
    private final ObjectTypeFiltersValidator filtersValidator;
    private final QName typeName;

    ObjectTypesSuggestionOperation(OperationContext context, ObjectTypeFiltersValidator filtersValidator) {
        this.ctx = context;
        this.filtersValidator = filtersValidator;
        this.typeName = ctx.objectClassDefinition.getTypeName();
    }

    /**
     * Calls the smart-integration service to obtain suggestions of resource object types for the current
     * resource/object class and converts them into a {@link ObjectTypesSuggestionType} consumable by midPoint.
     *
     * If validation fails on the first attempt, we build structured per-object-type validation feedback
     * and send it back to the service to guide a second attempt. If issues persist, partial results are used.
     */
    ObjectTypesSuggestionType suggestObjectTypes(
            ShadowObjectClassStatisticsType shadowObjectClassStatistics,
            OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        Collection<ObjectTypeWithFilters> suggestedObjectTypes = null;
        List<SiValidationErrorFeedbackEntryType> validationFeedback = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            var siResponse = generateObjectTypeSuggestion(shadowObjectClassStatistics, validationFeedback);
            try {
                suggestedObjectTypes = parseAndValidateFilters(siResponse.getObjectType(), parentResult);
                break;
            } catch (SuggestObjectTypesValidationException e) {
                validationFeedback = e.getValidationFeedback();
                if (attempt == 1) {
                    LOGGER.warn("Validation issues found on attempt 1; retrying with structured feedback ({} entries).",
                            validationFeedback != null ? validationFeedback.size() : 0);
                } else {
                    LOGGER.warn("Validation issues persist after retry; using partial result.");
                    suggestedObjectTypes = e.getObjectTypesWithFiltersAndErrors();
                }
            }
        }

        var response = new ObjectTypesSuggestionType();
        var typeIdsSeen = new HashSet<ResourceObjectTypeIdentification>();
        for (var objectTypeWithFilters : suggestedObjectTypes) {
            var siObjectType = objectTypeWithFilters.suggestedObjectType();
            var delineation = new ResourceObjectTypeDelineationType()
                    .objectClass(typeName);
            objectTypeWithFilters.filters().forEach(delineation::filter);
            AiUtil.markAsAiProvided(delineation, ResourceObjectTypeDelineationType.F_FILTER);
            if (objectTypeWithFilters.filterError() != null) {
                AiUtil.markAsInvalid(delineation, objectTypeWithFilters.filterError(), ResourceObjectTypeDelineationType.F_FILTER);
            }

            var baseCtx = objectTypeWithFilters.baseCtx();
            if (baseCtx != null) {
                delineation.baseContext(new ResourceObjectReferenceType()
                        .objectClass(baseCtx.classQName())
                        .filter(baseCtx.filter()));
                AiUtil.markAsAiProvided(delineation, ResourceObjectTypeDelineationType.F_BASE_CONTEXT);
                if (objectTypeWithFilters.baseCtxError() != null) {
                    AiUtil.markAsInvalid(delineation, objectTypeWithFilters.baseCtxError(), ResourceObjectTypeDelineationType.F_BASE_CONTEXT);
                }
            }

            var typeId = ResourceObjectTypeIdentification.of(
                    ShadowKindType.fromValue(siObjectType.getKind()),
                    SchemaConstants.INTENT_UNKNOWN.equals(siObjectType.getIntent()) // temporary hack
                            ? SchemaConstants.INTENT_DEFAULT : siObjectType.getIntent());
            if (!typeIdsSeen.add(typeId)) {
                LOGGER.warn("Duplicate typeId {}, ignoring the suggestion:\n{}",
                        typeId, PrismContext.get().xmlSerializer().serializeRealValue(
                                siObjectType, SiSuggestObjectTypesResponseType.F_OBJECT_TYPE));
                continue;
            }
            var displayName = siObjectType.getDisplayName();
            var description = siObjectType.getDescription();
            var objectType = new ResourceObjectTypeDefinitionType()
                    .kind(typeId.getKind())
                    .intent(typeId.getIntent())
                    .displayName(displayName)
                    .description(description)
                    .delineation(delineation);
            AiUtil.markAsAiProvided(
                    objectType,
                    ResourceObjectTypeDefinitionType.F_KIND,
                    ResourceObjectTypeDefinitionType.F_INTENT,
                    ResourceObjectTypeDefinitionType.F_DISPLAY_NAME,
                    ResourceObjectTypeDefinitionType.F_DESCRIPTION);
            response.getObjectType().add(objectType);
        }
        LOGGER.debug("Suggested object types for {}:\n{}", ctx.objectClassDefinition, response.debugDump(1));
        return response;
    }

    private Collection<ObjectTypeWithFilters> parseAndValidateFilters(
            List<SiSuggestedObjectTypeType> objectTypes,
            OperationResult parentResult)
            throws SuggestObjectTypesValidationException, CommunicationException, ConfigurationException, ObjectNotFoundException {

        final List<ObjectTypeWithFilters> objectTypesWithFilters = new ArrayList<>();
        boolean hasAnyErrors = false;

        for (final SiSuggestedObjectTypeType objectType : objectTypes) {
            List<SearchFilterType> filters = new ArrayList<>();
            ParsedBaseContext baseCtx = null;
            String filterError = null;
            String baseCtxError = null;

            if (objectType.getFilter() != null && !objectType.getFilter().isEmpty()) {
                try {
                    filters = parseObjectTypeFilters(objectType, ctx.objectClassDefinition.getPrismObjectDefinition());
                    filtersValidator.testObjectTypeFilter(ctx.resource.getOid(), typeName, filters, ctx.task, parentResult);
                } catch (SchemaException | ExpressionEvaluationException | SecurityViolationException e) {
                    LOGGER.warn("Failed validating suggested object type (kind={}, intent={}, displayName={}) for object class {}. Filters: {}",
                            objectType.getKind(), objectType.getIntent(), objectType.getDisplayName(), typeName, objectType.getFilter(), e);
                    filterError = e.getMessage();
                    hasAnyErrors = true;
                }
            }
            if (objectType.getBaseContextObjectClassName() != null || objectType.getBaseContextFilter() != null) {
                try {
                    baseCtx = parseBaseContext(objectType.getBaseContextObjectClassName(), objectType.getBaseContextFilter());
                    filtersValidator.testBaseContextFilter(ctx.resource.getOid(), baseCtx.classQName(), baseCtx.filter(), ctx.task, parentResult);
                } catch (SchemaException | ExpressionEvaluationException | SecurityViolationException | IllegalStateException e) {
                    LOGGER.warn("Failed validating base context for suggested object type (kind={}, intent={}, displayName={}). Base context objectClass={}, filter={}",
                            objectType.getKind(), objectType.getIntent(), objectType.getDisplayName(),
                            typeName, objectType.getBaseContextFilter(), e);
                    baseCtxError = e.getMessage();
                    hasAnyErrors = true;
                }
            }

            objectTypesWithFilters.add(new ObjectTypeWithFilters(objectType, filters, baseCtx, filterError, baseCtxError));
        }

        if (hasAnyErrors) {
            throw new SuggestObjectTypesValidationException(objectTypesWithFilters);
        }
        return objectTypesWithFilters;
    }

    private List<SearchFilterType> parseObjectTypeFilters(
            SiSuggestedObjectTypeType objectType,
            PrismObjectDefinition<ShadowType> shadowObjectDef) throws SchemaException {
        final List<SearchFilterType> filters = new ArrayList<>();
        for (String filterString : objectType.getFilter()) {
            filters.add(parseAndSerializeFilter(filterString, shadowObjectDef));
        }
        return filters;
    }

    private ParsedBaseContext parseBaseContext(
            String baseContextClassLocalName,
            String baseContextFilterString) throws SchemaException, IllegalStateException {
        stateCheck(baseContextClassLocalName != null,
                "Base context class name must be set if base context filter is set");
        stateCheck(baseContextFilterString != null,
                "Base context filter must be set if base context class name is set");
        QName baseContextClassQName = new QName(NS_RI, baseContextClassLocalName);
        var baseContextObjectDef = ctx.resourceSchema.findObjectClassDefinitionRequired(baseContextClassQName);
        var baseContextFilter = parseAndSerializeFilter(baseContextFilterString, baseContextObjectDef.getPrismObjectDefinition());
        return new ParsedBaseContext(baseContextClassQName, baseContextFilter);
    }

    private @NotNull SiSuggestObjectTypesResponseType generateObjectTypeSuggestion(
            ShadowObjectClassStatisticsType shadowObjectClassStatistics,
            @Nullable List<SiValidationErrorFeedbackEntryType> validationFeedback) throws SchemaException {
        var siRequest = new SiSuggestObjectTypesRequestType()
                .schema(ResourceObjectClassSchemaSerializer.serialize(ctx.objectClassDefinition, ctx.resource))
                .statistics(shadowObjectClassStatistics);
        if (validationFeedback != null && !validationFeedback.isEmpty()) {
            siRequest.getValidationErrorFeedback().addAll(validationFeedback);
        }

        var siResponse = ctx.serviceClient.invoke(SUGGEST_OBJECT_TYPES, siRequest, SiSuggestObjectTypesResponseType.class);
        stripBlankStrings(siResponse);
        return siResponse;
    }

    // FIXME we should fix prism parsing in midPoint to avoid this
    private void stripBlankStrings(SiSuggestObjectTypesResponseType response) {
        for (var objectType : response.getObjectType()) {
            objectType.getFilter().removeIf(f -> StringUtils.isBlank(f));
            objectType.setBaseContextObjectClassName(nullIfEmpty(objectType.getBaseContextObjectClassName()));
            objectType.setBaseContextFilter(nullIfEmpty(objectType.getBaseContextFilter()));
        }
    }

    private static SearchFilterType parseAndSerializeFilter(
            String filterString, PrismObjectDefinition<ShadowType> shadowObjectDef)
            throws SchemaException {
        LOGGER.trace("Parsing filter: {}", filterString);
        try {
            var parsedFilter = PrismContext.get().createQueryParser().parseFilter(shadowObjectDef, filterString);
            return PrismContext.get().querySerializer().serialize(parsedFilter).toSearchFilterType();
        } catch (Exception e) {
            throw new SchemaException("Cannot process suggested filter (%s): %s".formatted(filterString, e.getMessage()), e);
        }
    }

    private record ObjectTypeWithFilters(
            SiSuggestedObjectTypeType suggestedObjectType,
            Collection<SearchFilterType> filters,
            ParsedBaseContext baseCtx,
            @Nullable String filterError,
            @Nullable String baseCtxError) {}

    private record ParsedBaseContext(QName classQName, SearchFilterType filter) {}

    /**
     * Custom exception carrying aggregated validation context (structured feedback for the microservice)
     * and partial results to guide a retry and/or allow building a result even when some entries failed.
     */
    static class SuggestObjectTypesValidationException extends SchemaException {
        private final Collection<ObjectTypeWithFilters> objectTypesWithFiltersAndErrors;

        SuggestObjectTypesValidationException(Collection<ObjectTypeWithFilters> validObjectTypes) {
            super("Some suggested object types failed validation.");
            this.objectTypesWithFiltersAndErrors = validObjectTypes;
        }

        Collection<ObjectTypeWithFilters> getObjectTypesWithFiltersAndErrors() { return objectTypesWithFiltersAndErrors; }

        /**
         * Builds structured validation feedback for the microservice retry.
         * Each entry contains the original suggested object type and all related error messages.
         */
        List<SiValidationErrorFeedbackEntryType> getValidationFeedback() {
            List<SiValidationErrorFeedbackEntryType> feedback = new ArrayList<>();
            for (ObjectTypeWithFilters otwf : objectTypesWithFiltersAndErrors) {
                var entry = new SiValidationErrorFeedbackEntryType()
                        .objectType(otwf.suggestedObjectType());
                if (otwf.filterError() != null) {
                    entry.filterErrors(otwf.filterError());
                }
                if (otwf.baseCtxError() != null) {
                    entry.filterErrors(otwf.baseCtxError());
                }
                feedback.add(entry);
            }
            return feedback;
        }
    }

}
