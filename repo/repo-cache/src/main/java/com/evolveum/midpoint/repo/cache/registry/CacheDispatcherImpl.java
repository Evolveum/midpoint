/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.registry;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Dispatches cache-related events - mainly invalidation ones - to all relevant listeners:
 * {@link CacheRegistry} (grouping local caches) and ClusterCacheListener (for
 * inter-node distribution).
 * <p>
 * Could be reworked in the future.
 * <p>
 * Note that this class resides in repo-cache module almost by accident and perhaps should
 * be moved to a more appropriate place.
 */
@Component
public class CacheDispatcherImpl implements CacheDispatcher {

    private static final Trace LOGGER = TraceManager.getTrace(CacheDispatcherImpl.class);

    private final List<CacheListener> cacheListeners = new ArrayList<>();

    @Override
    public synchronized void registerCacheListener(CacheListener cacheListener) {
        if (cacheListeners.contains(cacheListener)) {
            LOGGER.warn("Registering listener {} which was already registered.", cacheListener);
            return;
        }
        cacheListeners.add(cacheListener);
    }

    @Override
    public synchronized void unregisterCacheListener(CacheListener cacheListener) {
        if (!cacheListeners.contains(cacheListener)) {
            LOGGER.warn("Unregistering listener {} which was already unregistered.", cacheListener);
            return;
        }
        cacheListeners.remove(cacheListener);
    }

    @Override
    public <O extends ObjectType> void dispatchInvalidation(Class<O> type, String oid, boolean clusterwide,
            @Nullable CacheInvalidationContext context) {
        for (CacheListener listener : cacheListeners) {
            listener.invalidate(type, oid, clusterwide, context);
        }
    }
}
