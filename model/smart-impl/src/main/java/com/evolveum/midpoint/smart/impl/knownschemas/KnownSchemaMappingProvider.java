/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeMappingsSuggestionType;

import java.util.List;
import java.util.Map;

/**
 * Provider of predefined mappings for a specific known schema type.
 * Each implementation contains the mapping rules for one schema (e.g., SCIM 2.0 User).
 */
public interface KnownSchemaMappingProvider {

    /**
     * Returns the schema type that this provider supports.
     */
    KnownSchemaType getSupportedSchemaType();

    /**
     * Returns all predefined schema matches (shadow attribute → focus property pairs).
     * This is used to bypass LLM schema matching when a known schema is detected.
     */
    Map<ItemPath, ItemPath> getSchemaMatches();

    /**
     * Returns all predefined inbound mappings as ready-to-use AttributeMappingsSuggestionType instances.
     * These are directly added to the mappings suggestion without further processing.
     */
    List<AttributeMappingsSuggestionType> getInboundMappings();

    /**
     * Returns all predefined outbound mappings as ready-to-use AttributeMappingsSuggestionType instances.
     * These are directly added to the mappings suggestion without further processing.
     */
    List<AttributeMappingsSuggestionType> getOutboundMappings();
}
