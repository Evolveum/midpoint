/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * State of the worker tasks (in the broad sense - i.e. tasks that do the real execution) in an activity:
 *
 * - how many workers are there in total,
 * - how many workers (of them) are executing,
 * - how many workers (of executing ones) have been stalled,
 * - on what cluster nodes,
 * - if stalled, then since when.
 */
public class ActivityWorkersInformation implements DebugDumpable, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityWorkersInformation.class);

    /**
     * How many workers and in which states are on individual nodes?
     */
    @NotNull private final Map<String, WorkerCounters> workersCountersPerNode = new HashMap<>();

    /** If stalled, then since when. To be set, all executing workers must be stalled; and the latest time is taken here. */
    @Nullable private XMLGregorianCalendar completelyStalledSince;

    static @NotNull ActivityWorkersInformation fromActivityStateOverview(
            @NotNull ActivityStateOverviewType stateOverview) {
        ActivityWorkersInformation workersInformation = new ActivityWorkersInformation();

        ActivityStateOverviewUtil.acceptStateOverviewVisitor(stateOverview, state -> {
            if (state.getRealizationState() == ActivitySimplifiedRealizationStateType.IN_PROGRESS) {
                workersInformation.updateWorkersCounters(state.getTask());
                workersInformation.updateCompletelyStalledSince(state.getTask());
            }
        });

        return workersInformation;
    }

    public static ActivityWorkersInformation empty() {
        return new ActivityWorkersInformation();
    }

    static ActivityWorkersInformation fromLegacyTask(@NotNull TaskType task) {
        ActivityWorkersInformation workersInformation = new ActivityWorkersInformation();
        workersInformation.updateFromLegacyTask(task);
        return workersInformation;
    }

    // Assuming no subtasks
    private void updateFromLegacyTask(@NotNull TaskType task) {
        if (task.getExecutionState() == TaskExecutionStateType.RUNNING) {
            updateWorkersCounters(task.getNode(),
                    true,
                    task.getStalledSince() != null);
            if (task.getStalledSince() != null) {
                completelyStalledSince = task.getStalledSince();
            }
        }
    }

    private void updateWorkersCounters(@NotNull List<ActivityTaskStateOverviewType> tasks) {
        for (ActivityTaskStateOverviewType task : tasks) {
            if (task.getNode() == null) {
                LOGGER.debug("No node information in: {}", task);
            }
            if (task.getExecutionState() == null) {
                LOGGER.debug("No execution state information in: {}", task);
            }
            updateWorkersCounters(task.getNode(),
                    task.getExecutionState() == ActivityTaskExecutionStateType.RUNNING,
                    task.getStalledSince() != null);
        }
    }

    private void updateWorkersCounters(@Nullable String node, boolean executing, boolean stalled) {
        workersCountersPerNode.compute(node,
                (key, counters) -> WorkerCounters.increment(counters, executing, stalled));
    }

    private void updateCompletelyStalledSince(List<ActivityTaskStateOverviewType> tasks) {
        for (ActivityTaskStateOverviewType task : tasks) {
            if (getWorkersStalled() < getWorkersExecuting() || task.getStalledSince() == null) {
                completelyStalledSince = null;
            } else {
                if (completelyStalledSince == null) {
                    completelyStalledSince = task.getStalledSince();
                } else {
                    completelyStalledSince =
                            XmlTypeConverter.createXMLGregorianCalendar(
                                    Math.max(
                                            XmlTypeConverter.toMillis(completelyStalledSince),
                                            XmlTypeConverter.toMillis(task.getStalledSince())));
                }
            }
        }
    }

    public int getWorkersCreated() {
        return workersCountersPerNode.values().stream()
                .mapToInt(v -> v.workersCreated)
                .sum();
    }

    public int getWorkersExecuting() {
        return workersCountersPerNode.values().stream()
                .mapToInt(v -> v.workersExecuting)
                .sum();
    }

    public int getWorkersStalled() {
        return workersCountersPerNode.values().stream()
                .mapToInt(v -> v.workersStalled)
                .sum();
    }

    public @Nullable XMLGregorianCalendar getCompletelyStalledSince() {
        return completelyStalledSince;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "workers map", workersCountersPerNode, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "stalled since", String.valueOf(completelyStalledSince), indent + 1);
        return sb.toString();
    }

    public @NotNull String toHumanReadableString() {
        return workersCountersPerNode.entrySet().stream()
                .filter(e -> e.getValue().workersExecuting > 0)
                .map(e -> e.getKey() + " (" + e.getValue().workersExecuting + ")")
                .collect(Collectors.joining(", "));
    }

    static class WorkerCounters implements Serializable {

        private final int workersCreated;
        private final int workersExecuting;
        private final int workersStalled;

        WorkerCounters(int workersCreated, int workersExecuting, int workersStalled) {
            this.workersCreated = workersCreated;
            this.workersExecuting = workersExecuting;
            this.workersStalled = workersStalled;
        }

        public static WorkerCounters increment(@Nullable ActivityWorkersInformation.WorkerCounters oldCounts, boolean executing, boolean stalled) {
            return new WorkerCounters(
                    (oldCounts != null ? oldCounts.workersCreated : 0) + 1,
                    (oldCounts != null ? oldCounts.workersExecuting : 0) + (executing ? 1 : 0),
                    (oldCounts != null ? oldCounts.workersStalled : 0) + (stalled ? 1 : 0));
        }

        @Override
        public String toString() {
            return "all workers: " + workersCreated +
                    ", executing: " + workersExecuting +
                    ", stalled: " + workersStalled +
                    '}';
        }
    }
}
