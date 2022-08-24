/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Dispatches events to cache listeners (currently CacheRegistry and ClusterCacheListener).
 */
public interface CacheDispatcher {

    default void registerCacheListener(CacheListener cacheListener) {
        registerCacheInvalidationListener(cacheListener);
    }

    default void unregisterCacheListener(CacheListener cacheListener) {
        unregisterCacheInvalidationListener(cacheListener);
    }

    void registerCacheInvalidationListener(CacheInvalidationListener cacheListener);

    void unregisterCacheInvalidationListener(CacheInvalidationListener cacheListener);


    /**
     * Dispatches "cache entry/entries invalidation" event to all relevant caches, even clusterwide if requested so.
     * @param type Type of object(s) to be invalidated. Null means 'all types' (implies oid is null as well).
     * @param oid Object(s) to be invalidated. Null means 'all objects of given type(s)'.
     * @param clusterwide True if the event has to be distributed clusterwide.
     * @param context Context of the invalidation request (optional).
     */
    <O extends ObjectType> void dispatchInvalidation(@Nullable Class<O> type, @Nullable String oid, boolean clusterwide,
            @Nullable CacheInvalidationContext context);

}
