/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.local;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.cache.AbstractThreadLocalCache;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local cache for storing objects.
 */
public class LocalObjectCache extends AbstractThreadLocalCache {

    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(LocalObjectCache.class.getName() + ".content");

    private final Map<String, LocalCacheObjectValue<? extends ObjectType>> data = new ConcurrentHashMap<>();

    public <T extends ObjectType> LocalCacheObjectValue<T> get(String oid) {
        //noinspection unchecked
        return (LocalCacheObjectValue<T>) data.get(oid);
    }

    public void put(PrismObject<? extends ObjectType> object, boolean complete) {
        put(object.getOid(), object, complete);
    }

    public <T extends ObjectType> void put(String oid, PrismObject<T> object, boolean complete) {
        object.checkImmutable();
        data.put(oid, new LocalCacheObjectValue<>(object, complete));
    }

    public void remove(String oid) {
        data.remove(oid);
    }

    @Override
    public String description() {
        return "O:" + data.size();
    }

    @Override
    protected int getSize() {
        return data.size();
    }

    public void dumpContent(String threadName) {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            data.forEach((k, v) -> LOGGER_CONTENT.info("Cached object [{}] {}: {}", threadName, k, v));
        }
    }
}
