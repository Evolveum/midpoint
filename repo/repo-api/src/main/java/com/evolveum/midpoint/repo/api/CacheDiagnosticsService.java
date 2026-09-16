/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesStateInformationType;

/**
 * Provides diagnostics functions (state reporting, content dump) for registered caches.
 *
 * Often used along with {@link CacheInvalidationDispatcher}: a cache needs registering to and unregistering from both.
 *
 * @see CacheInvalidationDispatcher
 */
public interface CacheDiagnosticsService {

    /** Registers a particular cache. */
    void registerCache(CacheDiagnostics cache);

    /** Unregisters a particular cache. */
    void unregisterCache(CacheDiagnostics cache);

    /** Returns aggregate state of all particular caches. */
    CachesStateInformationType getStateInformation();

    /** Requests content dump on all particular caches. */
    void dumpContent();
}
