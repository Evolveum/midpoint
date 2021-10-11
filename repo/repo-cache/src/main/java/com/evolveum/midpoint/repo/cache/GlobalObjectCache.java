/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;
import org.cache2k.Cache2kBuilder;
import org.cache2k.expiry.ExpiryPolicy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
@Component
public class GlobalObjectCache extends AbstractGlobalCache {

    private static final Trace LOGGER = TraceManager.getTrace(GlobalObjectCache.class);

    private static final String CACHE_NAME = "objectCache";

    private org.cache2k.Cache<String, GlobalCacheObjectValue> cache;

    public void initialize() {
        if (cache != null) {
            LOGGER.warn("Global object cache was already initialized -- ignoring this request.");
            return;
        }
        long capacity = getCapacity();
        if (capacity == 0) {
            LOGGER.warn("Capacity for " + getCacheType() + " is set to 0; this cache will be disabled (until system restart)");
            cache = null;
        } else {
            cache = new Cache2kBuilder<String, GlobalCacheObjectValue>() {}
                    .name(CACHE_NAME)
                    .entryCapacity(capacity)
                    .expiryPolicy(getExpirePolicy())
                    .storeByReference(true) // this is default in the current version of cache2k; we need this because we update TTL value for cached objects
                    .build();
            LOGGER.info("Created global repository object cache with a capacity of {} objects", capacity);
        }
    }

    private ExpiryPolicy<String, GlobalCacheObjectValue> getExpirePolicy() {
        return (key, value, loadTime, oldEntry) -> getExpiryTime(value.getObjectType());
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

    public <T extends ObjectType> GlobalCacheObjectValue<T> get(String oid) {
        //noinspection unchecked
        return cache != null ? cache.peek(oid) : null;
    }

    public void remove(@NotNull String oid) {
        if (cache != null) {
            cache.remove(oid);
        }
    }

    public void remove(@NotNull Class<?> type, String oid) {
        // todo deduplicate
        if (cache != null) {
            if (oid != null) {
                cache.remove(oid);
            } else {
                cache.invokeAll(cache.keys(), e -> {
                    if (e.getValue() != null && e.getValue().getObjectType() != null &&
                            type.isAssignableFrom(e.getValue().getObjectType())) {
                        e.remove();
                    }
                    return null;
                });
            }
        }
    }

    public <T extends ObjectType> void put(GlobalCacheObjectValue<T> cacheObject) {
        if (cache != null) {
            cache.put(cacheObject.getObjectOid(), cacheObject);
        }
    }

    @Override
    protected CacheType getCacheType() {
        return CacheType.GLOBAL_REPO_OBJECT_CACHE;
    }

    @Override
    public void clear() {
        if (cache != null) {
            cache.clear();
        }
    }

    Collection<SingleCacheStateInformationType> getStateInformation() {
        Map<Class<?>, Integer> counts = new HashMap<>();
        AtomicInteger size = new AtomicInteger(0);
        if (cache != null) {
            cache.invokeAll(cache.keys(), e -> {
                Class<?> objectType = e.getValue().getObjectType();
                counts.compute(objectType, (type, count) -> count != null ? count+1 : 1);
                size.incrementAndGet();
                return null;
            });
            SingleCacheStateInformationType info = new SingleCacheStateInformationType(prismContext)
                    .name(GlobalObjectCache.class.getName())
                    .size(size.get());
            counts.forEach((type, count) ->
                    info.beginComponent()
                        .name(type.getSimpleName())
                        .size(count));
            return Collections.singleton(info);
        } else {
            return Collections.emptySet();
        }
    }
}
