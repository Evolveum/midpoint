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
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.smart.impl.wellknownschemas.SystemMappingSuggestion;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaProvider;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.smart.impl.mappings.heuristics.HeuristicRuleMatcher;
import com.evolveum.midpoint.smart.impl.mappings.LowQualityMappingException;
import com.evolveum.midpoint.smart.impl.mappings.MappingDirection;
import com.evolveum.midpoint.smart.impl.mappings.MissingSourceDataException;
import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
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
    private final HeuristicRuleMatcher heuristicRuleMatcher;
    private final boolean isInbound;
    private final boolean useAiService;

    private MappingsSuggestionOperation(
            TypeOperationContext ctx,
            MappingsQualityAssessor qualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider,
            WellKnownSchemaService wellKnownSchemaService,
            HeuristicRuleMatcher heuristicRuleMatcher,
            boolean isInbound,
            boolean useAiService) {
        this.ctx = ctx;
        this.qualityAssessor = qualityAssessor;
        this.ownedShadowsProvider = ownedShadowsProvider;
        this.wellKnownSchemaService = wellKnownSchemaService;
        this.heuristicRuleMatcher = heuristicRuleMatcher;
        this.isInbound = isInbound;
        this.useAiService = useAiService;
    }

    static MappingsSuggestionOperation init(
            TypeOperationContext ctx,
            MappingsQualityAssessor qualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider,
            WellKnownSchemaService wellKnownSchemaService,
            HeuristicRuleMatcher heuristicRuleMatcher,
            boolean isInbound,
            boolean useAiService)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return new MappingsSuggestionOperation(
                ctx,
                qualityAssessor,
                ownedShadowsProvider,
                wellKnownSchemaService,
                heuristicRuleMatcher,
                isInbound,
                useAiService);
    }

    private MappingDirection resolveDirection() {
        return isInbound ? MappingDirection.INBOUND : MappingDirection.OUTBOUND;
    }

    MappingsSuggestionType suggestMappings(
            OperationResult result,
            SchemaMatchResultType schemaMatch,
            @Nullable List<ItemPath> targetPathsToIgnore)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityInterruptedException {
        ctx.checkIfCanRun();

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
            var existingMappingPaths = collectExistingMappingTargetPaths();
            var excludedMappingPaths = mergeExcludedPaths(existingMappingPaths, targetPathsToIgnore);
            var mappingCandidates = new AttributeMappingCandidateSet(excludedMappingPaths);

            collectSystemMappings(knownSchemaProvider, shadowsForValidation, result)
                    .forEach(mappingCandidates::propose);

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
                    var aiMapping = suggestMapping(matchPair, valuePairsForLLM, valuePairsForValidation, result);
                    mappingCandidates.propose(aiMapping);
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

            mappingCandidates.best()
                    .forEach(suggestion.getAttributeMappings()::add);

            return suggestion;
        } catch (Exception e) {
            mappingsSuggestionState.recordException(e);
            throw e;
        } finally {
            mappingsSuggestionState.close(result);
        }
    }

    /**
     * Collects target paths of existing mappings configured on the resource type.
     * For inbound direction, collects focus property paths from inbound mapping targets.
     * For outbound direction, collects shadow attribute paths that have outbound mappings.
     */
    private Collection<ItemPath> collectExistingMappingTargetPaths() {
        var existingPaths = new ArrayList<ItemPath>();
        for (var attrDef : ctx.typeDefinition.getAttributeDefinitions()) {
            if (isInbound) {
                for (var inbound : attrDef.getInboundMappingBeans()) {
                    var target = inbound.getTarget();
                    if (target != null && target.getPath() != null) {
                        existingPaths.add(target.getPath().getItemPath());
                    }
                }
            } else {
                if (attrDef.hasOutboundMapping()) {
                    existingPaths.add(attrDef.getStandardPath());
                }
            }
        }
        LOGGER.trace("Collected {} existing {} mapping target paths for deduplication.", existingPaths.size(), isInbound ? "inbound" : "outbound");
        return existingPaths;
    }

    /**
     * Merges existing mapping paths and accepted suggestion paths into a single list of excluded paths.
     * These paths should be excluded from new mapping suggestions.
     */
    private List<ItemPath> mergeExcludedPaths(
            Collection<ItemPath> existingMappingPaths,
            @Nullable List<ItemPath> acceptedSuggestionPaths) {
        if (acceptedSuggestionPaths == null || acceptedSuggestionPaths.isEmpty()) {
            return new ArrayList<>(existingMappingPaths);
        }
        var merged = new ArrayList<>(existingMappingPaths);
        merged.addAll(acceptedSuggestionPaths);
        LOGGER.trace("Merged {} existing mapping paths and {} accepted suggestion paths for exclusion.",
                existingMappingPaths.size(), acceptedSuggestionPaths.size());
        return merged;
    }

    private List<AttributeMappingsSuggestionType> collectSystemMappings(
            WellKnownSchemaProvider knownSchemaProvider,
            List<OwnedShadow> shadowsForValidation,
            OperationResult result) {
        if (knownSchemaProvider == null) {
            return List.of();
        }
        var mappings = isInbound
                ? knownSchemaProvider.suggestInboundMappings()
                : knownSchemaProvider.suggestOutboundMappings(
                        shadowsForValidation.stream().map(OwnedShadow::shadow).toList());
        return mappings.stream()
                .map(systemMapping -> assessAndBuildSystemMapping(systemMapping, shadowsForValidation, result))
                .flatMap(opt -> opt.stream())
                .toList();
    }

    private Optional<AttributeMappingsSuggestionType> assessAndBuildSystemMapping(
            SystemMappingSuggestion systemMapping,
            List<OwnedShadow> shadowsForValidation,
            OperationResult result) {
        try {
            var direction = resolveDirection();
            var valuePairs = ValuesPairSample.of(
                    systemMapping.focusPropertyPath(),
                    systemMapping.shadowAttributePath(),
                    direction).from(shadowsForValidation);

            Float quality = null;
            if (!valuePairs.pairs().isEmpty()) {
                var assessment = qualityAssessor.assessMappingsQuality(valuePairs, systemMapping.expression(), ctx.task, result);
                if (assessment != null && assessment.status() == MappingsQualityAssessor.AssessmentStatus.OK) {
                    quality = assessment.quality();
                }
            }

            var mappingSuggestion = buildAttributeMappingSuggestion(valuePairs, quality, systemMapping.expression());
            SmartMetadataUtil.markContainerProvenance(
                    mappingSuggestion.asPrismContainerValue(),
                    SmartMetadataUtil.ProvenanceKind.SYSTEM);
            return Optional.of(mappingSuggestion);
        } catch (Exception e) {
            LOGGER.debug("Failed to assess system mapping quality for {}: {}", systemMapping.shadowAttributePath(), e.getMessage());
            return Optional.empty();
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

        LOGGER.trace("Going to suggest {} mapping between shadow attribute: {} and focus property: {}.",
                valuePairsForValidation.direction(),
                matchPair.getShadowAttributePath(),
                matchPair.getFocusPropertyPath());

        // 1. Evaluate mapping strategy (as-is, heuristic, or AI)
        MappingEvaluationResult result = evaluateMappingStrategy(
                matchPair,
                valuePairsForLLM,
                valuePairsForValidation,
                parentResult);

        // 2. Validate quality threshold
        validateQualityThreshold(result, matchPair);

        // 3. Build and annotate suggestion
        AttributeMappingsSuggestionType suggestion = buildAttributeMappingSuggestion(
                valuePairsForValidation,
                result.expectedQuality() != null ? result.expectedQuality() : null,
                result.expression());

        // 4. Mark provenance
        SmartMetadataUtil.markContainerProvenance(
                suggestion.asPrismContainerValue(),
                result.isSystemProvided()
                        ? SmartMetadataUtil.ProvenanceKind.SYSTEM
                        : SmartMetadataUtil.ProvenanceKind.AI);

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

    private MappingEvaluationResult evaluateMappingStrategy(
            SchemaMatchOneResultType matchPair,
            ValuesPairSample<?, ?> valuePairsForLLM,
            ValuesPairSample<?, ?> valuePairsForValidation,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, MissingSourceDataException {

        boolean isSystemProvided = Boolean.TRUE.equals(matchPair.getIsSystemProvided());

        // Check if data is sufficient for evaluation
        if (valuePairsForLLM.pairs().isEmpty() || valuePairsForValidation.pairs().isEmpty()) {
            LOGGER.trace("No data pairs. We'll use 'asIs' mapping (no LLM call).");
            return MappingEvaluationResult.of(null, null, isSystemProvided);
        }

        // Check for missing source data. We don't want to suggest mapping if there is not enough data.
        if (valuePairsForValidation.isSourceDataMissing(MISSING_DATA_THRESHOLD)) {
            throw new MissingSourceDataException(matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath());
        }

        // Check for missing target data
        if (valuePairsForValidation.isTargetDataMissing(MISSING_DATA_THRESHOLD)) {
            LOGGER.trace("Target data missing. We'll use 'asIs' mapping (no LLM call).");
            return MappingEvaluationResult.of(null, null, isSystemProvided);
        }

        // Check if as-is mapping is sufficient
        if (valuePairsForValidation.allSourcesMatchTargets(ctx.getFocusTypeDefinition(), ctx.typeDefinition, ctx.b.protector)) {
            LOGGER.trace("AsIs {} mapping suffice according to the data (no LLM call).", valuePairsForValidation.direction());
            return MappingEvaluationResult.of(null, 1.0F, isSystemProvided);
        }

        // Find best mapping by comparing as-is, heuristic, and AI options
        return findBestMappingExpression(
                matchPair,
                valuePairsForLLM,
                valuePairsForValidation,
                isSystemProvided,
                parentResult);
    }

    private MappingEvaluationResult findBestMappingExpression(
            SchemaMatchOneResultType matchPair,
            ValuesPairSample<?, ?> valuePairsForLLM,
            ValuesPairSample<?, ?> valuePairsForValidation,
            boolean isSystemProvided,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException {

        // Start with as-is mapping quality as baseline
        float bestExpectedQuality = qualityAssessor.assessMappingsQuality(valuePairsForValidation, null, ctx.task, parentResult)
                .quality();
        ExpressionType bestExpression = null;

        // Try heuristic mappings
        var heuristicResult = heuristicRuleMatcher.findBestMatch(valuePairsForValidation, ctx.task, parentResult);
        if (heuristicResult.isPresent()) {
            var hr = heuristicResult.get();
            if (hr.quality() > bestExpectedQuality) {
                LOGGER.info("Found heuristic mapping '{}' with quality {}", hr.heuristicName(), hr.quality());
                bestExpression = hr.expression();
                bestExpectedQuality = hr.quality();
            }
        }

        // Try AI service if enabled
        if (useAiService) {
            var aiExpression = evaluateAiMappingWithRetry(matchPair, valuePairsForLLM, valuePairsForValidation, parentResult);
            if (aiExpression != null) {
                var aiQuality = qualityAssessor.assessMappingsQuality(valuePairsForValidation, aiExpression, ctx.task, parentResult)
                        .quality();
                if (aiQuality > bestExpectedQuality) {
                    LOGGER.info("AI mapping has better quality ({}) than current best ({})", aiQuality, bestExpectedQuality);
                    bestExpression = aiExpression;
                    bestExpectedQuality = aiQuality;
                    isSystemProvided = false;
                }
            }
        }

        return MappingEvaluationResult.of(bestExpression, bestExpectedQuality, isSystemProvided);
    }

    private ExpressionType evaluateAiMappingWithRetry(
            SchemaMatchOneResultType matchPair,
            ValuesPairSample<?, ?> valuePairsForLLM,
            ValuesPairSample<?, ?> valuePairsForValidation,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException {
        LOGGER.trace("Going to ask LLM about mapping script");
        String errorLog = null;
        String retryScript = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            var mappingResponse = askMicroservice(matchPair, valuePairsForLLM, errorLog, retryScript);
            retryScript = mappingResponse != null ? mappingResponse.getTransformationScript() : null;
            var aiExpression = buildScriptExpression(mappingResponse);
            try {
                var aiAssessment = qualityAssessor.assessMappingsQuality(
                        valuePairsForValidation, aiExpression, ctx.task, parentResult);
                if (aiAssessment != null && aiAssessment.status() == MappingsQualityAssessor.AssessmentStatus.OK) {
                    return aiExpression;
                }
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
        return null;
    }

    private void validateQualityThreshold(
            MappingEvaluationResult result,
            SchemaMatchOneResultType matchPair) throws LowQualityMappingException {
        if (result.expectedQuality() != null && result.expectedQuality() < MINIMUM_QUALITY_THRESHOLD) {
            throw new LowQualityMappingException(
                    result.expectedQuality(),
                    MINIMUM_QUALITY_THRESHOLD,
                    matchPair.getShadowAttributePath(),
                    matchPair.getFocusPropertyPath());
        }
    }

    /**
     * Result of mapping evaluation containing the expression, quality assessment, and provenance information.
     */
    private record MappingEvaluationResult(
            @Nullable ExpressionType expression,
            @Nullable Float expectedQuality,
            boolean isSystemProvided
    ) {
        static MappingEvaluationResult of(
                @Nullable ExpressionType expression,
                @Nullable Float expectedQuality,
                boolean isSystemProvided) {
            return new MappingEvaluationResult(expression, expectedQuality, isSystemProvided);
        }
    }

}
