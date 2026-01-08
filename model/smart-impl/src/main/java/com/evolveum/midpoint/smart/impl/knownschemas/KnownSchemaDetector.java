/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.util.Optional;

/**
 * Interface for detecting whether a resource schema matches a known schema type.
 * Implementations analyze resource configuration, connector type, and schema structure
 * to determine if predefined mappings can be used.
 */
public interface KnownSchemaDetector {

    /**
     * Attempts to detect the schema type for the given resource and object type.
     */
    Optional<KnownSchemaType> detectSchemaType(ResourceType resource, ResourceObjectTypeDefinition typeDefinition);

    /**
     * Returns the schema type that this detector can identify.
     */
    KnownSchemaType getSupportedSchemaType();
}
