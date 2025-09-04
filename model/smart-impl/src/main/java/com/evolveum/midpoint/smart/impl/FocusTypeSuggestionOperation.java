/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDelineation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ShadowQueryConversionUtil;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

import static com.evolveum.midpoint.smart.api.ServiceClient.Method.SUGGEST_FOCUS_TYPE;

/**
 * Implements "suggest focus type" operation when executing on existing type definition
 * ({@link #suggestFocusType()}) or on a new type definition ({@link #suggestFocusType(ResourceObjectTypeDefinitionType)}).
 */
class FocusTypeSuggestionOperation {

    private final OperationContext ctx;

    FocusTypeSuggestionOperation(OperationContext context) {
        this.ctx = context;
    }

    /** Using existing type definition in the context. */
    FocusTypeSuggestionType suggestFocusType() throws SchemaException {
        var ctx = (TypeOperationContext) this.ctx;
        return suggestFocusType(
                ctx.typeDefinition.getTypeIdentification(),
                ctx.typeDefinition.getObjectClassDefinition(),
                ctx.typeDefinition.getDelineation(),
                ctx.resource);
    }

    /** Using a new type definition provided as a parameter. */
    FocusTypeSuggestionType suggestFocusType(ResourceObjectTypeDefinitionType typeDefBean)
            throws SchemaException, ConfigurationException {
        var typeIdentification = ResourceObjectTypeIdentification.of(typeDefBean);
        var delineation = ResourceObjectTypeDelineation.of(
                typeDefBean.getDelineation(), ctx.objectClassDefinition.getObjectClassName(), List.of(), ctx.objectClassDefinition);
        return suggestFocusType(
                typeIdentification,
                ctx.objectClassDefinition,
                delineation,
                ctx.resource);
    }

    /** Calls the `suggestFocusType` method on the remote service. */
    private FocusTypeSuggestionType suggestFocusType(
            ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation,
            ResourceType resource)
            throws SchemaException {
        var request = new SiSuggestFocusTypeRequestType()
                .kind(typeIdentification.getKind().value())
                .intent(typeIdentification.getIntent())
                .schema(ResourceObjectClassSchemaSerializer.serialize(objectClassDef, resource));

        setBaseContextFilter(request, objectClassDef, delineation);

        var focusTypeName = ctx.serviceClient
                .invoke(SUGGEST_FOCUS_TYPE, request, SiSuggestFocusTypeResponseType.class)
                .getFocusTypeName();
        var suggestion = new FocusTypeSuggestionType().focusType(focusTypeName);
        return AiUtil.markAsAiProvided(suggestion, FocusTypeSuggestionType.F_FOCUS_TYPE);
    }

    private static void setBaseContextFilter(
            SiSuggestFocusTypeRequestType request,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation) throws SchemaException {
        var baseContext = delineation.getBaseContext();
        var baseContextFilter = baseContext != null ? baseContext.getFilter() : null;
        if (baseContextFilter != null) {
            // We hope that object class definition is sufficient to parse the filter.
            // It should be, because all the attributes are there.
            var filter = ShadowQueryConversionUtil.parseFilter(baseContextFilter, objectClassDef);
            try {
                request.setBaseContextFilter(
                        PrismContext.get().querySerializer().serialize(filter).filterText());
            } catch (PrismQuerySerialization.NotSupportedException e) {
                throw SystemException.unexpected(e, "Cannot serialize base context filter");
            }
        }
    }
}
