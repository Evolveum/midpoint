/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.statistics.Statistics;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of a "running task" i.e. a task that is being executed either under Quartz or as a LAT.
 * (For the latter case see {@link RunningLightweightTaskImpl}.)
 */
public class RunningTaskQuartzImpl extends TaskQuartzImpl implements RunningTask {

    private static final Trace LOGGER = TraceManager.getTrace(RunningTaskQuartzImpl.class);

    private static final long DEFAULT_OPERATION_STATS_UPDATE_INTERVAL = 3000L;

    private long operationStatsUpdateInterval = DEFAULT_OPERATION_STATS_UPDATE_INTERVAL;
    private volatile Long lastOperationStatsUpdateTimestamp;

    /**
     * Root of the task hierarchy. It is not guaranteed to be current. It is initialized when the task is started.
     * Can even point to the this task object.
     */
    @Experimental
    @NotNull private final Task rootTask;

    /**
     * Immediate parent. It is not guaranteed to be current. It is initialized when the task is started.
     */
    @Experimental
    @Nullable private final Task parentTask;

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
     * Execution context. Currently used to store activity execution during item processing in worker tasks.
     */
    private ExecutionSupport executionSupport;

    /** True if this task should not be checked for staleness. */
    private boolean excludedFromStalenessChecking;

    public RunningTaskQuartzImpl(@NotNull TaskManagerQuartzImpl taskManager, @NotNull PrismObject<TaskType> taskPrism,
            @NotNull Task rootTask, @Nullable Task parentTask) {
        super(taskManager, taskPrism);
        this.rootTask = rootTask;
        this.parentTask = parentTask;
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
                .toRunningLightweightTaskInstance(createSubtask(), rootTask, this, handler);
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
     *
     * 1. Compute current state of the statistics (using standard method).
     * 2. Store this state as new "initial values" into {@link Statistics} class.
     *
     * Beware that we hold the lock on async subtasks.
     */
    private void migrateStatisticsFromLightweightSubtasks() {
        updateOperationStatsInTaskPrism();
        statistics.restartCollectingStatisticsFromStoredValues(this, beans.sqlPerformanceMonitorsCollection);
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
    public void updateOperationStatsInTaskPrism(boolean updateThreadLocalStatistics) {
        if (updateThreadLocalStatistics) {
            refreshThreadLocalStatistics();
        }
        updateOperationStatsInTaskPrism();
    }

    private void updateOperationStatsInTaskPrism() {
        setOperationStatsTransient(
                getAggregatedLiveOperationStats());
    }

    @Override
    public boolean storeStatisticsIntoRepositoryIfTimePassed(Runnable additionalUpdater, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Long lastUpdateTimestamp = lastOperationStatsUpdateTimestamp;
        if (lastUpdateTimestamp != null &&
                System.currentTimeMillis() - lastUpdateTimestamp <= operationStatsUpdateInterval) {
            return false;
        } else {
            // This is a workaround to stop multiple worker threads updating the stats concurrently.
            // Besides wasting CPU cycles and DB operations, the problem with concurrent updates is that they
            // may leave the repository in obsolete state (if executed in wrong order because of DB retries).
            lastOperationStatsUpdateTimestamp = System.currentTimeMillis();
            try {
                if (additionalUpdater != null) {
                    additionalUpdater.run();
                }
                storeStatisticsIntoRepository(result);
                return true;
            } catch (Throwable t) {
                // We should try the next time.
                lastOperationStatsUpdateTimestamp = null;
                throw t;
            }
        }
    }

    @Override
    public void storeStatisticsIntoRepository(OperationResult result) throws SchemaException, ObjectNotFoundException {
        addPendingModification(createContainerDeltaIfPersistent(TaskType.F_OPERATION_STATS, getStoredOperationStatsOrClone()));
        addPendingModification(createPropertyDeltaIfPersistent(TaskType.F_PROGRESS, getLegacyProgress()));
        addPendingModification(createPropertyDeltaIfPersistent(TaskType.F_EXPECTED_TOTAL, getExpectedTotal()));
        try {
            LOGGER.trace("Storing statistics into repository: {} pending modifications", getPendingModificationsCount());
            flushPendingModifications(result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected ObjectAlreadyExistsException: " + e.getMessage(), e);
        }
        lastOperationStatsUpdateTimestamp = System.currentTimeMillis();
    }

    @Override
    public void updateAndStoreStatisticsIntoRepository(boolean updateThreadLocalStatistics, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        updateOperationStatsInTaskPrism(updateThreadLocalStatistics);
        storeStatisticsIntoRepository(result);
    }

    @Override
    public void setStatisticsRepoStoreInterval(long interval) {
        this.operationStatsUpdateInterval = interval;
    }

    @Override
    public void incrementLegacyProgressAndStoreStatisticsIfTimePassed(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        incrementLegacyProgressTransient();
        updateOperationStatsInTaskPrism(true);
        storeStatisticsIntoRepositoryIfTimePassed(null, result);
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
    }

    @Override
    public void restartCollectingStatisticsFromZero() {
        statistics.restartCollectingStatisticsFromZero(beans.sqlPerformanceMonitorsCollection);
    }

    private Statistics getStatistics() {
        return statistics;
    }

    //endregion

    //endregion

    //region Switching to waiting state
    @Override
    public void makeWaitingForOtherTasks(TaskUnpauseActionType unpauseAction) {
        setSchedulingState(TaskSchedulingStateType.WAITING);
        setWaitingReason(TaskWaitingReasonType.OTHER_TASKS);
        setUnpauseAction(unpauseAction);
    }

    @Override
    public void makeWaitingForOtherTasks(TaskExecutionStateType execState, TaskUnpauseActionType unpauseAction) {
        setExecutionState(execState);
        setSchedulingState(TaskSchedulingStateType.WAITING);
        setWaitingReason(TaskWaitingReasonType.OTHER_TASKS);
        setUnpauseAction(unpauseAction);
    }

    private void setUnpauseAction(TaskUnpauseActionType value) {
        setProperty(TaskType.F_UNPAUSE_ACTION, value);
    }

    //endregion

    //region Misc
    @Override
    public @NotNull String getRootTaskOid() {
        return rootTask.getOid();
    }

    @Override
    @NotNull
    public Task getRootTask() {
        return rootTask;
    }

    @Nullable
    @Override
    public Task getParentTask() {
        return parentTask;
    }

    @Override
    public @NotNull ParentAndRoot getParentAndRoot(OperationResult result) throws SchemaException, ObjectNotFoundException {
        return new ParentAndRoot(getParentTask(), getRootTask());
    }

    @Override
    public ExecutionSupport getExecutionSupport() {
        return executionSupport;
    }

    @Override
    public void setExecutionSupport(ExecutionSupport executionSupport) {
        this.executionSupport = executionSupport;
    }

    @Override
    public boolean isExcludedFromStalenessChecking() {
        return excludedFromStalenessChecking;
    }

    public void setExcludedFromStalenessChecking(boolean excludedFromStalenessChecking) {
        this.excludedFromStalenessChecking = excludedFromStalenessChecking;
    }
    //endregion
}
