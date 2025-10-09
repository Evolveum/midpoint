/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesStateInformationType;

/**
 * Registry of all local caches (various caching components/services).
 */
public interface CacheRegistry {

    void registerCache(Cache cache);

    void unregisterCache(Cache cache);

    CachesStateInformationType getStateInformation();

    void dumpContent();
}
