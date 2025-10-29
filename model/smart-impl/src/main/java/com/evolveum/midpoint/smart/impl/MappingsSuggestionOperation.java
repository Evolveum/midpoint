/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.smart.api.ServiceClient.Method.SUGGEST_MAPPING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;

import com.evolveum.midpoint.smart.impl.mappings.ValuesPair;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Implements "suggest mappings" operation.
 */
class MappingsSuggestionOperation {

    private static final Trace LOGGER = TraceManager.getTrace(MappingsSuggestionOperation.class);

    private static final int ATTRIBUTE_MAPPING_EXAMPLES = 20;

    private static final String ID_SCHEMA_MATCHING = "schemaMatching";
    private static final String ID_SHADOWS_COLLECTION = "shadowsCollection";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";
    private final TypeOperationContext ctx;
    private final MappingsQualityAssessor qualityAssessor;

    private MappingsSuggestionOperation(TypeOperationContext ctx, MappingsQualityAssessor qualityAssessor) {
        this.ctx = ctx;
        this.qualityAssessor = qualityAssessor;
    }

    static MappingsSuggestionOperation init(
            ServiceClient serviceClient,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable CurrentActivityState<?> activityState,
            MappingsQualityAssessor qualityAssessor,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return new MappingsSuggestionOperation(
                TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, activityState, task, result),
                qualityAssessor);
    }

    MappingsSuggestionType suggestMappings(OperationResult result, ShadowObjectClassStatisticsType statistics, SchemaMatchResultType schemaMatch)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityInterruptedException {
        ctx.checkIfCanRun();

        if (schemaMatch.getSchemaMatchResult().isEmpty()) {
            LOGGER.warn("No schema match found for {}. Returning empty suggestion.", this);
            return new MappingsSuggestionType();
        }

        var shadowsCollectionState = ctx.stateHolderFactory.create(ID_SHADOWS_COLLECTION, result);
        shadowsCollectionState.setExpectedProgress(ATTRIBUTE_MAPPING_EXAMPLES);
        shadowsCollectionState.flush(result); // because finding an owned shadow can take a while
        Collection<OwnedShadow> ownedShadows;
        try {
            ownedShadows = fetchOwnedShadows(shadowsCollectionState, result);
        } catch (Exception e) {
            shadowsCollectionState.recordException(e);
            throw e;
        } finally {
            shadowsCollectionState.close(result);
        }

        ctx.checkIfCanRun();

        var mappingsSuggestionState = ctx.stateHolderFactory.create(ID_MAPPINGS_SUGGESTION, result);
        mappingsSuggestionState.setExpectedProgress(schemaMatch.getSchemaMatchResult().size());
        try {
            var suggestion = new MappingsSuggestionType();
            for (SchemaMatchOneResultType matchPair : schemaMatch.getSchemaMatchResult()) {
                var op = mappingsSuggestionState.recordProcessingStart(matchPair.getShadowAttribute().getName());
                mappingsSuggestionState.flush(result);
                var pairs = getValuesPairs(matchPair, ownedShadows);
                try {
                    suggestion.getAttributeMappings().add(
                            suggestMapping(
                                    matchPair,
                                    pairs,
                                    result));
                    mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.SUCCESS);
                } catch (Exception e) {
                    // TODO Shouldn't we create an unfinished mapping with just error info?
                    LoggingUtils.logException(LOGGER, "Couldn't suggest mapping for {}", e, matchPair.getShadowAttributePath());
                    mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.FAILURE);

                    // Normally, the activity framework makes sure that the activity result status is computed properly at the end.
                    // But this is a special case where we must do that ourselves.
                    // FIXME temporarily disabled, as GUI cannot deal with it anyway
                    //mappingsSuggestionState.setResultStatus(OperationResultStatus.PARTIAL_ERROR);
                }
                ctx.checkIfCanRun();
            }
            return suggestion;
        } catch (Exception e) {
            mappingsSuggestionState.recordException(e);
            throw e;
        } finally {
            mappingsSuggestionState.close(result);
        }
    }

    private Collection<ValuesPair> getValuesPairs(SchemaMatchOneResultType m, Collection<OwnedShadow> ownedShadows) {
        return extractPairs(
                ownedShadows,
                PrismContext.get().itemPathParser().asItemPath(m.getShadowAttributePath()),
                PrismContext.get().itemPathParser().asItemPath(m.getFocusPropertyPath()));
    }

    private Collection<ValuesPair> extractPairs(
            Collection<OwnedShadow> ownedShadows, ItemPath shadowAttrPath, ItemPath focusPropPath) {
        return ownedShadows.stream()
                .map(ownedShadow -> new ValuesPair(
                        getItemRealValues(ownedShadow.shadow, shadowAttrPath),
                        getItemRealValues(ownedShadow.owner, focusPropPath)))
                .toList();
    }

    private Collection<?> getItemRealValues(ObjectType objectable, ItemPath itemPath) {
        var item = objectable.asPrismObject().findItem(itemPath);
        return item != null ? item.getRealValues() : List.of();
    }

    private Collection<OwnedShadow> fetchOwnedShadows(OperationContext.StateHolder state, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        // Maybe we should search the repository instead. The argument for going to the resource is to get some data even
        // if they are not in the repository yet. But this is not a good argument, because if we get an account from the resource,
        // it won't have the owner anyway.
        var ownedShadows = new ArrayList<OwnedShadow>(ATTRIBUTE_MAPPING_EXAMPLES);
        ctx.b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(ctx.resource)
                        .queryFor(ctx.typeDefinition.getTypeIdentification())
                        .build(),
                (object, lResult) -> {
                    try {
                        var owner = ctx.b.modelService.searchShadowOwner(object.getOid(), null, ctx.task, lResult);
                        if (owner != null) {
                            ownedShadows.add(new OwnedShadow(object.asObjectable(), owner.asObjectable()));
                            state.incrementProgress(result);
                        }
                    } catch (Exception e) {
                        LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, object);
                    }
                    return ctx.canRun() && ownedShadows.size() < ATTRIBUTE_MAPPING_EXAMPLES;
                },
                null, ctx.task, result);
        return ownedShadows;
    }

    private record OwnedShadow(ShadowType shadow, FocusType owner) {
    }

    private AttributeMappingsSuggestionType suggestMapping(
            SchemaMatchOneResultType matchPair,
            Collection<ValuesPair> valuesPairs,
            OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, SecurityViolationException {

        LOGGER.trace("Going to suggest mapping for {} -> {} based on {} values pairs",
                matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath(), valuesPairs.size());

        ItemPath focusPropPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getFocusPropertyPath());
        ItemPath shadowAttrPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getShadowAttributePath());
        var propertyDef = ctx.getFocusTypeDefinition().findPropertyDefinition(focusPropPath);
        ExpressionType expression;
        if (valuesPairs.isEmpty()) {
            LOGGER.trace(" -> no data pairs, so we'll use 'asIs' mapping (without calling LLM)");
            expression = null;
        } else if (valuesPairs.stream().allMatch(pair ->
                (pair.shadowValues() == null || pair.shadowValues().stream().allMatch(Objects::isNull))
                        || (pair.focusValues() == null || pair.focusValues().stream().allMatch(Objects::isNull)))) {
            LOGGER.trace(" -> all shadow or focus values are null, using 'asIs' mapping (without calling LLM)");
            expression = null;
        } else if (doesAsIsSuffice(valuesPairs, propertyDef)) {
            LOGGER.trace(" -> 'asIs' does suffice according to the data, so we'll use it (without calling LLM)");
            expression = null;
        } else if (isTargetDataMissing(valuesPairs)) {
            LOGGER.trace(" -> target data missing; we assume they are probably not there yet, so 'asIs' is fine (no LLM call)");
            expression = null;
        } else {
            LOGGER.trace(" -> going to ask LLM about mapping script");
            var transformationScript = askMicroservice(matchPair, valuesPairs, null);
            expression = buildScriptExpression(transformationScript);
        }

        var assessment = assessQuality(expression, matchPair, valuesPairs, parentResult);

        // TODO remove this ugly hack
        var serialized = PrismContext.get().itemPathSerializer().serializeStandalone(focusPropPath);
        var hackedSerialized = serialized.replace("ext:", "");
        var hackedReal = PrismContext.get().itemPathParser().asItemPath(hackedSerialized);
        var suggestion = new AttributeMappingsSuggestionType()
                .expectedQuality(assessment.quality())
                .definition(new ResourceAttributeDefinitionType()
                        .ref(shadowAttrPath.rest().toBean()) // FIXME! what about activation, credentials, etc?
                        .inbound(new InboundMappingType()
                                .name(shadowAttrPath.lastName().getLocalPart()
                                        + "-to-" + focusPropPath) //TODO TBD
                                .target(new VariableBindingDefinitionType()
                                        .path(hackedReal.toBean()))
                                .expression(expression)));
        AiUtil.markAsAiProvided(suggestion); // everything is AI-provided now
        return suggestion;
    }

    /**
     * Builds an {@link ExpressionType} containing a script evaluator
     * and optional description extracted from the suggested mapping response.
     */
    private static @Nullable ExpressionType buildScriptExpression(SiSuggestMappingResponseType suggestMappingResponse) {
        if (suggestMappingResponse == null) {
            return null;
        }

        var script = suggestMappingResponse.getTransformationScript();
        String scriptDescription = suggestMappingResponse.getDescription();

        if (script == null || "input".equals(script)) {
            return null;
        }

        return new ExpressionType()
                .description(scriptDescription)
                .expressionEvaluator(
                        new ObjectFactory().createScript(
                                new ScriptExpressionEvaluatorType().code(script)));
    }

    private MappingsQualityAssessor.AssessmentResult assessQuality(
            ExpressionType expression,
            SchemaMatchOneResultType matchPair,
            Collection<ValuesPair> valuesPairs,
            OperationResult parentResult
    ) throws SchemaException, ExpressionEvaluationException, SecurityViolationException {
        MappingsQualityAssessor.AssessmentResult assessment;
        try {
            assessment = this.qualityAssessor.assessMappingsQuality(
                    valuesPairs, expression, this.ctx.task, parentResult);
        } catch (ExpressionEvaluationException | SecurityViolationException e) {
            var retryScript = askMicroservice(matchPair, valuesPairs, e.getMessage());
            expression = buildScriptExpression(retryScript);
            try {
                assessment = this.qualityAssessor.assessMappingsQuality(
                        valuesPairs, expression, this.ctx.task, parentResult);
            } catch (ExpressionEvaluationException | SecurityViolationException e2) {
                LOGGER.trace("Expression evaluation failed even after retry for {}.", matchPair.getShadowAttributePath());
                throw e2;
            }
        }
        return assessment;
    }

    private SiSuggestMappingResponseType askMicroservice(
            SchemaMatchOneResultType matchPair,
            Collection<ValuesPair> valuesPairs,
            @Nullable String errorLog) throws SchemaException {
        var siRequest = new SiSuggestMappingRequestType()
                .applicationAttribute(matchPair.getShadowAttribute())
                .midPointAttribute(matchPair.getFocusProperty())
                .inbound(true)
                .errorLog(errorLog);
        valuesPairs.forEach(pair ->
                siRequest.getExample().add(
                        pair.toSiExample(
                                matchPair.getShadowAttribute().getName(),
                                matchPair.getFocusProperty().getName())));
        return ctx.serviceClient
                .invoke(SUGGEST_MAPPING, siRequest, SiSuggestMappingResponseType.class);
    }

    /** Returns {@code true} if a simple "asIs" mapping is sufficient. */
    private boolean doesAsIsSuffice(Collection<ValuesPair> valuesPairs, PrismPropertyDefinition<?> propertyDef) {
        for (var valuesPair : valuesPairs) {
            var shadowValues = valuesPair.shadowValues();
            var focusValues = valuesPair.focusValues();
            if (shadowValues.size() != focusValues.size()) {
                return false;
            }
            var expectedFocusValues = new ArrayList<>(focusValues.size());
            for (Object shadowValue : shadowValues) {
                Object converted;
                try {
                    converted = ExpressionUtil.convertValue(
                            propertyDef.getTypeClass(), null, shadowValue, ctx.b.protector);
                } catch (Exception e) {
                    // If the conversion is not possible e.g. because of different types, an exception is thrown
                    // We are OK with that (from performance point of view), because this is just a sample of values.
                    LOGGER.trace("Value conversion failed, assuming transformation is needed: {} (value: {})",
                            e.getMessage(), shadowValue); // no need to provide full stack trace here
                    return false;
                }
                if (converted != null) {
                    expectedFocusValues.add(converted);
                }
            }
            if (!MiscUtil.unorderedCollectionEquals(focusValues, expectedFocusValues)) {
                return false;
            }
        }
        return true;
    }

    /** Returns {@code true} if there are no target data altogether. */
    private boolean isTargetDataMissing(Collection<ValuesPair> valuesPairs) {
        return valuesPairs.stream().allMatch(pair -> pair.focusValues().isEmpty());
    }

}
