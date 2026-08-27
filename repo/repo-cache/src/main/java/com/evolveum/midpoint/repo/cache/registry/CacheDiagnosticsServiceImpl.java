/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.registry;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.api.CacheDiagnosticsService;
import com.evolveum.midpoint.repo.api.CacheDiagnostics;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesStateInformationType;

/**
 * A direct implementation of {@link CacheDiagnosticsService}.
 *
 * Note that this class resides in repo-cache module almost by accident and perhaps should
 * be moved to a more appropriate place.
 */
@Component
public class CacheDiagnosticsServiceImpl implements CacheDiagnosticsService {

    private final List<CacheDiagnostics> caches = new ArrayList<>();

    @Override
    public synchronized void registerCache(CacheDiagnostics cache) {
        if (!caches.contains(cache)) {
            caches.add(cache);
        }
    }

    @Override
    public synchronized void unregisterCache(CacheDiagnostics cache) {
        caches.remove(cache);
    }

    @Override
    public CachesStateInformationType getStateInformation() {
        CachesStateInformationType rv = new CachesStateInformationType();
        caches.forEach(cache -> rv.getEntry().addAll(cache.getStateInformation()));
        return rv;
    }

    @Override
    public void dumpContent() {
        caches.forEach(CacheDiagnostics::dumpContent);
    }
}
