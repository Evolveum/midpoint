/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static com.evolveum.midpoint.repo.cache.handlers.SearchOpHandler.QUERY_RESULT_SIZE_LIMIT;
import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.getLocalObjectCache;
import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.getLocalVersionCache;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.log;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.global.GlobalCacheObjectValue;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.local.LocalObjectCache;
import com.evolveum.midpoint.repo.cache.local.LocalVersionCache;
import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Responsible for inserting things into caches (in all possible flavors).
 * All this logic was concentrated here in facilitate its consolidation.
 *
 * Methods naming: "store" "Immutable"? (what) "To" (what)
 *
 * Notes:
 * - All methods here are conditional. Unconditional insertions are treated directly by caches.
 * - Version caches do not need to distinguish mutable and immutable objects.
 */
@Component
class CacheUpdater {

    /**
     * Results older than this time will not be cached. By "age" we mean the time interval between
     * starting a repository operation and caching the resulting data. This measure is currently
     * implemented only in a limited way: for search results, because we expect that searches
     * (and especially handling of objects during searchObjectsIterative calls) can take quite
     * a long time.
     */
    static final long DATA_STALENESS_LIMIT = 1000;

    @Autowired private CacheSetAccessInfoFactory cacheSetAccessInfoFactory;
    @Autowired private GlobalObjectCache globalObjectCache;
    @Autowired private GlobalVersionCache globalVersionCache;

    //region Lists of objects

    /**
     * @param loadedList List of objects as obtained from inner Repository service
     * @return List of objects suitable to be returned to the client w.r.t. readOnly flag.
     */
    <T extends ObjectType> SearchResultList<PrismObject<T>> storeSearchResultToAll(QueryKey<T> key,
            SearchResultList<PrismObject<T>> loadedList, CacheSetAccessInfo<T> caches, boolean readOnly, long started) {
        long age = System.currentTimeMillis() - started;
        if (age >= DATA_STALENESS_LIMIT) {
            CachePerformanceCollector.INSTANCE.registerSkippedStaleData(key.getType());
            log("Not caching stale search result with {} object(s) (age = {} ms)", false, loadedList.size(), age);
            return loadedList;
        }

        boolean sizeOk = loadedList.size() <= QUERY_RESULT_SIZE_LIMIT;
        if ((caches.localQuery.effectivelySupports() || caches.globalQuery.effectivelySupports()) && !sizeOk) {
            CachePerformanceCollector.INSTANCE.registerOverSizedQuery(key.getType());
        }

        if (sizeOk && (caches.localQuery.effectivelySupports() || caches.globalQuery.effectivelySupports())) {
            // We want to cache the query result, so cloning the whole list is not a waste of time.
            SearchResultList<PrismObject<T>> immutableList = loadedList.isImmutable() ? loadedList : loadedList.toDeeplyFrozenList();
            storeImmutableSearchResultToAllLocal(key, immutableList, caches);
            storeImmutableSearchResultToAllGlobal(key, immutableList, caches);
            return readOnly ? immutableList : loadedList;
        } else {
            // We are going to cache individual objects/versions only (if at all). So we will not clone the whole list.
            // Individual objects can be cloned/frozen as necessary by called methods.
            for (PrismObject<? extends T> loadedObject : loadedList) {
                storeLoadedObjectToAll(loadedObject, readOnly, age);
            }
            return loadedList;
        }
    }

    <T extends ObjectType> void storeImmutableSearchResultToAllLocal(QueryKey<T> key,
            SearchResultList<PrismObject<T>> immutableList, CacheSetAccessInfo<T> caches) {
        assert immutableList.size() <= QUERY_RESULT_SIZE_LIMIT;
        storeImmutableSearchResultToQueryLocal(key, immutableList, caches);
        storeImmutableObjectsToObjectAndVersionLocal(immutableList);
    }

    private <T extends ObjectType> void storeImmutableSearchResultToAllGlobal(QueryKey<T> key,
            SearchResultList<PrismObject<T>> immutableList, CacheSetAccessInfo<T> caches) {
        assert immutableList.size() <= QUERY_RESULT_SIZE_LIMIT;
        storeImmutableSearchResultToQueryGlobal(key, immutableList, caches);
        storeImmutableObjectsToObjectAndVersionGlobal(immutableList);
    }

    <T extends ObjectType> void storeImmutableSearchResultToQueryLocal(QueryKey<T> key,
            SearchResultList<PrismObject<T>> immutableList, CacheSetAccessInfo<T> caches) {
        if (caches.localQuery.effectivelySupports()) {
            caches.localQuery.getCache().put(key, immutableList);
        }
    }

    <T extends ObjectType> void storeImmutableSearchResultToQueryGlobal(QueryKey<T> key,
            SearchResultList<PrismObject<T>> immutableList, CacheSetAccessInfo<T> caches) {
        if (caches.globalQuery.effectivelySupports()) {
            caches.globalQuery.getCache().put(key, immutableList);
        }
    }

    private <T extends ObjectType> void storeImmutableObjectsToObjectAndVersionLocal(List<PrismObject<T>> immutableObjects) {
        LocalObjectCache localObjectCache = getLocalObjectCache();
        if (localObjectCache != null) {
            for (PrismObject<T> immutableObject : immutableObjects) {
                Class<? extends ObjectType> type = immutableObject.asObjectable().getClass();
                if (localObjectCache.supportsObjectType(type)) {
                    localObjectCache.put(immutableObject); // no need to clone immutable object
                }
            }
        }

        LocalVersionCache localVersionCache = getLocalVersionCache();
        if (localVersionCache != null) {
            for (PrismObject<T> immutableObject : immutableObjects) {
                Class<? extends ObjectType> type = immutableObject.asObjectable().getClass();
                if (localVersionCache.supportsObjectType(type)) {
                    localVersionCache.put(immutableObject);
                }
            }
        }
    }

    private <T extends ObjectType> void storeImmutableObjectsToObjectAndVersionGlobal(List<PrismObject<T>> immutableObjects) {
        if (globalObjectCache.isAvailable()) {
            for (PrismObject<T> immutableObject : immutableObjects) {
                storeImmutableObjectToObjectGlobal(immutableObject);
            }
        }
        if (globalVersionCache.isAvailable()) {
            for (PrismObject<T> immutableObject : immutableObjects) {
                storeObjectToVersionGlobal(immutableObject);
            }
        }
    }
    //endregion

    //region Single objects (content + version)

    // Assumption: object will be returned by the RepositoryCache
    // Side effect: object can be frozen if readOnly == true
    <T extends ObjectType> void storeLoadedObjectToAll(PrismObject<T> object, boolean readOnly, long age) {
        if (age < DATA_STALENESS_LIMIT) {
            CacheSetAccessInfo<T> caches = cacheSetAccessInfoFactory.determineSkippingQuery(object.getCompileTimeClass());
            storeLoadedObjectToAll(object, caches, readOnly);
        } else {
            CachePerformanceCollector.INSTANCE.registerSkippedStaleData(object.getCompileTimeClass());
            log("Not caching stale object {} (age = {} ms)", false, object, age);
        }
    }

    // Assumption: object will be returned by the RepositoryCache
    // Side effect: object can be frozen if readOnly == true
    private <T extends ObjectType> void storeLoadedObjectToAll(PrismObject<T> object, CacheSetAccessInfo<T> caches, boolean readOnly) {
        boolean putIntoLocalObject = caches.localObject.effectivelySupports();
        boolean putIntoGlobalObject = caches.globalObject.effectivelySupports();
        if (putIntoLocalObject || putIntoGlobalObject) {
            @NotNull PrismObject<T> immutable;
            if (readOnly) {
                // We are going to return the object as read-only, so we can cache the same (frozen) object as we are returning.
                object.freeze();
                immutable = object;
            } else {
                // We are going to return the object as mutable, so we must cache the frozen clone of the retrieved object.
                immutable = object.clone();
                immutable.freeze();
            }
            storeImmutableObjectToObjectLocal(immutable, caches);
            storeImmutableObjectToObjectGlobal(immutable);
        }

        storeObjectToVersionLocal(object, caches.localVersion);
        storeObjectToVersionGlobal(object, caches.globalVersion);
    }

    <T extends ObjectType> void storeImmutableObjectToAllLocal(PrismObject<T> immutable, CacheSetAccessInfo<T> caches) {
        storeImmutableObjectToObjectLocal(immutable, caches);
        storeObjectToVersionLocal(immutable, caches.localVersion);
    }
    //endregion

    //region Single objects (content)

    <T extends ObjectType> void storeImmutableObjectToObjectLocal(PrismObject<T> immutable, CacheSetAccessInfo<T> caches) {
        if (caches.localObject.effectivelySupports()) {
            caches.localObject.getCache().put(immutable); // no need to clone immutable object
        }
    }

    <T extends ObjectType> void storeImmutableObjectToObjectGlobal(PrismObject<T> immutable) {
        Long nextVersionCheckTime = globalObjectCache.getNextVersionCheckTime(immutable.asObjectable().getClass());
        if (nextVersionCheckTime != null) {
            globalObjectCache.put(new GlobalCacheObjectValue<>(immutable, nextVersionCheckTime));
        }
    }

    //endregion

    //region Single objects (version only)

    <T extends ObjectType> void storeObjectToVersionLocal(PrismObject<T> object, CacheAccessInfo<LocalVersionCache, T> localVersion) {
        storeVersionToVersionLocal(object.getOid(), object.getVersion(), localVersion);
    }

    void storeVersionToVersionLocal(String oid, String version, CacheAccessInfo<LocalVersionCache, ?> localVersion) {
        if (localVersion.effectivelySupports()) {
            localVersion.getCache().put(oid, version);
        }
    }

    private <T extends ObjectType> void storeObjectToVersionGlobal(PrismObject<T> object) {
        CacheConfiguration cacheConfiguration = globalVersionCache.getConfiguration();
        Class<? extends ObjectType> type = object.asObjectable().getClass();
        if (cacheConfiguration != null && cacheConfiguration.supportsObjectType(type)) {
            globalVersionCache.put(object);
        }
    }

    <T extends ObjectType> void storeObjectToVersionGlobal(PrismObject<T> object, CacheAccessInfo<GlobalVersionCache, T> globalVersion) {
        if (globalVersion.effectivelySupports()) {
            globalVersion.getCache().put(object);
        }
    }

    <T extends ObjectType> void storeVersionToVersionGlobal(@NotNull Class<T> type, String oid, String version,
            CacheAccessInfo<GlobalVersionCache, T> globalVersion) {
        if (globalVersion.effectivelySupports()) {
            globalVersionCache.put(oid, type, version);
        }
    }

    //endregion
}
