/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import ch.qos.logback.classic.Level;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.schema.util.task.TaskProgressUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.statistics.Statistics;
import com.evolveum.midpoint.task.quartzimpl.statistics.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationExecutionLogger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 */
public class RunningTaskQuartzImpl extends TaskQuartzImpl implements RunningTask {

    private static final Trace LOGGER = TraceManager.getTrace(RunningTaskQuartzImpl.class);

    private static final long DEFAULT_OPERATION_STATS_UPDATE_INTERVAL = 3000L;

    private long operationStatsUpdateInterval = DEFAULT_OPERATION_STATS_UPDATE_INTERVAL;
    private Long lastOperationStatsUpdateTimestamp;

    /**
     * OID of the root of this task hierarchy.
     * (Note that each running task must have a persistent root.)
     */
    @Experimental
    @NotNull private final String rootTaskOid;

    /**
     * Lightweight asynchronous subtasks.
     * Each task here is a LAT, i.e. transient and with assigned lightweight handler.
     * Access to this structure should be synchronized because of deleteLightweightAsynchronousSubtasks method.
     * (This means we could replace ConcurrentHashMap with plain HashMap but let's keep that just for certainty.)
     */
    private final Map<String, RunningLightweightTaskImpl> lightweightAsynchronousSubtasks = new ConcurrentHashMap<>();

    /**
     * Is the task handler allowed to run, or should it stop as soon as possible?
     */
    private volatile boolean canRun = true;

    /**
     * Thread in which this task's handler is executing.
     */
    private volatile Thread executingThread;

    /**
     * How many objects were seen by this task. This is to determine whether interval-based profiling is to be started.
     */
    private final AtomicInteger objectsSeen = new AtomicInteger(0);

    /** TODO */
    private Level originalProfilingLevel;

    public RunningTaskQuartzImpl(@NotNull TaskManagerQuartzImpl taskManager, @NotNull PrismObject<TaskType> taskPrism,
            @NotNull String rootTaskOid) {
        super(taskManager, taskPrism);
        this.rootTaskOid = rootTaskOid;
    }

    //region Task execution (canRun, executing thread)
    /**
     * Signal the task to shut down.
     * It may not stop immediately, but it should stop eventually.
     * <p>
     * BEWARE, this method has to be invoked on the same Task instance that is executing.
     * If called e.g. on a Task just retrieved from the repository, it will have no effect whatsoever.
     */

    public void unsetCanRun() {
        // beware: Do not touch task prism here, because this method can be called asynchronously
        canRun = false;
    }

    @Override
    public boolean canRun() {
        return canRun;
    }

    public Thread getExecutingThread() {
        return executingThread;
    }

    public void setExecutingThread(Thread executingThread) {
        this.executingThread = executingThread;
    }
    //endregion

    //region Subtasks
    @Override
    public @NotNull RunningLightweightTask createSubtask(@NotNull LightweightTaskHandler handler) {
        RunningLightweightTaskImpl sub = beans.taskInstantiator
                .toRunningLightweightTaskInstance(createSubtask(), rootTaskOid, this, handler);
        assert sub.getTaskIdentifier() != null;
        synchronized (lightweightAsynchronousSubtasks) {
            lightweightAsynchronousSubtasks.put(sub.getTaskIdentifier(), sub);
        }
        return sub;
    }

    @Override
    public Collection<? extends RunningLightweightTaskImpl> getLightweightAsynchronousSubtasks() {
        synchronized (lightweightAsynchronousSubtasks) {
            return List.copyOf(lightweightAsynchronousSubtasks.values());
        }
    }

    @Override
    public Collection<? extends RunningLightweightTaskImpl> getRunningLightweightAsynchronousSubtasks() {
        // beware: Do not touch task prism here, because of thread safety
        return getLightweightAsynchronousSubtasks().stream()
                .filter(subtask -> subtask.isRunning() &&
                        subtask.lightweightHandlerStartRequested())
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Collection<? extends RunningLightweightTaskImpl> getRunnableOrRunningLightweightAsynchronousSubtasks() {
        // beware: Do not touch task prism here, because of thread safety
        return getLightweightAsynchronousSubtasks().stream()
                .filter(subtask -> subtask.isRunnable() || subtask.isRunning())
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void deleteLightweightAsynchronousSubtasks() {
        synchronized (lightweightAsynchronousSubtasks) {
            List<? extends RunningTask> livingSubtasks = lightweightAsynchronousSubtasks.values().stream()
                    .filter(t -> t.isRunnable() || t.isRunning())
                    .collect(Collectors.toList());
            if (!livingSubtasks.isEmpty()) {
                LOGGER.error("Task {} has {} runnable/running lightweight subtasks: {}", this, livingSubtasks.size(), livingSubtasks);
                throw new IllegalStateException("There are runnable/running subtasks in the parent task");
            }
            migrateStatisticsFromLightweightSubtasks();
            lightweightAsynchronousSubtasks.clear();
        }
    }

    /**
     * In multithreaded scenarios some of the statistics exist only in LATs (e.g. iteration statistics).
     * Now we are going to remove these LATs. We have to preserve the data somehow.
     *
     * The easiest way seems to be:
     * 1. Compute current state of the statistics (using standard method).
     * 2. Store this state as new "initial values" into {@link Statistics} class.
     *
     * Beware that we hold the lock on async subtasks.
     */
    private void migrateStatisticsFromLightweightSubtasks() {
        updateOperationalStatsInTaskPrism();
        statistics.restartCollectingStatistics(this, beans.sqlPerformanceMonitorsCollection);
    }
    //endregion

    //region Statistics
    @Override
    public void refreshThreadLocalStatistics() {
        Thread taskThread = getExecutingThread();
        if (taskThread != null) {
            if (Thread.currentThread().getId() == taskThread.getId()) {
                statistics.refreshLowLevelStatistics(taskManager);
            } else {
                LOGGER.warn("Called refreshThreadLocalStatistics on wrong task. Task thread: {}, current thread: {}, task: {}",
                        taskThread, Thread.currentThread(), this);
            }
        } else {
            LOGGER.warn("Task thread is null for {}; current thread = {}", this, Thread.currentThread());
        }
    }

    @Override
    public void updateStatisticsInTaskPrism(boolean updateThreadLocalStatistics) {
        if (updateThreadLocalStatistics) {
            refreshThreadLocalStatistics();
        }
        updateOperationalStatsInTaskPrism();
        updateStructuredProgressAndProgressInTaskPrism();
    }

    private void updateOperationalStatsInTaskPrism() {
        setOperationStatsTransient(getAggregatedLiveOperationStats());
    }

    private void updateStructuredProgressAndProgressInTaskPrism() {
        StructuredTaskProgressType progress = statistics.getStructuredTaskProgress();
        if (progress != null) {
            setStructuredProgressTransient(progress);
            setProgressTransient(TaskProgressUtil.getTotalProgress(progress));
        } else {
            // structured progress is not maintained in this task
        }
    }

    @Override
    public void storeStatisticsIntoRepositoryIfTimePassed(OperationResult result) {
        if (lastOperationStatsUpdateTimestamp == null ||
                System.currentTimeMillis() - lastOperationStatsUpdateTimestamp > operationStatsUpdateInterval) {
            storeStatisticsIntoRepository(result);
        }
    }

    @Override
    public void storeStatisticsIntoRepository(OperationResult result) {
        try {
            addPendingModification(createContainerDeltaIfPersistent(TaskType.F_OPERATION_STATS, getStoredOperationStatsOrClone()));
            addPendingModification(createContainerDeltaIfPersistent(TaskType.F_STRUCTURED_PROGRESS, getStructuredProgressOrClone()));
            addPendingModification(createPropertyDeltaIfPersistent(TaskType.F_PROGRESS, getProgress()));
            addPendingModification(createPropertyDeltaIfPersistent(TaskType.F_EXPECTED_TOTAL, getExpectedTotal()));
            flushPendingModifications(result);
            lastOperationStatsUpdateTimestamp = System.currentTimeMillis();
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store statistical information into task {}", e, this);
        }
    }

    @Override
    public void updateAndStoreStatisticsIntoRepository(boolean updateThreadLocalStatistics, OperationResult result) {
        updateStatisticsInTaskPrism(updateThreadLocalStatistics);
        storeStatisticsIntoRepository(result);
    }

    @Override
    public void setStatisticsRepoStoreInterval(long interval) {
        this.operationStatsUpdateInterval = interval;
    }

    @Override
    public void incrementProgressAndStoreStatisticsIfTimePassed(OperationResult result) {
        incrementProgressTransient();
        updateStatisticsInTaskPrism(true);
        storeStatisticsIntoRepositoryIfTimePassed(result);
    }

    @Override
    public void setStructuredProgressPartInformation(String partUri, Integer partNumber, Integer expectedParts) {
        statistics.setStructuredProgressPartInformation(partUri, partNumber, expectedParts);
    }

    @Override
    public void incrementStructuredProgress(String partUri, QualifiedItemProcessingOutcomeType outcome) {
        statistics.incrementStructuredProgress(partUri, outcome);
    }

    @Override
    public void markStructuredProgressAsComplete() {
        statistics.markStructuredProgressAsComplete();
    }

    @Override
    public void changeStructuredProgressOnWorkBucketCompletion() {
        statistics.changeStructuredProgressOnWorkBucketCompletion();
    }

    /**
     * Returns true if the task runs asynchronously.
     */
    @Override
    public boolean isAsynchronous() {
        return true;
    }

    /**
     * Gets aggregated live operation statistics from this task and its subtasks.
     *
     * Clients beware: Update thread-local statistics before!
     *
     * Implementers beware: This method can be called from any thread: the task thread itself, one of the workers, or from
     * an external (unrelated) thread. This is important because some of the statistics retrieved are thread-local ones.
     * So we should NOT fetch thread-local statistics into task structures here!
     */
    @Override
    public OperationStatsType getAggregatedLiveOperationStats() {
        Collection<? extends RunningTaskQuartzImpl> subtasks = getLightweightAsynchronousSubtasks();
        List<Statistics> subCollections = subtasks.stream()
                .map(RunningTaskQuartzImpl::getStatistics)
                .collect(Collectors.toList());
        return statistics.getAggregatedOperationStats(subCollections);
    }

    @Override
    public void startCollectingStatistics(@NotNull StatisticsCollectionStrategy strategy) {
        statistics.startCollectingStatistics(this, strategy, beans.sqlPerformanceMonitorsCollection);
//
//        OperationStatsType stored = getStoredOperationStatsOrClone();
//        if (stored != null) {
//            String formatted = IterativeTaskInformation.format(stored.getIterativeTaskInformation());
//            System.out.println("In " + this + " ITI reset to:\n" + formatted);
//        }
    }

    private Statistics getStatistics() {
        return statistics;
    }

    public WorkBucketStatisticsCollector getWorkBucketStatisticsCollector() {
        return statistics;
    }
    //endregion

    //region Tracing and profiling
    @Override
    public int getAndIncrementObjectsSeen() {
        return objectsSeen.getAndIncrement();
    }

    @Override
    public void startDynamicProfilingIfNeeded(RunningTask coordinatorTask, int objectsSeen) {
        Integer interval = coordinatorTask.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_PROFILING_INTERVAL);
        if (interval != null && interval != 0 && objectsSeen%interval == 0) {
            LOGGER.info("Starting dynamic profiling for object number {} (interval is {})", objectsSeen, interval);
            originalProfilingLevel = OperationExecutionLogger.getLocalOperationInvocationLevelOverride();
            OperationExecutionLogger.setLocalOperationInvocationLevelOverride(Level.TRACE);
        }
    }

    @Override
    public void stopDynamicProfiling() {
        OperationExecutionLogger.setLocalOperationInvocationLevelOverride(originalProfilingLevel);
    }

    @Override
    public boolean requestTracingIfNeeded(RunningTask coordinatorTask, int objectsSeen, TracingRootType defaultTracingRoot) {
        ProcessTracingConfigurationType config = coordinatorTask.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_TRACING);
        int interval;
        if (config != null) {
            interval = defaultIfNull(config.getInterval(), 1);
        } else {
            // the old way
            interval = defaultIfNull(
                    coordinatorTask.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_TRACING_INTERVAL), 0);
        }

        if (interval != 0 && objectsSeen % interval == 0) {
            TracingProfileType tracingProfileConfigured;
            Collection<TracingRootType> pointsConfigured;
            if (config != null) {
                tracingProfileConfigured = config.getTracingProfile();
                pointsConfigured = config.getTracingPoint();
            } else {
                // the old way
                tracingProfileConfigured = coordinatorTask
                        .getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_TRACING_PROFILE);
                PrismProperty<TracingRootType> tracingRootProperty = coordinatorTask
                        .getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_TRACING_ROOT);
                pointsConfigured = tracingRootProperty != null ? tracingRootProperty.getRealValues() : emptyList();
            }

            LOGGER.info("Starting tracing for object number {} (interval is {})", this.objectsSeen, interval);

            TracingProfileType tracingProfile =
                    tracingProfileConfigured != null ? tracingProfileConfigured : beans.tracer.getDefaultProfile();
            Collection<TracingRootType> points = pointsConfigured.isEmpty() ? singleton(defaultTracingRoot) : pointsConfigured;

            points.forEach(this::addTracingRequest);
            setTracingProfile(tracingProfile);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stopTracing() {
        removeTracingRequests();
        setTracingProfile(null);
    }
    //endregion

    //region Misc
    @Override
    public @NotNull String getRootTaskOid() {
        return rootTaskOid;
    }
    //endregion
}
