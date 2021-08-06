/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import ch.qos.logback.classic.Level;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityMonitoringDefinition;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationExecutionLogger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingRootType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Starts and stops tracing and [dynamic] profiling for item processing that is a part of activity execution.
 *
 * Note that tracing is not really *started* here. It is just requested to be started at defined tracing point(s).
 * But such a point is - by default - iterative task processing moment, which is right after the control
 * is returning back to {@link ItemProcessingGatekeeper}.
 *
 * This class was originally part of {@link ItemProcessingGatekeeper}, but was factored out to attain more clarity.
 */
class ItemProcessingMonitor<I> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemProcessingMonitor.class);

    /**
     * Task on which the tracing is requested.
     */
    @NotNull private final RunningTask workerTask;

    /**
     * Execution of the related activity. Used to determine e.g. whether the tracing/profiling interval applies.
     */
    @NotNull private final IterativeActivityExecution<I, ?, ?, ?, ?, ?> activityExecution;

    /**
     * Definition of the monitoring (e.g. intervals, profile, points, etc).
     */
    @NotNull private final ActivityMonitoringDefinition monitoringDefinition;

    /**
     * Used to turn on dynamic profiling: keeps the original logging level of the corresponding logger.
     */
    private Level originalProfilingLevel;

    ItemProcessingMonitor(ItemProcessingGatekeeper<I> itemProcessingGatekeeper) {
        this.workerTask = itemProcessingGatekeeper.getWorkerTask();
        this.activityExecution = itemProcessingGatekeeper.getActivityExecution();
        this.monitoringDefinition = activityExecution.getActivity().getDefinition().getMonitoringDefinition();
    }

    void startProfilingAndTracingIfNeeded() {
        if (activityExecution.isExcludedFromProfilingAndTracing()) {
            return;
        }

        int itemsProcessed = activityExecution.getItemsProcessed();
        startProfilingIfNeeded(itemsProcessed);
        startTracingIfNeeded(itemsProcessed);
    }

    void stopProfilingAndTracing() {
        stopProfiling();
        stopTracing();
    }

    private void startProfilingIfNeeded(int itemsProcessed) {
        int interval = monitoringDefinition.getDynamicProfilingInterval();
        if (!intervalMatches(interval, itemsProcessed)) {
            return;
        }
        LOGGER.info("Starting dynamic profiling for object number {} (interval is {})", itemsProcessed, interval);
        originalProfilingLevel = OperationExecutionLogger.getLocalOperationInvocationLevelOverride();
        OperationExecutionLogger.setLocalOperationInvocationLevelOverride(Level.TRACE);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean intervalMatches(int interval, int itemsProcessed) {
        return interval != 0 && itemsProcessed%interval == 0;
    }

    private void stopProfiling() {
        OperationExecutionLogger.setLocalOperationInvocationLevelOverride(originalProfilingLevel);
    }

    private void startTracingIfNeeded(int itemsProcessed) {
        int interval = monitoringDefinition.getTracingInterval();
        if (!intervalMatches(monitoringDefinition.getTracingInterval(), itemsProcessed)) {
            return;
        }

        LOGGER.info("Starting tracing for object number {} (interval is {})", itemsProcessed, interval);

        TracingProfileType configuredProfile = monitoringDefinition.getTracing().getTracingProfile();
        List<TracingRootType> configuredPoints = monitoringDefinition.getTracing().getTracingPoint();

        TracingProfileType profile = configuredProfile != null ?
                configuredProfile : activityExecution.getBeans().tracer.getDefaultProfile();
        Collection<TracingRootType> points = !configuredPoints.isEmpty() ?
                configuredPoints : List.of(TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING);

        points.forEach(workerTask::addTracingRequest);
        workerTask.setTracingProfile(profile);
    }

    private void stopTracing() {
        workerTask.removeTracingRequests();
        workerTask.setTracingProfile(null);
    }
}
