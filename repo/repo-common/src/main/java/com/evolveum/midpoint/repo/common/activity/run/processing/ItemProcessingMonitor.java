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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTracingDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityReportingDefinition;
import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingConditionEvaluator.AdditionalVariableProvider;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationExecutionLogger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityProfilingDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingRootType;

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

    /** Tracing definition that was selected and applied (if any). */
    @Nullable private ActivityTracingDefinitionType tracingDefinitionUsed;

    private Collection<TracingRootType> previousTracingPoints;
    private TracingProfileType previousTracingProfile;

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
        ActivityProfilingDefinitionType profiling = reportingDefinition.getProfilingConfiguration();
        if (profiling == null ||
                conditionEvaluator.legacyIntervalRejects(profiling.getInterval()) ||
                !conditionEvaluator.anyItemReportingConditionApplies(profiling.getBeforeItemCondition(), result)) {
            return;
        }

        startProfiling();
    }

    private void startTracingIfNeeded(OperationResult result) {
        for (ActivityTracingDefinitionType tracing : reportingDefinition.getTracingConfigurationsSorted()) {
            if (!arePointsApplicable(tracing)) {
                continue;
            }
            if (conditionEvaluator.legacyIntervalRejects(tracing.getInterval())) {
                continue;
            }
            if (conditionEvaluator.anyItemReportingConditionApplies(tracing.getBeforeItemCondition(), result)) {
                startTracing(tracing);
                break;
            }
        }
    }

    private boolean arePointsApplicable(ActivityTracingDefinitionType tracing) {
        var points = tracing.getTracingPoint();
        return points.isEmpty() || points.stream().anyMatch(this::isPointApplicable);
    }

    private boolean isPointApplicable(TracingRootType point) {
        // These are treated elsewhere
        return point != TracingRootType.RETRIEVED_RESOURCE_OBJECT_PROCESSING
                && point != TracingRootType.ASYNCHRONOUS_MESSAGE_PROCESSING
                && point != TracingRootType.LIVE_SYNC_CHANGE_PROCESSING;
    }

    private void startProfiling() {
        LOGGER.info("Starting dynamic profiling for object number {}", activityRun.getItemsProcessed());
        originalProfilingLevel = OperationExecutionLogger.getLocalOperationInvocationLevelOverride();
        OperationExecutionLogger.setLocalOperationInvocationLevelOverride(Level.TRACE);
    }

    private void stopProfiling() {
        OperationExecutionLogger.setLocalOperationInvocationLevelOverride(originalProfilingLevel);
    }

    private void startTracing(@NotNull ActivityTracingDefinitionType tracingDefinition) {
        tracingDefinitionUsed = tracingDefinition;

        // This is on debug level because we may start tracing "just for sure" (with low overhead)
        // and write trace only if after-condition is true.
        LOGGER.debug("Starting tracing for object number {}", activityRun.getItemsProcessed());

        var configuredProfile = tracingDefinition.getTracingProfile();
        var effectiveProfile = configuredProfile != null ? configuredProfile : activityRun.getBeans().tracer.getDefaultProfile();

        var configuredPoints = tracingDefinition.getTracingPoint();
        var effectivePoints = !configuredPoints.isEmpty() ? configuredPoints : List.of(TracingRootType.ACTIVITY_ITEM_PROCESSING);

        previousTracingPoints = workerTask.getTracingRequestedFor();
        previousTracingProfile = workerTask.getTracingProfile();

        effectivePoints.forEach(workerTask::addTracingRequest);
        workerTask.setTracingProfile(effectiveProfile);
    }

    private void stopTracing() {
        if (tracingDefinitionUsed != null) {
            workerTask.setTracingRequestedFor(previousTracingPoints);
            workerTask.setTracingProfile(previousTracingProfile);
        }
    }

    void storeTrace(@NotNull Tracer tracer, @NotNull AdditionalVariableProvider additionalVariableProvider,
            @NotNull RunningTask workerTask, @NotNull OperationResult resultToStore, OperationResult opResult) {

        if (tracingDefinitionUsed != null) {
            if (!conditionEvaluator.anyItemReportingConditionApplies(tracingDefinitionUsed.getAfterItemCondition(),
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
