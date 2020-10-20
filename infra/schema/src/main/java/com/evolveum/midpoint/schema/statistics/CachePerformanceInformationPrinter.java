/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCachePerformanceInformationType;

/**
 * Prints cache performance information.
 */
public class CachePerformanceInformationPrinter extends AbstractStatisticsPrinter<CachesPerformanceInformationType> {

    public CachePerformanceInformationPrinter(@NotNull CachesPerformanceInformationType information, Options options) {
        super(information, options, null, null);
    }

    public String print() {
        List<SingleCachePerformanceInformationType> caches = getSortedCaches();
        createData(caches);
        createFormatting();
        return applyFormatting();
    }

    @NotNull
    private List<SingleCachePerformanceInformationType> getSortedCaches() {
        List<SingleCachePerformanceInformationType> rows = new ArrayList<>(information.getCache());
        rows.sort(Comparator.comparing(SingleCachePerformanceInformationType::getName));
        return rows;
    }

    private void createData(List<SingleCachePerformanceInformationType> caches) {
        initData();
        for (SingleCachePerformanceInformationType cache : caches) {
            createRecord(cache);
        }
    }

    private void createRecord(SingleCachePerformanceInformationType c) {
        long hits = zeroIfNull(c.getHitCount());
        long weakHits = zeroIfNull(c.getWeakHitCount());
        long misses = zeroIfNull(c.getMissCount());
        long passes = zeroIfNull(c.getPassCount());
        long notAvailable = zeroIfNull(c.getNotAvailableCount());
        long sum = hits + weakHits + misses + passes + notAvailable;

        Data.Record record = data.createRecord();
        record.add(c.getName());
        record.add(hits);
        record.add(percent(hits, sum));
        record.add(weakHits);
        record.add(percent(weakHits, sum));
        record.add(misses);
        record.add(percent(misses, sum));
        record.add(passes);
        record.add(percent(passes, sum));
        record.add(notAvailable);
        record.add(percent(notAvailable, sum));
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Cache", LEFT, formatString());
        addColumn("Hits", RIGHT, formatInt());
        addColumn("Hits %", RIGHT, formatPercent1());
        addColumn("Weak hits", RIGHT, formatInt());
        addColumn("Weak %", RIGHT, formatPercent1());
        addColumn("Misses", RIGHT, formatInt());
        addColumn("Misses %", RIGHT, formatPercent1());
        addColumn("Passes", RIGHT, formatInt());
        addColumn("Passes %", RIGHT, formatPercent1());
        addColumn("Not available", RIGHT, formatInt());
        addColumn("N/A %", RIGHT, formatPercent1());
    }
}
