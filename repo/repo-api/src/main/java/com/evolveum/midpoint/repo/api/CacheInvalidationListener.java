/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface CacheInvalidationListener {

    Collection<CacheInvalidationEventSpecification> getEventSpecifications();

    /**
     * Invalidates given object(s) in all relevant caches.
     * @param type Type of object (null means all types).
     * @param oid OID of object (null means all object(s) of given type(s)).
     * @param clusterwide Whether to distribute this event clusterwide.
     * @param context Context of the invalidation request (optional).
     */
    <O extends ObjectType> void invalidate(Class<O> type, String oid, boolean clusterwide, CacheInvalidationContext context);

}
