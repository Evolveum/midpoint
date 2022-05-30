/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.RelationRegistry;
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

    public static boolean matches(PrismObject<ShadowType> shadowToMatch,
            Collection<ResourceObjectPattern> protectedAccountPatterns, MatchingRuleRegistry matchingRuleRegistry,
            RelationRegistry relationRegistry) throws SchemaException {
        for (ResourceObjectPattern pattern: protectedAccountPatterns) {
            if (pattern.matches(shadowToMatch, matchingRuleRegistry, relationRegistry)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(
            PrismObject<ShadowType> shadowToMatch,
            MatchingRuleRegistry matchingRuleRegistry,
            RelationRegistry relationRegistry) throws SchemaException {
        ObjectTypeUtil.normalizeFilter(objectFilter, relationRegistry); // we suppose references in shadowToMatch are normalized (on return from repo)
        return ObjectQuery.match(shadowToMatch, objectFilter, matchingRuleRegistry);
    }

    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public @NotNull ObjectFilter getObjectFilter() {
        return objectFilter;
    }
}
