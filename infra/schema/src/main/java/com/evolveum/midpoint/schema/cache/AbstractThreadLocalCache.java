/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.cache;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common supertype for various thread-local caches (parts of RepositoryCache but also others).
 */
public abstract class AbstractThreadLocalCache {

    private int entryCount = 0;

    private int hits = 0;
    private int misses = 0;
    private int passes = 0;

    private CacheConfiguration configuration;

    public static <T extends AbstractThreadLocalCache> T enter(ConcurrentHashMap<Thread, T> cacheThreadMap, Class<T> cacheClass,
            CacheConfiguration configuration, Trace logger) {
        Thread currentThread = Thread.currentThread();
        T inst = cacheThreadMap.get(currentThread);
        logger.trace("Cache: ENTER for thread {}, {} ({})", currentThread.getName(), inst, cacheClass.getSimpleName());
        if (inst == null) {
            logger.trace("Cache: creating for thread {}", currentThread.getName());
            try {
                inst = cacheClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new SystemException("Couldn't instantiate cache: " + e.getMessage(), e);
            }
            inst.setConfiguration(configuration);
            cacheThreadMap.put(currentThread, inst);
        }
        inst.incrementEntryCount();
        return inst;
    }

    public static <T extends AbstractThreadLocalCache> T exit(ConcurrentHashMap<Thread, T> cacheThreadMap, Trace logger) {
        Thread currentThread = Thread.currentThread();
        T inst = cacheThreadMap.get(currentThread);
        logger.trace("Cache: EXIT for thread {}, {}", Thread.currentThread().getName(), inst);
        if (inst == null || inst.getEntryCount() == 0) {
            logger.error("Cache: Attempt to exit cache that does not exist or has entry count 0: {}", inst);
            cacheThreadMap.remove(currentThread);
        } else {
            inst.decrementEntryCount();
            if (inst.getEntryCount() <= 0) {
                destroy(cacheThreadMap, logger);
            }
        }
        return inst;
    }

    public static <T extends AbstractThreadLocalCache> void destroy(ConcurrentHashMap<Thread, T> cacheThreadMap, Trace logger) {
        Thread currentThread = Thread.currentThread();
        T inst = cacheThreadMap.get(currentThread);
        if (inst != null) {
            logger.trace("Cache: DESTROY for thread {}: {}", Thread.currentThread().getName(), inst.getCacheStatisticsString());
            //CachePerformanceCollector.INSTANCE.onCacheDestroy(inst);
            cacheThreadMap.remove(currentThread);
        }
    }

    String getCacheStatisticsString() {
        return "hits: " + hits + ", misses: " + misses + ", passes: " + passes +
                (hits+misses+passes != 0 ? ", % of hits: " + (100.0f * hits / (hits + misses + passes)) : "");
    }

    int getHits() {
        return hits;
    }

    int getMisses() {
        return misses;
    }

    int getPasses() {
        return passes;
    }

    void incrementEntryCount() {
        entryCount++;
    }

    void decrementEntryCount() {
        entryCount--;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public static boolean exists(ConcurrentHashMap<Thread, ? extends AbstractThreadLocalCache> instances) {
        return instances.get(Thread.currentThread()) != null;
    }

    public static <T extends AbstractThreadLocalCache> String debugDump(ConcurrentHashMap<Thread, T> instances) {
        T inst = instances.get(Thread.currentThread());
        StringBuilder sb = new StringBuilder("Cache ");
        if (inst != null) {
            sb.append("exists (").append(inst.getCacheStatisticsString()).append("), entry count ");
            sb.append(inst.getEntryCount());
            sb.append(", content: ");
            sb.append(inst.description());
        } else {
            sb.append("doesn't exist");
        }
        return sb.toString();
    }

    abstract public String description();

    public void registerHit() {
        hits++;
    }

    public void registerMiss() {
        misses++;
    }

    public void registerPass() {
        passes++;
    }

    public boolean supportsObjectType(Class<?> type) {
        return configuration != null && configuration.supportsObjectType(type, null);
    }

    public boolean isAvailable() {
        return configuration != null && configuration.isAvailable();
    }

    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(CacheConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "entryCount=" + entryCount +
                '}';
    }

    public static <T extends AbstractThreadLocalCache> int getTotalSize(ConcurrentHashMap<Thread, T> cacheInstances) {
        int rv = 0;
        for (T cacheInstance : cacheInstances.values()) {
            rv += cacheInstance.getSize();
        }
        return rv;
    }

    protected abstract int getSize();

    public static <T extends AbstractThreadLocalCache> void dumpContent(ConcurrentHashMap<Thread, T> cacheInstances) {
        cacheInstances.forEach((thread, cache) -> cache.dumpContent(thread.getName()));
    }

    protected abstract void dumpContent(String threadName);
}
