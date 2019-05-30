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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCachePerformanceInformationType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 *
 */
public class CachePerformanceInformationUtil {
	public static CachesPerformanceInformationType toCachesPerformanceInformationType(
			@NotNull Map<String, CachePerformanceCollector.CacheData> performanceMap) {
		CachesPerformanceInformationType rv = new CachesPerformanceInformationType();
		performanceMap.forEach((cache, info) -> rv.getCache().add(toSingleCachePerformanceInformationType(cache, info)));
		return rv;

	}

	private static SingleCachePerformanceInformationType toSingleCachePerformanceInformationType(String cache,
			CachePerformanceCollector.CacheData info) {
		SingleCachePerformanceInformationType rv = new SingleCachePerformanceInformationType();
		rv.setName(cache);
		rv.setHitCount(info.hits.intValue());
		rv.setWeakHitCount(info.weakHits.intValue());
		rv.setMissCount(info.misses.intValue());
		rv.setPassCount(info.passes.intValue());
		rv.setNotAvailableCount(info.notAvailable.intValue());
		return rv;
	}

	public static void addTo(@NotNull CachesPerformanceInformationType aggregate, @NotNull CachesPerformanceInformationType part) {
		for (SingleCachePerformanceInformationType partCacheInfo : part.getCache()) {
			SingleCachePerformanceInformationType matchingAggregateCacheInfo = null;
			for (SingleCachePerformanceInformationType aggregateCacheInfo : aggregate.getCache()) {
				if (Objects.equals(partCacheInfo.getName(), aggregateCacheInfo.getName())) {
					matchingAggregateCacheInfo = aggregateCacheInfo;
					break;
				}
			}
			if (matchingAggregateCacheInfo != null) {
				addTo(matchingAggregateCacheInfo, partCacheInfo);
			} else {
				aggregate.getCache().add(partCacheInfo.clone());
			}
		}
	}

	private static void addTo(@NotNull SingleCachePerformanceInformationType aggregate,
			@NotNull SingleCachePerformanceInformationType part) {
		aggregate.setHitCount(aggregate.getHitCount() + part.getHitCount());
		aggregate.setWeakHitCount(aggregate.getWeakHitCount() + part.getWeakHitCount());
		aggregate.setMissCount(aggregate.getMissCount() + part.getMissCount());
		aggregate.setPassCount(aggregate.getPassCount() + part.getPassCount());
		aggregate.setNotAvailableCount(aggregate.getNotAvailableCount() + part.getNotAvailableCount());
	}
}
