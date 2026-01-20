/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.smart.api.ServiceClient.Method.SUGGEST_MAPPING;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaProvider;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.smart.impl.mappings.LowQualityMappingException;
import com.evolveum.midpoint.smart.impl.mappings.MappingDirection;
import com.evolveum.midpoint.smart.impl.mappings.MissingSourceDataException;
import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Implements "suggest mappings" operation.
 * - Collect representative owned shadows for training and testing.
 * - For each matched attribute pair, evaluate whether simple as-is mapping suffices or a script is needed.
 * - If needed, ask the microservice for a script, validate and (if necessary) retry once with error feedback.
 * - Build {@link AttributeMappingsSuggestionType} with expected quality and expression.
 * - Direction-aware logic (based on optional filters):
 *   - Inbound (default when filters are absent or set to inbound): suggest mapping from shadow attribute to focus property.
 *   - Outbound (when filters request outbounds only): suggest mapping from focus property to shadow attribute.
 * - Optional filters: if provided, they are used only to determine direction of suggestions.
 */
class MappingsSuggestionOperation {

    private static final Trace LOGGER = TraceManager.getTrace(MappingsSuggestionOperation.class);

    private static final int LLM_EXAMPLES_COUNT = 20;
    private static final int VALIDATION_EXAMPLES_COUNT = 200;
    private static final float MISSING_DATA_THRESHOLD = 0.05f;
    private static final float MINIMUM_QUALITY_THRESHOLD = 0.1f;

    private static final String ID_SHADOWS_COLLECTION = "shadowsCollection";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";
    private final TypeOperationContext ctx;
    private final MappingsQualityAssessor qualityAssessor;
    private final OwnedShadowsProvider ownedShadowsProvider;
    private final WellKnownSchemaService wellKnownSchemaService;
    private final boolean isInbound;

    private MappingsSuggestionOperation(
            TypeOperationContext ctx,
            MappingsQualityAssessor qualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider,
            WellKnownSchemaService wellKnownSchemaService,
            boolean isInbound) {
        this.ctx = ctx;
        this.qualityAssessor = qualityAssessor;
        this.ownedShadowsProvider = ownedShadowsProvider;
        this.wellKnownSchemaService = wellKnownSchemaService;
        this.isInbound = isInbound;
    }

    static MappingsSuggestionOperation init(
            ServiceClient serviceClient,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable CurrentActivityState<?> activityState,
            MappingsQualityAssessor qualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider,
            WellKnownSchemaService wellKnownSchemaService,
            boolean isInbound,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return new MappingsSuggestionOperation(
                TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, activityState, task, result),
                qualityAssessor,
                ownedShadowsProvider,
                wellKnownSchemaService,
                isInbound);
    }

    private MappingDirection resolveDirection() {
        return isInbound ? MappingDirection.INBOUND : MappingDirection.OUTBOUND;
    }

    MappingsSuggestionType suggestMappings(OperationResult result, SchemaMatchResultType schemaMatch)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityInterruptedException {
        ctx.checkIfCanRun();

        if (schemaMatch.getSchemaMatchResult().isEmpty()) {
            LOGGER.warn("No schema match found for {}. Returning empty suggestion.", this);
            return new MappingsSuggestionType();
        }

        var knownSchemaProvider = wellKnownSchemaService.getProviderFromSchemaMatch(schemaMatch).orElse(null);

        var ownedList = collectOwnedShadows(result);
        int llmDataCount = Math.min(LLM_EXAMPLES_COUNT, ownedList.size());
        int validationDataCount = Math.min(VALIDATION_EXAMPLES_COUNT, ownedList.size());
        var shadowsForLLM = ownedList.subList(0, llmDataCount);
        var shadowsForValidation = ownedList.subList(ownedList.size() - validationDataCount, ownedList.size());
        LOGGER.trace("LLM data count = {}, Validation data count={}, Total={}.", llmDataCount, validationDataCount, ownedList.size());
        ctx.checkIfCanRun();

        var mappingsSuggestionState = ctx.stateHolderFactory.create(ID_MAPPINGS_SUGGESTION, result);
        mappingsSuggestionState.setExpectedProgress(schemaMatch.getSchemaMatchResult().size());
        try {
            var suggestion = new MappingsSuggestionType();
            var direction = resolveDirection();

            addSystemMappings(suggestion, knownSchemaProvider);

            for (SchemaMatchOneResultType matchPair : schemaMatch.getSchemaMatchResult()) {
                var op = mappingsSuggestionState.recordProcessingStart(matchPair.getShadowAttribute().getName());
                mappingsSuggestionState.flush(result);
                ItemPath shadowAttrPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getShadowAttributePath());
                ItemPath focusPropPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getFocusPropertyPath());
                var valuePairsForLLM = ValuesPairSample.of(focusPropPath, shadowAttrPath, direction)
                        .from(shadowsForLLM);
                var valuePairsForValidation = ValuesPairSample.of(focusPropPath, shadowAttrPath, direction)
                        .from(shadowsForValidation);
                try {
                    suggestion.getAttributeMappings().add(
                            suggestMapping(
                                    matchPair,
                                    valuePairsForLLM,
                                    valuePairsForValidation,
                                    result));
                    mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.SUCCESS);
                } catch (LowQualityMappingException e) {
                    LOGGER.debug("Skipping mapping due to low quality: {}", e.getMessage());
                    mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.SKIP);
                } catch (MissingSourceDataException e) {
                    LOGGER.debug("Skipping mapping due to missing source data: {}", e.getMessage());
                    mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.SKIP);
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

    private void addSystemMappings(MappingsSuggestionType suggestion, WellKnownSchemaProvider knownSchemaProvider) {
        if (knownSchemaProvider == null) {
            return;
        }
        LOGGER.info("Adding predefined mappings from known schema provider: {}", knownSchemaProvider.getSupportedSchemaType());
        if (isInbound) {
            for (AttributeMappingsSuggestionType predefinedSuggestion : knownSchemaProvider.suggestInboundMappings()) {
                SmartMetadataUtil.markAsSystemProvided(predefinedSuggestion);
                suggestion.getAttributeMappings().add(predefinedSuggestion);
            }
        } else {
            for (AttributeMappingsSuggestionType predefinedSuggestion : knownSchemaProvider.suggestOutboundMappings()) {
                SmartMetadataUtil.markAsSystemProvided(predefinedSuggestion);
                suggestion.getAttributeMappings().add(predefinedSuggestion);
            }
        }
    }

    private List<OwnedShadow> collectOwnedShadows(OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
        var state = ctx.stateHolderFactory.create(ID_SHADOWS_COLLECTION, result);
        state.setExpectedProgress(LLM_EXAMPLES_COUNT + VALIDATION_EXAMPLES_COUNT);
        state.flush(result); // because finding an owned shadow can take a while
        try {
            return ownedShadowsProvider.fetch(ctx, state, result, LLM_EXAMPLES_COUNT + VALIDATION_EXAMPLES_COUNT);
        } catch (Exception e) {
            state.recordException(e);
            throw e;
        } finally {
            state.close(result);
        }
    }

    private AttributeMappingsSuggestionType suggestMapping(
            SchemaMatchOneResultType matchPair,
            ValuesPairSample<?, ?> valuePairsForLLM,
            ValuesPairSample<?, ?> valuePairsForValidation,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, LowQualityMappingException,
            MissingSourceDataException {

        LOGGER.trace("Going to suggest {} mapping between shadow attribute: {} and focus property: {}.", valuePairsForValidation.direction(),
                matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath());

        ExpressionType expression = null;
        MappingsQualityAssessor.AssessmentResult assessment = null;

        if (valuePairsForLLM.pairs().isEmpty() || valuePairsForValidation.pairs().isEmpty()) {
            LOGGER.trace("No data pairs. We'll use 'asIs' mapping (no LLM call).");
        } else if (valuePairsForValidation.isSourceDataMissing(MISSING_DATA_THRESHOLD)) {
            throw new MissingSourceDataException(matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath());
        } else if (valuePairsForValidation.isTargetDataMissing(MISSING_DATA_THRESHOLD)) {
            LOGGER.trace("Target data missing. We'll use 'asIs' mapping (no LLM call).");
        } else if (valuePairsForValidation.doAllConvertedSourcesMatchTargets(ctx.getFocusTypeDefinition(), ctx.typeDefinition, ctx.b.protector)) {
            LOGGER.trace("AsIs {} mapping suffice according to the data (no LLM call).", valuePairsForValidation.direction());
            assessment = this.qualityAssessor.assessMappingsQuality(
                    valuePairsForValidation, expression, this.ctx.task, parentResult);
        } else {
            LOGGER.trace("Going to ask LLM about mapping script");
            String errorLog = null;
            String retryScript = null;

            for (int attempt = 1; attempt <= 2; attempt++) {
                var mappingResponse = askMicroservice(matchPair, valuePairsForLLM, errorLog, retryScript);
                retryScript = mappingResponse != null ? mappingResponse.getTransformationScript() : null;
                expression = buildScriptExpression(mappingResponse);
                try {
                    assessment = this.qualityAssessor.assessMappingsQuality(
                            valuePairsForValidation, expression, this.ctx.task, parentResult);
                    break;
                } catch (ExpressionEvaluationException | SecurityViolationException e) {
                    if (attempt == 1) {
                        errorLog = e.getMessage();
                        LOGGER.warn("Validation issues found on attempt 1; retrying.");
                    } else {
                        LOGGER.warn("Validation issues persist after retry; giving up.");
                        throw e;
                    }
                }
            }
        }

        if (assessment != null && assessment.quality() < MINIMUM_QUALITY_THRESHOLD) {
            throw new LowQualityMappingException(
                    assessment.quality(),
                    MINIMUM_QUALITY_THRESHOLD,
                    matchPair.getShadowAttributePath(),
                    matchPair.getFocusPropertyPath());
        }

        AttributeMappingsSuggestionType suggestion = buildAttributeMappingSuggestion(
                valuePairsForValidation, assessment != null ? assessment.quality() : null, expression);
        SmartMetadataUtil.markAsAiProvided(suggestion); // everything is AI-provided now
        return suggestion;
    }

    /** Builds the final suggestion structure for the given direction while encapsulating path handling quirks. */
    private static AttributeMappingsSuggestionType buildAttributeMappingSuggestion(
            ValuesPairSample<?, ?> pairSample,
            @Nullable Float expectedQuality,
            @Nullable ExpressionType expression) {
        var sanitizedFocusPath = sanitizeFocusPathForTarget(pairSample.focusPropertyPath());
        var def = new ResourceAttributeDefinitionType()
                .ref(pairSample.shadowAttributePath().rest().toBean()); // FIXME! what about activation, credentials, etc?

        if (pairSample.direction() == MappingDirection.INBOUND) {
            def.inbound(new InboundMappingType()
                    .name(pairSample.shadowAttributePath().lastName().getLocalPart() + "-into-" + pairSample.focusPropertyPath())
                    .strength(MappingStrengthType.STRONG)
                    .target(new VariableBindingDefinitionType().path(sanitizedFocusPath.toBean()))
                    .expression(expression));
        } else {
            def.outbound(new OutboundMappingType()
                    .name(pairSample.focusPropertyPath() + "-to-" + pairSample.shadowAttributePath().lastName().getLocalPart())
                    .strength(MappingStrengthType.STRONG)
                    .source(new VariableBindingDefinitionType().path(sanitizedFocusPath.toBean()))
                    .expression(expression));
        }

        return new AttributeMappingsSuggestionType()
                .expectedQuality(expectedQuality)
                .definition(def);
    }

    /** Centralized place for the historical "ext:" removal hack for focus path serialization. */
    private static ItemPath sanitizeFocusPathForTarget(ItemPath focusPropPath) {
        var serialized = PrismContext.get().itemPathSerializer().serializeStandalone(focusPropPath);
        var hackedSerialized = serialized.replace("ext:", "");
        return PrismContext.get().itemPathParser().asItemPath(hackedSerialized);
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

    private SiSuggestMappingResponseType askMicroservice(
            SchemaMatchOneResultType matchPair,
            ValuesPairSample<?, ?> valuesPairs,
            @Nullable String errorLog,
            @Nullable String retryScript) throws SchemaException {
        var siRequest = new SiSuggestMappingRequestType()
                .applicationAttribute(matchPair.getShadowAttribute())
                .midPointAttribute(matchPair.getFocusProperty())
                .inbound(valuesPairs.direction() == MappingDirection.INBOUND)
                .errorLog(errorLog)
                .previousScript(retryScript);
        valuesPairs.pairs().forEach(pair ->
                siRequest.getExample().add(
                        pair.toSiExample(
                                matchPair.getShadowAttribute().getName(),
                                matchPair.getFocusProperty().getName())));
        return ctx.serviceClient
                .invoke(SUGGEST_MAPPING, siRequest, SiSuggestMappingResponseType.class);
    }

}
