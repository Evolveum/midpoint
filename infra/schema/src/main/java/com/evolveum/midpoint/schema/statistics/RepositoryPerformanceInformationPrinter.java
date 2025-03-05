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

import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryOperationPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryPerformanceInformationType;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

/**
 * Formats sql (repo/audit) performance information.
 */
public class RepositoryPerformanceInformationPrinter extends AbstractStatisticsPrinter<RepositoryPerformanceInformationType> {

    public RepositoryPerformanceInformationPrinter(@NotNull RepositoryPerformanceInformationType information,
            Options options, Integer iterations, Integer seconds) {
        super(information, options, iterations, seconds);
    }

    @Override
    public void prepare() {
        List<RepositoryOperationPerformanceInformationType> operations = getSortedOperations();
        createData(operations);
        createFormatting();
    }

    @NotNull
    private List<RepositoryOperationPerformanceInformationType> getSortedOperations() {
        List<RepositoryOperationPerformanceInformationType> operations = new ArrayList<>(information.getOperation());
        operations.sort(getComparator());
        return operations;
    }

    private Comparator<RepositoryOperationPerformanceInformationType> getComparator() {
        return switch (options.sortBy) {
            case COUNT -> Comparator.comparing(RepositoryOperationPerformanceInformationType::getInvocationCount).reversed();
            case TIME, OWN_TIME -> Comparator.comparing(RepositoryOperationPerformanceInformationType::getTotalTime).reversed();
            default -> Comparator.comparing(RepositoryOperationPerformanceInformationType::getName);
        };
    }

    private void createData(List<RepositoryOperationPerformanceInformationType> operations) {
        initData();
        for (RepositoryOperationPerformanceInformationType op : operations) {
            createRecord(op);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void createRecord(RepositoryOperationPerformanceInformationType op) {
        int invocationCount = zeroIfNull(op.getInvocationCount());
        int executionCount = zeroIfNull(op.getExecutionCount());
        long totalTime = zeroIfNull(op.getTotalTime());
        int retriesCount = executionCount - invocationCount;
        boolean hasRetries = retriesCount > 0;

        Data.Record record = data.createRecord();
        record.add(op.getName());
        record.add(invocationCount);
        if (iterations != null) {
            record.add(avg(invocationCount, iterations));
        }
        if (seconds != null) {
            record.add(avg(invocationCount, seconds));
        }
        record.add(totalTime);
        record.add(op.getMinTime());
        record.add(op.getMaxTime());
        record.add(avg(totalTime, invocationCount));
        if (iterations != null) {
            record.add(avg(totalTime, iterations));
        }
        if (seconds != null) {
            record.add(avg(totalTime, seconds));
        }

        record.add(nullIfFalse(hasRetries, retriesCount));
        record.add(nullIfFalse(hasRetries, op.getTotalWastedTime()));
        record.add(nullIfFalse(hasRetries, op.getMinWastedTime()));
        record.add(nullIfFalse(hasRetries, op.getMaxWastedTime()));
        record.add(nullIfFalse(hasRetries, avg(op.getTotalWastedTime(), invocationCount)));
        record.add(nullIfFalse(hasRetries, percent(op.getTotalWastedTime(), totalTime)));
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Operation", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
        if (iterations != null) {
            addColumn("Count/iter", RIGHT, formatFloat1());
        }
        if (seconds != null) {
            addColumn("Count/sec", RIGHT, formatFloat1());
        }
        addColumn("Total time (ms)", RIGHT, formatInt());
        addColumn("Min", RIGHT, formatInt());
        addColumn("Max", RIGHT, formatInt());
        addColumn("Avg", RIGHT, formatFloat1());
        if (iterations != null) {
            addColumn("Time/iter", RIGHT, formatFloat1());
        }
        if (seconds != null) {
            addColumn("Time/sec", RIGHT, formatFloat1());
        }
        addColumn("Retries", RIGHT, formatInt());
        addColumn("Wasted time (ms)", RIGHT, formatInt());
        addColumn("Min", RIGHT, formatInt());
        addColumn("Max", RIGHT, formatInt());
        addColumn("Avg", RIGHT, formatFloat1());
        addColumn("Wasted %", RIGHT, formatPercent2());
    }
}
