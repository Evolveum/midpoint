/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.global;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;
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

/**
 *
 */
@Component
public class GlobalVersionCache extends AbstractGlobalCache {

    private static final Trace LOGGER = TraceManager.getTrace(GlobalVersionCache.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(GlobalVersionCache.class.getName() + ".content");

    private static final String CACHE_NAME = "versionCache";

    private org.cache2k.Cache<String, GlobalCacheObjectVersionValue> cache;

    public void initialize() {
        if (cache != null) {
            LOGGER.warn("Global version cache was already initialized -- ignoring this request.");
            return;
        }
        long capacity = getCapacity();
        if (capacity == 0) {
            LOGGER.warn("Capacity for " + getCacheType() + " is set to 0; this cache will be disabled (until system restart)");
            cache = null;
        } else {
            cache = new Cache2kBuilder<String, GlobalCacheObjectVersionValue>() {}
                    .name(CACHE_NAME)
                    .entryCapacity(capacity)
                    .expiryPolicy(getExpirePolicy())
                    .storeByReference(true) // this is default in the current version of cache2k; we need this because we update TTL value for cached objects
                    .build();
            LOGGER.info("Created global repository object version cache with a capacity of {} objects", capacity);
        }
    }

    private ExpiryPolicy<String, GlobalCacheObjectVersionValue> getExpirePolicy() {
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

    public String get(String oid) {
        if (cache != null) {
            GlobalCacheObjectVersionValue<?> value = cache.peek(oid);
            return value != null ? value.getVersion() : null;
        } else
            return null;
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
                    if (e.getValue() != null && type.isAssignableFrom(e.getValue().getObjectType())) {
                        e.remove();
                    }
                    return null;
                });
            }
        }
    }

    @Override
    protected CacheType getCacheType() {
        return CacheType.GLOBAL_REPO_VERSION_CACHE;
    }

    @Override
    public void clear() {
        if (cache != null) {
            cache.clear();
        }
    }

    public Collection<SingleCacheStateInformationType> getStateInformation() {
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
                    .name(GlobalVersionCache.class.getName())
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

    public <T extends ObjectType> void put(PrismObject<T> object) {
        if (cache != null) {
            cache.put(object.getOid(), new GlobalCacheObjectVersionValue<>(object.asObjectable().getClass(), object.getVersion()));
        }
    }

    public void put(String oid, Class<? extends ObjectType> type, String version) {
        if (cache != null) {
            cache.put(oid, new GlobalCacheObjectVersionValue<>(type, version));
        }
    }

    public void dumpContent() {
        if (cache != null && LOGGER_CONTENT.isInfoEnabled()) {
            cache.invokeAll(cache.keys(), e -> {
                LOGGER_CONTENT.info("Cached version: {}: {} (cached {} ms ago)", e.getKey(), e.getValue(), e.getValue().getAge());
                return null;
            });
        }
    }
}
