/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.EnvironmentalPerformanceInformation;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.statistics.ActionsExecutedInformation;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.handlers.WaitForSubtasksByPollingTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.handlers.WaitForTasksTaskHandler;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.concurrent.Future;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_MODEL_OPERATION_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

/**
 * Implementation of a Task.
 *
 * @see TaskManagerQuartzImpl
 *
 * Target state (not quite reached as for now): Functionality present in Task is related to the
 * data structure describing the task itself, i.e. to the embedded TaskType prism and accompanying data.
 * Everything related to the management of tasks is put into TaskManagerQuartzImpl and its helper classes.
 *
 * @author Radovan Semancik
 * @author Pavol Mederly
 *
 */
public class TaskQuartzImpl implements Task {

	public static final String DOT_INTERFACE = Task.class.getName() + ".";

	private TaskBinding DEFAULT_BINDING_TYPE = TaskBinding.TIGHT;
	private static final int TIGHT_BINDING_INTERVAL_LIMIT = 10;

	public static final long DEFAULT_OPERATION_STATS_UPDATE_INTERVAL = 3000L;

	private Long lastOperationStatsUpdateTimestamp;

	private long operationStatsUpdateInterval = DEFAULT_OPERATION_STATS_UPDATE_INTERVAL;

	private PrismObject<TaskType> taskPrism;

	private PrismObject<UserType> requestee;                                  // temporary information

	private EnvironmentalPerformanceInformation environmentalPerformanceInformation = new EnvironmentalPerformanceInformation();
	private SynchronizationInformation synchronizationInformation;                // has to be explicitly enabled
	private IterativeTaskInformation iterativeTaskInformation;                    // has to be explicitly enabled
	private ActionsExecutedInformation actionsExecutedInformation;            // has to be explicitly enabled

	/**
	 * Lightweight asynchronous subtasks.
	 * Each task here is a LAT, i.e. transient and with assigned lightweight handler.
	 * <p>
	 * This must be synchronized, because interrupt() method uses it.
	 */
	private Set<TaskQuartzImpl> lightweightAsynchronousSubtasks = Collections.synchronizedSet(new HashSet<TaskQuartzImpl>());
	private Task parentForLightweightAsynchronousTask;            // EXPERIMENTAL

	/*
     * Task result is stored here as well as in task prism.
     *
     * This one is the live value of this task's result. All operations working with this task
     * should work with this value. This value is explicitly updated from the value in prism
     * when fetching task from repo (or creating anew), see initializeFromRepo().
     *
     * The value in taskPrism is updated when necessary, e.g. when getting taskPrism
     * (for example, used when persisting task to repo), etc, see the code.
     *
     * Note that this means that we SHOULD NOT get operation result from the prism - we should
     * use task.getResult() instead!
     */
	private OperationResult taskResult;

	/**
	 * Is the task handler allowed to run, or should it stop as soon as possible?
	 */
	private volatile boolean canRun;

	private TaskManagerQuartzImpl taskManager;
	private RepositoryService repositoryService;

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
	 * An indication whether lighweight hander is currently executing or not.
	 * Used for waiting upon its completion (because java.util.concurrent facilities are not able
	 * to show this for cancelled/interrupted tasks).
	 */
	private volatile boolean lightweightHandlerExecuting;

	private static final Trace LOGGER = TraceManager.getTrace(TaskQuartzImpl.class);
	private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

	private TaskQuartzImpl(TaskManagerQuartzImpl taskManager) {
		this.taskManager = taskManager;
		this.canRun = true;
	}

	//region Constructors

	/**
	 * Note: This constructor assumes that the task is transient.
	 *
	 * @param taskManager
	 * @param taskIdentifier
	 * @param operationName  if null, default op. name will be used
	 */
	TaskQuartzImpl(TaskManagerQuartzImpl taskManager, LightweightIdentifier taskIdentifier, String operationName) {
		this(taskManager);
		this.repositoryService = taskManager.getRepositoryService();
		this.taskPrism = createPrism();

		setTaskIdentifier(taskIdentifier.toString());
		setExecutionStatusTransient(TaskExecutionStatus.RUNNABLE);
		setRecurrenceStatusTransient(TaskRecurrence.SINGLE);
		setBindingTransient(DEFAULT_BINDING_TYPE);
		setProgressTransient(0);
		setObjectTransient(null);
		createOrUpdateTaskResult(operationName);

		setDefaults();
	}

	/**
	 * Assumes that the task is persistent
	 *
	 * @param operationName if null, default op. name will be used
	 */
	TaskQuartzImpl(TaskManagerQuartzImpl taskManager, PrismObject<TaskType> taskPrism, RepositoryService repositoryService,
			String operationName) {
		this(taskManager);
		this.repositoryService = repositoryService;
		this.taskPrism = taskPrism;
		createOrUpdateTaskResult(operationName);

		setDefaults();
	}

	/**
	 * Analogous to the previous constructor.
	 *
	 * @param taskPrism
	 */
	private void replaceTaskPrism(PrismObject<TaskType> taskPrism) {
		this.taskPrism = taskPrism;
		updateTaskResult();
		setDefaults();
	}

	private PrismObject<TaskType> createPrism() {
		try {
			return getPrismContext().createObject(TaskType.class);
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	private void setDefaults() {
		if (getBinding() == null) {
			setBindingTransient(DEFAULT_BINDING_TYPE);
		}
	}

	private void updateTaskResult() {
		createOrUpdateTaskResult(null);
	}

	private void createOrUpdateTaskResult(String operationName) {
		OperationResultType resultInPrism = taskPrism.asObjectable().getResult();
		if (resultInPrism == null) {
			if (operationName == null) {
				resultInPrism = new OperationResult(DOT_INTERFACE + "run").createOperationResultType();
			} else {
				resultInPrism = new OperationResult(operationName).createOperationResultType();
			}
			taskPrism.asObjectable().setResult(resultInPrism);
		}
		try {
			taskResult = OperationResult.createOperationResult(resultInPrism);
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}
	//endregion

	public PrismObject<TaskType> getTaskPrismObject() {

		if (taskResult != null) {
			taskPrism.asObjectable().setResult(taskResult.createOperationResultType());
			taskPrism.asObjectable().setResultStatus(taskResult.getStatus().createStatusType());
		}

		return taskPrism;
	}

	@Override
	public TaskType getTaskType() {
		return getTaskPrismObject().asObjectable();
	}

	RepositoryService getRepositoryService() {
		return repositoryService;
	}

	void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@Override
	public boolean isAsynchronous() {
		return getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT
				|| isLightweightAsynchronousTask();     // note: if it has lightweight task handler, it must be transient
	}

	private boolean recreateQuartzTrigger = false;          // whether to recreate quartz trigger on next savePendingModifications and/or synchronizeWithQuartz

	public boolean isRecreateQuartzTrigger() {
		return recreateQuartzTrigger;
	}

	public void setRecreateQuartzTrigger(boolean recreateQuartzTrigger) {
		this.recreateQuartzTrigger = recreateQuartzTrigger;
	}

	@NotNull
	private final Collection<ItemDelta<?, ?>> pendingModifications = new ArrayList<>();

	public void addPendingModification(ItemDelta<?, ?> delta) {
		ItemDelta.merge(pendingModifications, delta);
	}

	@Override
	public void addModification(ItemDelta<?, ?> delta) throws SchemaException {
		addPendingModification(delta);
		delta.applyTo(taskPrism);
	}

	@Override
	public void addModifications(Collection<ItemDelta<?, ?>> deltas) throws SchemaException {
		for (ItemDelta<?, ?> delta : deltas) {
			addPendingModification(delta);
			delta.applyTo(taskPrism);
		}
	}

	@Override
	public void addModificationImmediate(ItemDelta<?, ?> delta, OperationResult parentResult)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		addPendingModification(delta);
		delta.applyTo(taskPrism);
		savePendingModifications(parentResult);
	}

	@Override
	public void savePendingModifications(OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		if (isTransient()) {
			synchronized (pendingModifications) {
				pendingModifications.clear();
			}
			return;
		}
		synchronized (pendingModifications) {        // todo perhaps we should put something like this at more places here...
			if (!pendingModifications.isEmpty()) {

				try {
					repositoryService.modifyObject(TaskType.class, getOid(), pendingModifications, parentResult);
				} finally {     // todo reconsider this (it's not ideal but we need at least to reset pendingModifications to stop repeating applying this change)
					synchronizeWithQuartzIfNeeded(pendingModifications, parentResult);
					pendingModifications.clear();
				}
			}
		}
		if (isRecreateQuartzTrigger()) {
			synchronizeWithQuartz(parentResult);
		}
	}

	@Override
	@NotNull
	public Collection<ItemDelta<?, ?>> getPendingModifications() {
		return pendingModifications;
	}

	public void synchronizeWithQuartz(OperationResult parentResult) {
		taskManager.synchronizeTaskWithQuartz(this, parentResult);
		setRecreateQuartzTrigger(false);
	}

	private static Set<QName> quartzRelatedProperties = new HashSet<>();

	static {
		quartzRelatedProperties.add(TaskType.F_BINDING);
		quartzRelatedProperties.add(TaskType.F_RECURRENCE);
		quartzRelatedProperties.add(TaskType.F_SCHEDULE);
		quartzRelatedProperties.add(TaskType.F_HANDLER_URI);
	}

	private void synchronizeWithQuartzIfNeeded(Collection<ItemDelta<?, ?>> deltas, OperationResult parentResult) {
		if (isRecreateQuartzTrigger()) {
			synchronizeWithQuartz(parentResult);
			return;
		}
		for (ItemDelta<?, ?> delta : deltas) {
			if (delta.getParentPath().isEmpty() && quartzRelatedProperties.contains(delta.getElementName())) {
				synchronizeWithQuartz(parentResult);
				return;
			}
		}
	}

	private void processModificationNow(ItemDelta<?, ?> delta, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		if (delta != null) {
			processModificationsNow(singleton(delta), parentResult);
		}
	}

	private void processModificationsNow(Collection<ItemDelta<?, ?>> deltas, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		repositoryService.modifyObject(TaskType.class, getOid(), deltas, parentResult);
		synchronizeWithQuartzIfNeeded(deltas, parentResult);
	}

	private void processModificationBatched(ItemDelta<?, ?> delta) {
		if (delta != null) {
			addPendingModification(delta);
		}
	}


	/*
	 * Getters and setters
	 * ===================
	 */

	/*
	 * Progress / expectedTotal
	 */

	@Override
	public long getProgress() {
		Long value = taskPrism.getPropertyRealValue(TaskType.F_PROGRESS, Long.class);
		return value != null ? value : 0;
	}

	@Override
	public void setProgress(long value) {
		processModificationBatched(setProgressAndPrepareDelta(value));
	}

	@Override
	public void setProgressImmediate(long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setProgressAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	@Override
	public void setProgressTransient(long value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_PROGRESS, value);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	private PropertyDelta<?> setProgressAndPrepareDelta(long value) {
		setProgressTransient(value);
		return createProgressDelta(value);
	}

	@Nullable
	private PropertyDelta<?> createProgressDelta(long value) {
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_PROGRESS, value) : null;
	}

	@Override
	public OperationStatsType getStoredOperationStats() {
		return taskPrism.asObjectable().getOperationStats();
	}

	public void setOperationStatsTransient(OperationStatsType value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_OPERATION_STATS, value);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	public void setOperationStats(OperationStatsType value) {
		processModificationBatched(setOperationStatsAndPrepareDelta(value));
	}

	private PropertyDelta<?> setOperationStatsAndPrepareDelta(OperationStatsType value) {
		setOperationStatsTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_OPERATION_STATS, value) : null;
	}

	@Override
	@Nullable
	public Long getExpectedTotal() {
		return taskPrism.getPropertyRealValue(TaskType.F_EXPECTED_TOTAL, Long.class);
	}

	@Override
	public void setExpectedTotal(Long value) {
		processModificationBatched(setExpectedTotalAndPrepareDelta(value));
	}

	@Override
	public void setExpectedTotalImmediate(Long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setExpectedTotalAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setExpectedTotalTransient(Long value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_EXPECTED_TOTAL, value);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	private PropertyDelta<?> setExpectedTotalAndPrepareDelta(Long value) {
		setExpectedTotalTransient(value);
		return createExpectedTotalDelta(value);
	}

	@Nullable
	private PropertyDelta<?> createExpectedTotalDelta(Long value) {
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_EXPECTED_TOTAL, value) : null;
	}

	/*
	 * Result
	 *
	 * setters set also result status type!
	 */

	@Override
	public OperationResult getResult() {
		return taskResult;
	}

	@Override
	public void setResult(OperationResult result) {
		processModificationBatched(setResultAndPrepareDelta(result));
		setResultStatusType(result != null ? result.getStatus().createStatusType() : null);
	}

	@Override
	public void setResultImmediate(OperationResult result, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setResultAndPrepareDelta(result), parentResult);
			setResultStatusTypeImmediate(result != null ? result.getStatus().createStatusType() : null, parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void updateStoredTaskResult() throws SchemaException, ObjectNotFoundException {
		setResultImmediate(getResult(), new OperationResult("dummy"));
	}

	public void setResultTransient(OperationResult result) {
		this.taskResult = result;
		this.taskPrism.asObjectable().setResult(result != null ? result.createOperationResultType() : null);
		setResultStatusTypeTransient(result != null ? result.getStatus().createStatusType() : null);
	}

	private PropertyDelta<?> setResultAndPrepareDelta(OperationResult result) {
		setResultTransient(result);
		if (isPersistent()) {
			return PropertyDelta.createReplaceDeltaOrEmptyDelta(taskManager.getTaskObjectDefinition(),
					TaskType.F_RESULT, result != null ? result.createOperationResultType() : null);
		} else {
			return null;
		}
	}

    /*
     *  Result status
     *
     *  We read the status from current 'taskResult', not from prism - to be sure to get the most current value.
     *  However, when updating, we update the result in prism object in order for the result to be stored correctly in
     *  the repo (useful for displaying the result in task list).
     *
     *  So, setting result type to a value that contradicts current taskResult leads to problems.
     *  Anyway, result type should not be set directly, only when updating OperationResult.
     */

	@Override
	public OperationResultStatusType getResultStatus() {
		return taskResult == null ? null : taskResult.getStatus().createStatusType();
	}

	public void setResultStatusType(OperationResultStatusType value) {
		processModificationBatched(setResultStatusTypeAndPrepareDelta(value));
	}

	public void setResultStatusTypeImmediate(OperationResultStatusType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		processModificationNow(setResultStatusTypeAndPrepareDelta(value), parentResult);
	}

	public void setResultStatusTypeTransient(OperationResultStatusType value) {
		taskPrism.asObjectable().setResultStatus(value);
	}

	private PropertyDelta<?> setResultStatusTypeAndPrepareDelta(OperationResultStatusType value) {
		setResultStatusTypeTransient(value);
		if (isPersistent()) {
			return PropertyDelta.createReplaceDeltaOrEmptyDelta(taskManager.getTaskObjectDefinition(),
					TaskType.F_RESULT_STATUS, value);
		} else {
			return null;
		}
	}

    /*
      * Handler URI
      */

	@Override
	public String getHandlerUri() {
		return taskPrism.getPropertyRealValue(TaskType.F_HANDLER_URI, String.class);
	}

	public void setHandlerUriTransient(String handlerUri) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_HANDLER_URI, handlerUri);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	@Override
	public void setHandlerUriImmediate(String value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setHandlerUriAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	@Override
	public void setHandlerUri(String value) {
		processModificationBatched(setHandlerUriAndPrepareDelta(value));
	}

	private PropertyDelta<?> setHandlerUriAndPrepareDelta(String value) {
		setHandlerUriTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_HANDLER_URI, value) : null;
	}


	/*
	 * Other handlers URI stack
	 */

	@Override
	public UriStack getOtherHandlersUriStack() {
		checkHandlerUriConsistency();
		return taskPrism.asObjectable().getOtherHandlersUriStack();
	}

	public void setOtherHandlersUriStackTransient(UriStack value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_OTHER_HANDLERS_URI_STACK, value);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
		checkHandlerUriConsistency();
	}

	public void setOtherHandlersUriStackImmediate(UriStack value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setOtherHandlersUriStackAndPrepareDelta(value), parentResult);
			checkHandlerUriConsistency();
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setOtherHandlersUriStack(UriStack value) {
		processModificationBatched(setOtherHandlersUriStackAndPrepareDelta(value));
		checkHandlerUriConsistency();
	}

	private PropertyDelta<?> setOtherHandlersUriStackAndPrepareDelta(UriStack value) {
		setOtherHandlersUriStackTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_OTHER_HANDLERS_URI_STACK, value) : null;
	}

	private UriStackEntry popFromOtherHandlersUriStack() {

		checkHandlerUriConsistency();

		UriStack stack = taskPrism.getPropertyRealValue(TaskType.F_OTHER_HANDLERS_URI_STACK, UriStack.class);
		// is this a live value or a copy? (should be live)

		if (stack == null || stack.getUriStackEntry().isEmpty())
			throw new IllegalStateException("Couldn't pop from OtherHandlersUriStack, because it is null or empty");
		int last = stack.getUriStackEntry().size() - 1;
		UriStackEntry retval = stack.getUriStackEntry().get(last);
		stack.getUriStackEntry().remove(last);

		//        UriStack stack2 = taskPrism.getPropertyRealValue(TaskType.F_OTHER_HANDLERS_URI_STACK, UriStack.class);
		//        LOGGER.info("Stack size after popping: " + stack.getUriStackEntry().size()
		//                + ", freshly got stack size: " + stack2.getUriStackEntry().size());

		setOtherHandlersUriStack(stack);

		return retval;
	}

	//    @Override
	//    public void pushHandlerUri(String uri) {
	//        pushHandlerUri(uri, null, null);
	//    }
	//
	//    @Override
	//    public void pushHandlerUri(String uri, ScheduleType schedule) {
	//        pushHandlerUri(uri, schedule, null);
	//    }

	@Override
	public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding) {
		pushHandlerUri(uri, schedule, binding, (Collection<ItemDelta<?, ?>>) null);
	}

	@Override
	public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, ItemDelta<?, ?> delta) {
		Collection<ItemDelta<?, ?>> deltas = null;
		if (delta != null) {
			deltas = new ArrayList<>();
			deltas.add(delta);
		}
		pushHandlerUri(uri, schedule, binding, deltas);
	}

	/**
	 * Makes (uri, schedule, binding) the current task properties, and pushes current (uri, schedule, binding, extensionChange)
	 * onto the stack.
	 *
	 * @param uri      New Handler URI
	 * @param schedule New schedule
	 * @param binding  New binding
	 */
	@Override
	public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding,
			Collection<ItemDelta<?, ?>> extensionDeltas) {

		Validate.notNull(uri);
		if (binding == null) {
			binding = bindingFromSchedule(schedule);
		}

		checkHandlerUriConsistency();

		if (this.getHandlerUri() != null) {

			UriStack stack = taskPrism.getPropertyRealValue(TaskType.F_OTHER_HANDLERS_URI_STACK, UriStack.class);
			if (stack == null) {
				stack = new UriStack();
			}

			UriStackEntry use = new UriStackEntry();
			use.setHandlerUri(getHandlerUri());
			use.setRecurrence(getRecurrenceStatus().toTaskType());
			use.setSchedule(getSchedule());
			use.setBinding(getBinding().toTaskType());
			if (extensionDeltas != null) {
				storeExtensionDeltas(use.getExtensionDelta(), extensionDeltas);
			}
			stack.getUriStackEntry().add(use);
			setOtherHandlersUriStack(stack);
		}

		setHandlerUri(uri);
		setSchedule(schedule);
		setRecurrenceStatus(recurrenceFromSchedule(schedule));
		setBinding(binding);

		this.setRecreateQuartzTrigger(true);            // will be applied on modifications save
	}

	public ItemDelta<?, ?> createExtensionDelta(PrismPropertyDefinition definition, Object realValue) {
		PrismProperty<Object> property = (PrismProperty<Object>) definition.instantiate();
		property.setRealValue(realValue);
		//        PropertyDelta propertyDelta = new PropertyDelta(new ItemPath(TaskType.F_EXTENSION, property.getElementName()), definition);
		//        propertyDelta.setValuesToReplace(PrismValue.cloneCollection(property.getValues()));
		return PropertyDelta.createModificationReplaceProperty(new ItemPath(TaskType.F_EXTENSION, property.getElementName()), definition, realValue);
	}

	private void storeExtensionDeltas(List<ItemDeltaType> result, Collection<ItemDelta<?, ?>> extensionDeltas) {

		for (ItemDelta itemDelta : extensionDeltas) {
			Collection<ItemDeltaType> deltaTypes;
			try {
				deltaTypes = DeltaConvertor.toItemDeltaTypes(itemDelta);
			} catch (SchemaException e) {
				throw new SystemException("Unexpected SchemaException when converting extension ItemDelta to ItemDeltaType", e);
			}
			result.addAll(deltaTypes);
		}
	}

	// derives default binding form schedule
	private TaskBinding bindingFromSchedule(ScheduleType schedule) {
		if (schedule == null) {
			return DEFAULT_BINDING_TYPE;
		} else if (schedule.getInterval() != null && schedule.getInterval() != 0) {
			return schedule.getInterval() <= TIGHT_BINDING_INTERVAL_LIMIT ? TaskBinding.TIGHT : TaskBinding.LOOSE;
		} else if (StringUtils.isNotEmpty(schedule.getCronLikePattern())) {
			return TaskBinding.LOOSE;
		} else {
			return DEFAULT_BINDING_TYPE;
		}
	}

	private TaskRecurrence recurrenceFromSchedule(ScheduleType schedule) {
		if (schedule == null) {
			return TaskRecurrence.SINGLE;
		} else if (schedule.getInterval() != null && schedule.getInterval() != 0) {
			return TaskRecurrence.RECURRING;
		} else if (StringUtils.isNotEmpty(schedule.getCronLikePattern())) {
			return TaskRecurrence.RECURRING;
		} else {
			return TaskRecurrence.SINGLE;
		}

	}

	//    @Override
	//    public void replaceCurrentHandlerUri(String newUri, ScheduleType schedule) {
	//
	//        checkHandlerUriConsistency();
	//        setHandlerUri(newUri);
	//        setSchedule(schedule);
	//    }

	@Override
	public void finishHandler(OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {

		// let us drop the current handler URI and nominate the top of the other
		// handlers stack as the current one

		LOGGER.trace("finishHandler called for handler URI {}, task {}", this.getHandlerUri(), this);

		UriStack otherHandlersUriStack = getOtherHandlersUriStack();
		if (otherHandlersUriStack != null && !otherHandlersUriStack.getUriStackEntry().isEmpty()) {
			UriStackEntry use = popFromOtherHandlersUriStack();
			setHandlerUri(use.getHandlerUri());
			setRecurrenceStatus(use.getRecurrence() != null ?
					TaskRecurrence.fromTaskType(use.getRecurrence()) :
					recurrenceFromSchedule(use.getSchedule()));
			setSchedule(use.getSchedule());
			if (use.getBinding() != null) {
				setBinding(TaskBinding.fromTaskType(use.getBinding()));
			} else {
				setBinding(bindingFromSchedule(use.getSchedule()));
			}
			for (ItemDeltaType itemDeltaType : use.getExtensionDelta()) {
				ItemDelta itemDelta = DeltaConvertor
						.createItemDelta(itemDeltaType, TaskType.class, taskManager.getPrismContext());
				LOGGER.trace("Applying ItemDelta to task extension; task = {}; itemDelta = {}", this, itemDelta.debugDump());
				this.modifyExtension(itemDelta);
			}
			this.setRecreateQuartzTrigger(true);
		} else {
			//setHandlerUri(null);                                                  // we want the last handler to remain set so the task can be revived
			taskManager.closeTaskWithoutSavingState(this,
					parentResult);            // as there are no more handlers, let us close this task
		}
		try {
			savePendingModifications(parentResult);
			checkDependentTasksOnClose(parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}

		LOGGER.trace("finishHandler: new current handler uri = {}, new number of handlers = {}", getHandlerUri(),
				getHandlersCount());
	}

	void checkDependentTasksOnClose(OperationResult result) throws SchemaException, ObjectNotFoundException {

		if (getExecutionStatus() != TaskExecutionStatus.CLOSED) {
			return;
		}

		for (Task dependent : listDependents(result)) {
			((TaskQuartzImpl) dependent).checkDependencies(result);
		}
		Task parentTask = getParentTask(result);
		if (parentTask != null) {
			((TaskQuartzImpl) parentTask).checkDependencies(result);
		}
	}

	public void checkDependencies(OperationResult result) throws SchemaException, ObjectNotFoundException {

		if (getExecutionStatus() != TaskExecutionStatus.WAITING || getWaitingReason() != TaskWaitingReason.OTHER_TASKS) {
			return;
		}

		List<Task> dependencies = listSubtasks(result);
		dependencies.addAll(listPrerequisiteTasks(result));

		LOGGER.trace("Checking {} dependencies for waiting task {}", dependencies.size(), this);

		for (Task dependency : dependencies) {
			if (!dependency.isClosed()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Dependency {} of {} is not closed (status = {})",
							dependency, this, dependency.getExecutionStatus());
				}
				return;
			}
		}

		// this could be a bit tricky, taking MID-1683 into account:
		// when a task finishes its execution, we now leave the last handler set
		// however, this applies to executable tasks; for WAITING tasks we can safely expect that if there is a handler
		// on the stack, we can run it (by unpausing the task)
		if (getHandlerUri() != null) {
			LOGGER.trace("All dependencies of {} are closed, unpausing the task (it has a handler defined)", this);
			taskManager.unpauseTask(this, result);
		} else {
			LOGGER.trace("All dependencies of {} are closed, closing the task (it has no handler defined).", this);
			taskManager.closeTask(this, result);
		}

	}

	public int getHandlersCount() {
		checkHandlerUriConsistency();
		int main = getHandlerUri() != null ? 1 : 0;
		int others = getOtherHandlersUriStack() != null ? getOtherHandlersUriStack().getUriStackEntry().size() : 0;
		return main + others;
	}

	private boolean isOtherHandlersUriStackEmpty() {
		UriStack stack = taskPrism.asObjectable().getOtherHandlersUriStack();
		return stack == null || stack.getUriStackEntry().isEmpty();
	}

	private void checkHandlerUriConsistency() {
		if (getHandlerUri() == null && !isOtherHandlersUriStackEmpty())
			throw new IllegalStateException(
					"Handler URI is null but there is at least one 'other' handler (otherHandlerUriStack size = "
							+ getOtherHandlersUriStack().getUriStackEntry().size() + ")");
	}


	/*
	 * Persistence status
	 */

	@Override
	public TaskPersistenceStatus getPersistenceStatus() {
		return StringUtils.isEmpty(getOid()) ? TaskPersistenceStatus.TRANSIENT : TaskPersistenceStatus.PERSISTENT;
	}

	//	public void setPersistenceStatusTransient(TaskPersistenceStatus persistenceStatus) {
	//		this.persistenceStatus = persistenceStatus;
	//	}

	public boolean isPersistent() {
		return getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT;
	}

	@Override
	public boolean isTransient() {
		return getPersistenceStatus() == TaskPersistenceStatus.TRANSIENT;
	}

	/*
	 * Oid
	 */

	@Override
	public String getOid() {
		return taskPrism.getOid();
	}

	public void setOid(String oid) {
		taskPrism.setOid(oid);
	}

	// obviously, there are no "persistent" versions of setOid

	/*
	 * Task identifier (again, without "persistent" versions)
	 */

	@Override
	public String getTaskIdentifier() {
		return taskPrism.getPropertyRealValue(TaskType.F_TASK_IDENTIFIER, String.class);
	}

	private void setTaskIdentifier(String value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_TASK_IDENTIFIER, value);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	/*
	 * Execution status
	 *
	 * IMPORTANT: do not set this attribute explicitly (due to the need of synchronization with Quartz scheduler).
	 * Use task life-cycle methods, like close(), suspendTask(), resumeTask(), and so on.
	 */

	@Override
	public TaskExecutionStatus getExecutionStatus() {
		TaskExecutionStatusType xmlValue = taskPrism
				.getPropertyRealValue(TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.class);
		if (xmlValue == null) {
			return null;
		}
		return TaskExecutionStatus.fromTaskType(xmlValue);
	}

	public void setExecutionStatusTransient(TaskExecutionStatus executionStatus) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_EXECUTION_STATUS, executionStatus.toTaskType());
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	@Override
	public void setInitialExecutionStatus(TaskExecutionStatus value) {
		if (isPersistent()) {
			throw new IllegalStateException("Initial execution state can be set only on transient tasks.");
		}
		taskPrism.asObjectable().setExecutionStatus(value.toTaskType());
	}

	public void setExecutionStatusImmediate(TaskExecutionStatus value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setExecutionStatusAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setExecutionStatus(TaskExecutionStatus value) {
		processModificationBatched(setExecutionStatusAndPrepareDelta(value));
	}

	private PropertyDelta<?> setExecutionStatusAndPrepareDelta(TaskExecutionStatus value) {
		setExecutionStatusTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_EXECUTION_STATUS, value.toTaskType()) : null;
	}

	@Override
	public void makeRunnable() {
		if (!isTransient()) {
			throw new IllegalStateException("makeRunnable can be invoked only on transient tasks; task = " + this);
		}
		setExecutionStatus(TaskExecutionStatus.RUNNABLE);
	}

	@Override
	public void makeWaiting() {
		if (!isTransient()) {
			throw new IllegalStateException("makeWaiting can be invoked only on transient tasks; task = " + this);
		}
		setExecutionStatus(TaskExecutionStatus.WAITING);
	}

	@Override
	public void makeWaiting(TaskWaitingReason reason) {
		makeWaiting();
		setWaitingReason(reason);
	}

	public boolean isClosed() {
		return getExecutionStatus() == TaskExecutionStatus.CLOSED;
	}

  	/*
	 * Waiting reason
	 */

	@Override
	public TaskWaitingReason getWaitingReason() {
		TaskWaitingReasonType xmlValue = taskPrism.asObjectable().getWaitingReason();
		if (xmlValue == null) {
			return null;
		}
		return TaskWaitingReason.fromTaskType(xmlValue);
	}

	public void setWaitingReasonTransient(TaskWaitingReason value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_WAITING_REASON, value.toTaskType());
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	public void setWaitingReason(TaskWaitingReason value) {
		processModificationBatched(setWaitingReasonAndPrepareDelta(value));
	}

	public void setWaitingReasonImmediate(TaskWaitingReason value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setWaitingReasonAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	private PropertyDelta<?> setWaitingReasonAndPrepareDelta(TaskWaitingReason value) {
		setWaitingReasonTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_WAITING_REASON, value.toTaskType()) : null;
	}

	// "safe" method
	@Override
	public void startWaitingForTasksImmediate(OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (getExecutionStatus() != TaskExecutionStatus.WAITING) {
			throw new IllegalStateException(
					"Task that has to start waiting for tasks should be in WAITING state (it is in " + getExecutionStatus()
							+ " now)");
		}
		setWaitingReasonImmediate(TaskWaitingReason.OTHER_TASKS, result);
		checkDependencies(result);
	}

    /*
    * Recurrence status
    */

	public TaskRecurrence getRecurrenceStatus() {
		TaskRecurrenceType xmlValue = taskPrism.getPropertyRealValue(TaskType.F_RECURRENCE, TaskRecurrenceType.class);
		if (xmlValue == null) {
			return null;
		}
		return TaskRecurrence.fromTaskType(xmlValue);
	}

	@Override
	public boolean isSingle() {
		return (getRecurrenceStatus() == TaskRecurrence.SINGLE);
	}

	@Override
	public boolean isCycle() {
		// TODO: binding
		return (getRecurrenceStatus() == TaskRecurrence.RECURRING);
	}

	public void setRecurrenceStatus(TaskRecurrence value) {
		processModificationBatched(setRecurrenceStatusAndPrepareDelta(value));
	}

	public void setRecurrenceStatusImmediate(TaskRecurrence value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setRecurrenceStatusAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setRecurrenceStatusTransient(TaskRecurrence value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_RECURRENCE, value.toTaskType());
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	private PropertyDelta<?> setRecurrenceStatusAndPrepareDelta(TaskRecurrence value) {
		setRecurrenceStatusTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_RECURRENCE, value.toTaskType()) : null;
	}

	@Override
	public void makeSingle() {
		setRecurrenceStatus(TaskRecurrence.SINGLE);
		setSchedule(new ScheduleType());
	}

	@Override
	public void makeSingle(ScheduleType schedule) {
		setRecurrenceStatus(TaskRecurrence.SINGLE);
		setSchedule(schedule);
	}

	@Override
	public void makeRecurring(ScheduleType schedule) {
		setRecurrenceStatus(TaskRecurrence.RECURRING);
		setSchedule(schedule);
	}

	@Override
	public void makeRecurringSimple(int interval) {
		setRecurrenceStatus(TaskRecurrence.RECURRING);

		ScheduleType schedule = new ScheduleType();
		schedule.setInterval(interval);

		setSchedule(schedule);
	}

	@Override
	public void makeRecurringCron(String cronLikeSpecification) {
		setRecurrenceStatus(TaskRecurrence.RECURRING);

		ScheduleType schedule = new ScheduleType();
		schedule.setCronLikePattern(cronLikeSpecification);

		setSchedule(schedule);
	}

	@Override
	public TaskExecutionConstraintsType getExecutionConstraints() {
		return taskPrism.asObjectable().getExecutionConstraints();
	}

	@Override
	public String getGroup() {
		TaskExecutionConstraintsType executionConstraints = getExecutionConstraints();
		return executionConstraints != null ? executionConstraints.getGroup() : null;
	}

	@NotNull
	@Override
	public Collection<String> getGroups() {
		return getGroupsWithLimits().keySet();
	}

	@NotNull
	@Override
	public Map<String, Integer> getGroupsWithLimits() {
		TaskExecutionConstraintsType executionConstraints = getExecutionConstraints();
		if (executionConstraints == null) {
			return emptyMap();
		}
		Map<String, Integer> rv = new HashMap<>();
		if (executionConstraints.getGroup() != null) {
			rv.put(executionConstraints.getGroup(), executionConstraints.getGroupTaskLimit());
		}
		for (TaskExecutionGroupConstraintType sg : executionConstraints.getSecondaryGroup()) {
			if (sg.getGroup() != null) {    // shouldn't occur but it's a user configurable field, so be prepared for the worst
				rv.put(sg.getGroup(), sg.getGroupTaskLimit());
			}
		}
		return rv;
	}

	/*
      * Schedule
      */

	@Override
	public ScheduleType getSchedule() {
		return taskPrism.asObjectable().getSchedule();
	}

	public void setSchedule(ScheduleType value) {
		processModificationBatched(setScheduleAndPrepareDelta(value));
	}

	public void setScheduleImmediate(ScheduleType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setScheduleAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	private void setScheduleTransient(ScheduleType schedule) {
		taskPrism.asObjectable().setSchedule(schedule);
	}

	private PropertyDelta<?> setScheduleAndPrepareDelta(ScheduleType value) {
		setScheduleTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_SCHEDULE, value) : null;
	}

    /*
     * ThreadStopAction
     */

	@Override
	public ThreadStopActionType getThreadStopAction() {
		return taskPrism.asObjectable().getThreadStopAction();
	}

	@Override
	public void setThreadStopAction(ThreadStopActionType value) {
		processModificationBatched(setThreadStopActionAndPrepareDelta(value));
	}

	public void setThreadStopActionImmediate(ThreadStopActionType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setThreadStopActionAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	private void setThreadStopActionTransient(ThreadStopActionType value) {
		taskPrism.asObjectable().setThreadStopAction(value);
	}

	private PropertyDelta<?> setThreadStopActionAndPrepareDelta(ThreadStopActionType value) {
		setThreadStopActionTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_THREAD_STOP_ACTION, value) : null;
	}

	@Override
	public boolean isResilient() {
		ThreadStopActionType tsa = getThreadStopAction();
		return tsa == null || tsa == ThreadStopActionType.RESCHEDULE || tsa == ThreadStopActionType.RESTART;
	}

/*
      * Binding
      */

	@Override
	public TaskBinding getBinding() {
		TaskBindingType xmlValue = taskPrism.getPropertyRealValue(TaskType.F_BINDING, TaskBindingType.class);
		if (xmlValue == null) {
			return null;
		}
		return TaskBinding.fromTaskType(xmlValue);
	}

	@Override
	public boolean isTightlyBound() {
		return getBinding() == TaskBinding.TIGHT;
	}

	@Override
	public boolean isLooselyBound() {
		return getBinding() == TaskBinding.LOOSE;
	}

	@Override
	public void setBinding(TaskBinding value) {
		processModificationBatched(setBindingAndPrepareDelta(value));
	}

	@Override
	public void setBindingImmediate(TaskBinding value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setBindingAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setBindingTransient(TaskBinding value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_BINDING, value.toTaskType());
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	private PropertyDelta<?> setBindingAndPrepareDelta(TaskBinding value) {
		setBindingTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_BINDING, value.toTaskType()) : null;
	}

	/*
	 * Owner
	 */

	@Override
	public PrismObject<UserType> getOwner() {
		PrismReference ownerRef = taskPrism.findReference(TaskType.F_OWNER_REF);
		if (ownerRef == null) {
			return null;
		}
		return ownerRef.getValue().getObject();
	}

	@Override
	public void setOwner(PrismObject<UserType> owner) {
		if (isPersistent()) {
			throw new IllegalStateException("setOwner method can be called only on transient tasks!");
		}

		PrismReference ownerRef;
		try {
			ownerRef = taskPrism.findOrCreateReference(TaskType.F_OWNER_REF);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
		ownerRef.getValue().setObject(owner);
	}

	PrismObject<UserType> resolveOwnerRef(OperationResult result) throws SchemaException {
		PrismReference ownerRef = taskPrism.findReference(TaskType.F_OWNER_REF);
		if (ownerRef == null) {
			throw new SchemaException("Task " + getOid() + " does not have an owner (missing ownerRef)");
		}
		try {
			PrismObject<UserType> owner = repositoryService.getObject(UserType.class, ownerRef.getOid(), null, result);
			ownerRef.getValue().setObject(owner);
			return owner;
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logExceptionAsWarning(LOGGER, "The owner of task {} cannot be found (owner OID: {})", e, getOid(),
					ownerRef.getOid());
			return null;
		}
	}

	@Override
	public String getChannel() {
		return taskPrism.asObjectable().getChannel();
	}

	@Override
	public void setChannel(String value) {
		processModificationBatched(setChannelAndPrepareDelta(value));
	}

	@Override
	public void setChannelImmediate(String value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setChannelAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setChannelTransient(String name) {
		taskPrism.asObjectable().setChannel(name);
	}

	private PropertyDelta<?> setChannelAndPrepareDelta(String value) {
		setChannelTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_CHANNEL, value) : null;
	}

	//    @Override
	//	public String getChannel() {
	//		PrismProperty<String> channelProperty = taskPrism.findProperty(TaskType.F_CHANNEL);
	//		if (channelProperty == null) {
	//			return null;
	//		}
	//		return channelProperty.getRealValue();
	//	}
	//	@Override
	//	public void setChannel(String channelUri) {
	//		// TODO: Is this OK?
	//		PrismProperty<String> channelProperty;
	//		try {
	//			channelProperty = taskPrism.findOrCreateProperty(TaskType.F_CHANNEL);
	//		} catch (SchemaException e) {
	//			// This should not happen
	//			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
	//		}
	//		channelProperty.setRealValue(channelUri);
	//	}

	/*
	 * Object
	 */

	@Override
	public ObjectReferenceType getObjectRef() {
		return taskPrism.asObjectable().getObjectRef();
	}

	@Override
	public void setObjectRef(ObjectReferenceType value) {
		processModificationBatched(setObjectRefAndPrepareDelta(value));
	}

	@Override
	public void setObjectRef(String oid, QName type) {
		ObjectReferenceType objectReferenceType = new ObjectReferenceType();
		objectReferenceType.setOid(oid);
		objectReferenceType.setType(type);
		setObjectRef(objectReferenceType);
	}

	@Override
	public void setObjectRefImmediate(ObjectReferenceType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		processModificationNow(setObjectRefAndPrepareDelta(value), parentResult);
	}

	public void setObjectRefTransient(ObjectReferenceType objectRefType) {
		PrismReference objectRef;
		try {
			objectRef = taskPrism.findOrCreateReference(TaskType.F_OBJECT_REF);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
		objectRef.getValue().setOid(objectRefType.getOid());
		objectRef.getValue().setTargetType(objectRefType.getType());
	}

	private ReferenceDelta setObjectRefAndPrepareDelta(ObjectReferenceType value) {
		setObjectRefTransient(value);

		PrismReferenceValue prismReferenceValue = new PrismReferenceValue();
		prismReferenceValue.setOid(value.getOid());
		prismReferenceValue.setTargetType(value.getType());

		return isPersistent() ? ReferenceDelta.createModificationReplace(TaskType.F_OBJECT_REF,
				taskManager.getTaskObjectDefinition(), prismReferenceValue) : null;
	}

	@Override
	public String getObjectOid() {
		PrismReference objectRef = taskPrism.findReference(TaskType.F_OBJECT_REF);
		if (objectRef == null) {
			return null;
		}
		return objectRef.getValue().getOid();
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {

		// Shortcut
		PrismReference objectRef = taskPrism.findReference(TaskType.F_OBJECT_REF);
		if (objectRef == null) {
			return null;
		}
		if (objectRef.getValue().getObject() != null) {
			PrismObject object = objectRef.getValue().getObject();
			if (object.canRepresent(type)) {
				return (PrismObject<T>) object;
			} else {
				throw new IllegalArgumentException(
						"Requested object type " + type + ", but the type of object in the task is " + object.getClass());
			}
		}

		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "getObject");
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

		try {
			PrismObject<T> object = repositoryService.getObject(type, objectRef.getOid(), null, result);
			objectRef.getValue().setObject(object);
			result.recordSuccess();
			return object;
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Object not found", ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordFatalError("Schema error", ex);
			throw ex;
		}
	}

	@Override
	public void setObjectTransient(PrismObject object) {
		if (object == null) {
			PrismReference objectRef = taskPrism.findReference(TaskType.F_OBJECT_REF);
			if (objectRef != null) {
				taskPrism.getValue().remove(objectRef);
			}
		} else {
			PrismReference objectRef;
			try {
				objectRef = taskPrism.findOrCreateReference(TaskType.F_OBJECT_REF);
			} catch (SchemaException e) {
				// This should not happen
				throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
			}
			objectRef.getValue().setObject(object);
		}
	}

	/*
	 * Name
	 */

	@Override
	public PolyStringType getName() {
		return taskPrism.asObjectable().getName();
	}

	@Override
	public void setName(PolyStringType value) {
		processModificationBatched(setNameAndPrepareDelta(value));
	}

	@Override
	public void setName(String value) {
		processModificationBatched(setNameAndPrepareDelta(new PolyStringType(value)));
	}

	@Override
	public void setNameImmediate(PolyStringType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		processModificationNow(setNameAndPrepareDelta(value), parentResult);
	}

	public void setNameTransient(PolyStringType name) {
		taskPrism.asObjectable().setName(name);
	}

	private PropertyDelta<?> setNameAndPrepareDelta(PolyStringType value) {
		setNameTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_NAME, value.toPolyString()) : null;
	}

    /*
      * Description
      */

	@Override
	public String getDescription() {
		return taskPrism.asObjectable().getDescription();
	}

	@Override
	public void setDescription(String value) {
		processModificationBatched(setDescriptionAndPrepareDelta(value));
	}

	@Override
	public void setDescriptionImmediate(String value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setDescriptionAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setDescriptionTransient(String name) {
		taskPrism.asObjectable().setDescription(name);
	}

	private PropertyDelta<?> setDescriptionAndPrepareDelta(String value) {
		setDescriptionTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_DESCRIPTION, value) : null;
	}

	@Override
	public PolicyRuleType getPolicyRule() {
		return taskPrism.asObjectable().getPolicyRule();
	}

    /*
    * Parent
    */

	@Override
	public String getParent() {
		return taskPrism.asObjectable().getParent();
	}

	@Override
	public Task getParentTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (getParent() == null) {
			return null;
		} else {
			return taskManager.getTaskByIdentifier(getParent(), result);
		}
	}

	@Override
	public Task getParentForLightweightAsynchronousTask() {
		return parentForLightweightAsynchronousTask;
	}

	public void setParent(String value) {
		processModificationBatched(setParentAndPrepareDelta(value));
	}

	public void setParentImmediate(String value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setParentAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setParentTransient(String name) {
		taskPrism.asObjectable().setParent(name);
	}

	private PropertyDelta<?> setParentAndPrepareDelta(String value) {
		setParentTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_PARENT, value) : null;
	}

   /*
    * Dependents
    */

	@Override
	public List<String> getDependents() {
		return taskPrism.asObjectable().getDependent();
	}

	@Override
	public List<Task> listDependents(OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listDependents");
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

		List<Task> dependents = new ArrayList<>(getDependents().size());
		for (String dependentId : getDependents()) {
			try {
				Task dependent = taskManager.getTaskByIdentifier(dependentId, result);
				dependents.add(dependent);
			} catch (ObjectNotFoundException e) {
				LOGGER.trace("Dependent task {} was not found. Probably it was not yet stored to repo; we just ignore it.",
						dependentId);
			}
		}

		result.recordSuccessIfUnknown();
		return dependents;
	}

	@Override
	public void addDependent(String value) {
		processModificationBatched(addDependentAndPrepareDelta(value));
	}

	public void addDependentTransient(String name) {
		taskPrism.asObjectable().getDependent().add(name);
	}

	private PropertyDelta<?> addDependentAndPrepareDelta(String value) {
		addDependentTransient(value);
		return isPersistent() ? PropertyDelta.createAddDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_DEPENDENT, value) : null;
	}

	@Override
	public void deleteDependent(String value) {
		processModificationBatched(deleteDependentAndPrepareDelta(value));
	}

	public void deleteDependentTransient(String name) {
		taskPrism.asObjectable().getDependent().remove(name);
	}

	private PropertyDelta<?> deleteDependentAndPrepareDelta(String value) {
		deleteDependentTransient(value);
		return isPersistent() ? PropertyDelta.createDeleteDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_DEPENDENT, value) : null;
	}

    /*
     *  Trigger
     */

	public void addTriggerTransient(TriggerType trigger) {
		taskPrism.asObjectable().getTrigger().add(trigger);
	}

	private ItemDelta<?, ?> addTriggerAndPrepareDelta(TriggerType trigger) throws SchemaException {
		addTriggerTransient(trigger.clone());
		return isPersistent() ?
				DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
						.item(TaskType.F_TRIGGER).add(trigger.clone())
						.asItemDelta()
				: null;
	}

    /*
     *  Extension
     */

	@Override
	public PrismContainer<?> getExtension() {
		return taskPrism.getExtension();
	}

	@Override
	public <T> PrismProperty<T> getExtensionProperty(QName propertyName) {
		if (getExtension() != null) {
			return getExtension().findProperty(propertyName);
		} else {
			return null;
		}
	}

	@Override
	public <T> T getExtensionPropertyRealValue(QName propertyName) {
		PrismProperty<T> property = getExtensionProperty(propertyName);
		if (property == null || property.isEmpty()) {
			return null;
		} else {
			return property.getRealValue();
		}
	}

	@Override
	public Item<?, ?> getExtensionItem(QName propertyName) {
		if (getExtension() != null) {
			return getExtension().findItem(propertyName);
		} else {
			return null;
		}
	}

	@Override
	public PrismReference getExtensionReference(QName propertyName) {
		Item item = getExtensionItem(propertyName);
		return (PrismReference) item;
	}

	@Override
	public void setExtensionItem(Item item) throws SchemaException {
		if (item instanceof PrismProperty) {
			setExtensionProperty((PrismProperty) item);
		} else if (item instanceof PrismReference) {
			setExtensionReference((PrismReference) item);
		} else if (item instanceof PrismContainer) {
			setExtensionContainer((PrismContainer) item);
		} else {
			throw new IllegalArgumentException("Unknown kind of item: " + (item == null ? "(null)" : item.getClass()));
		}
	}

	@Override
	public void setExtensionProperty(PrismProperty<?> property) throws SchemaException {
		processModificationBatched(setExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
				PrismValue.cloneCollection(property.getValues())));
	}

	@Override
	public void setExtensionReference(PrismReference reference) throws SchemaException {
		processModificationBatched(setExtensionReferenceAndPrepareDelta(reference.getElementName(), reference.getDefinition(),
				PrismValue.cloneCollection(reference.getValues())));
	}

	@Override
	public void addExtensionReference(PrismReference reference) throws SchemaException {
		processModificationBatched(addExtensionReferenceAndPrepareDelta(reference.getElementName(), reference.getDefinition(),
				PrismValue.cloneCollection(reference.getValues())));
	}

	@Override
	public <C extends Containerable> void setExtensionContainer(PrismContainer<C> container) throws SchemaException {
		processModificationBatched(setExtensionContainerAndPrepareDelta(container.getElementName(), container.getDefinition(),
				PrismValue.cloneCollection(container.getValues())));
	}

	// use this method to avoid cloning the value
	@Override
	public <T> void setExtensionPropertyValue(QName propertyName, T value) throws SchemaException {
		PrismPropertyDefinition propertyDef = getPrismContext().getSchemaRegistry()
				.findPropertyDefinitionByElementName(propertyName);
		if (propertyDef == null) {
			throw new SchemaException("Unknown property " + propertyName);
		}

		ArrayList<PrismPropertyValue<T>> values = new ArrayList<>(1);
		if (value != null) {
			values.add(new PrismPropertyValue<>(value));
		}
		processModificationBatched(setExtensionPropertyAndPrepareDelta(propertyName, propertyDef, values));
	}

	@Override
	public <T> void setExtensionPropertyValueTransient(QName propertyName, T value) throws SchemaException {
		PrismPropertyDefinition propertyDef = getPrismContext().getSchemaRegistry()
				.findPropertyDefinitionByElementName(propertyName);
		if (propertyDef == null) {
			throw new SchemaException("Unknown property " + propertyName);
		}
		ArrayList<PrismPropertyValue<T>> values = new ArrayList<>(1);
		if (value != null) {
			values.add(new PrismPropertyValue<>(value));
		}
		ItemDelta delta = new PropertyDelta<>(new ItemPath(TaskType.F_EXTENSION, propertyName), propertyDef, getPrismContext());
		delta.setValuesToReplace(values);

		Collection<ItemDelta<?, ?>> modifications = new ArrayList<>(1);
		modifications.add(delta);
		PropertyDelta.applyTo(modifications, taskPrism);
	}

	// use this method to avoid cloning the value
	@Override
	public <T extends Containerable> void setExtensionContainerValue(QName containerName, T value) throws SchemaException {
		PrismContainerDefinition containerDef = getPrismContext().getSchemaRegistry()
				.findContainerDefinitionByElementName(containerName);
		if (containerDef == null) {
			throw new SchemaException("Unknown container item " + containerName);
		}

		ArrayList<PrismContainerValue<T>> values = new ArrayList<>(1);
		values.add(value.asPrismContainerValue());
		processModificationBatched(setExtensionContainerAndPrepareDelta(containerName, containerDef, values));
	}

	@Override
	public void addExtensionProperty(PrismProperty<?> property) throws SchemaException {
		processModificationBatched(addExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
				PrismValue.cloneCollection(property.getValues())));
	}

	@Override
	public void deleteExtensionProperty(PrismProperty<?> property) throws SchemaException {
		processModificationBatched(deleteExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
				PrismValue.cloneCollection(property.getValues())));
	}

	@Override
	public void modifyExtension(ItemDelta itemDelta) throws SchemaException {
		if (itemDelta.getPath() == null ||
				itemDelta.getPath().first() == null ||
				!TaskType.F_EXTENSION.equals(ItemPath.getName(itemDelta.getPath().first()))) {
			throw new IllegalArgumentException(
					"modifyExtension must modify the Task extension element; however, the path is " + itemDelta.getPath());
		}
		processModificationBatched(modifyExtensionAndPrepareDelta(itemDelta));
	}

	@Override
	public void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
					PrismValue.cloneCollection(property.getValues())), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	private ItemDelta<?, ?> setExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition,
			Collection<? extends PrismPropertyValue> values) throws SchemaException {
		ItemDelta delta = new PropertyDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());
		return setExtensionItemAndPrepareDeltaCommon(delta, values);
	}

	private ItemDelta<?, ?> setExtensionReferenceAndPrepareDelta(QName itemName, PrismReferenceDefinition definition,
			Collection<? extends PrismReferenceValue> values) throws SchemaException {
		ItemDelta delta = new ReferenceDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());
		return setExtensionItemAndPrepareDeltaCommon(delta, values);
	}

	private ItemDelta<?, ?> addExtensionReferenceAndPrepareDelta(QName itemName, PrismReferenceDefinition definition,
			Collection<? extends PrismReferenceValue> values) throws SchemaException {
		ItemDelta delta = new ReferenceDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());
		return addExtensionItemAndPrepareDeltaCommon(delta, values);
	}

	private ItemDelta<?, ?> setExtensionContainerAndPrepareDelta(QName itemName, PrismContainerDefinition definition,
			Collection<? extends PrismContainerValue> values) throws SchemaException {
		ItemDelta delta = new ContainerDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());
		return setExtensionItemAndPrepareDeltaCommon(delta, values);
	}

	private <V extends PrismValue> ItemDelta<?, ?> setExtensionItemAndPrepareDeltaCommon(ItemDelta delta, Collection<V> values)
			throws SchemaException {

		// these values should have no parent, otherwise the following will fail
		delta.setValuesToReplace(values);

		Collection<ItemDelta<?, ?>> modifications = new ArrayList<>(1);
		modifications.add(delta);
		PropertyDelta.applyTo(modifications, taskPrism);        // i.e. here we apply changes only locally (in memory)

		return isPersistent() ? delta : null;
	}

	private <V extends PrismValue> ItemDelta<?, ?> addExtensionItemAndPrepareDeltaCommon(ItemDelta delta, Collection<V> values)
			throws SchemaException {

		// these values should have no parent, otherwise the following will fail
		delta.addValuesToAdd(values);

		Collection<ItemDelta<?, ?>> modifications = new ArrayList<>(1);
		modifications.add(delta);
		PropertyDelta.applyTo(modifications, taskPrism);        // i.e. here we apply changes only locally (in memory)

		return isPersistent() ? delta : null;
	}

	private ItemDelta<?, ?> modifyExtensionAndPrepareDelta(ItemDelta<?, ?> delta) throws SchemaException {

		Collection<ItemDelta<?, ?>> modifications = new ArrayList<>(1);
		modifications.add(delta);
		PropertyDelta.applyTo(modifications, taskPrism);        // i.e. here we apply changes only locally (in memory)

		return isPersistent() ? delta : null;
	}

	private ItemDelta<?, ?> addExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition,
			Collection<? extends PrismPropertyValue> values) throws SchemaException {
		ItemDelta delta = new PropertyDelta<>(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());

		delta.addValuesToAdd(values);

		Collection<ItemDelta<?, ?>> modifications = new ArrayList<>(1);
		modifications.add(delta);
		PropertyDelta.applyTo(modifications, taskPrism);        // i.e. here we apply changes only locally (in memory)

		return isPersistent() ? delta : null;
	}

	private ItemDelta<?, ?> deleteExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition,
			Collection<? extends PrismPropertyValue> values) throws SchemaException {
		ItemDelta delta = new PropertyDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());

		delta.addValuesToDelete(values);

		Collection<ItemDelta<?, ?>> modifications = new ArrayList<>(1);
		modifications.add(delta);
		PropertyDelta.applyTo(modifications, taskPrism);        // i.e. here we apply changes only locally (in memory)

		return isPersistent() ? delta : null;
	}

    /*
     * Requestee
     */

	@Override
	public PrismObject<UserType> getRequestee() {
		return requestee;
	}

	@Override
	public void setRequesteeTransient(PrismObject<UserType> user) {
		requestee = user;
	}

     /*
      * Model operation context
      */

	@Override
	public LensContextType getModelOperationContext() {
		return taskPrism.asObjectable().getModelOperationContext();
	}

	@Override
	public void setModelOperationContext(LensContextType value) throws SchemaException {
		processModificationBatched(setModelOperationContextAndPrepareDelta(value));
	}

	//@Override
	public void setModelOperationContextImmediate(LensContextType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setModelOperationContextAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setModelOperationContextTransient(LensContextType value) {
		taskPrism.asObjectable().setModelOperationContext(value);
	}

	private ItemDelta<?, ?> setModelOperationContextAndPrepareDelta(LensContextType value)
			throws SchemaException {
		setModelOperationContextTransient(value);
		if (!isPersistent()) {
			return null;
		}
		if (value != null) {
			return DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
					.item(F_MODEL_OPERATION_CONTEXT).replace(value.asPrismContainerValue().clone())
					.asItemDelta();
		} else {
			return DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
					.item(F_MODEL_OPERATION_CONTEXT).replace()
					.asItemDelta();
		}
	}

	/*
	 *  Workflow context
	 */

	public void setWorkflowContext(WfContextType value) throws SchemaException {
		processModificationBatched(setWorkflowContextAndPrepareDelta(value));
	}

	//@Override
	public void setWorkflowContextImmediate(WfContextType value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setWorkflowContextAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setWorkflowContextTransient(WfContextType value) {
		taskPrism.asObjectable().setWorkflowContext(value);
	}

	private ItemDelta<?, ?> setWorkflowContextAndPrepareDelta(WfContextType value) throws SchemaException {
		setWorkflowContextTransient(value);
		if (!isPersistent()) {
			return null;
		}
		if (value != null) {
			return DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
					.item(F_WORKFLOW_CONTEXT).replace(value.asPrismContainerValue().clone())
					.asItemDelta();
		} else {
			return DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
					.item(F_WORKFLOW_CONTEXT).replace()
					.asItemDelta();
		}
	}

	@Override
	public WfContextType getWorkflowContext() {
		return taskPrism.asObjectable().getWorkflowContext();
	}

	@Override
	public void initializeWorkflowContextImmediate(String processInstanceId, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		WfContextType wfContextType = new WfContextType(getPrismContext());
		wfContextType.setProcessInstanceId(processInstanceId);
		setWorkflowContextImmediate(wfContextType, result);
	}

	//    @Override
	//    public PrismReference getRequesteeRef() {
	//        return (PrismReference) getExtensionItem(SchemaConstants.C_TASK_REQUESTEE_REF);
	//    }

	//    @Override
	//    public String getRequesteeOid() {
	//        PrismProperty<String> property = (PrismProperty<String>) getExtensionProperty(SchemaConstants.C_TASK_REQUESTEE_OID);
	//        if (property != null) {
	//            return property.getRealValue();
	//        } else {
	//            return null;
	//        }
	//    }

	//    @Override
	//    public void setRequesteeRef(PrismReferenceValue requesteeRef) throws SchemaException {
	//        PrismReferenceDefinition itemDefinition = new PrismReferenceDefinition(SchemaConstants.C_TASK_REQUESTEE_REF,
	//                SchemaConstants.C_TASK_REQUESTEE_REF, ObjectReferenceType.COMPLEX_TYPE, taskManager.getPrismContext());
	//        itemDefinition.setTargetTypeName(UserType.COMPLEX_TYPE);
	//
	//        PrismReference ref = new PrismReference(SchemaConstants.C_TASK_REQUESTEE_REF);
	//        ref.setDefinition(itemDefinition);
	//        ref.add(requesteeRef);
	//        setExtensionReference(ref);
	//    }

	//    @Override
	//    public void setRequesteeRef(PrismObject<UserType> requestee) throws SchemaException {
	//        Validate.notNull(requestee.getOid());
	//        PrismReferenceValue value = new PrismReferenceValue(requestee.getOid());
	//        value.setTargetType(UserType.COMPLEX_TYPE);
	//        setRequesteeRef(value);
	//    }

	//    @Override
	//    public void setRequesteeOid(String oid) throws SchemaException {
	//        setExtensionProperty(prepareRequesteeProperty(oid));
	//    }

	//    @Override
	//    public void setRequesteeOidImmediate(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
	//        setExtensionPropertyImmediate(prepareRequesteeProperty(oid), result);
	//    }

	//    private PrismProperty<String> prepareRequesteeProperty(String oid) {
	//        PrismPropertyDefinition definition = taskManager.getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.C_TASK_REQUESTEE_OID);
	//        if (definition == null) {
	//            throw new SystemException("No definition for " + SchemaConstants.C_TASK_REQUESTEE_OID);
	//        }
	//        PrismProperty<String> property = definition.instantiate();
	//        property.setRealValue(oid);
	//        return property;
	//    }

    /*
    * Node
    */

	@Override
	public String getNode() {
		return taskPrism.asObjectable().getNode();
	}

	public void setNode(String value) {
		processModificationBatched(setNodeAndPrepareDelta(value));
	}

	public void setNodeImmediate(String value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setNodeAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setNodeTransient(String value) {
		taskPrism.asObjectable().setNode(value);
	}

	private PropertyDelta<?> setNodeAndPrepareDelta(String value) {
		setNodeTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_NODE, value) : null;
	}

	/*
	 * Last run start timestamp
	 */
	@Override
	public Long getLastRunStartTimestamp() {
		XMLGregorianCalendar gc = taskPrism.asObjectable().getLastRunStartTimestamp();
		return gc != null ? XmlTypeConverter.toMillis(gc) : null;
	}

	public void setLastRunStartTimestamp(Long value) {
		processModificationBatched(setLastRunStartTimestampAndPrepareDelta(value));
	}

	public void setLastRunStartTimestampImmediate(Long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setLastRunStartTimestampAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setLastRunStartTimestampTransient(Long value) {
		taskPrism.asObjectable().setLastRunStartTimestamp(
				createXMLGregorianCalendar(value));
	}

	private PropertyDelta<?> setLastRunStartTimestampAndPrepareDelta(Long value) {
		setLastRunStartTimestampTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(),
				TaskType.F_LAST_RUN_START_TIMESTAMP,
				taskPrism.asObjectable().getLastRunStartTimestamp())
				: null;
	}

	/*
	 * Last run finish timestamp
	 */

	@Override
	public Long getLastRunFinishTimestamp() {
		XMLGregorianCalendar gc = taskPrism.asObjectable().getLastRunFinishTimestamp();
		return gc != null ? XmlTypeConverter.toMillis(gc) : null;
	}

	public void setLastRunFinishTimestamp(Long value) {
		processModificationBatched(setLastRunFinishTimestampAndPrepareDelta(value));
	}

	public void setLastRunFinishTimestampImmediate(Long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setLastRunFinishTimestampAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setLastRunFinishTimestampTransient(Long value) {
		taskPrism.asObjectable().setLastRunFinishTimestamp(
				createXMLGregorianCalendar(value));
	}

	private PropertyDelta<?> setLastRunFinishTimestampAndPrepareDelta(Long value) {
		setLastRunFinishTimestampTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(),
				TaskType.F_LAST_RUN_FINISH_TIMESTAMP,
				taskPrism.asObjectable().getLastRunFinishTimestamp())
				: null;
	}

    /*
	 * Completion timestamp
	 */

	@Override
	public Long getCompletionTimestamp() {
		XMLGregorianCalendar gc = taskPrism.asObjectable().getCompletionTimestamp();
		return gc != null ? XmlTypeConverter.toMillis(gc) : null;
	}

	public void setCompletionTimestamp(Long value) {
		processModificationBatched(setCompletionTimestampAndPrepareDelta(value));
	}

	public void setCompletionTimestampImmediate(Long value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setCompletionTimestampAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setCompletionTimestampTransient(Long value) {
		taskPrism.asObjectable().setCompletionTimestamp(
				createXMLGregorianCalendar(value));
	}

	private PropertyDelta<?> setCompletionTimestampAndPrepareDelta(Long value) {
		setCompletionTimestampTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(),
				TaskType.F_COMPLETION_TIMESTAMP,
				taskPrism.asObjectable().getCompletionTimestamp())
				: null;
	}


	/*
	 * Next run start time
	 */

	@Override
	public Long getNextRunStartTime(OperationResult parentResult) {
		return taskManager.getNextRunStartTime(getOid(), parentResult);
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("Task(");
		sb.append(TaskQuartzImpl.class.getName());
		sb.append(")\n");
		sb.append(taskPrism.debugDump(indent + 1));
		sb.append("\n  persistenceStatus: ");
		sb.append(getPersistenceStatus());
		sb.append("\n  result: ");
		if (taskResult == null) {
			sb.append("null");
		} else {
			sb.append(taskResult.debugDump());
		}
		return sb.toString();
	}

    /*
     *  Handler and category
     */

	public TaskHandler getHandler() {
		String handlerUri = taskPrism.asObjectable().getHandlerUri();
		return handlerUri != null ? taskManager.getHandler(handlerUri) : null;
	}

	@Override
	public void setCategory(String value) {
		processModificationBatched(setCategoryAndPrepareDelta(value));
	}

	public void setCategoryImmediate(String value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			processModificationNow(setCategoryAndPrepareDelta(value), parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}

	public void setCategoryTransient(String value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_CATEGORY, value);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
		}
	}

	private PropertyDelta<?> setCategoryAndPrepareDelta(String value) {
		setCategoryTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_CATEGORY, value) : null;
	}

	@Override
	public String getCategory() {
		return taskPrism.asObjectable().getCategory();
	}

	public String getCategoryFromHandler() {
		TaskHandler h = getHandler();
		if (h != null) {
			return h.getCategoryName(this);
		} else {
			return null;
		}
	}

    /*
    *  Other methods
    */

	@Override
	public void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "refresh");
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		if (!isPersistent()) {
			// Nothing to do for transient tasks
			result.recordSuccess();
			return;
		}

		PrismObject<TaskType> repoObj;
		try {
			repoObj = repositoryService.getObject(TaskType.class, getOid(), null, result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Object not found", ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordFatalError("Schema error", ex);
			throw ex;
		}
		updateTaskInstance(repoObj, result);
		result.recordSuccess();
	}

	private void updateTaskInstance(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "updateTaskInstance");
		result.addArbitraryObjectAsParam("task", this);
		result.addParam("taskPrism", taskPrism);

		replaceTaskPrism(taskPrism);
		resolveOwnerRef(result);
		result.recordSuccessIfUnknown();
	}

	//	public void modify(Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
	//		throw new UnsupportedOperationException("Generic task modification is not supported. Please use concrete setter methods to modify a task");
	//		PropertyDelta.applyTo(modifications, taskPrism);
	//		if (isPersistent()) {
	//			getRepositoryService().modifyObject(TaskType.class, getOid(), modifications, parentResult);
	//		}
	//	}

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

	// checks latest start time (useful for recurring tightly coupled tasks)
	public boolean stillCanStart() {
		if (getSchedule() != null && getSchedule().getLatestStartTime() != null) {
			long lst = getSchedule().getLatestStartTime().toGregorianCalendar().getTimeInMillis();
			return lst >= System.currentTimeMillis();
		} else {
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Task(id:" + getTaskIdentifier() + ", name:" + getName() + ", oid:" + getOid() + ")";
	}

	@Override
	public int hashCode() {
		return taskPrism.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskQuartzImpl other = (TaskQuartzImpl) obj;
		//		if (persistenceStatus != other.persistenceStatus)
		//			return false;
		if (taskResult == null) {
			if (other.taskResult != null)
				return false;
		} else if (!taskResult.equals(other.taskResult))
			return false;
		if (taskPrism == null) {
			if (other.taskPrism != null)
				return false;
		} else if (!taskPrism.equals(other.taskPrism))
			return false;
		return true;
	}

	private PrismContext getPrismContext() {
		return taskManager.getPrismContext();
	}

	@Override
	public Task createSubtask() {

		TaskQuartzImpl sub = (TaskQuartzImpl) taskManager.createTaskInstance();
		sub.setParent(this.getTaskIdentifier());
		sub.setOwner(this.getOwner());
		sub.setChannel(this.getChannel());

		//        taskManager.registerTransientSubtask(sub, this);

		LOGGER.trace("New subtask {} has been created.", sub.getTaskIdentifier());
		return sub;
	}

	@Override
	public Task createSubtask(LightweightTaskHandler handler) {
		TaskQuartzImpl sub = ((TaskQuartzImpl) createSubtask());
		sub.setLightweightTaskHandler(handler);
		lightweightAsynchronousSubtasks.add(sub);
		sub.parentForLightweightAsynchronousTask = this;
		return sub;
	}

	@Deprecated
	public TaskRunResult waitForSubtasks(Integer interval, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		return waitForSubtasks(interval, null, parentResult);
	}

	@Deprecated
	public TaskRunResult waitForSubtasks(Integer interval, Collection<ItemDelta<?, ?>> extensionDeltas,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "waitForSubtasks");
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
		result.addContext(OperationResult.CONTEXT_OID, getOid());

		TaskRunResult trr = new TaskRunResult();
		trr.setRunResultStatus(TaskRunResult.TaskRunResultStatus.RESTART_REQUESTED);
		trr.setOperationResult(null);

		ScheduleType schedule = new ScheduleType();
		if (interval != null) {
			schedule.setInterval(interval);
		} else {
			schedule.setInterval(30);
		}
		pushHandlerUri(WaitForSubtasksByPollingTaskHandler.HANDLER_URI, schedule, null, extensionDeltas);
		setBinding(TaskBinding.LOOSE);
		savePendingModifications(result);

		return trr;
	}

	//    @Override
	//    public boolean waitForTransientSubtasks(long timeout, OperationResult parentResult) {
	//        long endTime = System.currentTimeMillis() + timeout;
	//        for (Task t : transientSubtasks) {
	//            TaskQuartzImpl tqi = (TaskQuartzImpl) t;
	//            if (!tqi.lightweightTaskHandlerFinished()) {
	//                long wait = endTime - System.currentTimeMillis();
	//                if (wait <= 0) {
	//                    return false;
	//                }
	//                try {
	//                    tqi.threadForLightweightTaskHandler.join(wait);
	//                } catch (InterruptedException e) {
	//                    return false;
	//                }
	//                if (tqi.threadForLightweightTaskHandler.isAlive()) {
	//                    return false;
	//                }
	//            }
	//        }
	//        LOGGER.trace("All transient subtasks finished for task {}", this);
	//        return true;
	//    }

	public List<PrismObject<TaskType>> listSubtasksRaw(OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listSubtasksRaw");
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

		if (!isPersistent()) {
			result.recordSuccessIfUnknown();
			return new ArrayList<>(0);
		}

		return taskManager.listSubtasksForTask(getTaskIdentifier(), result);
	}

	public List<PrismObject<TaskType>> listPrerequisiteTasksRaw(OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listPrerequisiteTasksRaw");
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

		ObjectQuery query = QueryBuilder.queryFor(TaskType.class, getPrismContext())
				.item(TaskType.F_DEPENDENT).eq(getTaskIdentifier())
				.build();

		List<PrismObject<TaskType>> list = taskManager.getRepositoryService().searchObjects(TaskType.class, query, null, result);
		result.recordSuccessIfUnknown();
		return list;
	}

	@Override
	public List<Task> listSubtasks(OperationResult parentResult) throws SchemaException {

		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listSubtasks");
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

		return listSubtasksInternal(result);
	}

	private List<Task> listSubtasksInternal(OperationResult result) throws SchemaException {
		List<Task> retval = new ArrayList<>();
		// persistent subtasks
		retval.addAll(taskManager.resolveTasksFromTaskTypes(listSubtasksRaw(result), result));
		// transient asynchronous subtasks - must be taken from the running task instance!
		retval.addAll(taskManager.getTransientSubtasks(this));
		return retval;
	}

	@Override
	public List<Task> listSubtasksDeeply(OperationResult parentResult) throws SchemaException {

		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listSubtasksDeeply");
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

		ArrayList<Task> retval = new ArrayList<>();
		addSubtasks(retval, this, result);
		return retval;
	}

	private void addSubtasks(ArrayList<Task> tasks, TaskQuartzImpl taskToProcess, OperationResult result) throws SchemaException {
		for (Task task : taskToProcess.listSubtasksInternal(result)) {
			tasks.add(task);
			addSubtasks(tasks, (TaskQuartzImpl) task, result);
		}
	}

	@Override
	public List<Task> listPrerequisiteTasks(OperationResult parentResult) throws SchemaException {

		OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listPrerequisiteTasks");
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

		return taskManager.resolveTasksFromTaskTypes(listPrerequisiteTasksRaw(result), result);
	}

	//    private List<Task> resolveTasksFromIdentifiers(OperationResult result, List<String> identifiers) throws SchemaException {
	//        List<Task> tasks = new ArrayList<Task>(identifiers.size());
	//        for (String identifier : identifiers) {
	//            tasks.add(taskManager.getTaskByIdentifier(result));
	//        }
	//
	//        result.recordSuccessIfUnknown();
	//        return tasks;
	//    }

	@Override
	public void pushWaitForTasksHandlerUri() {
		pushHandlerUri(WaitForTasksTaskHandler.HANDLER_URI, new ScheduleType(), null);
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
	public Set<? extends TaskQuartzImpl> getLightweightAsynchronousSubtasks() {
		return Collections.unmodifiableSet(lightweightAsynchronousSubtasks);
	}

	@Override
	public Set<? extends TaskQuartzImpl> getRunningLightweightAsynchronousSubtasks() {
		// beware: Do not touch task prism here, because this method can be called asynchronously
		Set<TaskQuartzImpl> retval = new HashSet<>();
		for (TaskQuartzImpl subtask : getLightweightAsynchronousSubtasks()) {
			if (subtask.getExecutionStatus() == TaskExecutionStatus.RUNNABLE && subtask.lightweightHandlerStartRequested()) {
				retval.add(subtask);
			}
		}
		return Collections.unmodifiableSet(retval);
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

	// Operational data

	private EnvironmentalPerformanceInformation getEnvironmentalPerformanceInformation() {
		return environmentalPerformanceInformation;
	}

	private SynchronizationInformation getSynchronizationInformation() {
		return synchronizationInformation;
	}

	private IterativeTaskInformation getIterativeTaskInformation() {
		return iterativeTaskInformation;
	}

	public ActionsExecutedInformation getActionsExecutedInformation() {
		return actionsExecutedInformation;
	}

	@Override
	public OperationStatsType getAggregatedLiveOperationStats() {
		EnvironmentalPerformanceInformationType env = getAggregateEnvironmentalPerformanceInformation();
		IterativeTaskInformationType itit = getAggregateIterativeTaskInformation();
		SynchronizationInformationType sit = getAggregateSynchronizationInformation();
		ActionsExecutedInformationType aeit = getAggregateActionsExecutedInformation();
		if (env == null && itit == null && sit == null && aeit == null) {
			return null;
		}
		OperationStatsType rv = new OperationStatsType();
		rv.setEnvironmentalPerformanceInformation(env);
		rv.setIterativeTaskInformation(itit);
		rv.setSynchronizationInformation(sit);
		rv.setActionsExecutedInformation(aeit);
		rv.setTimestamp(createXMLGregorianCalendar(new Date()));
		return rv;
	}

	@NotNull
	@Override
	public List<String> getLastFailures() {
		return iterativeTaskInformation != null ? iterativeTaskInformation.getLastFailures() : Collections.emptyList();
	}

	private EnvironmentalPerformanceInformationType getAggregateEnvironmentalPerformanceInformation() {
		if (environmentalPerformanceInformation == null) {
			return null;
		}
		EnvironmentalPerformanceInformationType rv = new EnvironmentalPerformanceInformationType();
		EnvironmentalPerformanceInformation.addTo(rv, environmentalPerformanceInformation.getAggregatedValue());
		for (Task subtask : getLightweightAsynchronousSubtasks()) {
			EnvironmentalPerformanceInformation info = ((TaskQuartzImpl) subtask).getEnvironmentalPerformanceInformation();
			if (info != null) {
				EnvironmentalPerformanceInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	private IterativeTaskInformationType getAggregateIterativeTaskInformation() {
		if (iterativeTaskInformation == null) {
			return null;
		}
		IterativeTaskInformationType rv = new IterativeTaskInformationType();
		IterativeTaskInformation.addTo(rv, iterativeTaskInformation.getAggregatedValue(), false);
		for (Task subtask : getLightweightAsynchronousSubtasks()) {
			IterativeTaskInformation info = ((TaskQuartzImpl) subtask).getIterativeTaskInformation();
			if (info != null) {
				IterativeTaskInformation.addTo(rv, info.getAggregatedValue(), false);
			}
		}
		return rv;
	}

	private SynchronizationInformationType getAggregateSynchronizationInformation() {
		if (synchronizationInformation == null) {
			return null;
		}
		SynchronizationInformationType rv = new SynchronizationInformationType();
		SynchronizationInformation.addTo(rv, synchronizationInformation.getAggregatedValue());
		for (Task subtask : getLightweightAsynchronousSubtasks()) {
			SynchronizationInformation info = ((TaskQuartzImpl) subtask).getSynchronizationInformation();
			if (info != null) {
				SynchronizationInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	private ActionsExecutedInformationType getAggregateActionsExecutedInformation() {
		if (actionsExecutedInformation == null) {
			return null;
		}
		ActionsExecutedInformationType rv = new ActionsExecutedInformationType();
		ActionsExecutedInformation.addTo(rv, actionsExecutedInformation.getAggregatedValue());
		for (Task subtask : getLightweightAsynchronousSubtasks()) {
			ActionsExecutedInformation info = ((TaskQuartzImpl) subtask).getActionsExecutedInformation();
			if (info != null) {
				ActionsExecutedInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	@Override
	public void recordState(String message) {
		if (LOGGER.isDebugEnabled()) {        // TODO consider this
			LOGGER.debug("{}", message);
		}
		if (PERFORMANCE_ADVISOR.isDebugEnabled()) {
			PERFORMANCE_ADVISOR.debug("{}", message);
		}
		environmentalPerformanceInformation.recordState(message);
	}

	@Override
	public void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
			ProvisioningOperation operation, boolean success, int count, long duration) {
		environmentalPerformanceInformation
				.recordProvisioningOperation(resourceOid, resourceName, objectClassName, operation, success, count, duration);
	}

	@Override
	public void recordNotificationOperation(String transportName, boolean success, long duration) {
		environmentalPerformanceInformation.recordNotificationOperation(transportName, success, duration);
	}

	@Override
	public void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName,
			long duration) {
		environmentalPerformanceInformation.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, duration);
	}

	@Override
	public synchronized void recordSynchronizationOperationEnd(String objectName, String objectDisplayName, QName objectType,
			String objectOid,
			long started, Throwable exception, SynchronizationInformation.Record originalStateIncrement,
			SynchronizationInformation.Record newStateIncrement) {
		if (synchronizationInformation != null) {
			synchronizationInformation
					.recordSynchronizationOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception,
							originalStateIncrement, newStateIncrement);
		}
	}

	@Override
	public synchronized void recordSynchronizationOperationStart(String objectName, String objectDisplayName, QName objectType,
			String objectOid) {
		if (synchronizationInformation != null) {
			synchronizationInformation.recordSynchronizationOperationStart(objectName, objectDisplayName, objectType, objectOid);
		}
	}

	@Override
	public synchronized void recordIterativeOperationEnd(String objectName, String objectDisplayName, QName objectType,
			String objectOid, long started, Throwable exception) {
		if (iterativeTaskInformation != null) {
			iterativeTaskInformation.recordOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception);
		}
	}

	@Override
	public void recordIterativeOperationEnd(ShadowType shadow, long started, Throwable exception) {
		recordIterativeOperationEnd(PolyString.getOrig(shadow.getName()), StatisticsUtil.getDisplayName(shadow),
				ShadowType.COMPLEX_TYPE, shadow.getOid(), started, exception);
	}

	@Override
	public void recordIterativeOperationStart(ShadowType shadow) {
		recordIterativeOperationStart(PolyString.getOrig(shadow.getName()), StatisticsUtil.getDisplayName(shadow),
				ShadowType.COMPLEX_TYPE, shadow.getOid());
	}

	@Override
	public synchronized void recordIterativeOperationStart(String objectName, String objectDisplayName, QName objectType,
			String objectOid) {
		if (iterativeTaskInformation != null) {
			iterativeTaskInformation.recordOperationStart(objectName, objectDisplayName, objectType, objectOid);
		}
	}

	@Override
	public void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
			ChangeType changeType, String channel, Throwable exception) {
		if (actionsExecutedInformation != null) {
			actionsExecutedInformation
					.recordObjectActionExecuted(objectName, objectDisplayName, objectType, objectOid, changeType, channel,
							exception);
		}
	}

	@Override
	public void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, Throwable exception) {
		recordObjectActionExecuted(object, null, null, changeType, getChannel(), exception);
	}

	@Override
	public <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass,
			String defaultOid, ChangeType changeType, String channel, Throwable exception) {
		if (actionsExecutedInformation != null) {
			String name, displayName, oid;
			PrismObjectDefinition definition;
			Class<T> clazz;
			if (object != null) {
				name = PolyString.getOrig(object.getName());
				displayName = StatisticsUtil.getDisplayName(object);
				definition = object.getDefinition();
				clazz = object.getCompileTimeClass();
				oid = object.getOid();
				if (oid == null) {        // in case of ADD operation
					oid = defaultOid;
				}
			} else {
				name = null;
				displayName = null;
				definition = null;
				clazz = objectTypeClass;
				oid = defaultOid;
			}
			if (definition == null && clazz != null) {
				definition = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
			}
			QName typeQName;
			if (definition != null) {
				typeQName = definition.getTypeName();
			} else {
				typeQName = ObjectType.COMPLEX_TYPE;
			}
			actionsExecutedInformation
					.recordObjectActionExecuted(name, displayName, typeQName, oid, changeType, channel, exception);
		}
	}

	@Override
	public void markObjectActionExecutedBoundary() {
		if (actionsExecutedInformation != null) {
			actionsExecutedInformation.markObjectActionExecutedBoundary();
		}
	}

	@Override
	public void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
		environmentalPerformanceInformation = new EnvironmentalPerformanceInformation(value);
	}

	@Override
	public void resetSynchronizationInformation(SynchronizationInformationType value) {
		synchronizationInformation = new SynchronizationInformation(value);
	}

	@Override
	public void resetIterativeTaskInformation(IterativeTaskInformationType value) {
		iterativeTaskInformation = new IterativeTaskInformation(value);
	}

	@Override
	public void resetActionsExecutedInformation(ActionsExecutedInformationType value) {
		actionsExecutedInformation = new ActionsExecutedInformation(value);
	}

	private void startCollectingOperationStatsFromZero(boolean enableIterationStatistics, boolean enableSynchronizationStatistics,
			boolean enableActionsExecutedStatistics) {
		resetEnvironmentalPerformanceInformation(null);
		if (enableIterationStatistics) {
			resetIterativeTaskInformation(null);
		}
		if (enableSynchronizationStatistics) {
			resetSynchronizationInformation(null);
		}
		if (enableActionsExecutedStatistics) {
			resetActionsExecutedInformation(null);
		}
		setProgress(0);
	}

	private void startCollectingOperationStatsFromStoredValues(boolean enableIterationStatistics,
			boolean enableSynchronizationStatistics, boolean enableActionsExecutedStatistics) {
		OperationStatsType stored = getStoredOperationStats();
		if (stored == null) {
			stored = new OperationStatsType();
		}
		resetEnvironmentalPerformanceInformation(stored.getEnvironmentalPerformanceInformation());
		if (enableIterationStatistics) {
			resetIterativeTaskInformation(stored.getIterativeTaskInformation());
		} else {
			iterativeTaskInformation = null;
		}
		if (enableSynchronizationStatistics) {
			resetSynchronizationInformation(stored.getSynchronizationInformation());
		} else {
			synchronizationInformation = null;
		}
		if (enableActionsExecutedStatistics) {
			resetActionsExecutedInformation(stored.getActionsExecutedInformation());
		} else {
			actionsExecutedInformation = null;
		}
	}

	@Override
	public void startCollectingOperationStats(@NotNull StatisticsCollectionStrategy strategy) {
		if (strategy.isStartFromZero()) {
			startCollectingOperationStatsFromZero(strategy.isMaintainIterationStatistics(), strategy.isMaintainSynchronizationStatistics(), strategy.isMaintainActionsExecutedStatistics());
			storeOperationStats();
		} else {
			startCollectingOperationStatsFromStoredValues(strategy.isMaintainIterationStatistics(), strategy.isMaintainSynchronizationStatistics(), strategy.isMaintainActionsExecutedStatistics());
		}
	}

	@Override
	public void storeOperationStatsDeferred() {
		setOperationStats(getAggregatedLiveOperationStats());
	}

	@Override
	public void storeOperationStats() {
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
	public void storeOperationStatsIfNeeded() {
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
	public void incrementProgressAndStoreStatsIfNeeded() {
		setProgress(getProgress() + 1);
		storeOperationStatsIfNeeded();
	}

	@Override
	public void close(OperationResult taskResult, boolean saveState, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		List<ItemDelta<?, ?>> deltas = new ArrayList<>();
		if (taskResult != null) {
			addIgnoreNull(deltas, setResultAndPrepareDelta(taskResult));
		}
		addIgnoreNull(deltas, setExecutionStatusAndPrepareDelta(TaskExecutionStatus.CLOSED));
		addIgnoreNull(deltas, setCompletionTimestampAndPrepareDelta(System.currentTimeMillis()));
		Duration cleanupAfterCompletion = taskPrism.asObjectable().getCleanupAfterCompletion();
		if (cleanupAfterCompletion != null) {
			TriggerType trigger = new TriggerType(getPrismContext())
					.timestamp(XmlTypeConverter.fromNow(cleanupAfterCompletion))
					.handlerUri(SchemaConstants.COMPLETED_TASK_CLEANUP_TRIGGER_HANDLER_URI);
			addIgnoreNull(deltas, addTriggerAndPrepareDelta(
					trigger));      // we just ignore any other triggers (they will do nothing if launched too early)
		}
		if (saveState) {
			try {
				processModificationsNow(deltas, parentResult);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException(e);
			}
		} else {
			pendingModifications.addAll(deltas);
		}
	}

	@Override
	public AbstractTaskWorkStateManagementConfigurationType getWorkStateManagement() {
		return taskPrism.asObjectable().getWorkStateManagement();
	}

	@Override
	public TaskWorkStateType getWorkState() {
		return taskPrism.asObjectable().getWorkState();
	}
}
