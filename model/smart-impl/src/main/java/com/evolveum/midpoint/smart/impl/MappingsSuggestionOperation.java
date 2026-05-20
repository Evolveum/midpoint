/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.smart.api.ServiceClient.Method.SUGGEST_CATEGORICAL_MAPPING;
import static com.evolveum.midpoint.smart.api.ServiceClient.Method.SUGGEST_MAPPING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.prism.path.PathSet;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.smart.impl.mappings.CategoricalAttributeRegistry;
import com.evolveum.midpoint.smart.impl.wellknownschemas.SystemMappingSuggestion;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaProvider;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.smart.impl.mappings.heuristics.HeuristicRuleMatcher;
import com.evolveum.midpoint.smart.impl.mappings.LowQualityMappingException;
import com.evolveum.midpoint.smart.impl.mappings.MappingDirection;
import com.evolveum.midpoint.smart.impl.mappings.MissingSourceDataException;
import com.evolveum.midpoint.smart.impl.mappings.ShadowWithOwner;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.smart.impl.scoring.MappingScriptValidator;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.smart.impl.scoring.ScriptValidationException;
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
 *
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

    private static final String ID_SHADOWS_COLLECTION = "collectingShadowsWithOwners";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";
    private final TypeOperationContext ctx;
    private final MappingsQualityAssessor qualityAssessor;
    private final MappingScriptValidator scriptValidator;
    private final ShadowsWithOwnersProvider shadowsWithOwnersProvider;
    private final WellKnownSchemaService wellKnownSchemaService;
    private final HeuristicRuleMatcher heuristicRuleMatcher;
    private final CategoricalAttributeRegistry categoricalAttributeRegistry;
    private final boolean isInbound;
    private final boolean useAiService;
    @Nullable private final ShadowObjectClassStatisticsType objectTypeStatistics;
    private final int retryCount;

    private MappingsSuggestionOperation(
            TypeOperationContext ctx,
            MappingsQualityAssessor qualityAssessor,
            ShadowsWithOwnersProvider shadowsWithOwnersProvider,
            MappingScriptValidator scriptValidator,
            WellKnownSchemaService wellKnownSchemaService,
            HeuristicRuleMatcher heuristicRuleMatcher,
            CategoricalAttributeRegistry categoricalAttributeRegistry,
            boolean isInbound,
            boolean useAiService,
            @Nullable ShadowObjectClassStatisticsType objectTypeStatistics,
            int retryCount) {
        this.ctx = ctx;
        this.qualityAssessor = qualityAssessor;
        this.scriptValidator = scriptValidator;
        this.shadowsWithOwnersProvider = shadowsWithOwnersProvider;
        this.wellKnownSchemaService = wellKnownSchemaService;
        this.heuristicRuleMatcher = heuristicRuleMatcher;
        this.categoricalAttributeRegistry = categoricalAttributeRegistry;
        this.isInbound = isInbound;
        this.useAiService = useAiService;
        this.objectTypeStatistics = objectTypeStatistics;
        this.retryCount = retryCount;
    }

    static MappingsSuggestionOperation init(
            TypeOperationContext ctx,
            MappingsQualityAssessor qualityAssessor,
            MappingScriptValidator scriptValidator,
            ShadowsWithOwnersProvider shadowsWithOwnersProvider,
            WellKnownSchemaService wellKnownSchemaService,
            HeuristicRuleMatcher heuristicRuleMatcher,
            CategoricalAttributeRegistry categoricalAttributeRegistry,
            boolean isInbound,
            boolean useAiService,
            @Nullable ShadowObjectClassStatisticsType objectTypeStatistics,
            int retryCount)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return new MappingsSuggestionOperation(
                ctx,
                qualityAssessor,
                shadowsWithOwnersProvider,
                scriptValidator,
                wellKnownSchemaService,
                heuristicRuleMatcher,
                categoricalAttributeRegistry,
                isInbound,
                useAiService,
                objectTypeStatistics,
                retryCount);
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

        var ownedShadows = collectOwnedShadows(result);
        int llmDataCount = Math.min(LLM_EXAMPLES_COUNT, ownedShadows.size());
        int validationDataCount = Math.min(VALIDATION_EXAMPLES_COUNT, ownedShadows.size());
        var shadowsForLLM = ownedShadows.subList(0, llmDataCount);
        var shadowsForValidation = ownedShadows.subList(ownedShadows.size() - validationDataCount, ownedShadows.size());
        LOGGER.trace("LLM data count = {}, Validation data count={}, Total={}.", llmDataCount, validationDataCount, ownedShadows.size());
        ctx.checkIfCanRun();

        var mappingsSuggestionState = ctx.stateHolderFactory.create(ID_MAPPINGS_SUGGESTION, result);
        mappingsSuggestionState.setExpectedProgress(schemaMatch.getSchemaMatchResult().size());
        try {
            var suggestion = new MappingsSuggestionType();
            var direction = resolveDirection();
            var existingTargetPaths = collectExistingMappingTargetPaths();
            var excludedTargetPaths = mergeExcludedPaths(existingTargetPaths, targetPathsToIgnore);
            var mappingCandidates = new AttributeMappingCandidateSet(excludedTargetPaths);

            wellKnownSchemaService.getProviderFromSchemaMatch(schemaMatch)
                    .map(knownSchemaProvider -> collectSystemMappings(knownSchemaProvider, shadowsForValidation, result))
                    .orElse(Collections.emptyList())
                    .forEach(mappingCandidates::proposeSystemMapping);

            var mappingFutures = new ArrayList<CompletableFuture<Void>>();

            for (SchemaMatchOneResultType matchPair : schemaMatch.getSchemaMatchResult()) {
                ItemPath shadowAttrPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getShadowAttributePath());
                ItemPath focusPropPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getFocusPropertyPath());

                AtomicReference<Operation> operationReference = new AtomicReference<>();
                AtomicReference<OperationResult> mappingResultReference = new AtomicReference<>();
                var future = CompletableFuture.supplyAsync(() -> {
                    try {
                        String matchPairDescription = shadowAttrPath + " <-> " + focusPropPath;
                        var op = mappingsSuggestionState.recordProcessingStart(matchPairDescription);

                        operationReference.set(op);
                        OperationResult mappingResult = result.createSubresult(
                                "Mapping suggestion for the %s pair".formatted(matchPairDescription));
                        mappingResultReference.set(mappingResult);

                        if (shouldSkipReadOnlyAttribute(shadowAttrPath)) {
                            LOGGER.debug("Skipping read-only attribute for {} mapping: {}", direction, shadowAttrPath);
                            return null;
                        }

                        var valuePairsForLLM = ValuesPairSample.of(focusPropPath, shadowAttrPath, direction)
                                .from(shadowsForLLM);
                        var valuePairsForValidation = ValuesPairSample.of(focusPropPath, shadowAttrPath, direction)
                                .from(shadowsForValidation);

                        mappingsSuggestionState.flush(result);
                        return suggestMapping(matchPair, valuePairsForLLM, valuePairsForValidation, mappingResult);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).thenAccept(aiMapping -> {
                    Operation op = operationReference.get();
                    if (aiMapping != null) {
                        mappingCandidates.propose(aiMapping);
                        mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.SUCCESS);
                    } else {
                        mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.SKIP);
                    }
                }).exceptionally(e -> {
                    Throwable cause = e.getCause() != null // e = CompletionException
                            ? e.getCause().getCause() != null // e.getCause = RuntimeException
                                ? e.getCause().getCause() // e.getCause.getCause = Actual interesting exception
                                : e.getCause()
                            : e;
                    Operation op = operationReference.get();
                    if (cause instanceof LowQualityMappingException) {
                        LOGGER.debug("Skipping mapping due to low quality: {}", cause.getMessage());
                        mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.SKIP);
                    } else if (cause instanceof MissingSourceDataException) {
                        LOGGER.debug("Skipping mapping due to missing source data: {}", cause.getMessage());
                        mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.SKIP);
                    } else {
                        LoggingUtils.logException(LOGGER, "Couldn't suggest mapping for {}", cause,
                                matchPair.getShadowAttributePath());
                        mappingsSuggestionState.recordProcessingEnd(op, ItemProcessingOutcomeType.FAILURE);
                    }
                    return null;
                }).thenRun(() -> {
                    OperationResult mappingResult = mappingResultReference.get();
                    mappingsSuggestionState.flushIfNeeded(mappingResult);
                    mappingResult.close();
                });
                mappingFutures.add(future);
            }

            CompletableFuture.allOf(mappingFutures.toArray(new CompletableFuture[]{})).join();

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
        var existingPaths = new PathSet();
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
                    existingPaths.add(attrDef.getStandardPath().rest());
                }
            }
        }
        LOGGER.trace("Collected {} existing {} mapping target paths for deduplication.", existingPaths.size(), isInbound ? "inbound" : "outbound");
        return existingPaths;
    }

    /**
     * Merges existing mapping paths and accepted suggestion paths into a single list of excluded paths.
     * These paths should be excluded from new mapping suggestions.
     *
     * Returned set is frozen.
     */
    private PathSet mergeExcludedPaths(
            Collection<ItemPath> existingTargetPaths,
            @Nullable Collection<ItemPath> targetPathsToIgnore) {
        var merged = new PathSet(existingTargetPaths);
        if (targetPathsToIgnore != null && !targetPathsToIgnore.isEmpty()) {
            merged.addAll(targetPathsToIgnore);
            LOGGER.trace("Merged {} existing mapping paths and {} accepted suggestion paths for exclusion.",
                    existingTargetPaths.size(), targetPathsToIgnore.size());
        }
        merged.freeze();
        return merged;
    }

    private List<AttributeMappingsSuggestionType> collectSystemMappings(
            WellKnownSchemaProvider knownSchemaProvider,
            List<ShadowWithOwner> shadowsForValidation,
            OperationResult result) {
        var mappings = isInbound
                ? knownSchemaProvider.suggestInboundMappings(ctx.resource.getName().getOrig())
                : knownSchemaProvider.suggestOutboundMappings(
                        shadowsForValidation.stream().map(ShadowWithOwner::shadow).toList());
        return mappings.stream()
                .map(systemMapping -> assessAndBuildSystemMapping(systemMapping, shadowsForValidation, result))
                .flatMap(opt -> opt.stream())
                .toList();
    }

    private Optional<AttributeMappingsSuggestionType> assessAndBuildSystemMapping(
            SystemMappingSuggestion systemMapping,
            List<ShadowWithOwner> shadowsForValidation,
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

            var mappingSuggestion = buildAttributeMappingSuggestion(valuePairs, quality, systemMapping.expression(), systemMapping.strength());
            SmartMetadataUtil.markContainerProvenance(
                    mappingSuggestion.asPrismContainerValue(),
                    SmartMetadataUtil.ProvenanceKind.SYSTEM);
            return Optional.of(mappingSuggestion);
        } catch (Exception e) {
            LOGGER.debug("Failed to assess system mapping quality for {}: {}", systemMapping.shadowAttributePath(), e.getMessage());
            return Optional.empty();
        }
    }

    private List<ShadowWithOwner> collectOwnedShadows(OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
        var state = ctx.stateHolderFactory.create(ID_SHADOWS_COLLECTION, result);
        state.setExpectedProgress(LLM_EXAMPLES_COUNT + VALIDATION_EXAMPLES_COUNT);
        state.flush(result); // because finding an owned shadow can take a while
        try {
            return shadowsWithOwnersProvider.fetch(ctx, state, result, LLM_EXAMPLES_COUNT + VALIDATION_EXAMPLES_COUNT);
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
                result.expression(),
                MappingStrengthType.STRONG);

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
            @Nullable ExpressionType expression,
            MappingStrengthType strength) {
        var sanitizedFocusPath = sanitizeFocusPathForTarget(pairSample.focusPropertyPath());
        var def = new ResourceAttributeDefinitionType()
                .ref(pairSample.shadowAttributePath().rest().toBean()); // FIXME! what about activation, credentials, etc?

        if (pairSample.direction() == MappingDirection.INBOUND) {
            def.inbound(new InboundMappingType()
                    .name(pairSample.shadowAttributePath().lastName().getLocalPart() + "-into-" + pairSample.focusPropertyPath())
                    .strength(strength)
                    .target(new VariableBindingDefinitionType().path(sanitizedFocusPath.toBean()))
                    .expression(expression));
        } else {
            def.outbound(new OutboundMappingType()
                    .name(pairSample.focusPropertyPath() + "-to-" + pairSample.shadowAttributePath().lastName().getLocalPart())
                    .strength(strength)
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
        if (script == null || script.isEmpty() || "input".equals(script) || "null".equals(script)) {
            return null;
        }
        return new ExpressionType()
                .description(scriptDescription)
                .expressionEvaluator(
                        new ObjectFactory().createScript(
                                new ScriptExpressionEvaluatorType()
                                        .language("mel")
                                        .code(script)));
    }

    private SiSuggestMappingResponseType askMicroserviceAsync(
            SchemaMatchOneResultType matchPair,
            ValuesPairSample<?, ?> valuesPairs,
            @Nullable String errorLog,
            @Nullable String retryScript) {
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
                .invokeAsync(SUGGEST_MAPPING, siRequest, SiSuggestMappingResponseType.class)
                .join();
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
            if (isInbound && objectTypeStatistics != null) {
                var shadowAttrPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getShadowAttributePath());
                var attrStats = findAttributeStatistics(shadowAttrPath);
                if (attrStats.isPresent()) {
                    int missingCount = attrStats.get().getMissingValueCount();
                    int totalSize = objectTypeStatistics.getSize();
                    if (totalSize > 0 && missingCount > MISSING_DATA_THRESHOLD * totalSize) {
                        LOGGER.trace("Skipping inbound mapping: attribute {} has low missingCount ({}) relative to total ({}).",
                                matchPair.getShadowAttributePath(), missingCount, totalSize);
                        throw new MissingSourceDataException(matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath());
                    }
                }
            }
            if (useAiService && isInbound) {
                var categoricalResult = tryCategoricalMappingSuggestion(matchPair);
                if (categoricalResult != null) {
                    return categoricalResult;
                }
            }
            LOGGER.trace("No data pairs. We'll use 'asIs' mapping (no LLM call).");
            return MappingEvaluationResult.of(null, null, isSystemProvided);
        }

        // Check for missing source data. We don't want to suggest mapping if there is not enough data.
        if (valuePairsForValidation.isSourceDataMissing(MISSING_DATA_THRESHOLD)) {
            throw new MissingSourceDataException(matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath());
        }

        // Check for missing target data
        if (valuePairsForValidation.isTargetDataMissing(MISSING_DATA_THRESHOLD)) {
            if (useAiService && isInbound) {
                var categoricalResult = tryCategoricalMappingSuggestion(matchPair);
                if (categoricalResult != null) {
                    return categoricalResult;
                }
            }
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

    /**
     * Attempts to suggest a categorical mapping when no correlated data pairs are available.
     */
    private @Nullable MappingEvaluationResult tryCategoricalMappingSuggestion(SchemaMatchOneResultType matchPair) {
        if (objectTypeStatistics == null) {
            return null;
        }

        var focusPropPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getFocusPropertyPath());
        var categoricalValues = categoricalAttributeRegistry.find(focusPropPath, ctx.getFocusClass());
        if (categoricalValues.isEmpty()) {
            return null;
        }

        var shadowAttrPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getShadowAttributePath());
        var attrStats = findAttributeStatistics(shadowAttrPath);
        if (attrStats.isEmpty()) {
            LOGGER.trace("No statistics found for shadow attribute {}, skipping categorical mapping.",
                    matchPair.getShadowAttributePath());
            return null;
        }

        if (attrStats.get().getValueCount().isEmpty()) {
            LOGGER.trace("No value distribution available for {}, skipping categorical mapping.",
                    matchPair.getShadowAttributePath());
            return null;
        }

        LOGGER.debug("Attempting categorical mapping suggestion for {} -> {}",
                matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath());

        var request = new SiSuggestCategoricalMappingRequestType()
                .applicationAttribute(matchPair.getShadowAttribute())
                .midPointAttribute(matchPair.getFocusProperty())
                .inbound(isInbound);
        attrStats.get().getValueCount().forEach(vc -> request.getApplicationAttributeValue().add(vc.getValue()));
        categoricalValues.get().forEach(v -> request.getMidPointCategoryValue().add(v));

        var response = ctx.serviceClient
                .invokeAsync(SUGGEST_CATEGORICAL_MAPPING, request, SiSuggestMappingResponseType.class)
                .join();

        var expression = buildScriptExpression(response);
        LOGGER.debug("Categorical mapping suggestion for {}: {}",
                matchPair.getShadowAttributePath(), expression != null ? "script provided" : "null/as-is");

        if (expression != null) {
            try {
                var result = new OperationResult("validateCategoricalMappingScript");
                String variableName = isInbound ? ExpressionConstants.VAR_INPUT : matchPair.getFocusProperty().getName();
                String testValue = attrStats.get().getValueCount().get(0).getValue();
                scriptValidator.testCategoricalMappingScript(expression, variableName, testValue, ctx.task, result);
            } catch (ScriptValidationException e) {
                LOGGER.warn("Categorical mapping script validation failed for {}: {}",
                        matchPair.getShadowAttributePath(), e.getMessage());
                return MappingEvaluationResult.of(null, null, false);
            }
        }

        return MappingEvaluationResult.of(expression, null, false);
    }

    /**
     * Finds the statistics entry for the given shadow attribute path.
     * Matches by the local part of the last path segment to avoid namespace sensitivity.
     */
    private Optional<ShadowAttributeStatisticsType> findAttributeStatistics(ItemPath shadowAttrPath) {
        var attrLocalName = shadowAttrPath.lastName().getLocalPart();
        return objectTypeStatistics.getAttribute().stream()
                .filter(attr -> {
                    var refPath = attr.getRef() != null ? attr.getRef().getItemPath() : null;
                    return refPath != null && refPath.lastName().getLocalPart().equals(attrLocalName);
                })
                .findFirst();
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

    @Nullable
    private ExpressionType evaluateAiMappingWithRetry(
            SchemaMatchOneResultType matchPair,
            ValuesPairSample<?, ?> valuePairsForLLM,
            ValuesPairSample<?, ?> valuePairsForValidation,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException {
        LOGGER.trace("Going to ask LLM about mapping script");
        String errorLog = null;
        String retryScript = null;

        for (int attempt = 0; attempt <= retryCount; attempt++) {
            var mappingResponse = askMicroserviceAsync(matchPair, valuePairsForLLM, errorLog, retryScript);
            retryScript = mappingResponse != null ? mappingResponse.getTransformationScript() : null;
            var aiExpression = buildScriptExpression(mappingResponse);
            try {
                var aiAssessment = qualityAssessor.assessMappingsQuality(
                        valuePairsForValidation, aiExpression, ctx.task, parentResult);
                if (aiAssessment != null && aiAssessment.status() == MappingsQualityAssessor.AssessmentStatus.OK) {
                    return aiExpression;
                }
            } catch (ExpressionEvaluationException | SecurityViolationException e) {
                if (attempt < retryCount) {
                    errorLog = e.getMessage();
                    LOGGER.warn("Validation issues found on attempt {}; retrying.", attempt);
                } else {
                    LOGGER.warn("Validation issues found in mapping script; giving up.");
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
     * Checks if the attribute should be skipped due to read-only status.
     * For outbound mappings, skip read-only attributes (canModify = false).
     * For inbound mappings, never skip (reading from resource is always allowed).
     */
    private boolean shouldSkipReadOnlyAttribute(ItemPath shadowAttrPath) {
        if (isInbound) {
            return false;
        }

        var attrName = shadowAttrPath.rest().asSingleNameOrFail();
        var attrDef = ctx.typeDefinition.findSimpleAttributeDefinition(attrName);

        if (attrDef == null) {
            LOGGER.warn("Attribute definition not found for {}, will not skip", shadowAttrPath);
            return false;
        }

        return !attrDef.canModify();
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
