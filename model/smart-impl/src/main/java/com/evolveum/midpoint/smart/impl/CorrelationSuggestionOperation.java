/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.InboundMappingConfigItem;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AiUtil;
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
     *
     * . identify correlation-capable properties (like name, personalNumber, emailAddress, ...)
     * . ask for schema matchings for these properties
     * . if there are any, suggest the correlation - for the first one, if there are multiple
     *
     * Future improvements:
     *
     * . when suggesting mappings to correlation-capable properties, LLM should take into account the information about
     * whether source attribute is unique or not
     *
     */
    CorrelationSuggestionsType suggestCorrelation(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var correlators = KnownCorrelator.getAllFor(ctx.getFocusTypeDefinition().getCompileTimeClass());
        var suggestions = suggestCorrelationMappings(ctx.typeDefinition, ctx.getFocusTypeDefinition(), correlators, ctx.resource);

        var evaluationsIterator = new CorrelatorEvaluator(ctx, suggestions)
                .evaluateSuggestions(result)
                .iterator();

        var suggestionsBean = new CorrelationSuggestionsType();
        for (var suggestion : suggestions) {
            var suggestionBean = new CorrelationSuggestionType();
            if (suggestion.attributeDefinitionBean() != null) {
                // no need to mark it as AI-provided, as it should already marked as such
                suggestionBean.getAttributes().add(suggestion.attributeDefinitionBean());
            }
            var correlationDefinition = new CorrelationDefinitionType()
                    .correlators(new CompositeCorrelatorType()
                            .items(new ItemsSubCorrelatorType()
                                    .name("Dummy name") //TODO change after (v2) correlation LLM integration
                                    .displayName("Dummy display name") //TODO
                                    .description("Dummy description. Suggested based on matching of "
                                            + suggestion.resourceAttrPath() + " to "
                                            + suggestion.focusItemPath()) //TODO
                                    .composition(new CorrelatorCompositionDefinitionType()
                                            .weight(1.0) //TODO
                                            .tier(1)     //TODO
                                    )
                                    .item(new CorrelationItemType()
                                            .ref(suggestion.focusItemPath().toBean()))));
            AiUtil.markAsAiProvided(correlationDefinition);
            suggestionBean.setCorrelation(correlationDefinition);
            suggestionBean.setQuality(evaluationsIterator.next());
            suggestionsBean.getSuggestion().add(suggestionBean);
        }
        return suggestionsBean;
    }

    /** Returns suggestions for correlators - in the same order as the correlators are provided. */
    private List<CorrelatorSuggestion> suggestCorrelationMappings(
            ResourceObjectTypeDefinition objectTypeDef,
            PrismObjectDefinition<?> focusDef,
            List<? extends ItemPath> correlators,
            ResourceType resource)
            throws SchemaException, ConfigurationException {
        SchemaMatchingOperation matchingOp = new SchemaMatchingOperation(ctx);
        var siResponse = matchingOp.matchSchema(objectTypeDef, focusDef, resource);
        var response = new ArrayList<CorrelatorSuggestion>();
        for (ItemPath correlator : correlators) {
            var existingInboundMapping = findExistingInboundMapping(correlator);
            if (existingInboundMapping != null) {
                response.add(
                        new CorrelatorSuggestion(
                                correlator,
                                existingInboundMapping.scoredAttributePath,
                                null));
            } else {
                for (var siAttributeMatch : siResponse.getAttributeMatch()) {
                    var focusItemPath = matchingOp.getFocusItemPath(siAttributeMatch.getMidPointAttribute());
                    if (correlator.equivalent(focusItemPath)) {
                        var resourceAttrPath = matchingOp.getApplicationItemPath(siAttributeMatch.getApplicationAttribute());
                        var resourceAttrName = resourceAttrPath.rest(); // skipping "c:attributes"; TODO handle or skip other cases
                        var inbound = new InboundMappingType()
                                .target(new VariableBindingDefinitionType()
                                        .path(focusItemPath.toBean()))
                                .use(InboundMappingUseType.CORRELATION);
                        var attrDefBean = new ResourceAttributeDefinitionType()
                                .ref(resourceAttrName.toBean())
                                .inbound(inbound);
                        AiUtil.markAsAiProvided(attrDefBean, ResourceAttributeDefinitionType.F_REF);
                        AiUtil.markAsAiProvided(inbound, InboundMappingType.F_TARGET);
                        // Use is not provided by AI, it is set to CORRELATION by default.
                        response.add(
                                new CorrelatorSuggestion(focusItemPath, resourceAttrPath, attrDefBean));
                        break; // we don't want to suggest multiple attributes for the same correlator
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

    /**
     * Information about whether inbound mapping for given correlator exists. The scoredAttributePath is present if it
     * makes sense to evaluate the correlation score also by looking at values of that attribute.
     */
    private record ExistingMapping(@Nullable ItemPath scoredAttributePath) {
    }

    /**
     * The suggestion for correlation may be based on an existing inbound mapping (in which case attributeDefinitionBean is null,
     * and resourceAttrPath is present if and only if the existing mapping is "as-is" mapping; at least for now), or it may
     * involve creation of a new inbound mapping (in which case attributeDefinitionBean is non-null,
     * and resourceAttrPath is always present).
     *
     * The rationale behind this is that when scoring the suggestion, we look at the statistics of both focus item and
     * resource attribute. So we need to know the resource attribute even if it is not being suggested to be created.
     * But only if it makes sense to score it, which is currently not the case for "transforming" mappings.
     */
    record CorrelatorSuggestion(
            ItemPath focusItemPath,
            @Nullable ItemPath resourceAttrPath,
            @Nullable ResourceAttributeDefinitionType attributeDefinitionBean) {
    }
}
