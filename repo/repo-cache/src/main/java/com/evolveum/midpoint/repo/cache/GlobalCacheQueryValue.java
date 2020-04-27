/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.schema.SearchResultList;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class GlobalCacheQueryValue<T extends ObjectType> extends AbstractGlobalCacheValue {

    @NotNull private final SearchResultList<T> result;

    GlobalCacheQueryValue(@NotNull SearchResultList<T> result) {
        this.result = result;
    }

    public @NotNull SearchResultList<T> getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "GlobalCacheQueryValue{" +
                "result=" + result +
                '}';
    }
}
