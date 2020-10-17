/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleOperationPerformanceInformationType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Prints operations performance information.
 */
public class OperationsPerformanceInformationPrinter extends AbstractStatisticsPrinter<OperationsPerformanceInformationType> {

    private final boolean viaAspect;

    public OperationsPerformanceInformationPrinter(@NotNull OperationsPerformanceInformationType information,
            Options options, Integer iterations, Integer seconds, boolean viaAspect) {
        super(information, options, iterations, seconds);
        this.viaAspect = viaAspect;
    }

    /**
     * @param nullIfNone If there are no data, returns empty string.
     */
    public String print(boolean nullIfNone) {
        List<SingleOperationPerformanceInformationType> operations = getSortedOperations();
        if (operations.isEmpty() && nullIfNone) {
            return null;
        } else {
            createData(operations);
            createFormatting();
            return applyFormatting();
        }
    }

    @NotNull
    private List<SingleOperationPerformanceInformationType> getSortedOperations() {
        return information.getOperation().stream()
                .filter(e -> e.getName().endsWith("#") == viaAspect)
                .sorted(getComparator())
                .collect(Collectors.toList());
    }

    private Comparator<SingleOperationPerformanceInformationType> getComparator() {
        if (options.sortBy == SortBy.COUNT) {
            return Comparator.comparing(SingleOperationPerformanceInformationType::getInvocationCount).reversed();
        } else if (options.sortBy == SortBy.TIME) {
            return Comparator.comparing(SingleOperationPerformanceInformationType::getTotalTime).reversed();
        } else {
            return Comparator.comparing(SingleOperationPerformanceInformationType::getName);
        }
    }

    private void createData(List<SingleOperationPerformanceInformationType> operations) {
        initData();
        for (SingleOperationPerformanceInformationType operation : operations) {
            createRecord(operation);
        }
    }

    private void createRecord(SingleOperationPerformanceInformationType op) {
        String name = StringUtils.stripEnd(op.getName(), "#");
        int count = zeroIfNull(op.getInvocationCount());
        float totalTime = zeroIfNull(op.getTotalTime()) / 1000.0f;
        float minTime = zeroIfNull(op.getMinTime()) / 1000.0f;
        float maxTime = zeroIfNull(op.getMaxTime()) / 1000.0f;

        Data.Record record = data.createRecord();
        record.add(name);
        record.add(count);
        if (iterations != null) {
            record.add(avg(count, iterations));
        }
        if (seconds != null) {
            record.add(avg(count, seconds));
        }
        record.add(totalTime);
        record.add(minTime);
        record.add(maxTime);
        record.add(avg(totalTime, count));
        if (iterations != null) {
            record.add(avg(totalTime, iterations));
        }
    }

    @SuppressWarnings("DuplicatedCode")
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
        addColumn("Total time (ms)", RIGHT, formatFloat1());
        addColumn("Min", RIGHT, formatFloat1());
        addColumn("Max", RIGHT, formatFloat1());
        addColumn("Avg", RIGHT, formatFloat1());
        if (iterations != null) {
            addColumn("Time/iter", RIGHT, formatFloat1());
        }
    }
}
