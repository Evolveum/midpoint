/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.util.caching;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;

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

		public void add(AbstractCache cache) {
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
			sb.append("hits: ").append(hits);
			if (sum > 0) {
				sb.append(String.format(" (%.1f%%)", 100.0f * hits / sum));
			}
			if (weakHits > 0) {
				sb.append(", weak hits: ").append(weakHits);
				if (sum > 0) {
					sb.append(String.format(" (%.1f%%)", 100.0f * weakHits / sum));
				}
			}
			sb.append(", misses: ").append(misses);
			sb.append(", passes: ").append(passes);
			sb.append(", not available: ").append(notAvailable);
		}
	}

	public void onCacheDestroy(AbstractCache cache) {
		getOrCreate(performanceMap, cache.getClass()).add(cache);
		Map<String, CacheData> localMap = threadLocalPerformanceMap.get();
		if (localMap != null) {
			getOrCreate(localMap, cache.getClass()).add(cache);
		}
	}

	private void increment(Class<?> cacheClass, Function<CacheData, AtomicInteger> selector) {
		selector.apply(getOrCreate(performanceMap, cacheClass)).incrementAndGet();
		Map<String, CacheData> localMap = threadLocalPerformanceMap.get();
		if (localMap != null) {
			selector.apply(getOrCreate(localMap, cacheClass)).incrementAndGet();
		}
	}

	public void registerHit(Class<?> cacheClass) {
		increment(cacheClass, CacheData::getHits);
	}

	public void registerWeakHit(Class<?> cacheClass) {
		increment(cacheClass, CacheData::getWeakHits);
	}

	public void registerMiss(Class<?> cacheClass) {
		increment(cacheClass, CacheData::getMisses);
	}

	public void registerPass(Class<?> cacheClass) {
		increment(cacheClass, CacheData::getPasses);
	}

	public void registerNotAvailable(Class<?> cacheClass) {
		increment(cacheClass, CacheData::getNotAvailable);
	}

	private CacheData getOrCreate(Map<String, CacheData> performanceMap, Class<?> cacheClass) {
		if (performanceMap != null) {
			CacheData existingData = performanceMap.get(cacheClass.getName());
			if (existingData != null) {
				return existingData;
			} else {
				CacheData newData = new CacheData();
				performanceMap.put(cacheClass.getName(), newData);
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
