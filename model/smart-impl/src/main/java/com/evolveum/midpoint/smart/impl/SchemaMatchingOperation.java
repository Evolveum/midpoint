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
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType;
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

/**
 * This is an operation used from {@link MappingsSuggestionOperation} as well as from {@link CorrelationSuggestionOperation}.
 *
 * The main purpose of this operation being wrapped in a separate class is to keep the mappings from real {@link ItemPath}s
 * to {@link DescriptiveItemPath}s, so that we can convert the latter back to the former when needed:
 * see {@link #getFocusItemPath(String)} and {@link #getApplicationItemPath(String)}.
 */
class SchemaMatchingOperation {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaMatchingOperation.class);

    private final ServiceClient serviceClient;
    private final WellKnownSchemaService wellKnownSchemaService;
    private final boolean useAiService;
    private ResourceObjectClassSchemaSerializer resourceSideSerializer;
    private PrismComplexTypeDefinitionSerializer midPointSideSerializer;
    private WellKnownSchemaType detectedSchemaType;

    SchemaMatchingOperation(ServiceClient serviceClient, WellKnownSchemaService wellKnownSchemaService, boolean useAiService) {
        this.serviceClient = serviceClient;
        this.wellKnownSchemaService = wellKnownSchemaService;
        this.useAiService = useAiService;
    }

    SiMatchSchemaResponseType matchSchema(
            ResourceObjectTypeDefinition objectTypeDef,
            PrismObjectDefinition<?> focusDef,
            ResourceType resource)
            throws SchemaException {

        MiscUtil.stateCheck(resourceSideSerializer == null, "matchSchema method was already called");

        resourceSideSerializer = ResourceObjectClassSchemaSerializer.create(objectTypeDef.getObjectClassDefinition(), resource);
        midPointSideSerializer = PrismComplexTypeDefinitionSerializer.create(focusDef);

        SiMatchSchemaResponseType systemSchemaMatch = wellKnownSchemaService.detectSchemaType(resource, objectTypeDef)
                .map(schemaType -> {
                    detectedSchemaType = schemaType;
                    return schemaType;
                })
                .flatMap(wellKnownSchemaService::getProvider)
                .map(provider -> convertToSchemaResponse(provider.suggestSchemaMatches()))
                .orElse(null);

        SiMatchSchemaResponseType aiSchemaMatch = useAiService ? invokeAiService(resource) : null;

        return mergeSchemaMatches(systemSchemaMatch, aiSchemaMatch, resource);
    }

    private SiMatchSchemaResponseType invokeAiService(ResourceType resource) throws SchemaException {
        LOGGER.debug("Calling AI service for schema matching on resource {}", resource.getOid());
        var siRequest = new SiMatchSchemaRequestType()
                .applicationSchema(resourceSideSerializer.serialize())
                .midPointSchema(midPointSideSerializer.serialize());
        return serviceClient.invoke(MATCH_SCHEMA, siRequest, SiMatchSchemaResponseType.class);
    }

    private SiMatchSchemaResponseType mergeSchemaMatches(
            SiMatchSchemaResponseType heuristicMatches,
            SiMatchSchemaResponseType aiMatches,
            ResourceType resource) {

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
                .map(match -> new MatchPair(
                        getApplicationItemPath(match.getApplicationAttribute()),
                        getFocusItemPath(match.getMidPointAttribute())))
                .collect(java.util.stream.Collectors.toSet());

        SiMatchSchemaResponseType mergedResponse = new SiMatchSchemaResponseType();
        mergedResponse.getAttributeMatch().addAll(heuristicMatches.getAttributeMatch());

        int addedAiMatches = 0;
        int skippedDuplicates = 0;
        for (SiAttributeMatchSuggestionType aiMatch : aiMatches.getAttributeMatch()) {
            MatchPair aiPair = new MatchPair(
                    getApplicationItemPath(aiMatch.getApplicationAttribute()),
                    getFocusItemPath(aiMatch.getMidPointAttribute()));
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

    WellKnownSchemaType getDetectedSchemaType() {
        return detectedSchemaType;
    }

    private SiMatchSchemaResponseType convertToSchemaResponse(Map<ItemPath, ItemPath> schemaMatches) {
        SiMatchSchemaResponseType response = new SiMatchSchemaResponseType();
        for (Map.Entry<ItemPath, ItemPath> entry : schemaMatches.entrySet()) {
            ItemPath shadowAttrPath = ItemPath.create(ShadowType.F_ATTRIBUTES, entry.getKey().first());
            ItemPath focusPath = entry.getValue();

            String shadowAttrPathString = DescriptiveItemPath.asStringSimple(shadowAttrPath);
            String focusPathString = DescriptiveItemPath.asStringSimple(focusPath);

            resourceSideSerializer.registerPathMapping(shadowAttrPathString, shadowAttrPath);
            midPointSideSerializer.registerPathMapping(focusPathString, focusPath);

            response.getAttributeMatch().add(new SiAttributeMatchSuggestionType()
                    .applicationAttribute(shadowAttrPathString)
                    .midPointAttribute(focusPathString));
        }
        return response;
    }

    private record MatchPair(ItemPath applicationAttribute, ItemPath midPointAttribute) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MatchPair other)) return false;
            String thisAttrName = applicationAttribute.rest().lastName().getLocalPart();
            String otherAttrName = other.applicationAttribute.rest().lastName().getLocalPart();
            return thisAttrName.equals(otherAttrName)
                    && midPointAttribute.equivalent(other.midPointAttribute);
        }

        @Override
        public int hashCode() {
            var appLocalPart = applicationAttribute.rest().lastName().getLocalPart();
            var focusLocalParts = midPointAttribute.namedSegmentsOnly().getSegments().stream()
                    .map(seg -> ItemPath.toName(seg).getLocalPart())
                    .toList();
            return java.util.Objects.hash(appLocalPart, focusLocalParts);
        }
    }
}
