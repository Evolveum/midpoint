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
import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPair;
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

    private static final int ATTRIBUTE_MAPPING_EXAMPLES = 20;
    private static final int ATTRIBUTE_TESTING_EXAMPLES = 200;

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

    private MappingDirection resolveDirection() throws ConfigurationException {
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
            var direction = resolveDirection();

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
                                    shadowAttrPath,
                                    focusPropPath,
                                    suggestionPairs,
                                    testingPairs,
                                    direction,
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
            ItemPath shadowAttrPath,
            ItemPath focusPropPath,
            Collection<ValuesPair> valuesPairs,
            Collection<ValuesPair> testingValuesPairs,
            MappingDirection direction,
            OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, SecurityViolationException {

        LOGGER.trace("Going to suggest {} mapping for {} <-> {} based on {} values pairs",
                direction, matchPair.getShadowAttributePath(), matchPair.getFocusPropertyPath(), valuesPairs.size());

        var propertyDef = ctx.getFocusTypeDefinition().findPropertyDefinition(focusPropPath);
        var shadowAttrName = shadowAttrPath.rest().asSingleNameOrFail();
        var shadowAttrDef = ctx.typeDefinition.findSimpleAttributeDefinition(shadowAttrName);

        ExpressionType expression = null;
        MappingsQualityAssessor.AssessmentResult assessment = null;
        String variableName = propertyDef.getItemName().getLocalPart();

        if (isScriptNeeded(valuesPairs, propertyDef, shadowAttrDef, direction)) {
            String errorLog = null;
            String retryScript = null;

            for (int attempt = 1; attempt <= 2; attempt++) {
                var mappingResponse = askMicroservice(matchPair, valuesPairs, errorLog, retryScript, direction);
                retryScript = mappingResponse != null ? mappingResponse.getTransformationScript() : null;
                expression = buildScriptExpression(mappingResponse);
                try {
                    assessment = this.qualityAssessor.assessMappingsQuality(
                            testingValuesPairs, expression, direction == MappingDirection.INBOUND, variableName, this.ctx.task, parentResult);
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
        } else {
            assessment = this.qualityAssessor.assessMappingsQuality(
                    testingValuesPairs, expression, direction == MappingDirection.INBOUND, variableName, this.ctx.task, parentResult);
        }

        AttributeMappingsSuggestionType suggestion = buildAttributeMappingSuggestion(
                shadowAttrPath, focusPropPath, assessment.quality(), expression, direction);
        SmartMetadataUtil.markAsAiProvided(suggestion); // everything is AI-provided now
        return suggestion;
    }

    /** Builds the final suggestion structure for the given direction while encapsulating path handling quirks. */
    private static AttributeMappingsSuggestionType buildAttributeMappingSuggestion(
            ItemPath shadowAttrPath,
            ItemPath focusPropPath,
            float expectedQuality,
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
            Collection<ValuesPair> valuesPairs,
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
    private boolean doesAsIsSuffice(
            Collection<ValuesPair> valuesPairs,
            PrismPropertyDefinition<?> targetDef,
            MappingDirection direction) {
        if (targetDef == null) {
            LOGGER.trace("No definition available; cannot verify asIs mapping.");
            return false;
        }
        for (var valuesPair : valuesPairs) {
            var sourceValues = direction == MappingDirection.INBOUND
                    ? (valuesPair.shadowValues() != null ? valuesPair.shadowValues() : List.of())
                    : (valuesPair.focusValues() != null ? valuesPair.focusValues() : List.of());
            var targetValues = direction == MappingDirection.INBOUND
                    ? (valuesPair.focusValues() != null ? valuesPair.focusValues() : List.of())
                    : (valuesPair.shadowValues() != null ? valuesPair.shadowValues() : List.of());
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
                            direction == MappingDirection.INBOUND ? "inbound" : "outbound",
                            e.getMessage(), sourceValue);
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

    /** Returns {@code true} if a transformation script is needed, direction-aware. */
    private boolean isScriptNeeded(
            Collection<ValuesPair> valuesPairs,
            PrismPropertyDefinition<?> focusPropertyDef,
            PrismPropertyDefinition<?> shadowAttrDef,
            MappingDirection direction) {
        if (valuesPairs.isEmpty()) {
            LOGGER.trace(" -> no data pairs, so we'll use 'asIs' mapping (without calling LLM)");
            return false;
        }
        boolean allNulls = valuesPairs.stream().allMatch(pair ->
                (pair.shadowValues() == null || pair.shadowValues().stream().allMatch(Objects::isNull))
                        || (pair.focusValues() == null || pair.focusValues().stream().allMatch(Objects::isNull)));
        if (allNulls) {
            LOGGER.trace(" -> all shadow or focus values are null, using 'asIs' mapping (without calling LLM)");
            return false;
        }
        if (direction == MappingDirection.INBOUND) {
            if (doesAsIsSuffice(valuesPairs, focusPropertyDef, direction)) {
                LOGGER.trace(" -> 'asIs' does suffice according to the data (inbound), so we'll use it (no LLM)");
                return false;
            }
            if (isTargetDataMissing(valuesPairs, direction)) {
                LOGGER.trace(" -> target data missing (focus); assuming 'asIs' is fine (no LLM call)");
                return false;
            }
        } else { // OUTBOUND
            if (doesAsIsSuffice(valuesPairs, shadowAttrDef, direction)) {
                LOGGER.trace(" -> 'asIs' does suffice according to the data (outbound), so we'll use it (no LLM)");
                return false;
            }
            if (isTargetDataMissing(valuesPairs, direction)) {
                LOGGER.trace(" -> target data missing (shadow); assuming 'asIs' is fine (no LLM call)");
                return false;
            }
        }

        LOGGER.trace(" -> going to ask LLM about mapping script");
        return true;
    }

    /** Returns {@code true} if there are no target data altogether, direction-aware. */
    private boolean isTargetDataMissing(Collection<ValuesPair> valuesPairs, MappingDirection direction) {
        if (direction == MappingDirection.INBOUND) {
            return valuesPairs.stream().allMatch(pair -> pair.focusValues() == null || pair.focusValues().isEmpty());
        } else {
            return valuesPairs.stream().allMatch(pair -> pair.shadowValues() == null || pair.shadowValues().isEmpty());
        }
    }

}
