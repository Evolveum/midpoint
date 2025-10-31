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
import com.evolveum.midpoint.smart.impl.scoring.ObjectTypesQualityAssessor;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
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
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Implements "suggest object types" operation.
 */
class ObjectTypesSuggestionOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypesSuggestionOperation.class);

    private final OperationContext ctx;
    private final ObjectTypesQualityAssessor qualityAssessor;

    ObjectTypesSuggestionOperation(OperationContext context, ObjectTypesQualityAssessor qualityAssessor) {
        this.ctx = context;
        this.qualityAssessor = qualityAssessor;
    }

    /**
     * Calls the smart-integration service to obtain suggestions of resource object types for the current
     * resource/object class and converts them into a {@link ObjectTypesSuggestionType} consumable by midPoint.
     *
     * If validation fails on the first attempt, the original filters and aggregated error log are sent back to the
     * service to guide a second attempt. If issues persist, partial results are used.
     */
    ObjectTypesSuggestionType suggestObjectTypes(
            OperationResult parentResult,
            ShadowObjectClassStatisticsType shadowObjectClassStatistics)
            throws SchemaException {
        Collection<ObjectTypeWithFilters> suggestedObjectTypes = null;
        String errorLog = null;
        String previousScript = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            var siResponse = generateObjectTypeSuggestion(shadowObjectClassStatistics, errorLog, previousScript);
            try {
                suggestedObjectTypes = parseAndValidateFilters(siResponse.getObjectType(), parentResult);
                break;
            } catch (SuggestObjectTypesValidationException e) {
                previousScript = e.getOriginalFilters();
                errorLog = e.getErrorLogs();
                if (attempt == 1) {
                    LOGGER.warn("Validation issues found on attempt 1; retrying with feedback.");
                } else {
                    LOGGER.warn("Validation issues persist after retry; using partial result.");
                    suggestedObjectTypes = e.getPartialResult();
                }
            }
        }

        var response = new ObjectTypesSuggestionType();
        var typeIdsSeen = new HashSet<ResourceObjectTypeIdentification>();
        for (var objectTypeWithFilters : suggestedObjectTypes) {
            var siObjectType = objectTypeWithFilters.suggestedObjectType();
            var delineation =
                    new ResourceObjectTypeDelineationType()
                            .objectClass(ctx.objectClassDefinition.getTypeName());
            objectTypeWithFilters.filters().forEach(delineation::filter);
            AiUtil.markAsAiProvided(delineation, ResourceObjectTypeDelineationType.F_FILTER);

            var baseContextClassQName = objectTypeWithFilters.baseContextClassQName();
            var baseContextFilter = objectTypeWithFilters.baseContextFilter();
            if (baseContextClassQName != null && baseContextFilter != null) {
                delineation.baseContext(new ResourceObjectReferenceType()
                        .objectClass(baseContextClassQName)
                        .filter(baseContextFilter));
                AiUtil.markAsAiProvided(delineation, ResourceObjectTypeDelineationType.F_BASE_CONTEXT);
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

    private Collection<ObjectTypeWithFilters> parseAndValidateFilters(List<SiSuggestedObjectTypeType> objectTypes, OperationResult parentResult)
            throws SuggestObjectTypesValidationException {
        final List<ObjectTypeWithFilters> objectTypesWithFilters = new ArrayList<>();

        final StringBuilder originalFiltersSb = new StringBuilder();
        final StringBuilder errorLogsSb = new StringBuilder();

        for (final SiSuggestedObjectTypeType objectType : objectTypes) {
            if (originalFiltersSb.length() > 0) {
                originalFiltersSb.append('\n').append("---").append('\n');
            }
            originalFiltersSb.append("[").append(objectType.getKind()).append("/").append(objectType.getIntent()).append("] ");
            List<SearchFilterType> filters = new ArrayList<>();
            ParsedBaseContext baseCtx = null;

            if (objectType.getFilter() != null && !objectType.getFilter().isEmpty()) {
                try {
                    originalFiltersSb.append('\n').append(String.join(" AND ", objectType.getFilter()));
                    filters = parseAndValidateObjectTypeFilters(
                            objectType,
                            ctx.objectClassDefinition.getPrismObjectDefinition(),
                            parentResult);
                } catch (SystemException | SchemaException | ExpressionEvaluationException | CommunicationException
                    | SecurityViolationException | ConfigurationException | ObjectNotFoundException | IllegalStateException e) {
                    LOGGER.warn("Failed validating suggested object type (kind={}, intent={}, displayName={}) for object class {}. Filters: {}",
                            objectType.getKind(), objectType.getIntent(), objectType.getDisplayName(), ctx.objectClassDefinition.getTypeName(), objectType.getFilter(), e);
                    appendError(errorLogsSb, objectType, e);
                }
            }
            if (objectType.getBaseContextObjectClassName() != null || objectType.getBaseContextFilter() != null) {
                try {
                    originalFiltersSb.append('\n').append("Base context: ").append(objectType.getBaseContextObjectClassName());
                    originalFiltersSb.append('\n').append("Filters: ").append(objectType.getBaseContextFilter());
                    baseCtx = parseAndValidateBaseContext(objectType, parentResult);
                } catch (SystemException | SchemaException | ExpressionEvaluationException | CommunicationException
                        | SecurityViolationException | ConfigurationException | ObjectNotFoundException | IllegalStateException e) {
                    LOGGER.warn("Failed validating base context for suggested object type (kind={}, intent={}, displayName={}). Base context objectClass={}, filter={}",
                            objectType.getKind(), objectType.getIntent(), objectType.getDisplayName(),
                            ctx.objectClassDefinition.getTypeName(), objectType.getBaseContextFilter(), e);
                    appendError(errorLogsSb, objectType, e);
                }
            }

            objectTypesWithFilters.add(new ObjectTypeWithFilters(
                    objectType,
                    filters,
                    baseCtx != null ? baseCtx.classQName() : null,
                    baseCtx != null ? baseCtx.filter() : null));
        }

        if (errorLogsSb.length() > 0) {
            throw new SuggestObjectTypesValidationException(
                    originalFiltersSb.toString(),
                    errorLogsSb.toString(),
                    objectTypesWithFilters);
        }
        return objectTypesWithFilters;
    }

    private void appendError(StringBuilder errorLogsSb, SiSuggestedObjectTypeType objectType, Exception e) {
        errorLogsSb.append("[").append(objectType.getKind()).append("/").append(objectType.getIntent()).append("] ");
        errorLogsSb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        errorLogsSb.append('\n').append("---").append('\n');
    }

    private List<SearchFilterType> parseAndValidateObjectTypeFilters(
            SiSuggestedObjectTypeType objectType,
            PrismObjectDefinition<ShadowType> shadowObjectDef,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException {
        final List<SearchFilterType> filters = new ArrayList<>();
        for (String filterString : objectType.getFilter()) {
            filters.add(parseAndSerializeFilter(filterString, shadowObjectDef));
        }
        qualityAssessor.isFilterRunnable(
                ctx.resource.getOid(),
                ctx.objectClassDefinition.getTypeName(),
                filters,
                ctx.task,
                parentResult);
        return filters;
    }

    private ParsedBaseContext parseAndValidateBaseContext(
            SiSuggestedObjectTypeType objectType,
            OperationResult parentResult) throws SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException, IllegalStateException {
        var baseContextClassLocalName = objectType.getBaseContextObjectClassName();
        var baseContextFilterString = objectType.getBaseContextFilter();

        stateCheck(baseContextClassLocalName != null,
                "Base context class name must be set if base context filter is set");
        stateCheck(baseContextFilterString != null,
                "Base context filter must be set if base context class name is set");

        QName baseContextClassQName = new QName(NS_RI, baseContextClassLocalName);
        var baseContextObjectDef = ctx.resourceSchema.findObjectClassDefinitionRequired(baseContextClassQName);
        var baseContextFilter = parseAndSerializeFilter(
                baseContextFilterString, baseContextObjectDef.getPrismObjectDefinition());

        qualityAssessor.isBaseContextFilterRunnable(
                ctx.resource.getOid(),
                baseContextClassQName,
                baseContextFilter,
                ctx.task,
                parentResult);

        return new ParsedBaseContext(baseContextClassQName, baseContextFilter);
    }

    private @NotNull SiSuggestObjectTypesResponseType generateObjectTypeSuggestion(
            ShadowObjectClassStatisticsType shadowObjectClassStatistics,
            @Nullable String errorLog,
            @Nullable String previousScript) throws SchemaException {
        var siRequest = new SiSuggestObjectTypesRequestType()
                .schema(ResourceObjectClassSchemaSerializer.serialize(ctx.objectClassDefinition, ctx.resource))
                .statistics(shadowObjectClassStatistics)
                .errorLog(errorLog)
                .previousScript(previousScript);

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
            throw new SchemaException(
                    "Cannot process suggested filter (%s): %s".formatted(filterString, e.getMessage()),
                    e);
        }
    }

    private record ObjectTypeWithFilters(
            SiSuggestedObjectTypeType suggestedObjectType,
            Collection<SearchFilterType> filters,
            QName baseContextClassQName,
            SearchFilterType baseContextFilter) {}

    private record ParsedBaseContext(QName classQName, SearchFilterType filter) {}

    /**
     * Custom exception carrying aggregated validation context and partial results
     * to guide a retry and/or allow building a result even when some entries failed.
     */
    static class SuggestObjectTypesValidationException extends SchemaException {
        private final String originalFilters;
        private final String errorLogs;
        private final Collection<ObjectTypeWithFilters> partialResult;

        SuggestObjectTypesValidationException(String originalFilters, String errorLogs,
                Collection<ObjectTypeWithFilters> partialResult) {
            super("Some suggested object types failed validation.");
            this.originalFilters = originalFilters;
            this.errorLogs = errorLogs;
            this.partialResult = partialResult;
        }

        String getOriginalFilters() { return originalFilters; }
        String getErrorLogs() { return errorLogs; }
        Collection<ObjectTypeWithFilters> getPartialResult() { return partialResult; }
    }

}
