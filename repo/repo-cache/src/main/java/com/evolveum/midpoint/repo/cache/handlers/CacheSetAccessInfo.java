/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.local.LocalObjectCache;
import com.evolveum.midpoint.repo.cache.local.LocalQueryCache;
import com.evolveum.midpoint.repo.cache.local.LocalVersionCache;

/**
 * CacheAccessInfo for all six caches.
 */
public class CacheSetAccessInfo {

    @NotNull final CacheAccessInfo<LocalObjectCache> localObject;
    @NotNull final CacheAccessInfo<LocalVersionCache> localVersion;
    @NotNull final CacheAccessInfo<LocalQueryCache> localQuery;
    @NotNull final CacheAccessInfo<GlobalObjectCache> globalObject;
    @NotNull final CacheAccessInfo<GlobalVersionCache> globalVersion;
    @NotNull final CacheAccessInfo<GlobalQueryCache> globalQuery;

    public CacheSetAccessInfo(@NotNull CacheAccessInfo<LocalObjectCache> localObject,
            @NotNull CacheAccessInfo<LocalVersionCache> localVersion,
            @NotNull CacheAccessInfo<LocalQueryCache> localQuery,
            @NotNull CacheAccessInfo<GlobalObjectCache> globalObject,
            @NotNull CacheAccessInfo<GlobalVersionCache> globalVersion,
            @NotNull CacheAccessInfo<GlobalQueryCache> globalQuery) {
        this.localObject = localObject;
        this.localVersion = localVersion;
        this.localQuery = localQuery;
        this.globalObject = globalObject;
        this.globalVersion = globalVersion;
        this.globalQuery = globalQuery;
    }
}
