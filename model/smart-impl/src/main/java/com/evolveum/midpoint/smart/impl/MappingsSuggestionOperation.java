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

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.mappings.LowQualityMappingException;
import com.evolveum.midpoint.smart.impl.mappings.MissingSourceDataException;
import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPair;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
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
    private static final int DEVELOPMENT_EXAMPLES_COUNT = 200;
    private static final float MISSING_DATA_THRESHOLD = 0.05f;
    private static final float MINIMUM_QUALITY_THRESHOLD = 0.1f;

    private static final String ID_SHADOWS_COLLECTION = "shadowsCollection";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";
    private final TypeOperationContext ctx;
    private final MappingsQualityAssessor qualityAssessor;
    private final OwnedShadowsProvider ownedShadowsProvider;
    private final boolean isInbound;

    private enum MappingDirection { INBOUND, OUTBOUND }

    private MappingsSuggestionOperation(
            TypeOperationContext ctx,
            MappingsQualityAssessor qualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider,
            boolean isInbound) {
        this.ctx = ctx;
        this.qualityAssessor = qualityAssessor;
        this.ownedShadowsProvider = ownedShadowsProvider;
        this.isInbound = isInbound;
    }

    static MappingsSuggestionOperation init(
            ServiceClient serviceClient,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable CurrentActivityState<?> activityState,
            MappingsQualityAssessor qualityAssessor,
            OwnedShadowsProvider ownedShadowsProvider,
            boolean isInbound,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return new MappingsSuggestionOperation(
                TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, activityState, task, result),
                qualityAssessor,
                ownedShadowsProvider,
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

        var ownedList = collectOwnedShadows(result);
        int llmDataCount = Math.min(LLM_EXAMPLES_COUNT, ownedList.size());
        int developmentDataCount = Math.min(DEVELOPMENT_EXAMPLES_COUNT, ownedList.size());
        var shadowsForLLM = ownedList.subList(0, llmDataCount);
        var shadowsForDevelopment = ownedList.subList(ownedList.size() - developmentDataCount, ownedList.size());
        LOGGER.trace("LLM data count = {}, Development data count={}, Total={}", llmDataCount, developmentDataCount, ownedList.size());
        ctx.checkIfCanRun();

        var mappingsSuggestionState = ctx.stateHolderFactory.create(ID_MAPPINGS_SUGGESTION, result);
        mappingsSuggestionState.setExpectedProgress(schemaMatch.getSchemaMatchResult().size());
        try {
            var suggestion = new MappingsSuggestionType();
            var direction = resolveDirection();

            for (SchemaMatchOneResultType matchPair : schemaMatch.getSchemaMatchResult()) {
                var op = mappingsSuggestionState.recordProcessingStart(matchPair.getShadowAttribute().getName());
                mappingsSuggestionState.flush(result);
                ItemPath shadowAttrPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getShadowAttributePath());
                ItemPath focusPropPath = PrismContext.get().itemPathParser().asItemPath(matchPair.getFocusPropertyPath());
                var valuePairsForLLM = ValuesPairSample.of(focusPropPath, shadowAttrPath)
                        .from(shadowsForLLM);
                var valuePairsForDevelopment = ValuesPairSample.of(focusPropPath, shadowAttrPath)
                        .from(shadowsForDevelopment);
                try {
                    suggestion.getAttributeMappings().add(
                            suggestMapping(
                                    matchPair,
                                    valuePairsForLLM,
                                    valuePairsForDevelopment,
                                    direction,
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

    private List<OwnedShadow> collectOwnedShadows(OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
        var state = ctx.stateHolderFactory.create(ID_SHADOWS_COLLECTION, result);
        state.setExpectedProgress(LLM_EXAMPLES_COUNT + DEVELOPMENT_EXAMPLES_COUNT);
        state.flush(result); // because finding an owned shadow can take a while
        try {
            return ownedShadowsProvider.fetch(ctx, state, result, LLM_EXAMPLES_COUNT + DEVELOPMENT_EXAMPLES_COUNT);
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
            ValuesPairSample<?, ?> valuePairsForDevelopment,
            MappingDirection direction,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, LowQualityMappingException,
            MissingSourceDataException {

        LOGGER.trace("Going to suggest {} mapping for {} <-> {}", direction,
                matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath());

        ExpressionType expression = null;
        MappingsQualityAssessor.AssessmentResult assessment = null;

        if (valuePairsForLLM.pairs().isEmpty() || valuePairsForDevelopment.pairs().isEmpty()) {
            LOGGER.trace(" -> no data pairs, so we'll use 'asIs' mapping (without calling LLM)");
        } else if (isSourceDataMissing(valuePairsForDevelopment.pairs(), direction)) {
            throw new MissingSourceDataException(matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath());
        } else if (isTargetDataMissing(valuePairsForDevelopment.pairs(), direction)) {
            LOGGER.trace(" -> target data missing; assuming 'asIs' is fine (no LLM call)");
        } else if (canUseAsIsMapping(valuePairsForDevelopment, direction)) {
            LOGGER.trace(" -> 'asIs' does suffice according to the data ({}), so we'll use it (no LLM)", direction);
            assessment = this.qualityAssessor.assessMappingsQuality(
                    valuePairsForDevelopment, expression, direction == MappingDirection.INBOUND, this.ctx.task, parentResult);
        } else {
            LOGGER.trace(" -> going to ask LLM about mapping script");
            String errorLog = null;
            String retryScript = null;

            for (int attempt = 1; attempt <= 2; attempt++) {
                var mappingResponse = askMicroservice(matchPair, valuePairsForLLM.pairs(), errorLog, retryScript, direction);
                retryScript = mappingResponse != null ? mappingResponse.getTransformationScript() : null;
                expression = buildScriptExpression(mappingResponse);
                try {
                    assessment = this.qualityAssessor.assessMappingsQuality(
                            valuePairsForDevelopment, expression, direction == MappingDirection.INBOUND, this.ctx.task, parentResult);
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
                valuePairsForDevelopment.shadowAttributePath(), valuePairsForDevelopment.focusPropertyPath(),
                assessment != null ? assessment.quality() : null, expression, direction);
        SmartMetadataUtil.markAsAiProvided(suggestion); // everything is AI-provided now
        return suggestion;
    }

    /** Builds the final suggestion structure for the given direction while encapsulating path handling quirks. */
    private static AttributeMappingsSuggestionType buildAttributeMappingSuggestion(
            ItemPath shadowAttrPath,
            ItemPath focusPropPath,
            Float expectedQuality,
            @Nullable ExpressionType expression,
            MappingDirection direction) {
        var sanitizedFocusPath = sanitizeFocusPathForTarget(focusPropPath);
        var def = new ResourceAttributeDefinitionType()
                .ref(shadowAttrPath.rest().toBean()); // FIXME! what about activation, credentials, etc?

        if (direction == MappingDirection.INBOUND) {
            def.inbound(new InboundMappingType()
                    .name(shadowAttrPath.lastName().getLocalPart() + "-into-" + focusPropPath)
                    .strength(MappingStrengthType.STRONG)
                    .target(new VariableBindingDefinitionType().path(sanitizedFocusPath.toBean()))
                    .expression(expression));
        } else {
            def.outbound(new OutboundMappingType()
                    .name(focusPropPath + "-to-" + shadowAttrPath.lastName().getLocalPart())
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
            Collection<? extends ValuesPair<?, ?>> valuesPairs,
            @Nullable String errorLog,
            @Nullable String retryScript,
            MappingDirection direction) throws SchemaException {
        var siRequest = new SiSuggestMappingRequestType()
                .applicationAttribute(matchPair.getShadowAttribute())
                .midPointAttribute(matchPair.getFocusProperty())
                .inbound(direction == MappingDirection.INBOUND)
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

    /** Returns {@code true} if a simple "asIs" mapping is sufficient for the given direction. */
    private boolean canUseAsIsMapping(ValuesPairSample<?, ?> sample, MappingDirection direction) {
        PrismPropertyDefinition<?> targetDef = getTargetDefinition(sample, direction);
        if (targetDef == null) {
            LOGGER.trace("No definition available; cannot verify asIs mapping.");
            return false;
        }
        for (var valuesPair : sample.pairs()) {
            var sourceValues = getSourceValues(valuesPair, direction);
            var targetValues = getTargetValues(valuesPair, direction);
            if (sourceValues.size() != targetValues.size()) {
                return false;
            }
            var expectedTargetValues = new ArrayList<>(sourceValues.size());
            for (Object sourceValue : sourceValues) {
                Object converted;
                try {
                    converted = ExpressionUtil.convertValue(
                            targetDef.getTypeClass(), null, sourceValue, ctx.b.protector);
                } catch (Exception e) {
                    // Conversion not possible (e.g., different types) -> transformation is needed.
                    LOGGER.trace("Value conversion failed ({}), assuming transformation is needed: {} (value: {})",
                            direction, e.getMessage(), sourceValue);
                    return false;
                }
                if (converted != null) {
                    expectedTargetValues.add(converted);
                }
            }
            if (!MiscUtil.unorderedCollectionEquals(targetValues, expectedTargetValues)) {
                return false;
            }
        }
        return true;
    }

    /** Direction-aware accessor for source values from a pair. */
    private Collection<?> getSourceValues(ValuesPair<?, ?> pair, MappingDirection direction) {
        var values = direction == MappingDirection.INBOUND ? pair.shadowValues() : pair.focusValues();
        return values != null ? values : List.of();
    }

    /** Direction-aware accessor for target values from a pair. */
    private Collection<?> getTargetValues(ValuesPair<?, ?> pair, MappingDirection direction) {
        var values = direction == MappingDirection.INBOUND ? pair.focusValues() : pair.shadowValues();
        return values != null ? values : List.of();
    }

    /** Direction-aware accessor for target definition. */
    private PrismPropertyDefinition<?> getTargetDefinition(ValuesPairSample<?, ?> sample, MappingDirection direction) {
        if (direction == MappingDirection.INBOUND) {
            return ctx.getFocusTypeDefinition().findPropertyDefinition(sample.focusPropertyPath());
        } else {
            var shadowAttrName = sample.shadowAttributePath().rest().asSingleNameOrFail();
            return ctx.typeDefinition.findSimpleAttributeDefinition(shadowAttrName);
        }
    }

    /**
     * Returns {@code true} if target data is missing.
     * Data is considered missing if less than 5% of pairs have non-empty target values.
     */
    private boolean isTargetDataMissing(Collection<? extends ValuesPair<?, ?>> valuesPairs, MappingDirection direction) {
        if (valuesPairs.isEmpty()) {
            return true;
        }
        long countWithValues = valuesPairs.stream()
                .filter(pair -> !getTargetValues(pair, direction).isEmpty())
                .count();
        double percentageWithData = (double) countWithValues / valuesPairs.size();
        return percentageWithData < MISSING_DATA_THRESHOLD;
    }

    /**
     * Returns {@code true} if source data is missing.
     * Data is considered missing if less than 5% of pairs have non-empty source values.
     */
    private boolean isSourceDataMissing(Collection<? extends ValuesPair<?, ?>> valuesPairs, MappingDirection direction) {
        if (valuesPairs.isEmpty()) {
            return true;
        }
        long countWithValues = valuesPairs.stream()
                .filter(pair -> !getSourceValues(pair, direction).isEmpty())
                .count();
        double percentageWithData = (double) countWithValues / valuesPairs.size();
        return percentageWithData < MISSING_DATA_THRESHOLD;
    }

}
