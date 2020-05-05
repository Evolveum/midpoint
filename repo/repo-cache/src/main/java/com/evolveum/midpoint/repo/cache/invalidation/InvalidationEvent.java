/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
