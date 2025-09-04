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
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiMatchSchemaRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiMatchSchemaResponseType;

/**
 * This is an operation used from {@link MappingsSuggestionOperation} as well as from {@link CorrelationSuggestionOperation}.
 *
 * The main purpose of this operation being wrapped in a separate class is to keep the mappings from real {@link ItemPath}s
 * to {@link DescriptiveItemPath}s, so that we can convert the latter back to the former when needed:
 * see {@link #getFocusItemPath(String)} and {@link #getApplicationItemPath(String)}.
 */
class SchemaMatchingOperation {

    private final TypeOperationContext ctx;
    private ResourceObjectClassSchemaSerializer resourceSideSerializer;
    private PrismComplexTypeDefinitionSerializer midPointSideSerializer;

    SchemaMatchingOperation(TypeOperationContext ctx) {
        this.ctx = ctx;
    }

    /** Should be called only once. */
    SiMatchSchemaResponseType matchSchema(
            ResourceObjectTypeDefinition objectTypeDef,
            PrismObjectDefinition<?> focusDef,
            ResourceType resource)
            throws SchemaException {

        MiscUtil.stateCheck(resourceSideSerializer == null, "matchSchema method was already called");

        var objectClassDef = objectTypeDef.getObjectClassDefinition();
        resourceSideSerializer = ResourceObjectClassSchemaSerializer.create(objectClassDef, resource);
        midPointSideSerializer = PrismComplexTypeDefinitionSerializer.create(focusDef);
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
}
