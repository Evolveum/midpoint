/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeMappingsSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutboundMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Provider of predefined mappings for a specific known schema type.
 * Each implementation contains the mapping rules for one schema (e.g., SCIM 2.0 User).
 */
public interface WellKnownSchemaProvider {

    /**
     * Returns the schema type that this provider supports.
     */
    WellKnownSchemaType getSupportedSchemaType();

    /**
     * Returns all predefined schema matches (shadow attribute → focus property pairs).
     * This is used to bypass LLM schema matching when a known schema is detected.
     */
    Map<ItemPath, ItemPath> suggestSchemaMatches();

    /**
     * Returns all predefined inbound mappings as ready-to-use AttributeMappingsSuggestionType instances.
     * These are directly added to the mappings suggestion without further processing.
     */
    List<AttributeMappingsSuggestionType> suggestInboundMappings();

    /**
     * Returns all predefined outbound mappings as ready-to-use AttributeMappingsSuggestionType instances.
     * These are directly added to the mappings suggestion without further processing.
     */
    List<AttributeMappingsSuggestionType> suggestOutboundMappings();

    static AttributeMappingsSuggestionType createInboundMapping(
            String shadowAttrName,
            ItemPath focusPropertyPath,
            String description,
            @Nullable ExpressionType expression) {
        var inboundMapping = new InboundMappingType()
                .name(shadowAttrName + "-into-" + focusPropertyPath.lastName())
                .description(description)
                .strength(MappingStrengthType.STRONG)
                .expression(expression)
                .target(new VariableBindingDefinitionType().path(focusPropertyPath.toBean()));

        return new AttributeMappingsSuggestionType()
                .expectedQuality(null)
                .definition(new ResourceAttributeDefinitionType()
                        .inbound(inboundMapping));
    }

    static AttributeMappingsSuggestionType createOutboundMapping(
            String shadowAttrName,
            ItemPath focusPropertyPath,
            String description,
            @Nullable ExpressionType expression) {
        var outboundMapping = new OutboundMappingType()
                .name(focusPropertyPath.lastName() + "-to-" + shadowAttrName)
                .description(description)
                .strength(MappingStrengthType.STRONG)
                .expression(expression)
                .source(new VariableBindingDefinitionType().path(focusPropertyPath.toBean()));

        return new AttributeMappingsSuggestionType()
                .expectedQuality(null)
                .definition(new ResourceAttributeDefinitionType()
                        .outbound(outboundMapping));
    }
}
