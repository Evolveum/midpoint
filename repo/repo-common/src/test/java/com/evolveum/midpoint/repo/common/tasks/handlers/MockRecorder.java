/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static org.apache.commons.collections4.ListUtils.synchronizedList;

@Component
public class MockRecorder implements DebugDumpable {

    @SuppressWarnings("unchecked")
    private final List<String> executions = synchronizedList(new ArrayList<>());

    private final List<OperationResultSnapshot> operationResultSnapshots = synchronizedList(new ArrayList<>());

    /** This is to verify that realization start timestamps in workers are the same. */
    @NotNull private final Set<XMLGregorianCalendar> realizationStartTimestamps = new HashSet<>();

    public void recordExecution(String value) {
        executions.add(value);
    }

    public List<String> getExecutions() {
        return executions;
    }

    /** Captures concrete result count and summarized hidden-record count for the given operation. */
    public void recordOperationResultSnapshot(OperationResult parentResult, String operation) {
        List<OperationResult> matching = parentResult.findSubresults(operation);
        operationResultSnapshots.add(
                new OperationResultSnapshot(
                        (int) matching.stream()
                                .filter(result -> result.getHiddenRecordsCount() == 0)
                                .count(),
                        matching.stream()
                                .mapToInt(OperationResult::getHiddenRecordsCount)
                                .sum()));
    }

    public List<OperationResultSnapshot> getOperationResultSnapshots() {
        return operationResultSnapshots;
    }

    public record OperationResultSnapshot(int concreteRecords, int hiddenRecords) {}

    public void recordRealizationStartTimestamp(XMLGregorianCalendar value) {
        realizationStartTimestamps.add(value);
    }

    public @NotNull Collection<XMLGregorianCalendar> getRealizationStartTimestamps() {
        return realizationStartTimestamps;
    }

    public void reset() {
        executions.clear();
        operationResultSnapshots.clear();
        realizationStartTimestamps.clear();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "MockRecorder", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "executions", executions, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "operation result snapshots", operationResultSnapshots, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "realization start timestamps", realizationStartTimestamps, indent + 1);
        return sb.toString();
    }
}
