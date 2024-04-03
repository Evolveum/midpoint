/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

import com.evolveum.midpoint.schema.util.AbstractShadow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * @author semancik
 */
public class ResourceObjectPattern implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @NotNull private final ResourceObjectDefinition objectDefinition;

    @NotNull private final ObjectFilter filter;

    public ResourceObjectPattern(@NotNull ResourceObjectDefinition objectDefinition, @NotNull ObjectFilter filter) {
        this.objectDefinition = objectDefinition;
        this.filter = filter;
    }

    public static boolean matches(
            @NotNull AbstractShadow shadowToMatch, @Nullable Collection<ResourceObjectPattern> protectedAccountPatterns)
            throws SchemaException {
        for (ResourceObjectPattern pattern : emptyIfNull(protectedAccountPatterns)) {
            if (pattern.matches(shadowToMatch)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public boolean matches(@NotNull AbstractShadow shadowToMatch) throws SchemaException {
        SchemaService schemaService = SchemaService.get();

        // We suppose there are no references in the object filter. But to be safe, let's keep this here.
        ObjectTypeUtil.normalizeFilter(filter, schemaService.relationRegistry());

        return ObjectQuery.match(shadowToMatch.getBean(), filter, schemaService.matchingRuleRegistry());
    }

    public @NotNull ResourceObjectDefinition getObjectDefinition() {
        return objectDefinition;
    }

    public @NotNull ObjectFilter getFilter() {
        return filter;
    }
}
