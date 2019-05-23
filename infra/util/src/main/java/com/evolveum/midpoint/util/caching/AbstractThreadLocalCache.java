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

/**
 * @author mederly
 */
public abstract class AbstractThreadLocalCache {

    private int entryCount = 0;

    private int hits = 0;
    private int misses = 0;
    private int passes = 0;

    private CacheConfiguration configuration;

    public static <T extends AbstractThreadLocalCache> T enter(ThreadLocal<T> cacheThreadLocal, Class<T> cacheClass,
            CacheConfiguration configuration, Trace logger) {
        T inst = cacheThreadLocal.get();
        logger.trace("Cache: ENTER for thread {}, {}", Thread.currentThread().getName(), inst);
        if (inst == null) {
            logger.trace("Cache: creating for thread {}", Thread.currentThread().getName());
            try {
                inst = cacheClass.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
                throw new SystemException("Couldn't instantiate cache: " + e.getMessage(), e);
            }
            inst.setConfiguration(configuration);
            cacheThreadLocal.set(inst);
        }
        inst.incrementEntryCount();
        return inst;
    }

    public static <T extends AbstractThreadLocalCache> T exit(ThreadLocal<T> cacheThreadLocal, Trace logger) {
        T inst = cacheThreadLocal.get();
        logger.trace("Cache: EXIT for thread {}, {}", Thread.currentThread().getName(), inst);
        if (inst == null || inst.getEntryCount() == 0) {
            logger.error("Cache: Attempt to exit cache that does not exist or has entry count 0: {}", inst);
            cacheThreadLocal.set(null);
        } else {
            inst.decrementEntryCount();
            if (inst.getEntryCount() <= 0) {
                destroy(cacheThreadLocal, logger);
            }
        }
        return inst;
    }

    public static <T extends AbstractThreadLocalCache> void destroy(ThreadLocal<T> cacheThreadLocal, Trace logger) {
        T inst = cacheThreadLocal.get();
        if (inst != null) {
            logger.trace("Cache: DESTROY for thread {}: {}", Thread.currentThread().getName(), inst.getCacheStatisticsString());
            //CachePerformanceCollector.INSTANCE.onCacheDestroy(inst);
            cacheThreadLocal.set(null);
        }
    }

    public String getCacheStatisticsString() {
        return "hits: " + hits + ", misses: " + misses + ", passes: " + passes +
                (hits+misses+passes != 0 ? ", % of hits: " + (100.0f * hits / (hits + misses + passes)) : "");
    }

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }

    public int getPasses() {
        return passes;
    }

    public void incrementEntryCount() {
        entryCount++;
    }

    public void decrementEntryCount() {
        entryCount--;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public static boolean exists(ThreadLocal<? extends AbstractThreadLocalCache> cacheThreadLocal) {
        return cacheThreadLocal.get() != null;
    }

    public static <T extends AbstractThreadLocalCache> String debugDump(ThreadLocal<T> cacheThreadLocal) {
        T inst = cacheThreadLocal.get();
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
}
