/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.cache;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.schema.cache.CacheConfiguration.StatisticsLevel;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Temporary implementation.
 */
public class CachePerformanceCollector implements DebugDumpable {

    public static final CachePerformanceCollector INSTANCE = new CachePerformanceCollector();

    @Experimental
    private static final String ALL_CACHES_NAME = "all";

    private final Map<String, CacheData> performanceMap = new ConcurrentHashMap<>();

    private final ThreadLocal<Map<String, CacheData>> threadLocalPerformanceMap = new ThreadLocal<>();

    public static class CacheData implements ShortDumpable {
        public final AtomicInteger hits = new AtomicInteger(0);
        public final AtomicInteger weakHits = new AtomicInteger(0); // e.g. hit but with getVersion call
        public final AtomicInteger misses = new AtomicInteger(0);
        public final AtomicInteger passes = new AtomicInteger(0);
        public final AtomicInteger notAvailable = new AtomicInteger(0);

        // The following two are counted only on global level. This will be probably fixed somehow.
        @Experimental public final AtomicInteger overSizedQueries = new AtomicInteger(0);
        @Experimental public final AtomicInteger skippedStaleData = new AtomicInteger(0);

        private AtomicInteger getHits() {
            return hits;
        }

        private AtomicInteger getWeakHits() {
            return weakHits;
        }

        private AtomicInteger getMisses() {
            return misses;
        }

        private AtomicInteger getPasses() {
            return passes;
        }

        private AtomicInteger getNotAvailable() {
            return notAvailable;
        }

        private AtomicInteger getOverSizedQueries() {
            return overSizedQueries;
        }

        private AtomicInteger getSkippedStaleData() {
            return skippedStaleData;
        }

        public void add(AbstractThreadLocalCache cache) {
            hits.addAndGet(cache.getHits());
            misses.addAndGet(cache.getMisses());
            passes.addAndGet(cache.getPasses());
        }

        @Override
        public void shortDump(StringBuilder sb) {
            int hits = this.hits.get();
            int weakHits = this.weakHits.get();
            int misses = this.misses.get();
            int passes = this.passes.get();
            int notAvailable = this.notAvailable.get();
            int sum = hits + weakHits + misses + passes + notAvailable;
            CacheUtil.formatPerformanceData(sb, hits, weakHits, misses, passes, notAvailable, sum);
        }
    }

    private void increment(Class<?> cacheClass, Class<?> type, StatisticsLevel statisticsLevel, Function<CacheData, AtomicInteger> selector) {
        String key = createKey(cacheClass, type, statisticsLevel);
        if (key != null) {
            selector.apply(getOrCreate(performanceMap, key)).incrementAndGet();
            Map<String, CacheData> localMap = threadLocalPerformanceMap.get();
            if (localMap != null) {
                selector.apply(getOrCreate(localMap, key)).incrementAndGet();
            }
        }
    }

    private String createKey(Class<?> cacheClass, Class<?> type, StatisticsLevel statisticsLevel) {
        if (statisticsLevel == StatisticsLevel.SKIP) {
            return null;
        } else {
            String cacheName = cacheClass != null ? cacheClass.getName() : ALL_CACHES_NAME;
            if (statisticsLevel == null || statisticsLevel == StatisticsLevel.PER_CACHE) {
                return cacheName;
            } else if (statisticsLevel == StatisticsLevel.PER_OBJECT_TYPE) {
                return cacheName + "." + (type != null ? type.getSimpleName() : "null");
            } else {
                throw new IllegalArgumentException("Unexpected statistics level: " + statisticsLevel);
            }
        }
    }

    public void registerHit(Class<?> cacheClass, Class<?> type, StatisticsLevel statisticsLevel) {
        increment(cacheClass, type, statisticsLevel, CacheData::getHits);
    }

    public void registerWeakHit(Class<?> cacheClass, Class<?> type, StatisticsLevel statisticsLevel) {
        increment(cacheClass, type, statisticsLevel, CacheData::getWeakHits);
    }

    public void registerMiss(Class<?> cacheClass, Class<?> type, StatisticsLevel statisticsLevel) {
        increment(cacheClass, type, statisticsLevel, CacheData::getMisses);
    }

    public void registerPass(Class<?> cacheClass, Class<?> type, StatisticsLevel statisticsLevel) {
        increment(cacheClass, type, statisticsLevel, CacheData::getPasses);
    }

    public void registerNotAvailable(Class<?> cacheClass, Class<?> type, StatisticsLevel statisticsLevel) {
        increment(cacheClass, type, statisticsLevel, CacheData::getNotAvailable);
    }

    @Experimental
    public void registerSkippedStaleData(Class<?> type) {
        increment(null, type, CacheConfiguration.StatisticsLevel.PER_OBJECT_TYPE, CacheData::getSkippedStaleData);
    }

    @Experimental
    public void registerOverSizedQuery(Class<?> type) {
        increment(null, type, CacheConfiguration.StatisticsLevel.PER_OBJECT_TYPE, CacheData::getOverSizedQueries);
    }

    private CacheData getOrCreate(Map<String, CacheData> performanceMap, String key) {
        if (performanceMap != null) {
            CacheData existingData = performanceMap.get(key);
            if (existingData != null) {
                return existingData;
            } else {
                CacheData newData = new CacheData();
                performanceMap.put(key, newData);
                return newData;
            }
        } else {
            return null;
        }
    }

    public void clear() {
        performanceMap.clear();
    }

    @Override
    public String debugDump(int indent) {
        ArrayList<String> names = new ArrayList<>(performanceMap.keySet());
        names.sort(String::compareTo);
        int maxLength = names.stream().mapToInt(String::length).max().orElse(0);
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            DebugUtil.indentDebugDump(sb, indent);
            sb.append(String.format("%-"+(maxLength+1)+"s %s\n", name+":", performanceMap.get(name).shortDump()));
        }
        return sb.toString();
    }

    public Map<String, CacheData> getGlobalPerformanceMap() {
        return performanceMap;
    }

    public Map<String, CacheData> getThreadLocalPerformanceMap() {
        return threadLocalPerformanceMap.get();
    }

    /**
     * Starts gathering thread-local performance information, clearing existing (if any).
     */
    public void startThreadLocalPerformanceInformationCollection() {
        threadLocalPerformanceMap.set(new ConcurrentHashMap<>());
    }

    /**
     * Stops gathering thread-local performance information, clearing existing (if any).
     */
    public void stopThreadLocalPerformanceInformationCollection() {
        threadLocalPerformanceMap.remove();
    }

    @Experimental
    public static boolean isExtra(String key) {
        return key.startsWith(ALL_CACHES_NAME + ".");
    }
}
