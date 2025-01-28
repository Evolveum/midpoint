/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.local;

import static com.evolveum.midpoint.repo.cache.handlers.SearchOpHandler.QUERY_RESULT_SIZE_LIMIT;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Thread-local cache for storing query results.
 */
public class LocalQueryCache extends AbstractThreadLocalCache {

    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(LocalQueryCache.class.getName() + ".content");

    private final Map<QueryKey<?>, LocalCacheQueryValue> data = new ConcurrentHashMap<>();

    public LocalCacheQueryValue get(QueryKey<?> key) {
        return data.get(key);
    }

    public void put(QueryKey<?> key, @NotNull SearchResultList<String> oidOnlyResult) {
        data.put(key, new LocalCacheQueryValue(oidOnlyResult));
    }

    public void remove(QueryKey<?> key) {
        data.remove(key);
    }

    @Override
    public String description() {
        return "Q:" + data.size();
    }

    @Override
    protected int getSize() {
        return data.size();
    }

    public void dumpContent(String threadName) {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            data.forEach((k, v) -> LOGGER_CONTENT.info("Cached query [{}] of {} ({} object(s)): {}: {}", threadName,
                    k.getType(), v.size(), k.getQuery(), v));
        }
    }

    @SuppressWarnings("SameParameterValue")
    static int getTotalCachedObjects(ConcurrentHashMap<Thread, LocalQueryCache> cacheInstances) {
        int rv = 0;
        for (LocalQueryCache cacheInstance : cacheInstances.values()) {
            rv += cacheInstance.getCachedObjects();
        }
        return rv;
    }

    private int getCachedObjects() {
        int rv = 0;
        for (var value : data.values()) {
            rv += value.size();
        }
        return rv;
    }

    public Iterator<Map.Entry<QueryKey<?>, LocalCacheQueryValue>> getEntryIterator() {
        return data.entrySet().iterator();
    }
}
