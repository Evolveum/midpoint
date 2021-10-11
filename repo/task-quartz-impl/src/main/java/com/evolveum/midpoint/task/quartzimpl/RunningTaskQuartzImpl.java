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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.statistics.Statistics;
import com.evolveum.midpoint.task.quartzimpl.statistics.WorkBucketStatisticsCollector;
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
import java.util.concurrent.Future;
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
     * Lightweight asynchronous subtasks.
     * Each task here is a LAT, i.e. transient and with assigned lightweight handler.
     * Access to this structure should be synchronized because of deleteLightweightAsynchronousSubtasks method.
     * (This means we could replace ConcurrentHashMap with plain HashMap but let's keep that just for certainty.)
     */
    private final Map<String, RunningTaskQuartzImpl> lightweightAsynchronousSubtasks = new ConcurrentHashMap<>();
    private RunningTaskQuartzImpl parentForLightweightAsynchronousTask;            // EXPERIMENTAL

    /**
     * Is the task handler allowed to run, or should it stop as soon as possible?
     */
    private volatile boolean canRun = true;

    /**
     * The code that should be run for asynchronous transient tasks.
     * (As opposed to asynchronous persistent tasks, where the handler is specified
     * via Handler URI in task prism object.)
     */
    private LightweightTaskHandler lightweightTaskHandler;

    /**
     * Future representing executing (or submitted-to-execution) lightweight task handler.
     */
    private Future lightweightHandlerFuture;

    /**
     * An indication whether lightweight handler is currently executing or not.
     * Used for waiting upon its completion (because java.util.concurrent facilities are not able
     * to show this for cancelled/interrupted tasks).
     */
    private volatile boolean lightweightHandlerExecuting;

    /**
     * Thread in which this task's lightweight handler is executing.
     */
    private volatile Thread executingThread;

    /**
     * How many objects were seen by this task. This is to determine whether interval-based profiling is to be started.
     */
    private AtomicInteger objectsSeen = new AtomicInteger(0);

    private Level originalProfilingLevel;

    RunningTaskQuartzImpl(@NotNull TaskManagerQuartzImpl taskManager, PrismObject<TaskType> taskPrism, RepositoryService repositoryService) {
        super(taskManager, taskPrism, repositoryService);
    }

    @Override
    public RunningTaskQuartzImpl getParentForLightweightAsynchronousTask() {
        return parentForLightweightAsynchronousTask;
    }

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

    @Override
    public RunningTask createSubtask(LightweightTaskHandler handler) {
        RunningTaskQuartzImpl sub = taskManager.createRunningTask(createSubtask());
        sub.setLightweightTaskHandler(handler);
        assert sub.getTaskIdentifier() != null;
        synchronized (lightweightAsynchronousSubtasks) {
            lightweightAsynchronousSubtasks.put(sub.getTaskIdentifier(), sub);
        }
        sub.parentForLightweightAsynchronousTask = this;
        return sub;
    }

    public void setLightweightTaskHandler(LightweightTaskHandler lightweightTaskHandler) {
        this.lightweightTaskHandler = lightweightTaskHandler;
    }

    @Override
    public LightweightTaskHandler getLightweightTaskHandler() {
        return lightweightTaskHandler;
    }

    @Override
    public boolean isLightweightAsynchronousTask() {
        return lightweightTaskHandler != null;
    }

    void setLightweightHandlerFuture(Future lightweightHandlerFuture) {
        this.lightweightHandlerFuture = lightweightHandlerFuture;
    }

    public Future getLightweightHandlerFuture() {
        return lightweightHandlerFuture;
    }

    @Override
    public Collection<? extends RunningTaskQuartzImpl> getLightweightAsynchronousSubtasks() {
        synchronized (lightweightAsynchronousSubtasks) {
            return Collections.unmodifiableList(new ArrayList<>(lightweightAsynchronousSubtasks.values()));
        }
    }

    @Override
    public Collection<? extends RunningTaskQuartzImpl> getRunningLightweightAsynchronousSubtasks() {
        // beware: Do not touch task prism here, because this method can be called asynchronously
        List<RunningTaskQuartzImpl> retval = new ArrayList<>();
        for (RunningTaskQuartzImpl subtask : getLightweightAsynchronousSubtasks()) {
            if (subtask.getExecutionStatus() == TaskExecutionStatus.RUNNABLE && subtask.lightweightHandlerStartRequested()) {
                retval.add(subtask);
            }
        }
        return Collections.unmodifiableList(retval);
    }

    @Override
    public void deleteLightweightAsynchronousSubtasks() {
        synchronized (lightweightAsynchronousSubtasks) {
            List<? extends RunningTask> livingSubtasks = lightweightAsynchronousSubtasks.values().stream()
                    .filter(t -> t.getExecutionStatus() == TaskExecutionStatus.RUNNABLE || t.getExecutionStatus() == TaskExecutionStatus.WAITING)
                    .collect(Collectors.toList());
            if (!livingSubtasks.isEmpty()) {
                LOGGER.error("Task {} has {} runnable or waiting lightweight subtasks: {}", this, livingSubtasks.size(), livingSubtasks);
                throw new IllegalStateException("There are runnable or waiting subtasks in the parent task");
            }
            lightweightAsynchronousSubtasks.clear();
        }
    }

    @Override
    public boolean lightweightHandlerStartRequested() {
        return lightweightHandlerFuture != null;
    }

    // just a shortcut
    @Override
    public void startLightweightHandler() {
        taskManager.startLightweightTask(this);
    }

    public void setLightweightHandlerExecuting(boolean lightweightHandlerExecuting) {
        this.lightweightHandlerExecuting = lightweightHandlerExecuting;
    }

    public boolean isLightweightHandlerExecuting() {
        return lightweightHandlerExecuting;
    }

    public Thread getExecutingThread() {
        return executingThread;
    }

    public void setExecutingThread(Thread executingThread) {
        this.executingThread = executingThread;
    }

    // Operational data

    @Override
    public void storeOperationStatsDeferred() {
        refreshLowLevelStatistics();
        setOperationStats(getAggregatedLiveOperationStats());
    }

    @Override
    public void refreshLowLevelStatistics() {
        Thread taskThread = getExecutingThread();
        if (taskThread != null) {
            if (Thread.currentThread().getId() == taskThread.getId()) {
                statistics.refreshLowLevelStatistics(getRepositoryService(), taskManager);
            }
        } else {
            LOGGER.warn("Task thread is null for {}; current thread = {}", this, Thread.currentThread());
        }
    }

    @Override
    public void storeOperationStats() {
        try {
            storeOperationStatsDeferred();
            addPendingModification(createPropertyDeltaIfPersistent(TaskType.F_PROGRESS, getProgress()));
            addPendingModification(createPropertyDeltaIfPersistent(TaskType.F_EXPECTED_TOTAL, getExpectedTotal()));
            flushPendingModifications(new OperationResult(DOT_INTERFACE + ".storeOperationStats"));    // TODO fixme
            lastOperationStatsUpdateTimestamp = System.currentTimeMillis();
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store statistical information into task {}", e, this);
        }
    }

    @Override
    public void storeOperationStatsIfNeeded() {
        if (lastOperationStatsUpdateTimestamp == null ||
                System.currentTimeMillis() - lastOperationStatsUpdateTimestamp > operationStatsUpdateInterval) {
            storeOperationStats();
        } else {
            refreshLowLevelStatistics();
        }
    }

    @Override
    public Long getLastOperationStatsUpdateTimestamp() {
        return lastOperationStatsUpdateTimestamp;
    }

    @Override
    public void setOperationStatsUpdateInterval(long interval) {
        this.operationStatsUpdateInterval = interval;
    }

    @Override
    public long getOperationStatsUpdateInterval() {
        return operationStatsUpdateInterval;
    }

    @Override
    public void incrementProgressAndStoreStatsIfNeeded() {
        setProgress(getProgress() + 1);
        storeOperationStatsIfNeeded();
    }

    @Override
    public boolean isAsynchronous() {
        return getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT
                || isLightweightAsynchronousTask();     // note: if it has lightweight task handler, it must be transient
    }

    /**
     * Beware: this can be called from any thread: the task thread itself, one of the workers, or from an external (unrelated)
     * thread. This is important because some of the statistics retrieved are thread-local ones.
     *
     * So we should NOT fetch thread-local statistics into task structures here!
     */
    @Override
    public OperationStatsType getAggregatedLiveOperationStats() {
        Collection<? extends RunningTaskQuartzImpl> subtasks = getLightweightAsynchronousSubtasks();
        List<Statistics> subCollections = subtasks.stream()
                .map(RunningTaskQuartzImpl::getStatistics)
                .collect(Collectors.toList());
        return statistics.getAggregatedLiveOperationStats(subCollections);
    }

    @Override
    public void startCollectingOperationStats(@NotNull StatisticsCollectionStrategy strategy, boolean initialExecution) {
        PerformanceMonitor performanceMonitor = repositoryService.getPerformanceMonitor();
        if (initialExecution && strategy.isStartFromZero()) {
            statistics.startCollectingOperationStatsFromZero(strategy.isMaintainIterationStatistics(),
                    strategy.isMaintainSynchronizationStatistics(), strategy.isMaintainActionsExecutedStatistics(),
                    performanceMonitor);
            setProgress(0L);
            storeOperationStats();
        } else {
            OperationStatsType stored = getStoredOperationStats();
            statistics.startCollectingOperationStatsFromStoredValues(stored, strategy.isMaintainIterationStatistics(),
                    strategy.isMaintainSynchronizationStatistics(), strategy.isMaintainActionsExecutedStatistics(),
                    initialExecution, performanceMonitor);
        }
    }

    void startCollectingLowLevelStatistics() {
        statistics.startCollectingLowLevelStatistics(repositoryService.getPerformanceMonitor());
    }

    Statistics getStatistics() {
        return statistics;
    }

    public WorkBucketStatisticsCollector getWorkBucketStatisticsCollector() {
        return statistics;
    }

    // TODO consider what to do with this

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
                    tracingProfileConfigured != null ? tracingProfileConfigured : taskManager.getTracer().getDefaultProfile();
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
}
