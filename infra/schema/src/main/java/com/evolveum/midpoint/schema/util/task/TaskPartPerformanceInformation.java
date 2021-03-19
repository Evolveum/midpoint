/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskPartItemsProcessingInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StructuredTaskProgressType;

import javax.xml.datatype.XMLGregorianCalendar;

import static java.util.Collections.emptyList;

/**
 * Extract of the most relevant performance information about a task part.
 */
public class TaskPartPerformanceInformation {

    private final int itemsProcessed;
    private final int errors;
    private final int progress;
    private final double processingTime;
    private final Long wallClockTime;
    private final XMLGregorianCalendar earliestStartTime;

    private TaskPartPerformanceInformation(int itemsProcessed, int errors, int progress, double processingTime,
            Long wallClockTime, XMLGregorianCalendar earliestStartTime) {
        this.itemsProcessed = itemsProcessed;
        this.errors = errors;
        this.progress = progress;
        this.processingTime = processingTime;
        this.wallClockTime = wallClockTime;
        this.earliestStartTime = earliestStartTime;
    }

    public static TaskPartPerformanceInformation forCurrentPart(OperationStatsType operationStats,
            StructuredTaskProgressType structuredProgress) {

        IterativeTaskPartItemsProcessingInformationType info = TaskOperationStatsUtil
                .getIterativeInfoForCurrentPart(operationStats, structuredProgress);

        int itemsProcessed = TaskOperationStatsUtil.getItemsProcessed(info);
        int errors = TaskOperationStatsUtil.getErrors(info);
        int progress = TaskProgressUtil.getTotalProgressForCurrentPart(structuredProgress);
        double processingTime = TaskOperationStatsUtil.getProcessingTime(info);

        WallClockTimeComputer wallClockTimeComputer =
                new WallClockTimeComputer(info != null ? info.getExecution() : emptyList());

        long wallClockTime = wallClockTimeComputer.getSummaryTime();
        XMLGregorianCalendar earliestStartTime = wallClockTimeComputer.getEarliestStartTime();

        return new TaskPartPerformanceInformation(itemsProcessed, errors, progress, processingTime,
                wallClockTime, earliestStartTime);
    }

    public int getItemsProcessed() {
        return itemsProcessed;
    }

    public int getProgress() {
        return progress;
    }

    public Long getWallClockTime() {
        return wallClockTime;
    }

    public int getErrors() {
        return errors;
    }

    public double getProcessingTime() {
        return processingTime;
    }

    public Double getAverageTime() {
        if (itemsProcessed > 0) {
            return processingTime / itemsProcessed;
        } else {
            return null;
        }
    }

    public Double getAverageWallClockTime() {
        if (wallClockTime != null && itemsProcessed > 0) {
            return (double) wallClockTime / itemsProcessed;
        } else {
            return null;
        }
    }

    public Double getThroughput() {
        Double averageWallClockTime = getAverageWallClockTime();
        if (averageWallClockTime != null) {
            return 60000 / averageWallClockTime;
        } else {
            return null;
        }
    }

    public XMLGregorianCalendar getEarliestStartTime() {
        return earliestStartTime;
    }
}
