/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
     * Returns all predefined inbound mappings with their paths for quality assessment.
     */
    List<SystemMappingSuggestion> suggestInboundMappings();

    /**
     * Returns all predefined outbound mappings with their paths for quality assessment.
     */
    List<SystemMappingSuggestion> suggestOutboundMappings(@Nullable List<ShadowType> sampleShadows);
}
