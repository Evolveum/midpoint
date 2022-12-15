/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.io.Serializable;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class ResourceObjectPattern implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull private final ResourceObjectDefinition resourceObjectDefinition;
    @NotNull private final ObjectFilter objectFilter;

    public ResourceObjectPattern(@NotNull ResourceObjectDefinition resourceObjectDefinition, @NotNull ObjectFilter objectFilter) {
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.objectFilter = objectFilter;
    }

    public static boolean matches(
            @NotNull ShadowType shadowToMatch, @Nullable Collection<ResourceObjectPattern> protectedAccountPatterns)
            throws SchemaException {
        for (ResourceObjectPattern pattern : emptyIfNull(protectedAccountPatterns)) {
            if (pattern.matches(shadowToMatch)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(@NotNull ShadowType shadowToMatch) throws SchemaException {
        // we suppose references in shadowToMatch are normalized (on return from repo)
        SchemaService schemaService = SchemaService.get();
        ObjectTypeUtil.normalizeFilter(objectFilter, schemaService.relationRegistry());
        return ObjectQuery.match(shadowToMatch, objectFilter, schemaService.matchingRuleRegistry());
    }

    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public @NotNull ObjectFilter getObjectFilter() {
        return objectFilter;
    }
}
