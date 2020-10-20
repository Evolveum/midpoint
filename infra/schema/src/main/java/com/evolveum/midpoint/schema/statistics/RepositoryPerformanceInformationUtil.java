/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.Options;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryOperationPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryPerformanceInformationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 *
 */
public class RepositoryPerformanceInformationUtil {

    public static void addTo(@NotNull RepositoryPerformanceInformationType aggregate, @Nullable RepositoryPerformanceInformationType part) {
        if (part == null) {
            return;
        }
        for (RepositoryOperationPerformanceInformationType partOperation : part.getOperation()) {
            RepositoryOperationPerformanceInformationType matchingAggregateOperation = null;
            for (RepositoryOperationPerformanceInformationType aggregateOperation : aggregate.getOperation()) {
                if (Objects.equals(partOperation.getName(), aggregateOperation.getName())) {
                    matchingAggregateOperation = aggregateOperation;
                    break;
                }
            }
            if (matchingAggregateOperation != null) {
                addTo(matchingAggregateOperation, partOperation);
            } else {
                aggregate.getOperation().add(partOperation.clone());
            }
        }
    }

    private static void addTo(@NotNull RepositoryOperationPerformanceInformationType aggregate,
            @NotNull RepositoryOperationPerformanceInformationType part) {
        aggregate.setInvocationCount(aggregate.getInvocationCount() + part.getInvocationCount());
        aggregate.setExecutionCount(aggregate.getExecutionCount() + part.getExecutionCount());
        aggregate.setTotalTime(aggregate.getTotalTime() + part.getTotalTime());
        aggregate.setMinTime(min(aggregate.getMinTime(), part.getMinTime()));
        aggregate.setMaxTime(max(aggregate.getMaxTime(), part.getMaxTime()));
        aggregate.setTotalWastedTime(aggregate.getTotalWastedTime() + part.getTotalWastedTime());
        aggregate.setMinWastedTime(min(aggregate.getMinWastedTime(), part.getMinWastedTime()));
        aggregate.setMaxWastedTime(max(aggregate.getMaxWastedTime(), part.getMaxWastedTime()));
    }

    private static Long min(Long a, Long b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return Math.min(a, b);
        }
    }

    private static Long max(Long a, Long b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return Math.max(a, b);
        }
    }

    public static String format(RepositoryPerformanceInformationType i) {
        return new RepositoryPerformanceInformationPrinter(i, null, null, null).print();
    }

    public static String format(RepositoryPerformanceInformationType i, Options options, Integer iterations, Integer seconds) {
        return new RepositoryPerformanceInformationPrinter(i, options, iterations, seconds)
                .print();
    }
}
