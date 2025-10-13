/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BucketManagementOperationStatisticsType;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketManagementStatisticsType;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

/**
 * Prints work buckets management performance information.
 */
public class TaskWorkBucketManagementPerformanceInformationPrinter extends AbstractStatisticsPrinter<ActivityBucketManagementStatisticsType> {

    TaskWorkBucketManagementPerformanceInformationPrinter(@NotNull ActivityBucketManagementStatisticsType information,
            Options options, Integer iterations, Integer seconds) {
        super(information, options, iterations, seconds);
    }

    @Override
    public void prepare() {
        List<BucketManagementOperationStatisticsType> operations = getSortedOperations();
        createData(operations);
        createFormatting();
    }

    @NotNull
    private List<BucketManagementOperationStatisticsType> getSortedOperations() {
        List<BucketManagementOperationStatisticsType> operations = new ArrayList<>(information.getOperation());
        operations.sort(Comparator.comparing(BucketManagementOperationStatisticsType::getName));
        return operations;
    }

    private void createData(List<BucketManagementOperationStatisticsType> operations) {
        initData();
        for (BucketManagementOperationStatisticsType op : operations) {
            createRecord(op);
        }
    }

    private void createRecord(BucketManagementOperationStatisticsType op) {
        long totalTime = zeroIfNull(op.getTotalTime());
        long totalWastedTime = zeroIfNull(op.getTotalWastedTime());
        long totalWaitTime = zeroIfNull(op.getTotalWaitTime());
        int count = zeroIfNull(op.getCount());
        int conflictCount = zeroIfNull(op.getConflictCount());
        int waitCount = zeroIfNull(op.getBucketWaitCount());
        boolean hasConflicts = conflictCount > 0;
        boolean hasWaits = waitCount > 0;

        Data.Record record = data.createRecord();
        record.add(op.getName());
        record.add(count);
        record.add(totalTime);
        record.add(op.getMinTime());
        record.add(op.getMaxTime());
        record.add(avg(totalTime, count));

        record.add(nullIfFalse(hasConflicts, conflictCount));
        record.add(nullIfFalse(hasConflicts, totalWastedTime));
        record.add(nullIfFalse(hasConflicts, op.getMinWastedTime()));
        record.add(nullIfFalse(hasConflicts, op.getMaxWastedTime()));
        record.add(nullIfFalse(hasConflicts, avg(op.getTotalWastedTime(), count)));
        record.add(nullIfFalse(hasConflicts, percent(totalWastedTime, totalTime)));

        record.add(nullIfFalse(hasWaits, waitCount));
        record.add(nullIfFalse(hasWaits, totalWaitTime));
        record.add(nullIfFalse(hasWaits, op.getMinWaitTime()));
        record.add(nullIfFalse(hasWaits, op.getMaxWaitTime()));
        record.add(nullIfFalse(hasWaits, avg(op.getTotalWaitTime(), count)));
        record.add(nullIfFalse(hasWaits, percent(totalWaitTime, totalTime)));
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Operation", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
        addColumn("Total time (ms)", RIGHT, formatInt());
        addColumn("Min", RIGHT, formatInt());
        addColumn("Max", RIGHT, formatInt());
        addColumn("Avg", RIGHT, formatFloat1());
        addColumn("Conflicts", RIGHT, formatInt());
        addColumn("Wasted time (ms)", RIGHT, formatInt());
        addColumn("Min", RIGHT, formatInt());
        addColumn("Max", RIGHT, formatInt());
        addColumn("Avg", RIGHT, formatFloat1());
        addColumn("%", RIGHT, formatPercent2());
        addColumn("Wait count", RIGHT, formatInt());
        addColumn("Waited time (ms)", RIGHT, formatInt());
        addColumn("Min", RIGHT, formatInt());
        addColumn("Max", RIGHT, formatInt());
        addColumn("Avg", RIGHT, formatFloat1());
        addColumn("%", RIGHT, formatPercent2());
    }
}
