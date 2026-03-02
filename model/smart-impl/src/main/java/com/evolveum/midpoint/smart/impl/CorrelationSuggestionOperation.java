/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.smart.impl.correlation.CorrelatedSuggestionWithScore;
import com.evolveum.midpoint.smart.impl.correlation.CorrelatorSuggestion;
import com.evolveum.midpoint.smart.impl.correlation.ExistingMapping;

import org.apache.commons.lang3.StringUtils;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.InboundMappingConfigItem;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Implements "suggest correlation" operation. Currently almost primitive algorithm; will be improved later. */
class CorrelationSuggestionOperation {

    private final TypeOperationContext ctx;

    CorrelationSuggestionOperation(TypeOperationContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Initial implementation of "suggest correlation" method:
     * . identify correlation-capable properties (like name, personalNumber, emailAddress, ...)
     * . ask for schema matchings for these properties
     * . if there are any, suggest the correlation - for the first one, if there are multiple
     *
     * Future improvements:
     * . when suggesting mappings to correlation-capable properties, LLM should take into account the information about
     * whether source attribute is unique or not
     *
     */
    CorrelationSuggestionsType suggestCorrelation(OperationResult result, SchemaMatchResultType schemaMatch)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var correlators = KnownCorrelator.getAllFor(ctx.getFocusTypeDefinition().getCompileTimeClass());
        var suggestions = suggestCorrelationMappings(schemaMatch, correlators);

        var allScores = new CorrelatorEvaluator(ctx, suggestions)
                .evaluateSuggestions(result);

        // For each correlator, select the attribute with highest score
        var bestSuggestionsMap = new java.util.HashMap<ItemPath, CorrelatedSuggestionWithScore>();
        for (int i = 0; i < suggestions.size(); i++) {
            var suggestion = suggestions.get(i);
            double score = allScores.get(i);
            var correlated = new CorrelatedSuggestionWithScore(suggestion, score);

            // For this correlator, keep only the highest score
            var prev = bestSuggestionsMap.get(suggestion.focusItemPath());
            if ((correlated.score() > 0) && (prev == null || correlated.score() > prev.score())) {
                bestSuggestionsMap.put(suggestion.focusItemPath(), correlated);
            }
        }

        var suggestionsBean = new CorrelationSuggestionsType();
        for (CorrelatedSuggestionWithScore correlated : bestSuggestionsMap.values()) {
            var suggestion = correlated.suggestion();
            double score = correlated.score();

            var suggestionBean = new CorrelationSuggestionType();
            if (suggestion.attributeDefinitionBean() != null) {
                // no need to mark it as AI-provided, as it should already marked as such
                suggestionBean.getAttributes().add(suggestion.attributeDefinitionBean());
            }

            String lastName = suggestion.focusItemPath().lastName().getLocalPart();
            String correlatorName = StringUtils.capitalize(lastName) + " correlator";
            var correlationDefinition = new CorrelationDefinitionType()
                    .correlators(new CompositeCorrelatorType()
                            .items(new ItemsSubCorrelatorType()
                                    .name(correlatorName) //TODO change after (v2) correlation LLM integration
                                    .displayName(correlatorName) //TODO
                                    .description("Suggested based on matching of "
                                            + suggestion.resourceAttrPath() + " to "
                                            + suggestion.focusItemPath()) //TODO
                                    .composition(new CorrelatorCompositionDefinitionType()
                                            .weight(1.0) //TODO
                                            .tier(1)     //TODO
                                    )
                                    .item(new CorrelationItemType()
                                            .ref(suggestion.focusItemPath().toBean()))));
            SmartMetadataUtil.markAsAiProvided(correlationDefinition);
            suggestionBean.setCorrelation(correlationDefinition);
            suggestionBean.setQuality(score);
            suggestionsBean.getSuggestion().add(suggestionBean);
        }
        return suggestionsBean;
    }

    /** Returns suggestions for correlators - in the same order as the correlators are provided. */
    private List<CorrelatorSuggestion> suggestCorrelationMappings(
            SchemaMatchResultType schemaMatch,
            List<? extends ItemPath> correlators) throws ConfigurationException {
        var response = new ArrayList<CorrelatorSuggestion>();
        for (ItemPath correlator : correlators) {
            var existingInboundMapping = findExistingInboundMapping(correlator);
            if (existingInboundMapping != null) {
                response.add(
                        new CorrelatorSuggestion(
                                correlator,
                                existingInboundMapping.scoredAttributePath(),
                                null));
            } else {
                for (var oneSchemaMatch : schemaMatch.getSchemaMatchResult()) {
                    var shadowItemPath = PrismContext.get().itemPathParser().asItemPath(oneSchemaMatch.getShadowAttributePath());
                    var focusItemPath = PrismContext.get().itemPathParser().asItemPath(oneSchemaMatch.getFocusPropertyPath());
                    if (correlator.equivalent(focusItemPath)) {
                        var resourceAttrName = shadowItemPath.rest(); // skipping "c:attributes"; TODO handle or skip other cases
                        var inbound = new InboundMappingType()
                                .name(shadowItemPath.lastName().getLocalPart()
                                        + "-to-" + focusItemPath) //TODO TBD
                                .target(new VariableBindingDefinitionType()
                                        .path(focusItemPath.toBean()))
                                .use(InboundMappingUseType.CORRELATION);
                        var attrDefBean = new ResourceAttributeDefinitionType()
                                .ref(resourceAttrName.toBean())
                                .inbound(inbound);
                        SmartMetadataUtil.markAsAiProvided(attrDefBean, ResourceAttributeDefinitionType.F_REF);
                        SmartMetadataUtil.markAsAiProvided(inbound, InboundMappingType.F_TARGET);
                        // Use is not provided by AI, it is set to CORRELATION by default.
                        response.add(
                                new CorrelatorSuggestion(focusItemPath, shadowItemPath, attrDefBean));
                    }
                }
            }

        }
        return response;
    }

    /**
     * Finds whether there is already an inbound mapping for the given correlator. Returns {@code null} if there is none.
     * We ignore conditions: if there is a mapping, but it has a condition, we ignore the condition and consider the mapping.
     */
    private @Nullable ExistingMapping findExistingInboundMapping(ItemPath correlator) throws ConfigurationException {
        for (ShadowAttributeDefinition<?, ?, ?, ?> attributeDefinition : ctx.typeDefinition.getAttributeDefinitions()) {
            ItemPath resourceAttrPath = attributeDefinition.getStandardPath();
            for (InboundMappingType inboundMappingBean : attributeDefinition.getInboundMappingBeans()) {
                var mappingCI = InboundMappingConfigItem.configItem(
                        inboundMappingBean, ConfigurationItemOrigin.undeterminedSafe(), InboundMappingConfigItem.class);

                if (!Boolean.FALSE.equals(mappingCI.determineApplicability(InboundMappingEvaluationPhaseType.BEFORE_CORRELATION))) {
                    // Get the target path on the focus side (e.g., user.name)
                    ItemPath targetPath = mappingCI.getTargetPath();
                    if (targetPath != null && correlator.equivalent(targetPath)) {
                        // applicable mapping for this correlator found - return resourceAttrPath
                        return new ExistingMapping(resourceAttrPath);
                    }
                }
            }
        }
        return null;
    }

}
