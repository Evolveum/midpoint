/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.local;

import static com.evolveum.midpoint.schema.cache.CacheType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

/**
 * Set of three thread-local repo caches (object, version, query).
 */
@Component
public class LocalRepoCacheCollection {

    public static final Trace LOGGER = TraceManager.getTrace(LocalRepoCacheCollection.class);

    @Autowired private PrismContext prismContext;

    private static final ConcurrentHashMap<Thread, LocalObjectCache> LOCAL_OBJECT_CACHE_INSTANCE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Thread, LocalVersionCache> LOCAL_VERSION_CACHE_INSTANCE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Thread, LocalQueryCache> LOCAL_QUERY_CACHE_INSTANCE = new ConcurrentHashMap<>();

    public static LocalObjectCache getLocalObjectCache() {
        return LOCAL_OBJECT_CACHE_INSTANCE.get(Thread.currentThread());
    }

    public static LocalVersionCache getLocalVersionCache() {
        return LOCAL_VERSION_CACHE_INSTANCE.get(Thread.currentThread());
    }

    public static LocalQueryCache getLocalQueryCache() {
        return LOCAL_QUERY_CACHE_INSTANCE.get(Thread.currentThread());
    }

    public static List<LocalQueryCache> getLocalQueryCaches() {
        List<LocalQueryCache> caches = new ArrayList<>();
        caches.addAll(LOCAL_QUERY_CACHE_INSTANCE.values());

        return Collections.unmodifiableList(caches);
    }

    public static void destroy() {
        LocalObjectCache.destroy(LOCAL_OBJECT_CACHE_INSTANCE, LOGGER);
        LocalVersionCache.destroy(LOCAL_VERSION_CACHE_INSTANCE, LOGGER);
        LocalQueryCache.destroy(LOCAL_QUERY_CACHE_INSTANCE, LOGGER);
    }

    public static void enter(CacheConfigurationManager mgr) {
        // let's compute configuration first -- an exception can be thrown there; so if it happens, none of the caches
        // will be entered into upon exit of this method
        CacheConfiguration objectCacheConfig = mgr.getConfiguration(LOCAL_REPO_OBJECT_CACHE);
        CacheConfiguration versionCacheConfig = mgr.getConfiguration(LOCAL_REPO_VERSION_CACHE);
        CacheConfiguration queryCacheConfig = mgr.getConfiguration(LOCAL_REPO_QUERY_CACHE);

        LocalObjectCache.enter(LOCAL_OBJECT_CACHE_INSTANCE, LocalObjectCache.class, objectCacheConfig, LOGGER);
        LocalVersionCache.enter(LOCAL_VERSION_CACHE_INSTANCE, LocalVersionCache.class, versionCacheConfig, LOGGER);
        LocalQueryCache.enter(LOCAL_QUERY_CACHE_INSTANCE, LocalQueryCache.class, queryCacheConfig, LOGGER);
    }

    public static void exit() {
        LocalObjectCache.exit(LOCAL_OBJECT_CACHE_INSTANCE, LOGGER);
        LocalVersionCache.exit(LOCAL_VERSION_CACHE_INSTANCE, LOGGER);
        LocalQueryCache.exit(LOCAL_QUERY_CACHE_INSTANCE, LOGGER);
    }

    public static boolean exists() {
        return LocalObjectCache.exists(LOCAL_OBJECT_CACHE_INSTANCE) ||
                LocalVersionCache.exists(LOCAL_VERSION_CACHE_INSTANCE) ||
                LocalQueryCache.exists(LOCAL_QUERY_CACHE_INSTANCE);
    }

    public static String debugDump() {
        // TODO
        return LocalObjectCache.debugDump(LOCAL_OBJECT_CACHE_INSTANCE) + "\n" +
                LocalVersionCache.debugDump(LOCAL_VERSION_CACHE_INSTANCE) + "\n" +
                LocalQueryCache.debugDump(LOCAL_QUERY_CACHE_INSTANCE);
    }

    public void getStateInformation(List<SingleCacheStateInformationType> rv) {
        rv.add(new SingleCacheStateInformationType(prismContext)
                .name(LocalObjectCache.class.getName())
                .size(LocalObjectCache.getTotalSize(LOCAL_OBJECT_CACHE_INSTANCE)));
        rv.add(new SingleCacheStateInformationType(prismContext)
                .name(LocalVersionCache.class.getName())
                .size(LocalVersionCache.getTotalSize(LOCAL_VERSION_CACHE_INSTANCE)));
        rv.add(new SingleCacheStateInformationType(prismContext)
                .name(LocalQueryCache.class.getName())
                .size(LocalQueryCache.getTotalSize(LOCAL_QUERY_CACHE_INSTANCE))
                .secondarySize(LocalQueryCache.getTotalCachedObjects(LOCAL_QUERY_CACHE_INSTANCE)));

    }

    public void dumpContent() {
        LocalObjectCache.dumpContent(LOCAL_OBJECT_CACHE_INSTANCE);
        LocalVersionCache.dumpContent(LOCAL_VERSION_CACHE_INSTANCE);
        LocalQueryCache.dumpContent(LOCAL_QUERY_CACHE_INSTANCE);
    }
}
