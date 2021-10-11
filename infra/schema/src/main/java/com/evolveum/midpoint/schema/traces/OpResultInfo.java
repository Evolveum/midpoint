/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;

@Experimental
public class OpResultInfo {
    private OperationResultType result;
    private OpType type;
    private OperationsPerformanceInformationType performance;
    private final Map<PerformanceCategory, PerformanceCategoryInfo> performanceByCategory = new HashMap<>();

    public OperationResultType getResult() {
        return result;
    }

    public void setResult(OperationResultType result) {
        this.result = result;
    }

    public OpType getType() {
        return type;
    }

    public OperationKindType getKind() {
        return result.getOperationKind();
    }

    public void setType(OpType type) {
        this.type = type;
    }

    public OperationsPerformanceInformationType getPerformance() {
        return performance;
    }

    public void setPerformance(OperationsPerformanceInformationType performance) {
        this.performance = performance;
    }

    public Map<PerformanceCategory, PerformanceCategoryInfo> getPerformanceByCategory() {
        return performanceByCategory;
    }

    public static OpResultInfo create(OperationResultType result, IdentityHashMap<OperationResultType,OpResultInfo> infoMap) {
        OpResultInfo info = new OpResultInfo();
        info.result = result;
        info.type = OpType.determine(result);
        info.performance = getPerformance(result);
        info.determinePerformanceByCategory(infoMap);

        infoMap.put(result, info);
        return info;
    }

    private void determinePerformanceByCategory(IdentityHashMap<OperationResultType,OpResultInfo> infoMap) {
        for (PerformanceCategory category : PerformanceCategory.values()) {
            if (!category.isDerived()) {
                PerformanceCategoryInfo info = determinePerformanceForCategory(category, infoMap);
                performanceByCategory.put(category, info);
            }
        }
        for (PerformanceCategory category : PerformanceCategory.values()) {
            if (category.isDerived()) {
                PerformanceCategoryInfo info = determinePerformanceForCategory(category, infoMap);
                performanceByCategory.put(category, info);
            }
        }
    }

    private PerformanceCategoryInfo determinePerformanceForCategory(PerformanceCategory category,
            IdentityHashMap<OperationResultType, OpResultInfo> infoMap) {
        PerformanceCategoryInfo rv = new PerformanceCategoryInfo();
        int count = 0;
        long time = 0;
        if (category.isDerived()) {
            for (PerformanceCategory plus : category.getPlus()) {
                count += performanceByCategory.get(plus).getOwnCount();
                time += performanceByCategory.get(plus).getOwnTime();
            }
            for (PerformanceCategory minus : category.getMinus()) {
                count -= performanceByCategory.get(minus).getOwnCount();
                time -= performanceByCategory.get(minus).getOwnTime();
            }
        } else {
            boolean matches = category.matches(result);
            if (matches) {
                count = 1;
                time = result.getMicroseconds() != null ? result.getMicroseconds() : 0;
            }
        }
        rv.setOwnCount(count);
        rv.setOwnTime(time);
        rv.setTotalCount(count);
        rv.setTotalTime(time);
        for (OperationResultType subresult : result.getPartialResults()) {
            OpResultInfo subInfo = getInfo(subresult, infoMap);
            PerformanceCategoryInfo subPerf = subInfo.getPerformanceFor(category);
            rv.setTotalCount(rv.getTotalCount() + subPerf.getTotalCount());
            rv.setTotalTime(rv.getTotalTime() + subPerf.getTotalTime());
        }
        return rv;
    }

    private OpResultInfo getInfo(OperationResultType result, IdentityHashMap<OperationResultType, OpResultInfo> infoMap) {
        OpResultInfo info = infoMap.get(result);
        if (info != null) {
            return info;
        } else {
            return create(result, infoMap);
        }
    }

    private PerformanceCategoryInfo getPerformanceFor(PerformanceCategory category) {
        return performanceByCategory.get(category);

    }
    private static OperationsPerformanceInformationType getPerformance(OperationResultType result) {
        OperationsPerformanceInformationType rv = new OperationsPerformanceInformationType();
        addPerformance(rv, result);
        return rv;
    }

    private static void addPerformance(OperationsPerformanceInformationType rv, OperationResultType result) {
        if (result.getMicroseconds() != null) {
            OperationsPerformanceInformationType oper = new OperationsPerformanceInformationType();
            oper.beginOperation()
                    .name(result.getOperation())
                    .invocationCount(1)
                    .totalTime(result.getMicroseconds())
                    .minTime(result.getMicroseconds())
                    .maxTime(result.getMicroseconds());
            OperationsPerformanceInformationUtil.addTo(rv, oper);
        }
        for (OperationResultType child : result.getPartialResults()) {
            addPerformance(rv, child);
        }
    }


}
