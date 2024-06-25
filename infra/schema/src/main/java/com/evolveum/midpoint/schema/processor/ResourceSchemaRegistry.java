/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps {@link CompleteResourceSchema} objects.
 */
@Component
@Experimental // unfinished!
public class ResourceSchemaRegistry {

    @NotNull private final Map<String, CompleteResourceSchema> schemaMap = new ConcurrentHashMap<>();

    public void putSchema(@NotNull String oid, @Nullable CompleteResourceSchema completeSchema) {
        schemaMap.put(oid, completeSchema);
    }

    public @Nullable ResourceObjectDefinition getDefinitionForShadow(@NotNull ShadowType shadow) throws SchemaException {
        var resourceOid = ShadowUtil.getResourceOid(shadow);
        if (resourceOid == null) {
            return null;
        }
        var resourceSchema = schemaMap.get(resourceOid);
        if (resourceSchema == null) {
            return null;
        } else {
            return resourceSchema.findDefinitionForShadow(shadow);
        }
    }
}
