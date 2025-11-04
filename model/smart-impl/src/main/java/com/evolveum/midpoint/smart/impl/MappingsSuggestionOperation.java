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

import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
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
    private static final int ATTRIBUTE_TESTING_EXAMPLES = 200;

    private static final String ID_SHADOWS_COLLECTION = "shadowsCollection";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";
    private final TypeOperationContext ctx;
    private final MappingsQualityAssessor qualityAssessor;
    private final OwnedShadowsProvider ownedShadowsProvider;

    private MappingsSuggestionOperation(
            TypeOperationContext ctx,
            MappingsQualityAssessor qualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider) {
        this.ctx = ctx;
        this.qualityAssessor = qualityAssessor;
        this.ownedShadowsProvider = ownedShadowsProvider;
    }

    static MappingsSuggestionOperation init(
            ServiceClient serviceClient,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable CurrentActivityState<?> activityState,
            MappingsQualityAssessor qualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return new MappingsSuggestionOperation(
                TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, activityState, task, result),
                qualityAssessor,
                ownedShadowsProvider);
    }

    MappingsSuggestionType suggestMappings(OperationResult result, ShadowObjectClassStatisticsType statistics, SchemaMatchResultType schemaMatch)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityInterruptedException {
        ctx.checkIfCanRun();

        if (schemaMatch.getSchemaMatchResult().isEmpty()) {
            LOGGER.warn("No schema match found for {}. Returning empty suggestion.", this);
            return new MappingsSuggestionType();
        }

        var ownedList = collectOwnedShadows(result);
        int trainCount = Math.min(ATTRIBUTE_MAPPING_EXAMPLES, ownedList.size());
        int testCount = Math.min(ATTRIBUTE_TESTING_EXAMPLES, ownedList.size());
        var suggestionShadows = ownedList.subList(0, trainCount);
        var testingShadows = ownedList.subList(ownedList.size() - testCount, ownedList.size());
        LOGGER.trace("Train={}, Test={}, Total={}", trainCount, testCount, ownedList.size());
        ctx.checkIfCanRun();

        var mappingsSuggestionState = ctx.stateHolderFactory.create(ID_MAPPINGS_SUGGESTION, result);
        mappingsSuggestionState.setExpectedProgress(schemaMatch.getSchemaMatchResult().size());
        try {
            var suggestion = new MappingsSuggestionType();
            for (SchemaMatchOneResultType matchPair : schemaMatch.getSchemaMatchResult()) {
                var op = mappingsSuggestionState.recordProcessingStart(matchPair.getShadowAttribute().getName());
                mappingsSuggestionState.flush(result);
                ItemPath shadowAttrPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getShadowAttributePath());
                ItemPath focusPropPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getFocusPropertyPath());
                var suggestionPairs = toValuesPairs(suggestionShadows, shadowAttrPath, focusPropPath);
                var testingPairs = toValuesPairs(testingShadows, shadowAttrPath, focusPropPath);
                try {
                    suggestion.getAttributeMappings().add(
                            suggestMapping(
                                    matchPair,
                                    suggestionPairs,
                                    testingPairs,
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

    private List<OwnedShadow> collectOwnedShadows(OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
        var state = ctx.stateHolderFactory.create(ID_SHADOWS_COLLECTION, result);
        state.setExpectedProgress(ATTRIBUTE_MAPPING_EXAMPLES + ATTRIBUTE_TESTING_EXAMPLES);
        state.flush(result); // because finding an owned shadow can take a while
        try {
            return ownedShadowsProvider.fetch(ctx, state, result, ATTRIBUTE_MAPPING_EXAMPLES + ATTRIBUTE_TESTING_EXAMPLES);
        } catch (Exception e) {
            state.recordException(e);
            throw e;
        } finally {
            state.close(result);
        }
    }

    private static List<ValuesPair> toValuesPairs(
            List<OwnedShadow> shadows,
            ItemPath shadowAttrPath,
            ItemPath focusPropPath) {
        return shadows.stream()
                .map(os -> os.toValuesPair(shadowAttrPath, focusPropPath))
                .toList();
    }


    private AttributeMappingsSuggestionType suggestMapping(
            SchemaMatchOneResultType matchPair,
            Collection<ValuesPair> valuesPairs,
            Collection<ValuesPair> testingValuesPairs,
            OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, SecurityViolationException {

        LOGGER.trace("Going to suggest mapping for {} -> {} based on {} values pairs",
                matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath(), valuesPairs.size());

        ItemPath focusPropPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getFocusPropertyPath());
        ItemPath shadowAttrPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getShadowAttributePath());
        var propertyDef = ctx.getFocusTypeDefinition().findPropertyDefinition(focusPropPath);
        String transformationScript = null;
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
            var mappingResponse = askMicroservice(matchPair, valuesPairs, null, null);
            transformationScript = mappingResponse.getTransformationScript();
            expression = buildScriptExpression(mappingResponse);
        }

        var assessment = assessQualityWithRetry(expression, matchPair, testingValuesPairs, transformationScript, parentResult);

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
                                .strength(MappingStrengthType.STRONG)
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

    private MappingsQualityAssessor.AssessmentResult assessQualityWithRetry(
            ExpressionType expression,
            SchemaMatchOneResultType matchPair,
            Collection<ValuesPair> valuesPairs,
            @Nullable String firstScript,
            OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, SecurityViolationException {
        MappingsQualityAssessor.AssessmentResult assessment;
        try {
            assessment = this.qualityAssessor.assessMappingsQuality(
                    valuesPairs, expression, this.ctx.task, parentResult);
        } catch (ExpressionEvaluationException | SecurityViolationException e) {
            var retryResponse = askMicroservice(matchPair, valuesPairs, e.getMessage(), firstScript);
            expression = buildScriptExpression(retryResponse);
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
            @Nullable String errorLog,
            @Nullable String retryScript) throws SchemaException {
        var siRequest = new SiSuggestMappingRequestType()
                .applicationAttribute(matchPair.getShadowAttribute())
                .midPointAttribute(matchPair.getFocusProperty())
                .inbound(true)
                .errorLog(errorLog)
                .previousScript(retryScript);
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
