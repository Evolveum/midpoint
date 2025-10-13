/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

/**
 * Provides more specific information e.g. about the nature of the change that triggered the invalidation event.
 * For example, invalidations caused by repo add/modify/delete operation can contain information about the object
 * added/modified/deleted to help with the evaluation of the impact of the operation on cached query results.
 *
 * Usually it can be safely ignored by individual caches.
 *
 * This is very experimental, to say it mildly. It will probably change in the future.
 * (E.g. there is currently no mechanism how to transfer this data throughout the cluster.)
 */
public interface CacheInvalidationDetails {
}
