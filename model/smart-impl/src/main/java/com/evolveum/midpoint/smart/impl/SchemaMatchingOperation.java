/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.smart.api.ServiceClient.Method.MATCH_SCHEMA;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaMappingProvider;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaService;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaType;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiAttributeMatchSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiMatchSchemaRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiMatchSchemaResponseType;

import java.util.Map;
import java.util.Optional;

/**
 * This is an operation used from {@link MappingsSuggestionOperation} as well as from {@link CorrelationSuggestionOperation}.
 *
 * The main purpose of this operation being wrapped in a separate class is to keep the mappings from real {@link ItemPath}s
 * to {@link DescriptiveItemPath}s, so that we can convert the latter back to the former when needed:
 * see {@link #getFocusItemPath(String)} and {@link #getApplicationItemPath(String)}.
 */
class SchemaMatchingOperation {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaMatchingOperation.class);

    private final TypeOperationContext ctx;
    private final KnownSchemaService knownSchemaService;
    private ResourceObjectClassSchemaSerializer resourceSideSerializer;
    private PrismComplexTypeDefinitionSerializer midPointSideSerializer;
    private KnownSchemaType detectedSchemaType;

    SchemaMatchingOperation(TypeOperationContext ctx, KnownSchemaService knownSchemaService) {
        this.ctx = ctx;
        this.knownSchemaService = knownSchemaService;
    }

    SiMatchSchemaResponseType matchSchema(
            ResourceObjectTypeDefinition objectTypeDef,
            PrismObjectDefinition<?> focusDef,
            ResourceType resource,
            boolean useAiService)
            throws SchemaException {

        MiscUtil.stateCheck(resourceSideSerializer == null, "matchSchema method was already called");

        resourceSideSerializer = ResourceObjectClassSchemaSerializer.create(objectTypeDef.getObjectClassDefinition(), resource);
        midPointSideSerializer = PrismComplexTypeDefinitionSerializer.create(focusDef);

        SiMatchSchemaResponseType systemSchemaMatch = null;
        Optional<KnownSchemaType> detection = knownSchemaService.detectSchemaType(resource, objectTypeDef);
        if (detection.isPresent()) {
            detectedSchemaType = detection.get();
            Optional<KnownSchemaMappingProvider> provider = knownSchemaService.getProvider(detection.get());
            if (provider.isPresent()) {
                LOGGER.info("Using known schema mappings for resource {} (detected: {})",
                        resource.getOid(), detection.get());
                systemSchemaMatch = convertToSchemaResponse(provider.get().getSchemaMatches());
            }
        }

        SiMatchSchemaResponseType aiSchemaMatch = null;
        if (useAiService) {
            LOGGER.debug("Calling AI service for schema matching on resource {}", resource.getOid());
            var siRequest = new SiMatchSchemaRequestType()
                    .applicationSchema(resourceSideSerializer.serialize())
                    .midPointSchema(midPointSideSerializer.serialize());
            aiSchemaMatch = ctx.serviceClient.invoke(MATCH_SCHEMA, siRequest, SiMatchSchemaResponseType.class);
        }

        return mergeSchemaMatches(systemSchemaMatch, aiSchemaMatch);
    }

    private SiMatchSchemaResponseType mergeSchemaMatches(
            SiMatchSchemaResponseType heuristicMatches,
            SiMatchSchemaResponseType aiMatches) {

        if (heuristicMatches == null && aiMatches == null) {
            LOGGER.debug("No schema matches available from either heuristic or AI service");
            return new SiMatchSchemaResponseType();
        }

        if (aiMatches == null || aiMatches.getAttributeMatch().isEmpty()) {
            LOGGER.debug("Using only heuristic matches (AI matches not available)");
            return heuristicMatches != null ? heuristicMatches : new SiMatchSchemaResponseType();
        }

        if (heuristicMatches == null || heuristicMatches.getAttributeMatch().isEmpty()) {
            LOGGER.debug("Using only AI matches (heuristic matches not available)");
            return aiMatches;
        }

        var heuristicMatchPairs = heuristicMatches.getAttributeMatch().stream()
                .map(match -> new MatchPair(match.getApplicationAttribute(), match.getMidPointAttribute()))
                .collect(java.util.stream.Collectors.toSet());

        SiMatchSchemaResponseType mergedResponse = new SiMatchSchemaResponseType();
        mergedResponse.getAttributeMatch().addAll(heuristicMatches.getAttributeMatch());

        int addedAiMatches = 0;
        int skippedDuplicates = 0;
        for (SiAttributeMatchSuggestionType aiMatch : aiMatches.getAttributeMatch()) {
            MatchPair aiPair = new MatchPair(aiMatch.getApplicationAttribute(), aiMatch.getMidPointAttribute());
            if (!heuristicMatchPairs.contains(aiPair)) {
                mergedResponse.getAttributeMatch().add(aiMatch);
                addedAiMatches++;
            } else {
                skippedDuplicates++;
                LOGGER.debug("Skipping AI match '{}' -> '{}' - already covered by heuristic match",
                        aiMatch.getApplicationAttribute(), aiMatch.getMidPointAttribute());
            }
        }

        LOGGER.info("Schema match merge complete: {} heuristic + {} AI = {} total ({} duplicates skipped)",
                heuristicMatches.getAttributeMatch().size(), addedAiMatches, mergedResponse.getAttributeMatch().size(), skippedDuplicates);

        return mergedResponse;
    }

    ItemPath getFocusItemPath(String descriptivePath) {
        return midPointSideSerializer.getItemPath(descriptivePath);
    }

    ItemPath getApplicationItemPath(String descriptivePath) {
        return resourceSideSerializer.getItemPath(descriptivePath);
    }

    KnownSchemaType getDetectedSchemaType() {
        return detectedSchemaType;
    }

    private SiMatchSchemaResponseType convertToSchemaResponse(Map<ItemPath, ItemPath> schemaMatches) {
        SiMatchSchemaResponseType response = new SiMatchSchemaResponseType();
        for (Map.Entry<ItemPath, ItemPath> entry : schemaMatches.entrySet()) {
            ItemPath shadowAttrPath = ItemPath.create(ShadowType.F_ATTRIBUTES, entry.getKey().first());
            ItemPath focusPath = entry.getValue();

            response.getAttributeMatch().add(new SiAttributeMatchSuggestionType()
                    .applicationAttribute(DescriptiveItemPath.asStringSimple(shadowAttrPath))
                    .midPointAttribute(DescriptiveItemPath.asStringSimple(focusPath)));
        }
        return response;
    }

    private record MatchPair(String applicationAttribute, String midPointAttribute) {
    }
}
