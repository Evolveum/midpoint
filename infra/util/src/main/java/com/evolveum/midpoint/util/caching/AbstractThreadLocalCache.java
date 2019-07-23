/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.util.caching;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mederly
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
        logger.trace("Cache: ENTER for thread {}, {}", currentThread.getName(), inst);
        if (inst == null) {
            logger.trace("Cache: creating for thread {}", currentThread.getName());
            try {
                inst = cacheClass.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
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
        return configuration != null && configuration.supportsObjectType(type);
    }

    public boolean isAvailable() {
        return configuration != null && configuration.isAvailable();
    }

    public CacheConfiguration.CacheObjectTypeConfiguration getConfiguration(Class<?> type) {
        return configuration != null ? configuration.getForObjectType(type) : null;
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
}
