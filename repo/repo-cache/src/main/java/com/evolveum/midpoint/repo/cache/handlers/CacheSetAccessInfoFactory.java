/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;

import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;

import com.evolveum.midpoint.repo.cache.local.LocalObjectCache;
import com.evolveum.midpoint.repo.cache.local.LocalQueryCache;
import com.evolveum.midpoint.repo.cache.local.LocalVersionCache;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.*;
import static com.evolveum.midpoint.schema.cache.CacheType.*;

/**
 * Creates CacheSetAccessInfo objects.
 */
@Component
public class CacheSetAccessInfoFactory {

    @Autowired GlobalObjectCache globalObjectCache;
    @Autowired GlobalVersionCache globalVersionCache;
    @Autowired GlobalQueryCache globalQueryCache;
    @Autowired CacheConfigurationManager cacheConfigurationManager;

    <T extends ObjectType> CacheSetAccessInfo<T> determine(Class<T> type) {
        return determine(type, true);
    }

    <T extends ObjectType> CacheSetAccessInfo<T> determineSkippingQuery(Class<T> type) {
        return determine(type, false);
    }

    private <T extends ObjectType> CacheSetAccessInfo<T> determine(Class<T> type, boolean alsoQuery) {

        CacheAccessInfo<GlobalObjectCache, T> globalObject = new CacheAccessInfo<>(globalObjectCache,
                globalObjectCache.getConfiguration(), type, globalObjectCache.isAvailable());

        CacheAccessInfo<GlobalVersionCache, T> globalVersion = new CacheAccessInfo<>(globalVersionCache,
                globalVersionCache.getConfiguration(), type, globalVersionCache.isAvailable());

        CacheAccessInfo<GlobalQueryCache, T> globalQuery =
                alsoQuery ?
                        new CacheAccessInfo<>(globalQueryCache, globalQueryCache.getConfiguration(), type, globalQueryCache.isAvailable())
                        : CacheAccessInfo.createNotAvailable();

        LocalObjectCache localObjectCache = getLocalObjectCache();
        LocalVersionCache localVersionCache = getLocalVersionCache();
        LocalQueryCache localQueryCache = getLocalQueryCache();

        CacheAccessInfo<LocalObjectCache, T> localObject = localObjectCache != null ?
                new CacheAccessInfo<>(localObjectCache, localObjectCache.getConfiguration(), type, true) :
                new CacheAccessInfo<>(null, cacheConfigurationManager.getConfiguration(LOCAL_REPO_OBJECT_CACHE), type, false);
        CacheAccessInfo<LocalVersionCache, T> localVersion = localVersionCache != null ?
                new CacheAccessInfo<>(localVersionCache, localVersionCache.getConfiguration(), type, true) :
                new CacheAccessInfo<>(null, cacheConfigurationManager.getConfiguration(LOCAL_REPO_VERSION_CACHE), type, false);
        CacheAccessInfo<LocalQueryCache, T> localQuery;
        if (alsoQuery) {
            localQuery = localQueryCache != null ?
                    new CacheAccessInfo<>(localQueryCache, localQueryCache.getConfiguration(), type, true) :
                    new CacheAccessInfo<>(null, cacheConfigurationManager.getConfiguration(LOCAL_REPO_QUERY_CACHE), type, false);
        } else {
            localQuery = CacheAccessInfo.createNotAvailable();
        }

        return new CacheSetAccessInfo<>(localObject, localVersion, localQuery, globalObject, globalVersion, globalQuery);
    }
}
