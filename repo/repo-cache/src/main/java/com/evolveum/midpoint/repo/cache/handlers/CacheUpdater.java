/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.global.GlobalCacheObjectValue;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.local.LocalObjectCache;
import com.evolveum.midpoint.repo.cache.local.LocalQueryCache;
import com.evolveum.midpoint.repo.cache.local.LocalVersionCache;
import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.evolveum.midpoint.repo.cache.handlers.SearchOpHandler.QUERY_RESULT_SIZE_LIMIT;
import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.getLocalObjectCache;
import static com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection.getLocalVersionCache;

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

    @Autowired private GlobalObjectCache globalObjectCache;
    @Autowired private GlobalVersionCache globalVersionCache;

    //region Lists of objects

    <T extends ObjectType> void storeSearchResultToAll(QueryKey<T> key,
            SearchResultList<PrismObject<T>> list, CacheSetAccessInfo caches) {
        // The condition is there to avoid needless object cloning.
        if (isQueryCacheableLocally(list, caches.localQuery) || areObjectsCacheableLocally(caches.localObject)
                || isQueryCacheableGlobally(list, caches.globalQuery) || areObjectsCacheableGlobally(caches.globalObject)) {
            SearchResultList<PrismObject<T>> immutableObjectList = list.toDeeplyFrozenList();
            storeImmutableSearchResultToAllLocal(key, immutableObjectList, true, caches);
            storeImmutableSearchResultToAllGlobal(key, immutableObjectList, true, caches);
        } else {
            // For simplicity we ignore the situation if versions are cacheable but objects are not.
            // In such cases object versions will not be cached.
        }
    }

    <T extends ObjectType> void storeImmutableSearchResultToAllLocal(QueryKey<T> key,
            SearchResultList<PrismObject<T>> immutableList, boolean isComplete, CacheSetAccessInfo caches) {
        if (isComplete && isQueryCacheableLocally(immutableList, caches.localQuery)) {
            caches.localQuery.cache.put(key, immutableList);
        }
        storeImmutableObjectsToObjectAndVersionLocal(immutableList);
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

    <T extends ObjectType> void storeImmutableSearchResultToAllGlobal(QueryKey<T> key,
            SearchResultList<PrismObject<T>> immutableList, boolean isComplete, CacheSetAccessInfo caches) {
        if (isComplete && isQueryCacheableGlobally(immutableList, caches.globalQuery)) {
            caches.globalQuery.cache.put(key, immutableList);
        }
        storeImmutableObjectsToObjectAndVersionGlobal(immutableList);
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

    private <T extends ObjectType> boolean isQueryCacheableLocally(SearchResultList<PrismObject<T>> list,
            CacheAccessInfo<LocalQueryCache> localQuery) {
        return localQuery.available && localQuery.supports && list.size() <= QUERY_RESULT_SIZE_LIMIT;
    }

    private boolean areObjectsCacheableLocally(CacheAccessInfo<LocalObjectCache> localObject) {
        return localObject.available && localObject.supports;
    }

    private <T extends ObjectType> boolean isQueryCacheableGlobally(SearchResultList<PrismObject<T>> list,
            CacheAccessInfo<GlobalQueryCache> globalQuery) {
        return globalQuery.available && globalQuery.supports && list.size() <= QUERY_RESULT_SIZE_LIMIT;
    }

    private boolean areObjectsCacheableGlobally(CacheAccessInfo<GlobalObjectCache> globalObject) {
        return globalObject.available && globalObject.supports;
    }

    //endregion

    //region Single objects (content + version)

    <T extends ObjectType> void storeImmutableObjectToObjectAndVersionLocal(PrismObject<T> immutable, CacheSetAccessInfo caches) {
        storeImmutableObjectToObjectLocal(immutable, caches);
        storeObjectToVersionLocal(immutable, caches.localVersion);
    }

    // Assumption: object will be returned by the RepositoryCache
    <T extends ObjectType> void storeLoadedObjectToAll(PrismObject<T> object, CacheSetAccessInfo caches, boolean readOnly) {
        boolean putIntoLocalObject = areObjectsCacheableLocally(caches.localObject);
        boolean putIntoGlobalObject = areObjectsCacheableGlobally(caches.globalObject);
        if (putIntoLocalObject || putIntoGlobalObject) {
            @NotNull PrismObject<T> immutable;
            if (readOnly) {
                // We are going to return the object as read-only, so we can cache the same (frozen) object as we are returning.
                immutable = object;
            } else {
                // We are going to return the object as mutable, so we must cache the frozen clone of the retrieved object.
                immutable = object.clone();
            }
            immutable.freeze();

            if (putIntoLocalObject) {
                caches.localObject.cache.put(immutable);
            }
            if (putIntoGlobalObject) {
                storeImmutableObjectToObjectGlobal(immutable);
            }
        }

        storeObjectToVersionLocal(object, caches.localVersion);
        storeObjectToVersionGlobal(object, caches.globalVersion);
    }
    //endregion

    //region Single objects (content)

    <T extends ObjectType> void storeImmutableObjectToObjectLocal(PrismObject<T> immutable, CacheSetAccessInfo caches) {
        if (areObjectsCacheableLocally(caches.localObject)) {
            caches.localObject.cache.put(immutable); // no need to clone immutable object
        }
    }

    <T extends ObjectType> void storeImmutableObjectToObjectGlobal(PrismObject<T> immutable) {
        CacheConfiguration cacheConfiguration = globalObjectCache.getConfiguration();
        Class<? extends ObjectType> type = immutable.asObjectable().getClass();
        CacheConfiguration.CacheObjectTypeConfiguration typeConfiguration = globalObjectCache.getConfiguration(type);
        if (cacheConfiguration != null && cacheConfiguration.supportsObjectType(type)) {
            long nextVersionCheckTime = computeNextVersionCheckTime(typeConfiguration, cacheConfiguration);
            globalObjectCache.put(new GlobalCacheObjectValue<>(immutable, nextVersionCheckTime));
        }
    }

    //endregion

    //region Single objects (version only)

    private <T extends ObjectType> void storeObjectToVersionGlobal(PrismObject<T> object) {
        CacheConfiguration cacheConfiguration = globalVersionCache.getConfiguration();
        Class<? extends ObjectType> type = object.asObjectable().getClass();
        if (cacheConfiguration != null && cacheConfiguration.supportsObjectType(type)) {
            globalVersionCache.put(object);
        }
    }

    <T extends ObjectType> void storeObjectToVersionGlobal(PrismObject<T> object, CacheAccessInfo<GlobalVersionCache> globalVersion) {
        if (globalVersion.available && globalVersion.supports) {
            globalVersion.cache.put(object);
        }
    }

    <T extends ObjectType> void storeObjectToVersionLocal(PrismObject<T> object, CacheAccessInfo<LocalVersionCache> localVersion) {
        storeVersionToVersionLocal(object.getOid(), object.getVersion(), localVersion);
    }

    void storeVersionToVersionLocal(String oid, String version, CacheAccessInfo<LocalVersionCache> localVersion) {
        if (localVersion.available && localVersion.supports) {
            localVersion.cache.put(oid, version);
        }
    }

    void storeVersionToVersionGlobal(@NotNull Class<? extends ObjectType> type, String oid, String version,
            CacheAccessInfo<GlobalVersionCache> globalVersion) {
        if (globalVersion.available && globalVersion.supports) {
            globalVersionCache.put(oid, type, version);
        }
    }

    //endregion

    <T extends ObjectType> void updateTimeToVersionCheck(GlobalCacheObjectValue<T> cachedValue,
            CacheAccessInfo<GlobalObjectCache> globalObject) {
        assert globalObject.typeConfig != null && globalObject.cacheConfig != null;
        long newTimeToVersionCheck = computeNextVersionCheckTime(globalObject.typeConfig, globalObject.cacheConfig);
        cachedValue.setCheckVersionTime(newTimeToVersionCheck);
    }

    private long computeNextVersionCheckTime(@NotNull CacheConfiguration.CacheObjectTypeConfiguration typeConfig,
            @NotNull CacheConfiguration cacheConfig) {
        return System.currentTimeMillis() + computeTimeToVersionCheck(typeConfig, cacheConfig);
    }

    private long computeTimeToVersionCheck(@NotNull CacheConfiguration.CacheObjectTypeConfiguration typeConfig,
            @NotNull CacheConfiguration cacheConfig) {
        if (typeConfig.getEffectiveTimeToVersionCheck() != null) {
            return typeConfig.getEffectiveTimeToVersionCheck() * 1000L;
        } else if (typeConfig.getEffectiveTimeToLive() != null) {
            return typeConfig.getEffectiveTimeToLive() * 1000L;
        } else if (cacheConfig.getTimeToLive() != null) {
            return cacheConfig.getTimeToLive() * 1000L;
        } else {
            return GlobalObjectCache.DEFAULT_TIME_TO_LIVE * 1000L;
        }
    }
}
