/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

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
     */
    public static @NotNull ResourceObjectTypeDefinition findObjectTypeDefinitionRequired(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind,
            @Nullable String intent) {
        ResourceObjectDefinition definition =
                intent != null ?
                        schema.findObjectDefinitionRequired(kind, intent) :
                        schema.findDefaultDefinitionForKindRequired(kind);
        return MiscUtil.requireNonNull(
                definition.getTypeDefinition(),
                () -> new IllegalStateException(
                        String.format("No type definition for %s/%s could be found; only %s", kind, intent, definition)));
    }
}
