/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.local;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.cache.values.CachedQueryValue;
import com.evolveum.midpoint.schema.SearchResultList;

public class LocalCacheQueryValue implements CachedQueryValue {

    @NotNull private final SearchResultList<String> oidOnlyResult;

    LocalCacheQueryValue(@NotNull SearchResultList<String> oidOnlyResult) {
        CachedQueryValue.checkConsistency(oidOnlyResult);
        this.oidOnlyResult = oidOnlyResult;
    }

    @Override
    public @NotNull SearchResultList<String> getOidOnlyResult() {
        return oidOnlyResult;
    }

    @Override
    public String toString() {
        return "LocalCacheQueryValue{" + oidOnlyResult + '}';
    }
}
