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
            ResourceType resource)
            throws SchemaException {

        MiscUtil.stateCheck(resourceSideSerializer == null, "matchSchema method was already called");

        resourceSideSerializer = ResourceObjectClassSchemaSerializer.create(objectTypeDef.getObjectClassDefinition(), resource);
        midPointSideSerializer = PrismComplexTypeDefinitionSerializer.create(focusDef);

        Optional<KnownSchemaType> detection = knownSchemaService.detectSchemaType(resource, objectTypeDef);
        if (detection.isPresent()) {
            detectedSchemaType = detection.get();
            Optional<KnownSchemaMappingProvider> provider = knownSchemaService.getProvider(detection.get());
            if (provider.isPresent()) {
                LOGGER.info("Using known schema mappings for resource {} (detected: {})",
                        resource.getOid(), detection.get());
                return convertToSchemaResponse(provider.get().getSchemaMatches());
            }
        }

        LOGGER.debug("No known schema detected for resource {}, calling AI service", resource.getOid());
        var siRequest = new SiMatchSchemaRequestType()
                .applicationSchema(resourceSideSerializer.serialize())
                .midPointSchema(midPointSideSerializer.serialize());
        return ctx.serviceClient.invoke(MATCH_SCHEMA, siRequest, SiMatchSchemaResponseType.class);
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
}
