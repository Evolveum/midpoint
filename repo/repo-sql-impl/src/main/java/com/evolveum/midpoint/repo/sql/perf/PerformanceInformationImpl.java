/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.perf;

import com.evolveum.midpoint.repo.api.perf.OperationPerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.OperationRecord;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryPerformanceInformationType;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class PerformanceInformationImpl implements PerformanceInformation {

    // operation kind -> performance information
    private final Map<String, OperationPerformanceInformation> operationMap = new ConcurrentHashMap<>();

    public void clear() {
        operationMap.clear();
    }

    @Override
    public Map<String, OperationPerformanceInformation> getAllData() {
        return operationMap;
    }

    @Override
    public RepositoryPerformanceInformationType toRepositoryPerformanceInformationType() {
        RepositoryPerformanceInformationType rv = new RepositoryPerformanceInformationType();
        operationMap.forEach((kind, info) -> rv.getOperation().add(info.toRepositoryOperationPerformanceInformationType(kind)));
        return rv;
    }

    public void register(OperationRecord operation, boolean alsoObjectType) {
        // operationMap.compute is also atomic, but always replaces new value (even if the reference did not change)
        // so I think this is more efficient, even if it creates empty object each time
        String key = operation.getKind();
        if (alsoObjectType) {
            key += "." + operation.getObjectTypeName();
        }
        operationMap.putIfAbsent(key, new OperationPerformanceInformation());
        operationMap.get(key).register(operation);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "Repository performance information", indent);
        ArrayList<String> operations = new ArrayList<>(operationMap.keySet());
        operations.sort(String::compareTo);
        for (String operation : operations) {
            OperationPerformanceInformation info = operationMap.get(operation);
            if (info != null) {
                DebugUtil.debugDumpWithLabelLn(sb, operation, info.shortDump(), indent+1);
            }
        }
        return sb.toString();
    }

    @Override
    public int getInvocationCount(String operation) {
        OperationPerformanceInformation info = operationMap.get(operation);
        if (info != null) {
            return info.getInvocationCount();
        } else if (operation.contains(".")) {
            return 0;       // we expect only single level of aggregation
        } else {
            String prefix = operation + ".";
            int rv = 0;
            for (Map.Entry<String, OperationPerformanceInformation> entry : operationMap.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    rv += entry.getValue().getInvocationCount();
                }
            }
            return rv;
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public PerformanceInformation clone() {
        PerformanceInformationImpl clone = new PerformanceInformationImpl();
        operationMap.forEach((op, opPerfInfo) -> clone.operationMap.put(op, opPerfInfo.clone()));
        return clone;
    }

    /**
     * Merges an increment to this information.
     * BEWARE: Assumes distinct sets of operation (does not check for overlaps).
     */
    public void mergeDistinct(PerformanceInformationImpl increment) {
        if (increment != null) {
            operationMap.putAll(increment.getAllData());
        }
    }
}
