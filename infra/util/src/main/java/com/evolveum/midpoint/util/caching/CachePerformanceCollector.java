/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.caching;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.caching.CacheConfiguration.StatisticsLevel;

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

	private final Map<String, CacheData> performanceMap = new ConcurrentHashMap<>();

	private final ThreadLocal<Map<String, CacheData>> threadLocalPerformanceMap = new ThreadLocal<>();

	public static class CacheData implements ShortDumpable {
		public final AtomicInteger hits = new AtomicInteger(0);
		public final AtomicInteger weakHits = new AtomicInteger(0);             // e.g. hit but with getVersion call
		public final AtomicInteger misses = new AtomicInteger(0);
		public final AtomicInteger passes = new AtomicInteger(0);
		public final AtomicInteger notAvailable = new AtomicInteger(0);

		public AtomicInteger getHits() {
			return hits;
		}

		public AtomicInteger getWeakHits() {
			return weakHits;
		}

		public AtomicInteger getMisses() {
			return misses;
		}

		public AtomicInteger getPasses() {
			return passes;
		}

		public AtomicInteger getNotAvailable() {
			return notAvailable;
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

	//	public void onCacheDestroy(AbstractThreadLocalCache cache) {
//		getOrCreate(performanceMap, cache.getClass()).add(cache);
//		Map<String, CacheData> localMap = threadLocalPerformanceMap.get();
//		if (localMap != null) {
//			getOrCreate(localMap, cache.getClass()).add(cache);
//		}
//	}

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
		} else if (statisticsLevel == null || statisticsLevel == StatisticsLevel.PER_CACHE) {
			return cacheClass.getName();
		} else if (statisticsLevel == StatisticsLevel.PER_OBJECT_TYPE) {
			return cacheClass.getName() + "." + (type != null ? type.getSimpleName() : "null");
		} else {
			throw new IllegalArgumentException("Unexpected statistics level: " + statisticsLevel);
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
}
