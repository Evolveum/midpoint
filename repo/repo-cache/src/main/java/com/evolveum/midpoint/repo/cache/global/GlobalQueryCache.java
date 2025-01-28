/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.global;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.evolveum.midpoint.repo.cache.local.SingleTypeQueryKey;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import jakarta.annotation.PreDestroy;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.expiry.ExpiryPolicy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

@Component
public class GlobalQueryCache extends AbstractGlobalCache {

    private static final Trace LOGGER = TraceManager.getTrace(GlobalQueryCache.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(GlobalQueryCache.class.getName() + ".content");

    private static final String CACHE_NAME_PREFIX = "queryCache";

    private boolean initialized;
    private boolean available;

    private final Map<Class<? extends ObjectType>, TypeSpecificCache> cacheMap = new HashMap<>();

    public synchronized void initialize() {
        if (initialized) {
            LOGGER.warn("Global query cache was already initialized -- ignoring this request.");
            return;
        }
        long capacity = getCapacity();
        if (capacity == 0) {
            LOGGER.warn("Capacity for {} is set to 0; this cache will be disabled", getCacheType());
            available = false;
        } else {
            ObjectTypes[] typeRecords = ObjectTypes.values();
            for (var typeRecord : typeRecords) {
                // TODO Could we make the expire policy faster, i.e. not requiring consultation of the configuration?
                //  But that would need to re-create the cache on configuration change.
                var cache = new Cache2kBuilder<SingleTypeQueryKey, GlobalCacheQueryValue>() {}
                        .name(CACHE_NAME_PREFIX + "_" + typeRecord.getClassDefinition().getSimpleName())
                        .expiryPolicy(getExpirePolicy(typeRecord.getClassDefinition()))
                        .build();
                cacheMap.put(typeRecord.getClassDefinition(), new TypeSpecificCache(cache));

            }
            available = true;
            LOGGER.info("Created global repository query cache for {} object types, each with {}",
                    typeRecords.length,
                    capacity < 0 ? "unlimited capacity" : "a capacity of " + capacity + " queries per object type");
        }
        initialized = true;
    }

    private ExpiryPolicy<SingleTypeQueryKey, GlobalCacheQueryValue> getExpirePolicy(Class<? extends ObjectType> type) {
        return (key, value, loadTime, oldEntry) -> getExpiryTime(type);
    }

    @PreDestroy
    public synchronized void destroy() {
        if (initialized) {
            cacheMap.forEach((type, cache) -> cache.cache.close());
            cacheMap.clear();
            initialized = false;
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public GlobalCacheQueryValue get(QueryKey<?> key) {
        var typeSpecificCache = cacheMap.get(key.getType());
        if (typeSpecificCache != null) {
            return typeSpecificCache.cache.peek(new SingleTypeQueryKey(key.getQuery()));
        } else {
            return null;
        }
    }

    public void remove(QueryKey<?> key) {
        var typeSpecificCache = cacheMap.get(key.getType());
        if (typeSpecificCache != null) {
            typeSpecificCache.cache.remove(new SingleTypeQueryKey(key.getQuery()));
        }
    }

    public void put(QueryKey<?> key, @NotNull SearchResultList<String> immutableList) {
        var typeSpecificCache = cacheMap.get(key.getType());
        if (typeSpecificCache != null) {
            typeSpecificCache.cache.put(
                    new SingleTypeQueryKey(key.getQuery()),
                    new GlobalCacheQueryValue(immutableList));
        }
    }

    public void deleteMatching(
            Class<? extends ObjectType> type, Predicate<Map.Entry<SingleTypeQueryKey, GlobalCacheQueryValue>> predicate) {
        var typeSpecificCache = cacheMap.get(type);
        if (typeSpecificCache != null) {
            typeSpecificCache.cache.asMap().entrySet().removeIf(predicate);
        }
    }

    @Override
    protected CacheType getCacheType() {
        return CacheType.GLOBAL_REPO_QUERY_CACHE;
    }

    public int size() {
        return cacheMap.values().stream()
                .mapToInt(cache -> cache.cache.asMap().size())
                .sum();
    }

    @Override
    public void clear() {
        cacheMap.forEach(
                (type, cache) -> cache.cache.clear());
    }

    public Collection<SingleCacheStateInformationType> getStateInformation() {
        if (!available) {
            return List.of();
        }
        Map<Class<?>, Stats> perTypeStatsMap = new HashMap<>();
        Stats totalStats = new Stats();
        cacheMap.forEach((type, typeSpecificCache) ->
                typeSpecificCache.cache.invokeAll(typeSpecificCache.cache.keys(), e -> {
                    GlobalCacheQueryValue value = e.getValue();
                    if (value != null) {
                        int objects = value.getOidOnlyResult().size();
                        perTypeStatsMap
                                .computeIfAbsent(type, k -> new Stats())
                                .addQuery(objects);
                        totalStats.addQuery(objects);
                    }
                    return null;
                }));
        SingleCacheStateInformationType info = new SingleCacheStateInformationType()
                .name(GlobalQueryCache.class.getName())
                .size(totalStats.queries)
                .secondarySize(totalStats.objects);
        perTypeStatsMap.forEach((type, stats) ->
                info.beginComponent()
                        .name(type.getSimpleName())
                        .size(stats.queries)
                        .secondarySize(stats.objects));
        return List.of(info);
    }

    private static class Stats {
        int queries;
        int objects;

        void addQuery(int objects) {
            queries++;
            this.objects += objects;
        }
    }

    public void dumpContent() {
        if (available && LOGGER_CONTENT.isInfoEnabled()) {
            cacheMap.forEach((type, typeSpecificCache) ->
                    typeSpecificCache.cache.invokeAll(typeSpecificCache.cache.keys(), e -> {
                        SingleTypeQueryKey key = e.getKey();
                        GlobalCacheQueryValue value = e.getValue();
                        if (value != null) {
                            var oidList = value.getOidOnlyResult();
                            LOGGER_CONTENT.info("Cached query of {} ({} object(s), cached {} ms ago): {}: {}",
                                    type.getSimpleName(), oidList.size(), value.getAge(), key.getQuery(), oidList);
                        }
                        return null;
                    }));
        }
    }

    private record TypeSpecificCache(Cache<SingleTypeQueryKey, GlobalCacheQueryValue> cache) {
    }
}
