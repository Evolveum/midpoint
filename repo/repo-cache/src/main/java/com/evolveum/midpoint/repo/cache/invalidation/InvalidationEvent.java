/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.invalidation;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Wrapping invalidation event.
 */
@Experimental
public class InvalidationEvent {

    final Class<?> type;
    final String oid;
    final CacheInvalidationContext context;

    InvalidationEvent(Class<?> type, String oid, CacheInvalidationContext context) {
        this.type = type;
        this.oid = oid;
        this.context = context;
    }
}
