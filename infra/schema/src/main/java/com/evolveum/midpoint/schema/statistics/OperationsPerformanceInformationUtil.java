/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.statistics.SingleOperationPerformanceInformation;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleOperationPerformanceInformationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.util.MiscUtil.or0;

/**
 *
 */
public class OperationsPerformanceInformationUtil {

    public static OperationsPerformanceInformationType toOperationsPerformanceInformationType(
            @NotNull OperationsPerformanceInformation methodsInfo) {
        OperationsPerformanceInformationType rv = new OperationsPerformanceInformationType();
        methodsInfo.getAllData().forEach((cache, info) -> rv.getOperation().add(toSingleMethodPerformanceInformationType(cache, info)));
        return rv;
    }

    private static SingleOperationPerformanceInformationType toSingleMethodPerformanceInformationType(String method,
            SingleOperationPerformanceInformation info) {
        SingleOperationPerformanceInformationType rv = new SingleOperationPerformanceInformationType();
        rv.setName(method);
        rv.setInvocationCount(info.getInvocationCount());
        rv.setTotalTime(info.getTotalTime());
        rv.setMinTime(info.getMinTime());
        rv.setMaxTime(info.getMaxTime());
        rv.setOwnTime(info.getOwnTime());
        rv.setMinOwnTime(info.getMinOwnTime());
        rv.setMaxOwnTime(info.getMaxOwnTime());
        return rv;
    }

    public static void addTo(@NotNull OperationsPerformanceInformationType aggregate, @Nullable OperationsPerformanceInformationType part) {
        if (part == null) {
            return;
        }
        for (SingleOperationPerformanceInformationType partMethodInfo : part.getOperation()) {
            SingleOperationPerformanceInformationType matchingAggregateCacheInfo = null;
            for (SingleOperationPerformanceInformationType aggregateMethodInfo : aggregate.getOperation()) {
                if (Objects.equals(partMethodInfo.getName(), aggregateMethodInfo.getName())) {
                    matchingAggregateCacheInfo = aggregateMethodInfo;
                    break;
                }
            }
            if (matchingAggregateCacheInfo != null) {
                addTo(matchingAggregateCacheInfo, partMethodInfo);
            } else {
                aggregate.getOperation().add(partMethodInfo.clone());
            }
        }
    }

    private static void addTo(@NotNull SingleOperationPerformanceInformationType aggregate,
            @NotNull SingleOperationPerformanceInformationType part) {
        aggregate.setInvocationCount(aggregate.getInvocationCount() + part.getInvocationCount());
        aggregate.setTotalTime(aggregate.getTotalTime() + part.getTotalTime());
        aggregate.setMinTime(min(aggregate.getMinTime(), part.getMinTime()));
        aggregate.setMaxTime(max(aggregate.getMaxTime(), part.getMaxTime()));
        var partOwnTime = part.getOwnTime();
        if (partOwnTime != null) {
            aggregate.setOwnTime(or0(aggregate.getOwnTime()) + partOwnTime);
        }
        aggregate.setMinOwnTime(min(aggregate.getMinOwnTime(), part.getMinOwnTime()));
        aggregate.setMaxOwnTime(max(aggregate.getMaxOwnTime(), part.getMaxOwnTime()));
    }

    private static Long min(Long a, Long b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else return Math.min(a, b);
    }

    private static Long max(Long a, Long b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else return Math.max(a, b);
    }

    public static String format(OperationsPerformanceInformationType i) {
        return format(i, null, null, null);
    }

    public static String format(OperationsPerformanceInformationType i, AbstractStatisticsPrinter.Options options,
            Integer iterations, Integer seconds) {
        StringBuilder sb = new StringBuilder();

        String viaOpResult = new OperationsPerformanceInformationPrinter(i, options, iterations, seconds, false).print(false);
        String viaAspect = new OperationsPerformanceInformationPrinter(i, options, iterations, seconds, true).print(true);

        sb.append(viaOpResult);
        if (viaAspect != null) {
            sb.append("\nObtained using method interceptor:\n\n");
            sb.append(viaAspect);
        }
        return sb.toString();
    }
}
