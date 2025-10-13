/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.invalidation;

/**
 * Listens for invalidation events that are received by RepositoryCache/Invalidator.
 */
public interface InvalidationEventListener {

    /**
     * Called when specified invalidation event occurs.
     */
    void onInvalidationEvent(InvalidationEvent event);
}
