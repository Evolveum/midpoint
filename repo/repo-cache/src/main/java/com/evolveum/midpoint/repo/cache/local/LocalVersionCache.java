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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local cache for object version.
 */
public class LocalVersionCache extends AbstractThreadLocalCache {

    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(LocalVersionCache.class.getName() + ".content");

    private final Map<String, String> data = new ConcurrentHashMap<>();

    public String get(String oid) {
        return data.get(oid);
    }

    public void put(PrismObject<?> object) {
        put(object.getOid(), object.getVersion());
    }

    public void put(String oid, String version) {
        data.put(oid, version);
    }

    public void remove(String oid) {
        data.remove(oid);
    }

    @Override
    public String description() {
        return "V:" + data.size();
    }

    @Override
    protected int getSize() {
        return data.size();
    }

    public void dumpContent(String threadName) {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            data.forEach((k, v) -> LOGGER_CONTENT.info("Cached version [{}] {}: {}", threadName, k, v));
        }
    }
}
