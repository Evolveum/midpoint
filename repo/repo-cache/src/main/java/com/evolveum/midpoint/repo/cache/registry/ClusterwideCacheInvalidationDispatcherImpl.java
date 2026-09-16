/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.registry;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.repo.api.CacheInvalidationDispatcher;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.repo.api.ClusterwideCacheInvalidationDispatcher;
import com.evolveum.midpoint.repo.api.ClusterwideCacheInvalidationListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Dispatches cache-related events - mainly invalidation ones - to all relevant listeners: both for local node and for remote
 * nodes in the cluster.
 *
 * Notes:
 *
 * - This class intentionally does not filter events. Filtering is done in the local {@link CacheInvalidationDispatcher}.
 * - This class resides in repo-cache module almost by accident and perhaps should be moved to a more appropriate place.
 */
@Component
public class ClusterwideCacheInvalidationDispatcherImpl implements ClusterwideCacheInvalidationDispatcher {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterwideCacheInvalidationDispatcherImpl.class);

    private final List<ClusterwideCacheInvalidationListener> listeners = new ArrayList<>();

    @Override
    public synchronized void registerListener(ClusterwideCacheInvalidationListener listener) {
        LOGGER.debug("Registering listener {}", listener);
        if (listeners.contains(listener)) {
            LOGGER.warn("Registering listener {} which was already registered.", listener);
            return;
        }
        listeners.add(listener);
    }

    @Override
    public synchronized void unregisterListener(ClusterwideCacheInvalidationListener listener) {
        if (!listeners.contains(listener)) {
            LOGGER.warn("Unregistering listener {} which was already unregistered.", listener);
            return;
        }
        listeners.remove(listener);
    }

    @Override
    public <O extends ObjectType> void dispatchInvalidation(
            Class<O> type, String oid, boolean clusterwide, @Nullable CacheInvalidationContext context) {
        for (ClusterwideCacheInvalidationListener listener : listeners) {
            listener.invalidate(type, oid, clusterwide, context);
        }
    }
}
