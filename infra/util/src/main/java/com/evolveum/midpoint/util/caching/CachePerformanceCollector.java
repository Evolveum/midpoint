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
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Temporary implementation.
 */
public class CachePerformanceCollector implements DebugDumpable {

	public static final CachePerformanceCollector INSTANCE = new CachePerformanceCollector();

	public static class CacheData implements ShortDumpable {
		public final AtomicInteger hits = new AtomicInteger(0);
		public final AtomicInteger weakHits = new AtomicInteger(0);             // e.g. hit but with getVersion call
		public final AtomicInteger misses = new AtomicInteger(0);
		public final AtomicInteger passes = new AtomicInteger(0);
		public final AtomicInteger notAvailable = new AtomicInteger(0);

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

	private final Map<String, CacheData> performanceMap = new ConcurrentHashMap<>();

	public void onCacheDestroy(AbstractCache cache) {
		getOrCreate(cache.getClass()).add(cache);
	}

	public void registerHit(Class<?> cacheClass) {
		getOrCreate(cacheClass).hits.incrementAndGet();
	}

	public void registerWeakHit(Class<?> cacheClass) {
		getOrCreate(cacheClass).weakHits.incrementAndGet();
	}

	public void registerMiss(Class<?> cacheClass) {
		getOrCreate(cacheClass).misses.incrementAndGet();
	}

	public void registerPass(Class<?> cacheClass) {
		getOrCreate(cacheClass).passes.incrementAndGet();
	}

	public void registerNotAvailable(Class<?> cacheClass) {
		getOrCreate(cacheClass).notAvailable.incrementAndGet();
	}

	private CacheData getOrCreate(Class<?> cacheClass) {
		CacheData existingData = performanceMap.get(cacheClass.getName());
		if (existingData != null) {
			return existingData;
		} else {
			CacheData newData = new CacheData();
			performanceMap.put(cacheClass.getName(), newData);
			return newData;
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
}
