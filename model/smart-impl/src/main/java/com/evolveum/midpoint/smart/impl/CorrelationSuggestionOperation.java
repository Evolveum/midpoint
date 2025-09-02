/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    CorrelationSuggestionType suggestCorrelation() throws SchemaException {
        var correlators = KnownCorrelator.getAllFor(ctx.getFocusTypeDefinition().getCompileTimeClass());
        var attributeDefinitionsForCorrelators =
                suggestCorrelationMappings(ctx.typeDefinition, ctx.getFocusTypeDefinition(), correlators, ctx.resource);
        var suggestion = new CorrelationSuggestionType();
        if (!attributeDefinitionsForCorrelators.isEmpty()) {
            var first = attributeDefinitionsForCorrelators.get(0); // already marked as AI-provided
            suggestion.getAttributes().add(first.attributeDefinitionBean());
            var correlationDefinition = new CorrelationDefinitionType()
                    .correlators(new CompositeCorrelatorType()
                            .items(new ItemsSubCorrelatorType()
                                    .item(new CorrelationItemType()
                                            .ref(first.focusItemPath().toBean()))));
            AiUtil.markAsAiProvided(correlationDefinition);
            suggestion.setCorrelation(correlationDefinition);
        }
        return suggestion;
    }

    /** Returns suggestions for correlators - in the same order as the correlators are provided. */
    private List<CorrelatorSuggestion> suggestCorrelationMappings(
            ResourceObjectTypeDefinition objectTypeDef,
            PrismObjectDefinition<?> focusDef,
            List<? extends ItemPath> correlators,
            ResourceType resource)
            throws SchemaException {
        SchemaMatchingOperation matchingOp = new SchemaMatchingOperation(ctx);
        var siResponse = matchingOp.matchSchema(objectTypeDef, focusDef, resource);
        var response = new ArrayList<CorrelatorSuggestion>();
        for (ItemPath correlator : correlators) {
            for (var siAttributeMatch : siResponse.getAttributeMatch()) {
                var focusItemPath = matchingOp.getFocusItemPath(siAttributeMatch.getMidPointAttribute());
                if (correlator.equivalent(focusItemPath)) {
                    var resourceAttrPath = matchingOp.getApplicationItemPath(siAttributeMatch.getApplicationAttribute());
                    var inbound = new InboundMappingType()
                            .target(new VariableBindingDefinitionType()
                                    .path(focusItemPath.toBean()))
                            .use(InboundMappingUseType.CORRELATION);
                    var attrDefBean = new ResourceAttributeDefinitionType()
                            .ref(resourceAttrPath.toBean())
                            .inbound(inbound);
                    AiUtil.markAsAiProvided(attrDefBean, ResourceAttributeDefinitionType.F_REF);
                    AiUtil.markAsAiProvided(inbound, InboundMappingType.F_TARGET);
                    // Use is not provided by AI, it is set to CORRELATION by default.
                    response.add(
                            new CorrelatorSuggestion(focusItemPath, attrDefBean));
                }
            }
        }
        return response;
    }

    private record CorrelatorSuggestion(
            ItemPath focusItemPath,
            ResourceAttributeDefinitionType attributeDefinitionBean) {
    }
}
