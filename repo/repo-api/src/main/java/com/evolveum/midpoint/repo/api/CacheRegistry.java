/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesStateInformationType;

/**
 * Registry of all local cacheable services.
 */
public interface CacheRegistry {

    void registerCacheableService(Cacheable cacheableService);

    void unregisterCacheableService(Cacheable cacheableService);

    CachesStateInformationType getStateInformation();

    void dumpContent();
}
