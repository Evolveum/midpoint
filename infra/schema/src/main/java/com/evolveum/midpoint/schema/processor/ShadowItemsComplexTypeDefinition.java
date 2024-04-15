/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;

import org.jetbrains.annotations.NotNull;

/**
 * Supertype for a CTD for `attributes` and `associations` shadow containers.
 *
 * Before 4.9, the {@link ResourceObjectDefinition} played this role, by extending the {@link ComplexTypeDefinition}.
 *
 * However, after associations became first-class citizens in the schema, this is no longer possible. The object-level definition
 * is not a CTD any longer. Instead, there are two specialized CTDs: one for attributes, and the second one for associations.
 * This is their common supertype.
 */
public interface ShadowItemsComplexTypeDefinition extends ComplexTypeDefinition {

    /** "Owning" resource object definition. */
    @NotNull ResourceObjectDefinition getResourceObjectDefinition();
}
