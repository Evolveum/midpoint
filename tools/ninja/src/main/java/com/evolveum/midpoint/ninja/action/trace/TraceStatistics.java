/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.trace;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

/**
 *
 */
public class TraceStatistics {

    private final Map<String, Info> operationsMap = new HashMap<>();

    private final PrismContext prismContext;
    private final boolean extra;

    private TraceStatistics(PrismContext prismContext, boolean extra) {
        this.prismContext = prismContext;
        this.extra = extra;
    }

    static TraceStatistics extra(TracingOutputType tracingOutput) {
        TraceStatistics traceStatistics = new TraceStatistics(tracingOutput.asPrismContainerValue().getPrismContext(), true);
        traceStatistics.update(tracingOutput.getResult());
        return traceStatistics;
    }

    static TraceStatistics simple(TracingOutputType tracingOutput) {
        TraceStatistics traceStatistics = new TraceStatistics(tracingOutput.asPrismContainerValue().getPrismContext(), false);
        traceStatistics.update(tracingOutput.getResult());
        return traceStatistics;
    }

    private void update(OperationResultType result) {
        operationsMap.compute(result.getOperation(),
                (op, info) -> Info.update(result, info, extra, prismContext));

        result.getPartialResults().forEach(this::update);
    }

    @SuppressWarnings("SameParameterValue")
    String dump(SortBy sortBy) {

        int maxOp = operationsMap.keySet().stream()
                .mapToInt(String::length)
                .max().orElse(0);

        String details = operationsMap.entrySet().stream()
                .sorted((e1, e2) -> compare(e1, e2, sortBy))
                .map(entry -> String.format("%-" + maxOp + "s %,10d %,15d %,10d", entry.getKey(),
                        entry.getValue().count, entry.getValue().size, entry.getValue().getAvgSize()))
                .collect(Collectors.joining("\n"));

        int nodesCount = operationsMap.values().stream()
                .mapToInt(info -> info.count)
                .sum();

        int totalSize = operationsMap.values().stream()
                .mapToInt(info -> info.size)
                .sum();

        return "Total nodes: " + nodesCount + "\n" +
                (extra ? "Total size: " + totalSize + "\n" : "") +
                "\nDetails:\n\n" + details;
    }

    private int compare(Map.Entry<String, Info> e1, Map.Entry<String, Info> e2, SortBy sortBy) {
        switch (sortBy) {
            case OPERATION:
                return e1.getKey().compareTo(e2.getKey());
            case COUNT:
                return Integer.compare(e2.getValue().count, e1.getValue().count);
            case SIZE:
                return Integer.compare(e2.getValue().size, e1.getValue().size);
            default:
                throw new AssertionError(sortBy);
        }
    }

    private static class Info {
        private int count;
        private int size;

        private static Info update(OperationResultType result, Info info, boolean extra, PrismContext prismContext) {
            if (info == null) {
                info = new Info();
            }
            info.count++;
            if (extra) {
                info.size += getSize(result, prismContext);
            }
            return info;
        }

        private static int getSize(OperationResultType result, PrismContext prismContext) {
            OperationResultType resultNoChildren = OperationResultUtil.shallowClone(result, false, true, true);
            try {
                return prismContext.xmlSerializer().serializeRealValue(resultNoChildren).length();
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        }

        public int getAvgSize() {
            return size / count;
        }
    }

    public enum SortBy {
        OPERATION, COUNT, SIZE
    }
}
