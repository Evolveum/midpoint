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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.statistics.Statistics;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 *
 */
public class RunningTaskQuartzImpl implements RunningTask, InternalTaskInterface {

	private static final Trace LOGGER = TraceManager.getTrace(RunningTaskQuartzImpl.class);

	private static final long DEFAULT_OPERATION_STATS_UPDATE_INTERVAL = 3000L;

	/**
	 * Real task that holds all the information.
	 */
	@NotNull private final TaskQuartzImpl realTask;

	private long operationStatsUpdateInterval = DEFAULT_OPERATION_STATS_UPDATE_INTERVAL;
	private Long lastOperationStatsUpdateTimestamp;

	/**
	 * Lightweight asynchronous subtasks.
	 * Each task here is a LAT, i.e. transient and with assigned lightweight handler.
	 * <p>
	 * This must be concurrent, because interrupt() method uses it.
	 */
	private Map<String, RunningTaskQuartzImpl> lightweightAsynchronousSubtasks = new ConcurrentHashMap<>();
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
	private volatile Thread lightweightThread;

	RunningTaskQuartzImpl(TaskManagerQuartzImpl taskManager, PrismObject<TaskType> taskPrism, RepositoryService repositoryService) {
		realTask = new TaskQuartzImpl(taskManager, taskPrism, repositoryService);
	}

	@Override
	public RunningTaskQuartzImpl getParentForLightweightAsynchronousTask() {
		return parentForLightweightAsynchronousTask;
	}

	public void unsetCanRun() {
		// beware: Do not touch task prism here, because this method can be called asynchronously
		canRun = false;
	}

	@Override
	public boolean canRun() {
		return canRun;
	}

	@Override
	public synchronized RunningTask createSubtask(LightweightTaskHandler handler) {
		RunningTaskQuartzImpl sub = realTask.taskManager.createRunningTask(createSubtask());
		sub.setLightweightTaskHandler(handler);
		assert sub.getTaskIdentifier() != null;
		lightweightAsynchronousSubtasks.put(sub.getTaskIdentifier(), sub);
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
		return Collections.unmodifiableList(new ArrayList<>(lightweightAsynchronousSubtasks.values()));
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
	public boolean lightweightHandlerStartRequested() {
		return lightweightHandlerFuture != null;
	}

	// just a shortcut
	@Override
	public synchronized void startLightweightHandler() {
		realTask.taskManager.startLightweightTask(this);
	}

	public void setLightweightHandlerExecuting(boolean lightweightHandlerExecuting) {
		this.lightweightHandlerExecuting = lightweightHandlerExecuting;
	}

	public boolean isLightweightHandlerExecuting() {
		return lightweightHandlerExecuting;
	}

	public Thread getLightweightThread() {
		return lightweightThread;
	}

	public void setLightweightThread(Thread lightweightThread) {
		this.lightweightThread = lightweightThread;
	}

	// Operational data

	@Override
	public synchronized void storeOperationStatsDeferred() {
		setOperationStats(getAggregatedLiveOperationStats());
	}

	@Override
	public synchronized void storeOperationStats() {
		try {
			storeOperationStatsDeferred();
			processModificationBatched(createProgressDelta(getProgress()));
			processModificationBatched(createExpectedTotalDelta(getExpectedTotal()));
			savePendingModifications(new OperationResult(DOT_INTERFACE + ".storeOperationStats"));    // TODO fixme
			lastOperationStatsUpdateTimestamp = System.currentTimeMillis();
		} catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store statistical information into task {}", e, this);
		}
	}

	@Override
	public synchronized void storeOperationStatsIfNeeded() {
		if (lastOperationStatsUpdateTimestamp == null ||
				System.currentTimeMillis() - lastOperationStatsUpdateTimestamp > operationStatsUpdateInterval) {
			storeOperationStats();
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
	public synchronized void incrementProgressAndStoreStatsIfNeeded() {
		setProgress(getProgress() + 1);
		storeOperationStatsIfNeeded();
	}

	@Override
	public synchronized boolean isAsynchronous() {
		return getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT
				|| isLightweightAsynchronousTask();     // note: if it has lightweight task handler, it must be transient
	}

	@Override
	public synchronized OperationStatsType getAggregatedLiveOperationStats() {
		List<Statistics> subCollectors = getLightweightAsynchronousSubtasks().stream()
				.map(task -> task.getStatistics()).collect(Collectors.toList());
		return realTask.statistics.getAggregatedLiveOperationStats(subCollectors);
	}

	@Override
	public synchronized void startCollectingOperationStats(@NotNull StatisticsCollectionStrategy strategy) {
		realTask.startCollectingOperationStats(strategy);
		if (strategy.isStartFromZero()) {
			storeOperationStats();
		}
	}

	//region ================================================================================ DELEGATED METHODS

	Statistics getStatistics() {
		return realTask.statistics;
	}

	@Override
	public synchronized boolean isRecreateQuartzTrigger() {
		return realTask.isRecreateQuartzTrigger();
	}

	@Override
	public synchronized void setRecreateQuartzTrigger(boolean recreateQuartzTrigger) {
		realTask.setRecreateQuartzTrigger(recreateQuartzTrigger);
	}

	public synchronized void addPendingModification(ItemDelta<?, ?> delta) {
		realTask.addPendingModification(delta);
	}

	public synchronized void synchronizeWithQuartz(OperationResult parentResult) {
		realTask.synchronizeWithQuartz(parentResult);
	}

	public synchronized void processModificationBatched(ItemDelta<?, ?> delta) {
		realTask.processModificationBatched(delta);
	}

	@Nullable
	public synchronized PropertyDelta<?> createProgressDelta(long value) {
		return realTask.createProgressDelta(value);
	}

	public synchronized void setOperationStatsTransient(OperationStatsType value) {
		realTask.setOperationStatsTransient(value);
	}

	public synchronized void setOperationStats(OperationStatsType value) {
		realTask.setOperationStats(value);
	}

	public synchronized void setExpectedTotalTransient(Long value) {
		realTask.setExpectedTotalTransient(value);
	}

	@Nullable
	public synchronized PropertyDelta<?> createExpectedTotalDelta(Long value) {
		return realTask.createExpectedTotalDelta(value);
	}

	public synchronized void updateStoredTaskResult() throws SchemaException, ObjectNotFoundException {
		realTask.updateStoredTaskResult();
	}

	public synchronized void setResultStatusType(OperationResultStatusType value) {
		realTask.setResultStatusType(value);
	}

	public synchronized void setResultStatusTypeImmediate(OperationResultStatusType value,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		realTask.setResultStatusTypeImmediate(value, parentResult);
	}

	public synchronized void setResultStatusTypeTransient(OperationResultStatusType value) {
		realTask.setResultStatusTypeTransient(value);
	}

	public synchronized void setHandlerUriTransient(String handlerUri) {
		realTask.setHandlerUriTransient(handlerUri);
	}

	public synchronized void setOtherHandlersUriStackTransient(UriStack value) {
		realTask.setOtherHandlersUriStackTransient(value);
	}

	public synchronized void setOtherHandlersUriStackImmediate(UriStack value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setOtherHandlersUriStackImmediate(value, parentResult);
	}

	public synchronized void setOtherHandlersUriStack(UriStack value) {
		realTask.setOtherHandlersUriStack(value);
	}

	@Override
	public synchronized void checkDependentTasksOnClose(OperationResult result) throws SchemaException, ObjectNotFoundException {
		realTask.checkDependentTasksOnClose(result);
	}

	@Override
	public synchronized void checkDependencies(OperationResult result) throws SchemaException, ObjectNotFoundException {
		realTask.checkDependencies(result);
	}

	public synchronized int getHandlersCount() {
		return realTask.getHandlersCount();
	}

	@Override
	public synchronized void setOid(String oid) {
		realTask.setOid(oid);
	}

	public synchronized void setTaskIdentifier(String value) {
		realTask.setTaskIdentifier(value);
	}

	public synchronized void setExecutionStatusTransient(TaskExecutionStatus executionStatus) {
		realTask.setExecutionStatusTransient(executionStatus);
	}

	@Override
	public synchronized void setExecutionStatusImmediate(TaskExecutionStatus value,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		realTask.setExecutionStatusImmediate(value, parentResult);
	}

	@Override
	public synchronized void setExecutionStatusImmediate(TaskExecutionStatus value,
			TaskExecutionStatusType previousValue, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, PreconditionViolationException {
		realTask.setExecutionStatusImmediate(value, previousValue, parentResult);
	}

	public synchronized void setExecutionStatus(TaskExecutionStatus value) {
		realTask.setExecutionStatus(value);
	}

	public synchronized void setWaitingReasonTransient(TaskWaitingReason value) {
		realTask.setWaitingReasonTransient(value);
	}

	public synchronized void setWaitingReason(TaskWaitingReason value) {
		realTask.setWaitingReason(value);
	}

	@Override
	public synchronized void setWaitingReasonImmediate(TaskWaitingReason value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setWaitingReasonImmediate(value, parentResult);
	}

	public synchronized void setUnpauseActionTransient(TaskUnpauseActionType value) {
		realTask.setUnpauseActionTransient(value);
	}

	public synchronized void setUnpauseAction(TaskUnpauseActionType value) {
		realTask.setUnpauseAction(value);
	}

	public synchronized void setRecurrenceStatus(TaskRecurrence value) {
		realTask.setRecurrenceStatus(value);
	}

	public synchronized void setRecurrenceStatusImmediate(TaskRecurrence value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setRecurrenceStatusImmediate(value, parentResult);
	}

	public synchronized void setRecurrenceStatusTransient(TaskRecurrence value) {
		realTask.setRecurrenceStatusTransient(value);
	}

	public synchronized void setSchedule(ScheduleType value) {
		realTask.setSchedule(value);
	}

	public synchronized void setScheduleImmediate(ScheduleType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setScheduleImmediate(value, parentResult);
	}

	public synchronized void setThreadStopActionImmediate(ThreadStopActionType value,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		realTask.setThreadStopActionImmediate(value, parentResult);
	}

	public synchronized void setBindingTransient(TaskBinding value) {
		realTask.setBindingTransient(value);
	}

	public synchronized PrismObject<UserType> resolveOwnerRef(OperationResult result) throws SchemaException {
		return realTask.resolveOwnerRef(result);
	}

	public synchronized void setChannelTransient(String name) {
		realTask.setChannelTransient(name);
	}

	public synchronized void setObjectRefTransient(ObjectReferenceType objectRefType) {
		realTask.setObjectRefTransient(objectRefType);
	}

	public synchronized void setNameTransient(PolyStringType name) {
		realTask.setNameTransient(name);
	}

	public synchronized void setDescriptionTransient(String name) {
		realTask.setDescriptionTransient(name);
	}

	public synchronized void setParent(String value) {
		realTask.setParent(value);
	}

	public synchronized void setParentImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		realTask.setParentImmediate(value, parentResult);
	}

	public synchronized void setParentTransient(String name) {
		realTask.setParentTransient(name);
	}

	public synchronized void addDependentTransient(String name) {
		realTask.addDependentTransient(name);
	}

	public synchronized void deleteDependentTransient(String name) {
		realTask.deleteDependentTransient(name);
	}

	public synchronized void addTriggerTransient(TriggerType trigger) {
		realTask.addTriggerTransient(trigger);
	}

	@Override
	public synchronized PrismContainer<?> getExtension() {
		return realTask.getExtension();
	}

	@Override
	public synchronized Item<?, ?> getExtensionItem(ItemName propertyName) {
		return realTask.getExtensionItem(propertyName);
	}

	public synchronized void setModelOperationContextImmediate(LensContextType value,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		realTask.setModelOperationContextImmediate(value, parentResult);
	}

	public synchronized void setModelOperationContextTransient(LensContextType value) {
		realTask.setModelOperationContextTransient(value);
	}

	public synchronized void setWorkflowContextImmediate(WfContextType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setWorkflowContextImmediate(value, parentResult);
	}

	public synchronized void setWorkflowContextTransient(WfContextType value) {
		realTask.setWorkflowContextTransient(value);
	}

	public synchronized void setNode(String value) {
		realTask.setNode(value);
	}

	public synchronized void setNodeImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		realTask.setNodeImmediate(value, parentResult);
	}

	public synchronized void setNodeTransient(String value) {
		realTask.setNodeTransient(value);
	}

	public synchronized void setLastRunStartTimestamp(Long value) {
		realTask.setLastRunStartTimestamp(value);
	}

	public synchronized void setLastRunStartTimestampImmediate(Long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setLastRunStartTimestampImmediate(value, parentResult);
	}

	public synchronized void setLastRunStartTimestampTransient(Long value) {
		realTask.setLastRunStartTimestampTransient(value);
	}

	public synchronized void setLastRunFinishTimestamp(Long value) {
		realTask.setLastRunFinishTimestamp(value);
	}

	public synchronized void setLastRunFinishTimestampImmediate(Long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setLastRunFinishTimestampImmediate(value, parentResult);
	}

	public synchronized void setLastRunFinishTimestampTransient(Long value) {
		realTask.setLastRunFinishTimestampTransient(value);
	}

	public synchronized void setCompletionTimestamp(Long value) {
		realTask.setCompletionTimestamp(value);
	}

	public synchronized void setCompletionTimestampImmediate(Long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setCompletionTimestampImmediate(value, parentResult);
	}

	public synchronized void setCompletionTimestampTransient(Long value) {
		realTask.setCompletionTimestampTransient(value);
	}

	public synchronized TaskHandler getHandler() {
		return realTask.getHandler();
	}

	public synchronized void setCategoryImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		realTask.setCategoryImmediate(value, parentResult);
	}

	public synchronized void setCategoryTransient(String value) {
		realTask.setCategoryTransient(value);
	}

	public synchronized String getCategoryFromHandler() {
		return realTask.getCategoryFromHandler();
	}

	public synchronized boolean stillCanStart() {
		return realTask.stillCanStart();
	}

	public synchronized PrismContext getPrismContext() {
		return realTask.getPrismContext();
	}

	@Deprecated
	public synchronized TaskRunResult waitForSubtasks(Integer interval, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		return realTask.waitForSubtasks(interval, parentResult);
	}

	@Deprecated
	public synchronized TaskRunResult waitForSubtasks(Integer interval,
			Collection<ItemDelta<?, ?>> extensionDeltas,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		return realTask.waitForSubtasks(interval, extensionDeltas, parentResult);
	}

	@Override
	public synchronized List<PrismObject<TaskType>> listPersistentSubtasksRaw(OperationResult parentResult) throws SchemaException {
		return realTask.listPersistentSubtasksRaw(parentResult);
	}

	public synchronized List<PrismObject<TaskType>> listPrerequisiteTasksRaw(OperationResult parentResult) throws SchemaException {
		return realTask.listPrerequisiteTasksRaw(parentResult);
	}

	@NotNull
	@Override
	public synchronized List<Task> listSubtasksInternal(boolean persistentOnly, OperationResult result) throws SchemaException {
		return realTask.listSubtasksInternal(persistentOnly, result);
	}

	@Override
	public synchronized void applyDeltasImmediate(Collection<ItemDelta<?, ?>> itemDeltas,
			OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		realTask.applyDeltasImmediate(itemDeltas, result);
	}

	@NotNull
	public synchronized OperationResult createUnnamedTaskResult() {
		return realTask.createUnnamedTaskResult();
	}

	@Override
	public synchronized void applyModificationsTransient(Collection<ItemDelta<?, ?>> modifications) throws SchemaException {
		realTask.applyModificationsTransient(modifications);
	}

	@Override
	public synchronized void addSubtask(TaskType subtaskBean) {
		realTask.addSubtask(subtaskBean);
	}

	@Override
	public synchronized String getTaskIdentifier() {
		return realTask.getTaskIdentifier();
	}

	@Override
	public synchronized String getOid() {
		return realTask.getOid();
	}

	@Override
	public synchronized PrismObject<UserType> getOwner() {
		return realTask.getOwner();
	}

	@Override
	public synchronized void setOwner(PrismObject<UserType> owner) {
		realTask.setOwner(owner);
	}

	@Override
	public synchronized PolyStringType getName() {
		return realTask.getName();
	}

	@Override
	public synchronized void setName(PolyStringType value) {
		realTask.setName(value);
	}

	@Override
	public synchronized void setName(String value) {
		realTask.setName(value);
	}

	@Override
	public synchronized void setNameImmediate(PolyStringType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		realTask.setNameImmediate(value, parentResult);
	}

	@Override
	public synchronized String getDescription() {
		return realTask.getDescription();
	}

	@Override
	public synchronized void setDescription(String value) {
		realTask.setDescription(value);
	}

	@Override
	public synchronized void setDescriptionImmediate(String value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setDescriptionImmediate(value, parentResult);
	}

	@Override
	public synchronized PolicyRuleType getPolicyRule() {
		return realTask.getPolicyRule();
	}

	@Override
	public synchronized TaskExecutionStatus getExecutionStatus() {
		return realTask.getExecutionStatus();
	}

	@Override
	public synchronized void makeWaiting() {
		realTask.makeWaiting();
	}

	@Override
	public synchronized void makeWaiting(TaskWaitingReason reason) {
		realTask.makeWaiting(reason);
	}

	@Override
	public synchronized void makeWaiting(TaskWaitingReason reason, TaskUnpauseActionType unpauseAction) {
		realTask.makeWaiting(reason, unpauseAction);
	}

	@Override
	public synchronized void makeRunnable() {
		realTask.makeRunnable();
	}

	@Override
	public synchronized void setInitialExecutionStatus(TaskExecutionStatus value) {
		realTask.setInitialExecutionStatus(value);
	}

	@Override
	public synchronized boolean isClosed() {
		return realTask.isClosed();
	}

	@Override
	public synchronized Long getCompletionTimestamp() {
		return realTask.getCompletionTimestamp();
	}

	@Override
	public synchronized TaskWaitingReason getWaitingReason() {
		return realTask.getWaitingReason();
	}

	@Override
	public synchronized String getNode() {
		return realTask.getNode();
	}

	@Override
	public synchronized String getNodeAsObserved() {
		return realTask.getNodeAsObserved();
	}

	@Override
	public synchronized TaskPersistenceStatus getPersistenceStatus() {
		return realTask.getPersistenceStatus();
	}

	@Override
	public synchronized boolean isTransient() {
		return realTask.isTransient();
	}

	@Override
	public synchronized boolean isPersistent() {
		return realTask.isPersistent();
	}

	@Override
	public synchronized TaskRecurrence getRecurrenceStatus() {
		return realTask.getRecurrenceStatus();
	}

	@Override
	public synchronized boolean isSingle() {
		return realTask.isSingle();
	}

	@Override
	public synchronized boolean isCycle() {
		return realTask.isCycle();
	}

	@Override
	public synchronized void makeRecurring(ScheduleType schedule) {
		realTask.makeRecurring(schedule);
	}

	@Override
	public synchronized void makeRecurringSimple(int interval) {
		realTask.makeRecurringSimple(interval);
	}

	@Override
	public synchronized void makeRecurringCron(String cronLikeSpecification) {
		realTask.makeRecurringCron(cronLikeSpecification);
	}

	@Override
	public synchronized void makeSingle() {
		realTask.makeSingle();
	}

	@Override
	public synchronized void makeSingle(ScheduleType schedule) {
		realTask.makeSingle(schedule);
	}

	@Override
	public synchronized TaskExecutionConstraintsType getExecutionConstraints() {
		return realTask.getExecutionConstraints();
	}

	@Override
	public synchronized String getGroup() {
		return realTask.getGroup();
	}

	@NotNull
	@Override
	public synchronized Collection<String> getGroups() {
		return realTask.getGroups();
	}

	@NotNull
	@Override
	public synchronized Map<String, Integer> getGroupsWithLimits() {
		return realTask.getGroupsWithLimits();
	}

	@Override
	public synchronized ScheduleType getSchedule() {
		return realTask.getSchedule();
	}

	@Override
	public synchronized Long getLastRunStartTimestamp() {
		return realTask.getLastRunStartTimestamp();
	}

	@Override
	public synchronized Long getLastRunFinishTimestamp() {
		return realTask.getLastRunFinishTimestamp();
	}

	@Override
	public synchronized Long getNextRunStartTime(OperationResult parentResult) {
		return realTask.getNextRunStartTime(parentResult);
	}

	@Override
	public synchronized ThreadStopActionType getThreadStopAction() {
		return realTask.getThreadStopAction();
	}

	@Override
	public synchronized void setThreadStopAction(ThreadStopActionType value) {
		realTask.setThreadStopAction(value);
	}

	@Override
	public synchronized boolean isResilient() {
		return realTask.isResilient();
	}

	@Override
	public synchronized TaskBinding getBinding() {
		return realTask.getBinding();
	}

	@Override
	public synchronized boolean isTightlyBound() {
		return realTask.isTightlyBound();
	}

	@Override
	public synchronized boolean isLooselyBound() {
		return realTask.isLooselyBound();
	}

	@Override
	public synchronized void setBinding(TaskBinding value) {
		realTask.setBinding(value);
	}

	@Override
	public synchronized void setBindingImmediate(TaskBinding value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setBindingImmediate(value, parentResult);
	}

	@Override
	public synchronized String getHandlerUri() {
		return realTask.getHandlerUri();
	}

	@Override
	public synchronized void setHandlerUri(String value) {
		realTask.setHandlerUri(value);
	}

	@Override
	public synchronized void setHandlerUriImmediate(String value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setHandlerUriImmediate(value, parentResult);
	}

	@Override
	public synchronized UriStack getOtherHandlersUriStack() {
		return realTask.getOtherHandlersUriStack();
	}

	@Override
	public synchronized void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding,
			Collection<ItemDelta<?, ?>> extensionDeltas) {
		realTask.pushHandlerUri(uri, schedule, binding, extensionDeltas);
	}

	@Override
	public synchronized void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, ItemDelta<?, ?> delta) {
		realTask.pushHandlerUri(uri, schedule, binding, delta);
	}

	@Override
	public synchronized void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding) {
		realTask.pushHandlerUri(uri, schedule, binding);
	}

	@Override
	public synchronized void finishHandler(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		realTask.finishHandler(parentResult);
	}

	@Override
	public synchronized String getCategory() {
		return realTask.getCategory();
	}

	@Override
	public synchronized void setCategory(String category) {
		realTask.setCategory(category);
	}

	@Override
	public synchronized <T> PrismProperty<T> getExtensionProperty(QName propertyName) {
		return realTask.getExtensionProperty(propertyName);
	}

	@Override
	public synchronized <T> T getExtensionPropertyRealValue(QName propertyName) {
		return realTask.getExtensionPropertyRealValue(propertyName);
	}

	@Override
	public synchronized <T extends Containerable> T getExtensionContainerRealValue(QName containerName) {
		return realTask.getExtensionContainerRealValue(containerName);
	}

	@Override
	public synchronized PrismReference getExtensionReference(QName name) {
		return realTask.getExtensionReference(name);
	}

	@Override
	public synchronized void setExtensionProperty(PrismProperty<?> property) throws SchemaException {
		realTask.setExtensionProperty(property);
	}

	@Override
	public synchronized void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setExtensionPropertyImmediate(property, parentResult);
	}

	@Override
	public synchronized <T> void setExtensionPropertyValue(QName propertyName, T value) throws SchemaException {
		realTask.setExtensionPropertyValue(propertyName, value);
	}

	@Override
	public synchronized <T> void setExtensionPropertyValueTransient(QName propertyName, T value) throws SchemaException {
		realTask.setExtensionPropertyValueTransient(propertyName, value);
	}

	@Override
	public synchronized void setExtensionReference(PrismReference reference) throws SchemaException {
		realTask.setExtensionReference(reference);
	}

	@Override
	public synchronized <C extends Containerable> void setExtensionContainer(PrismContainer<C> item) throws SchemaException {
		realTask.setExtensionContainer(item);
	}

	@Override
	public synchronized <T extends Containerable> void setExtensionContainerValue(QName containerName, T value) throws SchemaException {
		realTask.setExtensionContainerValue(containerName, value);
	}

	@Override
	public synchronized void setExtensionItem(Item item) throws SchemaException {
		realTask.setExtensionItem(item);
	}

	@Override
	public synchronized void addExtensionProperty(PrismProperty<?> property) throws SchemaException {
		realTask.addExtensionProperty(property);
	}

	@Override
	public synchronized void addExtensionReference(PrismReference reference) throws SchemaException {
		realTask.addExtensionReference(reference);
	}

	@Override
	public synchronized void deleteExtensionProperty(PrismProperty<?> property) throws SchemaException {
		realTask.deleteExtensionProperty(property);
	}

	@Override
	public synchronized void modifyExtension(ItemDelta itemDelta) throws SchemaException {
		realTask.modifyExtension(itemDelta);
	}

	@Override
	public synchronized <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		return realTask.getObject(type, parentResult);
	}

	@Override
	public synchronized ObjectReferenceType getObjectRef() {
		return realTask.getObjectRef();
	}

	@Override
	public synchronized void setObjectRef(ObjectReferenceType objectRef) {
		realTask.setObjectRef(objectRef);
	}

	@Override
	public synchronized void setObjectRef(String oid, QName type) {
		realTask.setObjectRef(oid, type);
	}

	@Override
	public synchronized void setObjectRefImmediate(ObjectReferenceType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		realTask.setObjectRefImmediate(value, parentResult);
	}

	@Override
	public synchronized void setObjectTransient(PrismObject object) {
		realTask.setObjectTransient(object);
	}

	@Override
	public synchronized String getObjectOid() {
		return realTask.getObjectOid();
	}

	@Override
	public synchronized OperationResult getResult() {
		return realTask.getResult();
	}

	@Override
	public synchronized void setResultTransient(OperationResult result) {
		realTask.setResultTransient(result);
	}

	@Override
	public synchronized OperationResultStatusType getResultStatus() {
		return realTask.getResultStatus();
	}

	@Override
	public synchronized void setResult(OperationResult result) {
		realTask.setResult(result);
	}

	@Override
	public synchronized void setResultImmediate(OperationResult result, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setResultImmediate(result, parentResult);
	}

	@Override
	public synchronized long getProgress() {
		return realTask.getProgress();
	}

	@Override
	public synchronized void setProgress(long value) {
		realTask.setProgress(value);
	}

	@Override
	public synchronized void setProgressImmediate(long progress, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setProgressImmediate(progress, parentResult);
	}

	@Override
	public synchronized void setProgressTransient(long value) {
		realTask.setProgressTransient(value);
	}

	@Override
	public synchronized OperationStatsType getStoredOperationStats() {
		return realTask.getStoredOperationStats();
	}

	@Nullable
	@Override
	public synchronized Long getExpectedTotal() {
		return realTask.getExpectedTotal();
	}

	@Override
	public synchronized void setExpectedTotal(Long value) {
		realTask.setExpectedTotal(value);
	}

	@Override
	public synchronized void setExpectedTotalImmediate(Long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setExpectedTotalImmediate(value, parentResult);
	}

	@Override
	public synchronized Task createSubtask() {
		return realTask.createSubtask();
	}

	@Override
	public synchronized String getParent() {
		return realTask.getParent();
	}

	@Override
	public synchronized Task getParentTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
		return realTask.getParentTask(result);
	}

	@NotNull
	@Override
	public synchronized List<Task> listSubtasks(boolean persistentOnly, OperationResult parentResult) throws SchemaException {
		return realTask.listSubtasks(persistentOnly, parentResult);
	}

	@Override
	public synchronized List<Task> listSubtasksDeeply(boolean persistentOnly, OperationResult result) throws SchemaException {
		return realTask.listSubtasksDeeply(persistentOnly, result);
	}

	@Override
	public synchronized List<Task> listDependents(OperationResult result) throws SchemaException, ObjectNotFoundException {
		return realTask.listDependents(result);
	}

	@Override
	public synchronized List<String> getDependents() {
		return realTask.getDependents();
	}

	@Override
	public synchronized void addDependent(String taskIdentifier) {
		realTask.addDependent(taskIdentifier);
	}

	@Override
	public synchronized void deleteDependent(String taskIdentifier) {
		realTask.deleteDependent(taskIdentifier);
	}

	@Override
	public synchronized List<Task> listPrerequisiteTasks(OperationResult parentResult) throws SchemaException {
		return realTask.listPrerequisiteTasks(parentResult);
	}

	@Override
	public synchronized void startWaitingForTasksImmediate(OperationResult result) throws SchemaException, ObjectNotFoundException {
		realTask.startWaitingForTasksImmediate(result);
	}

	@Override
	public synchronized void pushWaitForTasksHandlerUri() {
		realTask.pushWaitForTasksHandlerUri();
	}

	@Override
	public synchronized String getChannel() {
		return realTask.getChannel();
	}

	@Override
	public synchronized void setChannel(String channelUri) {
		realTask.setChannel(channelUri);
	}

	@Override
	public synchronized void setChannelImmediate(String channelUri, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.setChannelImmediate(channelUri, parentResult);
	}

	@Override
	public synchronized PrismObject<UserType> getRequestee() {
		return realTask.getRequestee();
	}

	@Override
	public synchronized void setRequesteeTransient(PrismObject<UserType> user) {
		realTask.setRequesteeTransient(user);
	}

	@Override
	public synchronized LensContextType getModelOperationContext() {
		return realTask.getModelOperationContext();
	}

	@Override
	public synchronized void setModelOperationContext(LensContextType modelOperationContext) throws SchemaException {
		realTask.setModelOperationContext(modelOperationContext);
	}

	@Override
	public synchronized void initializeWorkflowContextImmediate(String processInstanceId, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		realTask.initializeWorkflowContextImmediate(processInstanceId, result);
	}

	@Override
	public synchronized PrismObject<TaskType> getTaskPrismObject() {
		return realTask.getTaskPrismObject().clone();
	}

	@Override
	public synchronized TaskType getTaskType() {
		return realTask.getTaskType().clone();
	}

	@Override
	public synchronized void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		realTask.refresh(parentResult);
	}

	@Override
	public synchronized void addModification(ItemDelta<?, ?> delta) throws SchemaException {
		realTask.addModification(delta);
	}

	@Override
	public synchronized void addModifications(Collection<ItemDelta<?, ?>> deltas) throws SchemaException {
		realTask.addModifications(deltas);
	}

	@Override
	public synchronized void addModificationImmediate(ItemDelta<?, ?> delta, OperationResult parentResult)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		realTask.addModificationImmediate(delta, parentResult);
	}

	@Override
	public synchronized void savePendingModifications(OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		realTask.savePendingModifications(parentResult);
	}

	@Override
	public synchronized Collection<ItemDelta<?, ?>> getPendingModifications() {
		return realTask.getPendingModifications();
	}

	@Override
	public synchronized WfContextType getWorkflowContext() {
		return realTask.getWorkflowContext();
	}

	@Override
	public synchronized void setWorkflowContext(WfContextType context) throws SchemaException {
		realTask.setWorkflowContext(context);
	}

	@Override
	public synchronized void close(OperationResult taskResult, boolean saveState, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		realTask.close(taskResult, saveState, parentResult);
	}

	@Override
	public synchronized TaskWorkManagementType getWorkManagement() {
		return realTask.getWorkManagement();
	}

	@Override
	public synchronized TaskWorkStateType getWorkState() {
		return realTask.getWorkState();
	}

	@Override
	public synchronized TaskKindType getKind() {
		return realTask.getKind();
	}

	@Override
	public synchronized TaskUnpauseActionType getUnpauseAction() {
		return realTask.getUnpauseAction();
	}

	@Override
	public synchronized TaskExecutionStatusType getStateBeforeSuspend() {
		return realTask.getStateBeforeSuspend();
	}

	@Override
	public synchronized boolean isPartitionedMaster() {
		return realTask.isPartitionedMaster();
	}

	@Override
	public synchronized String getExecutionGroup() {
		return realTask.getExecutionGroup();
	}

	@Override
	public synchronized ObjectReferenceType getReference() {
		return realTask.getReference();
	}

	@Override
	public synchronized String getVersion() {
		return realTask.getVersion();
	}

	@Override
	public synchronized Collection<? extends TriggerType> getTriggers() {
		return realTask.getTriggers();
	}

	@Override
	public synchronized Collection<? extends AssignmentType> getAssignments() {
		return realTask.getAssignments();
	}

	@Override
	public synchronized ObjectReferenceType getOwnerRef() {
		return realTask.getOwnerRef();
	}

	@Override
	public synchronized void recordState(String message) {
		realTask.recordState(message);
	}

	@Override
	public synchronized void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
			ProvisioningOperation operation, boolean success, int count, long duration) {
		realTask.recordProvisioningOperation(resourceOid, resourceName, objectClassName, operation, success, count, duration);
	}

	@Override
	public synchronized void recordNotificationOperation(String transportName, boolean success, long duration) {
		realTask.recordNotificationOperation(transportName, success, duration);
	}

	@Override
	public synchronized void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName,
			long duration) {
		realTask.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, duration);
	}

	@Override
	public synchronized void recordIterativeOperationStart(String objectName, String objectDisplayName, QName objectType, String objectOid) {
		realTask.recordIterativeOperationStart(objectName, objectDisplayName, objectType, objectOid);
	}

	@Override
	public synchronized void recordIterativeOperationStart(ShadowType shadow) {
		realTask.recordIterativeOperationStart(shadow);
	}

	@Override
	public synchronized void recordIterativeOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid,
			long started, Throwable exception) {
		realTask.recordIterativeOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception);
	}

	@Override
	public synchronized void recordIterativeOperationEnd(ShadowType shadow, long started, Throwable exception) {
		realTask.recordIterativeOperationEnd(shadow, started, exception);
	}

	@Override
	public synchronized void recordSynchronizationOperationStart(String objectName, String objectDisplayName, QName objectType,
			String objectOid) {
		realTask.recordSynchronizationOperationStart(objectName, objectDisplayName, objectType, objectOid);
	}

	@Override
	public synchronized void recordSynchronizationOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid,
			long started, Throwable exception, SynchronizationInformation.Record originalStateIncrement,
			SynchronizationInformation.Record newStateIncrement) {
		realTask.recordSynchronizationOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception, originalStateIncrement, newStateIncrement);
	}

	@Override
	public synchronized void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
			ChangeType changeType, String channel, Throwable exception) {
		realTask.recordObjectActionExecuted(objectName, objectDisplayName, objectType, objectOid, changeType, channel, exception);
	}

	@Override
	public synchronized void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, Throwable exception) {
		realTask.recordObjectActionExecuted(object, changeType, exception);
	}

	@Override
	public synchronized <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass,
			String defaultOid, ChangeType changeType, String channel, Throwable exception) {
		realTask.recordObjectActionExecuted(object, objectTypeClass, defaultOid, changeType, channel, exception);
	}

	@Override
	public synchronized void markObjectActionExecutedBoundary() {
		realTask.markObjectActionExecutedBoundary();
	}

	@Override
	public synchronized void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
		realTask.resetEnvironmentalPerformanceInformation(value);
	}

	@Override
	public synchronized void resetSynchronizationInformation(SynchronizationInformationType value) {
		realTask.resetSynchronizationInformation(value);
	}

	@Override
	public synchronized void resetIterativeTaskInformation(IterativeTaskInformationType value) {
		realTask.resetIterativeTaskInformation(value);
	}

	@Override
	public synchronized void resetActionsExecutedInformation(ActionsExecutedInformationType value) {
		realTask.resetActionsExecutedInformation(value);
	}

	@NotNull
	@Override
	public synchronized List<String> getLastFailures() {
		return realTask.getLastFailures();
	}

	@Override
	public synchronized String debugDump(int indent) {
		return realTask.debugDump(indent);
	}

	//endregion
}
