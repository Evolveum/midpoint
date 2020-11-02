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
        boolean matches = category.matches(result);
        int ownCount = matches ? 1 : 0;
        long ownTime = matches && result.getMicroseconds() != null ? result.getMicroseconds() : 0;
        rv.setOwnCount(ownCount);
        rv.setOwnTime(ownTime);
        if (ownCount > 0) {
            // We do not want to resort to children regarding this category
            rv.setTotalCount(ownCount);
            rv.setTotalTime(ownTime);
        } else {
            int totalCount = 0;
            long totalTime = 0;
            for (OperationResultType subresult : result.getPartialResults()) {
                OpResultInfo subInfo = getInfo(subresult, infoMap);
                PerformanceCategoryInfo subPerf = subInfo.getPerformanceFor(category);
                totalCount += subPerf.getTotalCount();
                totalTime += subPerf.getTotalTime();
            }
            rv.setTotalCount(totalCount);
            rv.setTotalTime(totalTime);
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
