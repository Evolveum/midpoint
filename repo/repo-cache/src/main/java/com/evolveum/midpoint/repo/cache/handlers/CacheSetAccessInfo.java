/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.local.LocalObjectCache;
import com.evolveum.midpoint.repo.cache.local.LocalQueryCache;
import com.evolveum.midpoint.repo.cache.local.LocalVersionCache;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * {@link CacheAccessInfo} for all six caches.
 *
 * `T` is object type to which this information is related.
 * It is here to avoid application of cache access information to wrong object types.
 */
public class CacheSetAccessInfo<T extends ObjectType> {

    @NotNull final CacheAccessInfo<LocalObjectCache, T> localObject;
    @NotNull final CacheAccessInfo<LocalVersionCache, T> localVersion;
    @NotNull final CacheAccessInfo<LocalQueryCache, T> localQuery;
    @NotNull final CacheAccessInfo<GlobalObjectCache, T> globalObject;
    @NotNull final CacheAccessInfo<GlobalVersionCache, T> globalVersion;
    @NotNull final CacheAccessInfo<GlobalQueryCache, T> globalQuery;

    CacheSetAccessInfo(
            @NotNull CacheAccessInfo<LocalObjectCache, T> localObject,
            @NotNull CacheAccessInfo<LocalVersionCache, T> localVersion,
            @NotNull CacheAccessInfo<LocalQueryCache, T> localQuery,
            @NotNull CacheAccessInfo<GlobalObjectCache, T> globalObject,
            @NotNull CacheAccessInfo<GlobalVersionCache, T> globalVersion,
            @NotNull CacheAccessInfo<GlobalQueryCache, T> globalQuery) {
        this.localObject = localObject;
        this.localVersion = localVersion;
        this.localQuery = localQuery;
        this.globalObject = globalObject;
        this.globalVersion = globalVersion;
        this.globalQuery = globalQuery;
    }

    boolean isEffectivelySupportedByAnyObjectCache() {
        return localObject.effectivelySupports() || globalObject.effectivelySupports();
    }

    boolean isEffectivelySupportedByAnyQueryCache() {
        return localQuery.effectivelySupports() || globalQuery.effectivelySupports();
    }

}
