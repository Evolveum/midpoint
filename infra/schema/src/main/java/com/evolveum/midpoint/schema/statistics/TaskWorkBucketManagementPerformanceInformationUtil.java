/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketManagementOperationPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketManagementPerformanceInformationType;

public class TaskWorkBucketManagementPerformanceInformationUtil {

    public static String format(WorkBucketManagementPerformanceInformationType i) {
        return format(i, null, null, null);
    }

    public static String format(WorkBucketManagementPerformanceInformationType i, AbstractStatisticsPrinter.Options options,
            Integer iterations, Integer seconds) {
        return new TaskWorkBucketManagementPerformanceInformationPrinter(i, options, iterations, seconds)
                .print();
    }

    public static void addTo(@NotNull WorkBucketManagementPerformanceInformationType aggregate,
            @Nullable WorkBucketManagementPerformanceInformationType part) {
        if (part == null) {
            return;
        }

        for (WorkBucketManagementOperationPerformanceInformationType operation : part.getOperation()) {
            WorkBucketManagementOperationPerformanceInformationType matchingOperation =
                    findMatchingOperation(aggregate, operation);
            if (matchingOperation != null) {
                addTo(matchingOperation, operation);
            } else {
                aggregate.getOperation().add(operation.clone());
            }
        }
    }

    @Nullable
    private static WorkBucketManagementOperationPerformanceInformationType findMatchingOperation(
            @NotNull WorkBucketManagementPerformanceInformationType info,
            @NotNull WorkBucketManagementOperationPerformanceInformationType operation) {
        for (WorkBucketManagementOperationPerformanceInformationType existingOperation : info.getOperation()) {
            if (Objects.equals(existingOperation.getName(), operation.getName())) {
                return existingOperation;
            }
        }
        return null;
    }

    private static void addTo(@NotNull WorkBucketManagementOperationPerformanceInformationType aggregate,
            @NotNull WorkBucketManagementOperationPerformanceInformationType part) {
        aggregate.setCount(or0(aggregate.getCount()) + or0(part.getCount()));
        aggregate.setTotalTime(or0(aggregate.getTotalTime()) + or0(part.getTotalTime()));
        aggregate.setMinTime(min(aggregate.getMinTime(), part.getMinTime()));
        aggregate.setMaxTime(max(aggregate.getMaxTime(), part.getMaxTime()));
        aggregate.setConflictCount(or0(aggregate.getConflictCount()) + or0(part.getConflictCount()));
        aggregate.setTotalWastedTime(or0(aggregate.getTotalWastedTime()) + or0(part.getTotalWastedTime()));
        aggregate.setMinWastedTime(min(aggregate.getMinWastedTime(), part.getMinWastedTime()));
        aggregate.setMaxWastedTime(max(aggregate.getMaxWastedTime(), part.getMaxWastedTime()));
        aggregate.setBucketWaitCount(or0(aggregate.getBucketWaitCount()) + or0(part.getBucketWaitCount()));
        aggregate.setBucketsReclaimed(or0(aggregate.getBucketsReclaimed()) + or0(part.getBucketsReclaimed()));
        aggregate.setTotalWaitTime(or0(aggregate.getTotalWaitTime()) + or0(part.getTotalWaitTime()));
        aggregate.setMinWaitTime(min(aggregate.getMinWaitTime(), part.getMinWaitTime()));
        aggregate.setMaxWaitTime(max(aggregate.getMaxWaitTime(), part.getMaxWaitTime()));
    }
}
