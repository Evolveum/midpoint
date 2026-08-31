/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Defines a listener that is notified about object change (cache invalidation) events - events describing that an object
 * of given type was added, modified, or deleted.
 *
 * This is a cluster-wide version of {@link CacheInvalidationListener}.
 *
 * NOT TO BE IMPLEMENTED BY GENERAL PUBLIC.
 *
 * Normally, there should be only two classes implementing this interface: one for local-node distribution and second one
 * for distribution onto remote nodes in cluster.
 *
 * @see CacheInvalidationListener
 */
public interface ClusterwideCacheInvalidationListener {

    /**
     * Signals that given object(s) in all relevant caches.
     *
     * @param type Type of object ({@code null} means all types).
     * @param oid OID of object ({@code null} means all object(s) of given type(s)).
     * @param clusterwide Whether to distribute this event clusterwide.
     * @param context More details regarding invalidation request (optional).
     */
    <O extends ObjectType> void invalidate(Class<O> type, String oid, boolean clusterwide, CacheInvalidationContext context);

}
