/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.cache.registry;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.api.CacheRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.repo.api.Cacheable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesStateInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Registry of all local cacheable services.
 *
 * Note that this class resides in repo-cache module almost by accident and perhaps should
 * be moved to a more appropriate place.
 */
@Component("cacheRegistry")
public class CacheRegistryImpl implements CacheListener, CacheRegistry {

    private final List<Cacheable> cacheableServices = new ArrayList<>();

    @Autowired private CacheDispatcher dispatcher;
    @Autowired private PrismContext prismContext;

    @PostConstruct
    public void registerListener() {
        dispatcher.registerCacheListener(this);
    }

    @PreDestroy
    public void unregisterListener() {
        dispatcher.unregisterCacheListener(this);
    }

    @Override
    public synchronized void registerCacheableService(Cacheable cacheableService) {
        if (!cacheableServices.contains(cacheableService)) {
            cacheableServices.add(cacheableService);
        }
    }

    @Override
    public synchronized void unregisterCacheableService(Cacheable cacheableService) {
        cacheableServices.remove(cacheableService);
    }

    @Override
    public <O extends ObjectType> void invalidate(Class<O> type, String oid, boolean clusterwide,
            CacheInvalidationContext context) {
        // We currently ignore clusterwide parameter, because it's used by ClusterCacheListener only.
        // So we assume that the invalidation event - from this point on - is propagated only locally.
        for (Cacheable cacheableService : cacheableServices) {
            cacheableService.invalidate(type, oid, context);
        }
    }

    @Override
    public CachesStateInformationType getStateInformation() {
        CachesStateInformationType rv = new CachesStateInformationType(prismContext);
        cacheableServices.forEach(cacheable -> rv.getEntry().addAll(cacheable.getStateInformation()));
        return rv;
    }

    @Override
    public void dumpContent() {
        cacheableServices.forEach(Cacheable::dumpContent);
    }
}

