/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchHierarchyScopeType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author semancik
 */
public enum SearchHierarchyScope {
    ONE("one", SearchHierarchyScopeType.ONE),
    SUB("sub", SearchHierarchyScopeType.SUB);

    @NotNull private final String string;
    @NotNull private final SearchHierarchyScopeType beanValue;

    SearchHierarchyScope(@NotNull String string, @NotNull SearchHierarchyScopeType beanValue) {
        this.string = string;
        this.beanValue = beanValue;
    }

    public @NotNull String getString() {
        return string;
    }

    public @NotNull SearchHierarchyScopeType getBeanValue() {
        return beanValue;
    }

    @Contract("null -> null; !null -> !null")
    public static SearchHierarchyScope fromBeanValue(SearchHierarchyScopeType beanValue) {
        if (beanValue == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.beanValue == beanValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown value " + beanValue));
    }
}
