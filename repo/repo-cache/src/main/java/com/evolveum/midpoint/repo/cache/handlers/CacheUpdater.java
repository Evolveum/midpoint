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

import java.util.Collection;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.cache.values.CachedObjectValue;

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
 *
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
     * Stores a search result to all caches.
     *
     * @param loadedResultList List of objects as obtained from inner Repository service
     * @return List of objects suitable to be returned to the client w.r.t. readOnly flag.
     */
    <T extends ObjectType> SearchResultList<PrismObject<T>> storeSearchResultToAll(
            SearchOpExecution<T> exec, SearchResultList<PrismObject<T>> loadedResultList) {

        if (!exec.cacheUseMode.canUpdateCache()) {
            return exec.toReturnValueFromAny(loadedResultList);
        }

        var key = exec.queryKey;

        long age = exec.getAge();
        if (age >= DATA_STALENESS_LIMIT) {
            CachePerformanceCollector.INSTANCE.registerSkippedStaleData(key.getType());
            log("Not caching stale search result with {} object(s) (age = {} ms)", false, loadedResultList.size(), age);
            return exec.toReturnValueFromAny(loadedResultList);
        }

        var localQueryAccess = exec.cachesInfo.localQuery;
        var globalQueryAccess = exec.cachesInfo.globalQuery;

        boolean sizeOk = loadedResultList.size() <= QUERY_RESULT_SIZE_LIMIT;
        boolean effectivelySupported = localQueryAccess.effectivelySupports() || globalQueryAccess.effectivelySupports();

        if (effectivelySupported && !sizeOk) {
            CachePerformanceCollector.INSTANCE.registerOverSizedQuery(key.getType());
        }

        if (effectivelySupported && sizeOk) {
            Collection<PrismObject<T>> objectsToCache;
            if (exec.readOnly) {
                // May be costly, as this is a deep freeze. But we need it, as we want to store the objects in object caches.
                loadedResultList.freeze();
                objectsToCache = loadedResultList;
            } else {
                // This is even more costly, as it involves cloning. But we need to store objects in their immutable form,
                // and return them as mutable objects.
                objectsToCache = loadedResultList.toDeeplyFrozenList();
            }
            var immutableOidList = toImmutableOidList(loadedResultList);
            storeImmutableSearchResultToAllLocal(exec, objectsToCache, immutableOidList);
            storeImmutableSearchResultToAllGlobal(exec, objectsToCache, immutableOidList);
        } else {
            // Either oversize, or not storing into query caches -> let's take only the objects
            if (exec.readOnly) {
                // May be costly, as this is a deep freeze. But we need it, as we want to store the objects in object caches.
                // (So they must be immutable anyway.)
                loadedResultList.freeze();
            }
            // We are going to cache individual objects/versions only (if at all). So we will not clone the whole list.
            // Individual objects can be cloned/frozen as necessary by called methods.
            for (var loadedObject : loadedResultList) {
                storeLoadedObjectToAll(loadedObject, age);
            }
        }
        // Assuming that loadedList is mutable as it was returned from repo (if readOnly == false)
        // and that it was frozen above (if readOnly == true)
        return loadedResultList;
    }

    static SearchResultList<String> toImmutableOidList(SearchResultList<? extends PrismObject<?>> objectList) {
        var oidList = new SearchResultList<>(
                objectList.stream()
                        .map(PrismObject::getOid)
                        .toList(),
                objectList.getMetadata());
        oidList.freeze();
        return oidList;
    }

    /**
     * Stores the data into all local caches (object and query).
     *
     * Assumes the data was already in the cache or that we are allowed to update the caches, i.e. that
     * {@link CacheUseMode#canUpdateCache()} is `true`.
     */
    <T extends ObjectType> void storeImmutableSearchResultToAllLocal(
            SearchOpExecution<T> exec,
            Collection<PrismObject<T>> objects,
            SearchResultList<String> immutableOidList) {
        assert exec.cacheUseMode.canUpdateCache();
        assert objects.size() == immutableOidList.size();
        assert objects.size() <= QUERY_RESULT_SIZE_LIMIT;
        storeImmutableSearchResultToQueryLocal(exec.queryKey, immutableOidList, exec.cachesInfo);
        storeImmutableObjectsToObjectAndVersionLocal(objects);
    }

    private <T extends ObjectType> void storeImmutableSearchResultToAllGlobal(
            SearchOpExecution<T> exec,
            Collection<PrismObject<T>> objects,
            SearchResultList<String> immutableOidList) {
        assert exec.cacheUseMode.canUpdateCache();
        assert objects.size() == immutableOidList.size();
        assert objects.size() <= QUERY_RESULT_SIZE_LIMIT;
        storeImmutableSearchResultToQueryGlobal(exec.queryKey, immutableOidList, exec.cachesInfo);
        storeImmutableObjectsToObjectAndVersionGlobal(objects);
    }

    <T extends ObjectType> void storeImmutableSearchResultToQueryLocal(QueryKey<T> key,
            SearchResultList<String> immutableList, CacheSetAccessInfo<T> cachesInfo) {
        if (cachesInfo.localQuery.effectivelySupports()) {
            cachesInfo.localQuery.getCache().put(key, immutableList);
        }
    }

    <T extends ObjectType> void storeImmutableSearchResultToQueryGlobal(QueryKey<T> key,
            SearchResultList<String> immutableList, CacheSetAccessInfo<T> cachesInfo) {
        if (cachesInfo.globalQuery.effectivelySupports()) {
            cachesInfo.globalQuery.getCache().put(key, immutableList);
        }
    }

    private <T extends ObjectType> void storeImmutableObjectsToObjectAndVersionLocal(
            Collection<PrismObject<T>> immutableObjects) {
        LocalObjectCache localObjectCache = getLocalObjectCache();
        if (localObjectCache != null) {
            for (var immutableObject : immutableObjects) {
                var type = immutableObject.asObjectable().getClass();
                if (localObjectCache.supportsObjectType(type)) {
                    // 1. No need to clone immutable object
                    // 2. We may (later) try to optimize computation of the complete flag - it is done for both local and global
                    // object cache
                    localObjectCache.put(immutableObject, CachedObjectValue.computeCompleteFlag(immutableObject));
                }
            }
        }

        LocalVersionCache localVersionCache = getLocalVersionCache();
        if (localVersionCache != null) {
            for (var immutableObject : immutableObjects) {
                var type = immutableObject.asObjectable().getClass();
                if (localVersionCache.supportsObjectType(type)) {
                    localVersionCache.put(immutableObject);
                }
            }
        }
    }

    private <T extends ObjectType> void storeImmutableObjectsToObjectAndVersionGlobal(
            Collection<PrismObject<T>> immutableObjects) {
        if (globalObjectCache.isAvailable()) {
            for (PrismObject<T> immutableObject : immutableObjects) {
                storeImmutableObjectToObjectGlobal(immutableObject, CachedObjectValue.computeCompleteFlag(immutableObject));
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

    // Assumption: object will be returned by the RepositoryCache; also that it is immutable if R/O option is present
    <T extends ObjectType> void storeLoadedObjectToAll(PrismObject<T> object, long age) {
        if (age >= DATA_STALENESS_LIMIT) {
            CachePerformanceCollector.INSTANCE.registerSkippedStaleData(object.getCompileTimeClass());
            log("Not caching stale object {} (age = {} ms)", false, object, age);
        } else {
            var cachesAccessInfo = cacheSetAccessInfoFactory.determineExceptForQuery(object.getCompileTimeClass());
            storeLoadedObjectToAll(object, cachesAccessInfo);
        }
    }

    // Assumption: object will be returned by the RepositoryCache; also that it is immutable if R/O option is present
    private <T extends ObjectType> void storeLoadedObjectToAll(
            PrismObject<T> object, CacheSetAccessInfo<T> cachesAccessInfo) {
        boolean putIntoLocalObject = cachesAccessInfo.localObject.effectivelySupports();
        boolean putIntoGlobalObject = cachesAccessInfo.globalObject.effectivelySupports();
        if (putIntoLocalObject || putIntoGlobalObject) {
            // Creating an immutable version:
            // - For R/O, the object is already immutable (-> this is no-op)
            // - For R/W, the clone is necessary (and unavoidable) here
            var immutable = CloneUtil.toImmutable(object);
            var complete = CachedObjectValue.computeCompleteFlag(immutable);
            storeImmutableObjectToObjectLocal(immutable, cachesAccessInfo, complete);
            storeImmutableObjectToObjectGlobal(immutable, complete);
        }

        storeObjectToVersionLocal(object, cachesAccessInfo.localVersion);
        storeObjectToVersionGlobal(object, cachesAccessInfo.globalVersion);
    }

    <T extends ObjectType> void storeImmutableObjectToAllLocal(
            PrismObject<T> immutable, CacheSetAccessInfo<T> caches, boolean complete) {
        storeImmutableObjectToObjectLocal(immutable, caches, complete);
        storeObjectToVersionLocal(immutable, caches.localVersion);
    }
    //endregion

    //region Single objects (content)

    <T extends ObjectType> void storeImmutableObjectToObjectLocal(
            PrismObject<T> immutable, CacheSetAccessInfo<T> caches, boolean complete) {
        if (caches.localObject.effectivelySupports()) {
            caches.localObject.getCache().put(immutable, complete); // no need to clone immutable object
        }
    }

    <T extends ObjectType> void storeImmutableObjectToObjectGlobal(PrismObject<T> immutable, boolean complete) {
        Long nextVersionCheckTime = globalObjectCache.getNextVersionCheckTime(immutable.asObjectable().getClass());
        if (nextVersionCheckTime != null) {
            globalObjectCache.put(new GlobalCacheObjectValue<>(immutable, nextVersionCheckTime, complete));
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
