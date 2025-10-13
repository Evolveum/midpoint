/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

/**
 * @author semancik
 */
public class SearchHierarchyConstraints {

    private final ResourceObjectIdentification.WithPrimary baseContext;
    private final SearchHierarchyScope scope;

    public SearchHierarchyConstraints(ResourceObjectIdentification.WithPrimary baseContext, SearchHierarchyScope scope) {
        this.baseContext = baseContext;
        this.scope = scope;
    }

    public ResourceObjectIdentification.WithPrimary getBaseContext() {
        return baseContext;
    }

    public SearchHierarchyScope getScope() {
        return scope;
    }
}
