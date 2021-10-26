/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import java.util.Collection;
import java.util.List;

import ch.qos.logback.classic.Level;

import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingConditionEvaluator.AdditionalVariableProvider;
import com.evolveum.midpoint.task.api.Tracer;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityReportingDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationExecutionLogger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessProfilingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessTracingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingRootType;

import org.jetbrains.annotations.Nullable;

/**
 * Starts and stops tracing and [dynamic] profiling for item processing that is a part of activity run.
 *
 * Note that tracing is not really *started* here. It is just requested to be started at defined tracing point(s).
 * But such a point is - by default - iterative task processing moment, which is right after the control
 * is returning back to {@link ItemProcessingGatekeeper}.
 *
 * This class was originally part of {@link ItemProcessingGatekeeper}, but was factored out to attain more clarity.
 *
 * TODO extend to all reporting (i.e. also reports)?
 */
class ItemProcessingMonitor<I> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemProcessingMonitor.class);

    /** Task on which the tracing is requested. */
    @NotNull private final RunningTask workerTask;

    /** Run of the related activity. */
    @NotNull private final IterativeActivityRun<I, ?, ?, ?> activityRun;

    /** Definition of the monitoring (e.g. intervals, profile, points, etc). */
    @NotNull private final ActivityReportingDefinition reportingDefinition;

    /** Helper for before/after conditions evaluation. */
    @NotNull private final ItemProcessingConditionEvaluator conditionEvaluator;

    /** Used to turn on dynamic profiling: keeps the original logging level of the corresponding logger. */
    private Level originalProfilingLevel;

    /** Tracing configuration that was selected and applied (if any). */
    @Nullable private ProcessTracingConfigurationType tracingConfigurationUsed;

    ItemProcessingMonitor(ItemProcessingGatekeeper<I> itemProcessingGatekeeper) {
        this.workerTask = itemProcessingGatekeeper.getWorkerTask();
        this.activityRun = itemProcessingGatekeeper.getActivityRun();
        this.reportingDefinition = activityRun.getActivity().getDefinition().getReportingDefinition();
        this.conditionEvaluator = itemProcessingGatekeeper.conditionEvaluator;
    }

    void startProfilingAndTracingIfNeeded(OperationResult result) {
        startProfilingIfNeeded(result);
        startTracingIfNeeded(result);
    }

    void stopProfilingAndTracing() {
        stopProfiling();
        stopTracing();
    }

    private void startProfilingIfNeeded(OperationResult result) {
        ProcessProfilingConfigurationType profilingConfig = reportingDefinition.getProfilingConfiguration();
        if (profilingConfig == null ||
                conditionEvaluator.legacyIntervalRejects(profilingConfig.getInterval()) ||
                !conditionEvaluator.anyItemReportingConditionApplies(profilingConfig.getBeforeItemCondition(), result)) {
            return;
        }

        startProfiling();
    }

    private void startTracingIfNeeded(OperationResult result) {
        for (ProcessTracingConfigurationType tracingConfig : reportingDefinition.getTracingConfigurationsSorted()) {
            if (conditionEvaluator.legacyIntervalRejects(tracingConfig.getInterval())) {
                continue;
            }
            if (conditionEvaluator.anyItemReportingConditionApplies(tracingConfig.getBeforeItemCondition(), result)) {
                startTracing(tracingConfig);
                break;
            }
        }
    }

    private void startProfiling() {
        LOGGER.info("Starting dynamic profiling for object number {}", activityRun.getItemsProcessed());
        originalProfilingLevel = OperationExecutionLogger.getLocalOperationInvocationLevelOverride();
        OperationExecutionLogger.setLocalOperationInvocationLevelOverride(Level.TRACE);
    }

    private void stopProfiling() {
        OperationExecutionLogger.setLocalOperationInvocationLevelOverride(originalProfilingLevel);
    }

    private void startTracing(@NotNull ProcessTracingConfigurationType tracingConfiguration) {
        tracingConfigurationUsed = tracingConfiguration;

        // This is on debug level because we may start tracing "just for sure" (with low overhead)
        // and write trace only if after-condition is true.
        LOGGER.debug("Starting tracing for object number {}", activityRun.getItemsProcessed());

        TracingProfileType configuredProfile = tracingConfiguration.getTracingProfile();
        List<TracingRootType> configuredPoints = tracingConfiguration.getTracingPoint();

        TracingProfileType profile = configuredProfile != null ?
                configuredProfile : activityRun.getBeans().tracer.getDefaultProfile();
        Collection<TracingRootType> points = !configuredPoints.isEmpty() ?
                configuredPoints : List.of(TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING);

        points.forEach(workerTask::addTracingRequest);
        workerTask.setTracingProfile(profile);
    }

    private void stopTracing() {
        workerTask.removeTracingRequests();
        workerTask.setTracingProfile(null);
    }

    public void storeTrace(@NotNull Tracer tracer, @NotNull AdditionalVariableProvider additionalVariableProvider,
            @NotNull RunningTask workerTask, @NotNull OperationResult resultToStore, OperationResult opResult) {

        if (tracingConfigurationUsed != null) {
            if (!conditionEvaluator.anyItemReportingConditionApplies(tracingConfigurationUsed.getAfterItemCondition(),
                    additionalVariableProvider, opResult)) {
                LOGGER.debug("Trace is discarded because of after-item conditions not matching");
                return;
            }
        } else {
            LOGGER.trace("We were not the one that requested the tracing. So we are not checking after-item conditions.");
        }

        tracer.storeTrace(workerTask, resultToStore, opResult);
    }
}
