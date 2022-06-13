/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * TEMPORARY!
 *
 * These methods are used in tests. But there is currently no reasonable place in the test code where to put them in.
 * (Or at least I don't see it.)
 */
@VisibleForTesting
public class ResourceSchemaTestUtil {

    /**
     * Returns object _type_ definition for given kind and intent.
     *
     * Fragile! May break if the definition class hierarchy changes.
     */
    @VisibleForTesting
    public static @NotNull ResourceObjectTypeDefinition findObjectTypeDefinitionRequired(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind,
            @Nullable String intent) {
        ResourceObjectDefinition definition =
                intent != null ?
                        schema.findObjectDefinitionRequired(kind, intent) :
                        schema.findDefaultDefinitionForKindRequired(kind);
        stateCheck(definition instanceof ResourceObjectTypeDefinition,
                "No type definition for %s/%s could be found; only %s", kind, intent, definition);
        return (ResourceObjectTypeDefinition) definition;
    }

    /**
     * Returns the definition for given kind. If default one is present, it is returned.
     * Otherwise, any definition is returned.
     *
     * This is similar to pre-4.5 behavior observed when looking for "refined definitions".
     * (Although not exactly the same: now we require type definition, whereas in 4.4 and before
     * we could return a definition even if no schemaHandling was present.)
     *
     * This method is quite obscure, mainly because of its non-determinism, and shouldn't be used much.
     */
    public static @Nullable ResourceObjectTypeDefinition findDefaultOrAnyObjectTypeDefinition(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind) {
        return ResourceObjectDefinitionResolver.findDefaultOrAnyObjectTypeDefinition(schema, kind);
    }
}
