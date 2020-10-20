/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketManagementOperationPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketManagementPerformanceInformationType;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

/**
 * Prints work buckets management performance information.
 */
public class TaskWorkBucketManagementPerformanceInformationPrinter extends AbstractStatisticsPrinter<WorkBucketManagementPerformanceInformationType> {

    public TaskWorkBucketManagementPerformanceInformationPrinter(@NotNull WorkBucketManagementPerformanceInformationType information,
            Options options, Integer iterations, Integer seconds) {
        super(information, options, iterations, seconds);
    }

    public String print() {
        List<WorkBucketManagementOperationPerformanceInformationType> operations = getSortedOperations();
        createData(operations);
        createFormatting();
        return applyFormatting();
    }

    @NotNull
    private List<WorkBucketManagementOperationPerformanceInformationType> getSortedOperations() {
        List<WorkBucketManagementOperationPerformanceInformationType> operations = new ArrayList<>(information.getOperation());
        operations.sort(Comparator.comparing(WorkBucketManagementOperationPerformanceInformationType::getName));
        return operations;
    }

    private void createData(List<WorkBucketManagementOperationPerformanceInformationType> operations) {
        initData();
        for (WorkBucketManagementOperationPerformanceInformationType op : operations) {
            createRecord(op);
        }
    }

    private void createRecord(WorkBucketManagementOperationPerformanceInformationType op) {
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
