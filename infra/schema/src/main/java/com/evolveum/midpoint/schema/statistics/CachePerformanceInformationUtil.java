/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.schema.cache.CachePerformanceCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCachePerformanceInformationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.schema.cache.CachePerformanceCollector.isExtra;

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

    public static void addTo(@NotNull CachesPerformanceInformationType aggregate, @Nullable CachesPerformanceInformationType part) {
        if (part == null) {
            return;
        }
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

    public static String format(CachesPerformanceInformationType information) {
        return format(information, null);
    }

    public static String format(CachesPerformanceInformationType information, AbstractStatisticsPrinter.Options options) {
        return new CachePerformanceInformationPrinter(information, options)
                .print();
    }

    public static String format(Map<String, CachePerformanceCollector.CacheData> performanceMap) {
        return performanceMap != null ? format(toCachesPerformanceInformationType(performanceMap)) : "";
    }

    public static String formatExtra(Map<String, CachePerformanceCollector.CacheData> performanceMap) {
        StringBuilder sb = new StringBuilder();
        performanceMap.entrySet().stream()
                .filter(entry -> isExtra(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sb.append(String.format("%-30s oversized: %10d stale: %10d", entry.getKey()+":", entry.getValue().overSizedQueries.get(),
                        entry.getValue().skippedStaleData.get())));
        return sb.toString();
    }
}
