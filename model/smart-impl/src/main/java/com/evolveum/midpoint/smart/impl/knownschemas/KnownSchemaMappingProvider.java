/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Returns all predefined mappings for the supported schema type.
     */
    List<InboundMappingType> getInboundMappings();

    /**
     * Finds a specific predefined mapping for the given attribute and property paths.
     */
    default Optional<InboundMappingType> findMapping(ItemPath focusPropertyPath) {
        return getInboundMappings().stream()
                .filter(m -> {
                    if (m.getTarget() == null || m.getTarget().getPath() == null) {
                        return false;
                    }
                    try {
                        ItemPath targetPath = m.getTarget().getPath().getItemPath();
                        return targetPath.equivalent(focusPropertyPath);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst();
    }
}
