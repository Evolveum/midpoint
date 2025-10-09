/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.global;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.cache.values.CachedQueryValue;
import com.evolveum.midpoint.schema.SearchResultList;

public class GlobalCacheQueryValue extends AbstractGlobalCacheValue implements CachedQueryValue {

    @NotNull private final SearchResultList<String> oidOnlyResult;

    GlobalCacheQueryValue(@NotNull SearchResultList<String> oidOnlyResult) {
        CachedQueryValue.checkConsistency(oidOnlyResult);
        this.oidOnlyResult = oidOnlyResult;
    }

    @Override
    public @NotNull SearchResultList<String> getOidOnlyResult() {
        return oidOnlyResult;
    }

    @Override
    public String toString() {
        return "GlobalCacheQueryValue{" + oidOnlyResult + '}';
    }
}
