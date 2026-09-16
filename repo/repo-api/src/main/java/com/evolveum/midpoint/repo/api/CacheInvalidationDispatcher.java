/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

/**
 * Dispatches "object modified" (invalidation) events to node-local listeners.
 *
 * Often used along with {@link CacheDiagnostics}.
 *
 * @see ClusterwideCacheInvalidationDispatcher
 * @see CacheDiagnostics
 */
public interface CacheInvalidationDispatcher {

    /** Registers a particular listener (cache). */
    void registerListener(CacheInvalidationListener listener);

    /** Unregisters a particular listener (cache). */
    void unregisterListener(CacheInvalidationListener listener);
}
