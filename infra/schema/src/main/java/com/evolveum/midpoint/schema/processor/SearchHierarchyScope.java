/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

/**
 * @author semancik
 */
public enum SearchHierarchyScope {
    ONE("one"), SUB("sub");

    private final String scopeString;

    SearchHierarchyScope(String scopeString) {
        this.scopeString = scopeString;
    }

    public String getScopeString() {
        return scopeString;
    }

}
