/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
