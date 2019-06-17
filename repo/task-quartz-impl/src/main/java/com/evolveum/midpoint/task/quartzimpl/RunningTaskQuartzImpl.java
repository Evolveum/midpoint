/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
	 * How many objects were processed by this task (it's the responsibility of the task handler to maintain it)
	 */
	private AtomicInteger objectsProcessed = new AtomicInteger(0);

	/**
	 * When last profiling interval started.
	 */
	private Integer lastProfilingStartedAt;

	RunningTaskQuartzImpl(TaskManagerQuartzImpl taskManager, PrismObject<TaskType> taskPrism, RepositoryService repositoryService) {
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
			statistics.startCollectingOperationStatsFromZero(strategy.isMaintainIterationStatistics(), strategy.isMaintainSynchronizationStatistics(), strategy.isMaintainActionsExecutedStatistics(), performanceMonitor);
			setProgress(0L);
			storeOperationStats();
		} else {
			OperationStatsType stored = getStoredOperationStats();
			statistics.startCollectingOperationStatsFromStoredValues(stored, strategy.isMaintainIterationStatistics(), strategy.isMaintainSynchronizationStatistics(), strategy.isMaintainActionsExecutedStatistics(), performanceMonitor);
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


	@Override
	public void startDynamicProfilingIfNeeded(RunningTask coordinatorTask) {
		// todo implement
	}

	@Override
	public void stopDynamicProfiling() {
		// todo implement
	}
}
