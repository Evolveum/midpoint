/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

/**
 * @author semancik
 */
public class SearchHierarchyConstraints {

    ResourceObjectIdentification baseContext;
    SearchHierarchyScope scope;

    public SearchHierarchyConstraints(ResourceObjectIdentification baseContext, SearchHierarchyScope scope) {
        super();
        this.baseContext = baseContext;
        this.scope = scope;
    }

    public ResourceObjectIdentification getBaseContext() {
        return baseContext;
    }

    public SearchHierarchyScope getScope() {
        return scope;
    }


}
