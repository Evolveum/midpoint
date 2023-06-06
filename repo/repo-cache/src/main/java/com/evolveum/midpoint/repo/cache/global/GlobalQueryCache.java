/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.global;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.local.QueryKey;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

import org.apache.commons.lang3.tuple.MutablePair;
import org.cache2k.Cache2kBuilder;
import org.cache2k.expiry.ExpiryPolicy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.evolveum.midpoint.repo.cache.handlers.SearchOpHandler.QUERY_RESULT_SIZE_LIMIT;

/**
 *
 */
@Component
public class GlobalQueryCache extends AbstractGlobalCache {

    private static final Trace LOGGER = TraceManager.getTrace(GlobalQueryCache.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(GlobalQueryCache.class.getName() + ".content");

    private static final String CACHE_NAME = "queryCache";

    private org.cache2k.Cache<QueryKey, GlobalCacheQueryValue> cache;

    public void initialize() {
        if (cache != null) {
            LOGGER.warn("Global query cache was already initialized -- ignoring this request.");
            return;
        }
        long capacity = getCapacity();
        if (capacity == 0) {
            LOGGER.warn("Capacity for " + getCacheType() + " is set to 0; this cache will be disabled (until system restart)");
            cache = null;
        } else {
            cache = new Cache2kBuilder<QueryKey, GlobalCacheQueryValue>() {}
                    .name(CACHE_NAME)
                    .entryCapacity(capacity)
                    .expiryPolicy(getExpirePolicy())
                    .build();
            LOGGER.info("Created global repository query cache with a capacity of {} queries", capacity);
        }
    }

    private ExpiryPolicy<QueryKey, GlobalCacheQueryValue> getExpirePolicy() {
        return (key, value, loadTime, oldEntry) -> getExpiryTime(key.getType());
    }

    @PreDestroy
    public void destroy() {
        if (cache != null) {
            cache.close();
            cache = null;
        }
    }

    public boolean isAvailable() {
        return cache != null;
    }

    public <T extends ObjectType> SearchResultList<PrismObject<T>> get(QueryKey key) {
        if (cache != null) {
            GlobalCacheQueryValue value = cache.peek(key);
            //noinspection unchecked
            return value != null ? value.getResult() : null;
        } else {
            return null;
        }
    }

    public void remove(QueryKey cacheKey) {
        if (cache != null) {
            cache.remove(cacheKey);
        }
    }

    public <T extends ObjectType> void put(QueryKey<T> key, @NotNull SearchResultList<PrismObject<T>> cacheObject) {
        if (cache != null) {
            cacheObject.checkImmutable();
            if (cacheObject.size() > QUERY_RESULT_SIZE_LIMIT) {
                throw new IllegalStateException("Trying to cache result list greater than " + QUERY_RESULT_SIZE_LIMIT + ": " + cacheObject.size());
            }
            //noinspection unchecked
            cache.put(key, new GlobalCacheQueryValue(cacheObject));
        }
    }

    public void deleteMatching(Predicate<Map.Entry<QueryKey, GlobalCacheQueryValue>> predicate) {
        if (cache != null) {
            cache.asMap().entrySet().removeIf(predicate);
        }
    }

    @Override
    protected CacheType getCacheType() {
        return CacheType.GLOBAL_REPO_QUERY_CACHE;
    }

    public int size() {
        return cache.asMap().size();
    }

    @Override
    public void clear() {
        if (cache != null) {
            cache.clear();
        }
    }

    public Collection<SingleCacheStateInformationType> getStateInformation() {
        Map<Class<?>, MutablePair<Integer, Integer>> counts = new HashMap<>();
        AtomicInteger queries = new AtomicInteger(0);
        AtomicInteger objects = new AtomicInteger(0);
        if (cache != null) {
            cache.invokeAll(cache.keys(), e -> {
                QueryKey queryKey = e.getKey();
                Class<?> objectType = queryKey.getType();
                int resultingObjects = e.getValue().getResult().size();
                MutablePair<Integer, Integer> value = counts.get(objectType);
                if (value == null) {
                    value = new MutablePair<>(0, 0);
                    counts.put(objectType, value);
                }
                value.setLeft(value.getLeft() + 1);
                value.setRight(value.getRight() + resultingObjects);
                queries.incrementAndGet();
                objects.addAndGet(resultingObjects);
                return null;
            });
            SingleCacheStateInformationType info = new SingleCacheStateInformationType(prismContext)
                    .name(GlobalQueryCache.class.getName())
                    .size(queries.get())
                    .secondarySize(objects.get());
            counts.forEach((type, pair) ->
                    info.beginComponent()
                            .name(type.getSimpleName())
                            .size(pair.getLeft())
                            .secondarySize(pair.getRight()));
            return Collections.singleton(info);
        } else {
            return Collections.emptySet();
        }
    }

    public void dumpContent() {
        if (cache != null && LOGGER_CONTENT.isInfoEnabled()) {
            cache.invokeAll(cache.keys(), e -> {
                QueryKey key = e.getKey();
                GlobalCacheQueryValue<?> value = e.getValue();
                @NotNull SearchResultList<?> queryResult = value.getResult();
                LOGGER_CONTENT.info("Cached query of {} ({} object(s), cached {} ms ago): {}: {}",
                        key.getType().getSimpleName(), queryResult.size(), value.getAge(), key.getQuery(), queryResult);
                return null;
            });
        }
    }
}
