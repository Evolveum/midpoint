/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.task.quartzimpl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.task.api.TaskRecurrence;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UriStack;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * Implementation of a Task.
 * 
 * This is very simplistic now. It does not even serialize itself.
 * 
 * @see TaskManagerQuartzImpl
 * 
 * @author Radovan Semancik
 *
 */
public class TaskQuartzImpl implements Task {
	
	private TaskBinding DEFAULT_BINDING_TYPE = TaskBinding.TIGHT;
	
	private PrismObject<TaskType> taskPrism;
	
	private TaskPersistenceStatus persistenceStatus;
	private TaskManagerQuartzImpl taskManager;
	private RepositoryService repositoryService;
	private OperationResult result;
	private volatile boolean canRun;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(TaskQuartzImpl.class);

	/**
	 * Note: This constructor assumes that the task is transient.
	 * @param taskManager
	 * @param taskIdentifier
	 */	
	TaskQuartzImpl(TaskManagerQuartzImpl taskManager, LightweightIdentifier taskIdentifier) {
		this.taskManager = taskManager;
		this.repositoryService = null;
		this.taskPrism = createPrism();
		this.result = null;
		this.canRun = true;
		
		setTaskIdentifier(taskIdentifier.toString());
		setExecutionStatusTransient(TaskExecutionStatus.RUNNING);
//		setExclusivityStatusTransient(TaskExclusivityStatus.CLAIMED);
		setPersistenceStatusTransient(TaskPersistenceStatus.TRANSIENT);
		setRecurrenceStatusTransient(TaskRecurrence.SINGLE);
		setBindingTransient(DEFAULT_BINDING_TYPE);
		setProgressTransient(0);
		setObjectTransient(null);
		
		setDefaults();
	}

	/**
	 * Assumes that the task is persistent
	 */
	TaskQuartzImpl(TaskManagerQuartzImpl taskManager, PrismObject<TaskType> taskPrism, RepositoryService repositoryService) {
		this.taskManager = taskManager;
		this.repositoryService = repositoryService;
		this.taskPrism = taskPrism;
		canRun = true;
		
		setDefaults();
	}

	private PrismObject<TaskType> createPrism() {
		PrismObjectDefinition<TaskType> taskTypeDef = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
		PrismObject<TaskType> taskPrism = taskTypeDef.instantiate();
		return taskPrism;
	}

	private void setDefaults() {
		if (getBinding() == null)
			setBindingTransient(DEFAULT_BINDING_TYPE);
		
		if (StringUtils.isEmpty(getOid())) {
			persistenceStatus = TaskPersistenceStatus.TRANSIENT;
		} else {
			persistenceStatus = TaskPersistenceStatus.PERSISTENT;
		}
		
		OperationResultType resultType = taskPrism.asObjectable().getResult();
		if (resultType != null) {
			result = OperationResult.createOperationResult(resultType);
		} else {
			result = null;
		}
	}

	void initialize(OperationResult initResult) throws SchemaException {
		resolveOwnerRef(initResult);
	}
	
	@Override
	public PrismObject<TaskType> getTaskPrismObject() {
				
		if (result != null) {
			taskPrism.asObjectable().setResult(result.createOperationResultType());
		}				
		
		return taskPrism;
	}

	RepositoryService getRepositoryService() {
		return repositoryService;
	}
	
	void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}
	

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#isAsynchronous()
	 */
	@Override
	public boolean isAsynchronous() {
		// This is very simple now. It may complicate later.
		return (persistenceStatus==TaskPersistenceStatus.PERSISTENT);
	}
	
	
	
	private Collection<PropertyDelta<?>> pendingModifications = null;
	
	public void addPendingModification(PropertyDelta<?> delta) {
		if (pendingModifications == null)
			pendingModifications = new Vector<PropertyDelta<?>>();
		pendingModifications.add(delta);
	}
	
	@Override
	public void savePendingModifications(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		if (pendingModifications != null) {
			synchronized (pendingModifications) {		// perhaps we should put something like this at more places here...
				if (!pendingModifications.isEmpty()) {

                    taskManager.modifyTaskChecked(getOid(), pendingModifications, parentResult, this);
					//repositoryService.modifyObject(TaskType.class, getOid(), pendingModifications, parentResult);
					addOrReplaceQuartzTaskIfNeeded(pendingModifications);
					pendingModifications.clear();
				}
			}
		}
	}
	
	public void addOrReplaceQuartzTask() {

        LOGGER.trace("addOrReplaceQuartzTask for {}", this);

		JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTask(this);
		JobDetail job = TaskQuartzImplUtil.createJobDetailForTask(this);
		TriggerKey triggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(this);
		Trigger trigger;
		try {
			trigger = TaskQuartzImplUtil.createTriggerForTask(this);
		} catch (ParseException e1) {
			LoggingUtils.logException(LOGGER, "Cannot add/replace a task {} because of a cron expression parsing exception", e1, this);
			throw new SystemException("Cannot add/replace a task because of a cron expression parsing exception", e1);
		}
		
		Scheduler quartzScheduler = taskManager.getQuartzScheduler();
		
		try {

			/*
			 * TODO: this has to be rethought again -- how to map execution states (and their changes) to quartz task/triggers
			 * 
			 * Currently, task in any state maps to a quartz job
			 * However, trigger is created only if task is RUNNING.
			 * 
			 * But, for a task suspended through API, its trigger is not deleted, but paused. 
			 * So a suspended task can have no trigger (if the task was created as suspended), or paused one (if it was suspended after creation).
			 */

			// if the job does not exist, create it (and schedule trigger at the same time, if there is any)
			if (!quartzScheduler.checkExists(jobKey)) {

                LOGGER.trace(" - Quartz job does not exist for {}", this);
				
				if (trigger != null) {
					LOGGER.trace(" - Adding quartz job and trigger for task " + this);
					quartzScheduler.scheduleJob(job, trigger);
				} else {
					LOGGER.trace(" - Adding quartz job (without a trigger) for task " + this);
					quartzScheduler.addJob(job, false);
				}

			} else {  // job exists

                LOGGER.trace(" - Quartz job exists for {}", this);
				
				if (quartzScheduler.checkExists(triggerKey)) {

                    LOGGER.trace(" - Quartz trigger exists for {}", this);
					
					if (trigger == null) {
						LOGGER.trace(" - Removing existing quartz trigger for task " + this);
						quartzScheduler.unscheduleJob(triggerKey);
					} else {
						LOGGER.trace(" - Replacing quartz trigger for task " + this);
						quartzScheduler.rescheduleJob(triggerKey, trigger);
					}
				
				} else {

                    LOGGER.trace(" - Quartz trigger does not exist for {}", this);
					
					if (trigger != null) {
						LOGGER.trace(" - Adding quartz trigger for task " + this);
						quartzScheduler.scheduleJob(trigger);
					}
				}
			}

			
		} catch (SchedulerException e) {
			throw new SystemException("Cannot create or update quartz job and/or trigger for task " + this, e);
		}
	}

	private static Set<QName> quartzRelatedProperties = new HashSet<QName>();
	static {
		quartzRelatedProperties.add(TaskType.F_BINDING);
		quartzRelatedProperties.add(TaskType.F_RECURRENCE);
		quartzRelatedProperties.add(TaskType.F_SCHEDULE);
	}
	
	private void addOrReplaceQuartzTaskIfNeeded(Collection<PropertyDelta<?>> deltas) {
		for (PropertyDelta<?> delta : deltas) {
			if (delta.getParentPath().isEmpty() && quartzRelatedProperties.contains(delta.getName())) {
				addOrReplaceQuartzTask();
				return;
			}
		}
	}

	private void processModificationNow(PropertyDelta<?> delta, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		if (delta != null) {
			Collection<PropertyDelta<?>> deltas = new ArrayList<PropertyDelta<?>>(1);
			deltas.add(delta);
            taskManager.modifyTaskChecked(getOid(), deltas, parentResult, this);
			//repositoryService.modifyObject(TaskType.class, getOid(), deltas, parentResult);
			addOrReplaceQuartzTaskIfNeeded(deltas);
		}
	}

	private void processModificationBatched(PropertyDelta<?> delta) {
		if (delta != null) {
			addPendingModification(delta);
		}
	}


	/*
	 * Getters and setters
	 * ===================
	 */
	
	/*
	 * Progress
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
	public void setProgressImmediate(long value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setProgressAndPrepareDelta(value), result);
	}

	public void setProgressTransient(long value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_PROGRESS, value);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}			
	}
	
	private PropertyDelta<?> setProgressAndPrepareDelta(long value) {
		setProgressTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
				taskManager.getTaskObjectDefinition(), TaskType.F_PROGRESS, value) : null;
	}	

	/*
	 * Result
	 */
	
	@Override
	public OperationResult getResult() {
		return result;
	}

	@Override
	public void setResult(OperationResult result) {
		processModificationBatched(setResultAndPrepareDelta(result));
	}

	@Override
	public void setResultImmediate(OperationResult result, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setResultAndPrepareDelta(result), parentResult);
	}
	
	public void setResultTransient(OperationResult result) {
		this.result = result;
	}
	
	private PropertyDelta<?> setResultAndPrepareDelta(OperationResult result) {
		setResultTransient(result);
		if (isPersistent()) {
			PropertyDelta<?> d = PropertyDelta.createReplaceDeltaOrEmptyDelta(taskManager.getTaskObjectDefinition(), 
						TaskType.F_RESULT, result != null ? result.createOperationResultType() : null);
			LOGGER.trace("setResult delta = " + d.debugDump());
			return d;
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
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}

	@Override
	public void setHandlerUriImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setHandlerUriAndPrepareDelta(value), parentResult);
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
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
		checkHandlerUriConsistency();
	}

	public void setOtherHandlersUriStackImmediate(UriStack value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setOtherHandlersUriStackAndPrepareDelta(value), parentResult);
		checkHandlerUriConsistency();
	}

	public void setOtherHandlersUriStack(UriStack value) {
		processModificationBatched(setOtherHandlersUriStackAndPrepareDelta(value));
		checkHandlerUriConsistency();
	}

	private PropertyDelta<?> setOtherHandlersUriStackAndPrepareDelta(UriStack value) {
		setOtherHandlersUriStackTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_OTHER_HANDLERS_URI_STACK, value) : null;
	}
	
	private String popFromOtherHandlersUriStack() {
		
		checkHandlerUriConsistency();
		
		UriStack stack = taskPrism.getPropertyRealValue(TaskType.F_OTHER_HANDLERS_URI_STACK, UriStack.class);
		if (stack == null || stack.getUri().isEmpty())
			throw new IllegalStateException("Couldn't pop from OtherHandlersUriStack, becaus it is null or empty");
		int last = stack.getUri().size() - 1;
		String retval = stack.getUri().get(last);
		stack.getUri().remove(last);
		setOtherHandlersUriStack(stack);
		
		return retval;
	}
	
	public void pushHandlerUri(String uri) {
		
		checkHandlerUriConsistency();
		
		if (getHandlerUri() == null) {
			setHandlerUri(uri);
		} else {
		
			UriStack stack = taskPrism.getPropertyRealValue(TaskType.F_OTHER_HANDLERS_URI_STACK, UriStack.class);
			if (stack == null)
				stack = new UriStack();
			
			stack.getUri().add(getHandlerUri());
			setHandlerUri(uri);
			setOtherHandlersUriStack(stack);
		}
	}
	
	public void finishHandler(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		// let us drop the current handler URI and nominate the top of the other
		// handlers stack as the current one
		
		UriStack otherHandlersUriStack = getOtherHandlersUriStack();
		if (otherHandlersUriStack != null && !otherHandlersUriStack.getUri().isEmpty()) {
			setHandlerUri(popFromOtherHandlersUriStack());
		} else {
			setHandlerUri(null);
			close();			// if there are no more handlers, let us close this task
		}
		savePendingModifications(parentResult);
		
		LOGGER.trace("finishHandler: new current handler uri = {}, new number of handlers = {}", getHandlerUri(), getHandlersCount());
	}


	public int getHandlersCount() {
		checkHandlerUriConsistency();
		int main = getHandlerUri() != null ? 1 : 0;
		int others = getOtherHandlersUriStack() != null ? getOtherHandlersUriStack().getUri().size() : 0;
		return main + others;
	}
	
	private boolean isOtherHandlersUriStackEmpty() {
		UriStack stack = taskPrism.asObjectable().getOtherHandlersUriStack();
		return stack == null || stack.getUri().isEmpty();
	}
	
	
	private void checkHandlerUriConsistency() {
		if (getHandlerUri() == null && !isOtherHandlersUriStackEmpty())
			throw new IllegalStateException("Handler URI is null but there is at least one 'other' handler (otherHandlerUriStack size = " + getOtherHandlersUriStack().getUri().size() + ")");
	}

	
	/*
	 * Persistence status
	 */

	@Override
	public TaskPersistenceStatus getPersistenceStatus() {
		return persistenceStatus;
	}
	
	public void setPersistenceStatusTransient(TaskPersistenceStatus persistenceStatus) {
		this.persistenceStatus = persistenceStatus;
	}
	
	public boolean isPersistent() {
		return persistenceStatus == TaskPersistenceStatus.PERSISTENT;
	}

	
	// obviously, there are no "persistent" versions of setPersistenceStatus

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
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
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
		TaskExecutionStatusType xmlValue = taskPrism.getPropertyRealValue(TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.class);
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
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}

	public void setExecutionStatusImmediate(TaskExecutionStatus value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setExecutionStatusAndPrepareDelta(value), parentResult);
	}

	public void setExecutionStatus(TaskExecutionStatus value) {
		processModificationBatched(setExecutionStatusAndPrepareDelta(value));
	}

	private PropertyDelta<?> setExecutionStatusAndPrepareDelta(TaskExecutionStatus value) {
		setExecutionStatusTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_EXECUTION_STATUS, value.toTaskType()) : null;
	}
	

	public void close() {
		
		setExecutionStatus(TaskExecutionStatus.CLOSED);
		
		// remove the trigger, if it exists
		Scheduler scheduler = taskManager.getQuartzScheduler();
		TriggerKey triggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(this);
		
		try {
			if (scheduler.checkExists(triggerKey))
				scheduler.unscheduleJob(triggerKey);
		} catch(SchedulerException e) {
			LoggingUtils.logException(LOGGER, "Cannot remove trigger for task {}", e, this);
		}
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

	public void setRecurrenceStatusImmediate(TaskRecurrence value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setRecurrenceStatusAndPrepareDelta(value), parentResult);
	}

	public void setRecurrenceStatusTransient(TaskRecurrence value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_RECURRENCE, value.toTaskType());
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}
	
	private PropertyDelta<?> setRecurrenceStatusAndPrepareDelta(TaskRecurrence value) {
		setRecurrenceStatusTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_RECURRENCE, value.toTaskType()) : null;
	}
	
	public void makeRecurrent(int interval, OperationResult parentResult) throws ObjectNotFoundException, SchemaException 
	{
		setRecurrenceStatus(TaskRecurrence.RECURRING);

		ScheduleType schedule = new ScheduleType();
		schedule.setInterval(interval);
		
		setSchedule(schedule);
		savePendingModifications(parentResult);
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

	public void setScheduleImmediate(ScheduleType value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setScheduleAndPrepareDelta(value), parentResult);
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
	public void setBindingImmediate(TaskBinding value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setBindingAndPrepareDelta(value), parentResult);
	}
	
	public void setBindingTransient(TaskBinding value) {
		try {
			taskPrism.setPropertyRealValue(TaskType.F_BINDING, value.toTaskType());
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}
	
	private PropertyDelta<?> setBindingAndPrepareDelta(TaskBinding value) {
		setBindingTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
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
		PrismReference ownerRef;
		try {
			ownerRef = taskPrism.findOrCreateReference(TaskType.F_OWNER_REF);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
		ownerRef.getValue().setObject(owner);
	}
	
	private PrismObject<UserType> resolveOwnerRef(OperationResult result) throws SchemaException {
		PrismReference ownerRef = taskPrism.findReference(TaskType.F_OWNER_REF);
		if (ownerRef == null) {
			throw new SchemaException("Task "+getOid()+" does not have an owner (missing ownerRef)");
		}
		try {
			return repositoryService.getObject(UserType.class, ownerRef.getOid(), null, result);
		} catch (ObjectNotFoundException e) {
			throw new SystemException("The owner of task "+getOid()+" cannot be found (owner OID: "+ownerRef.getOid()+")",e);
		}
	}
	
	@Override
	public ObjectReferenceType getObjectRef() {
		PrismReference objectRef = taskPrism.findReference(TaskType.F_OBJECT_REF);
		if (objectRef == null) {
			return null;
		}
		ObjectReferenceType objRefType = new ObjectReferenceType();
		objRefType.setOid(objectRef.getOid());
		objRefType.setType(objectRef.getValue().getTargetType());
		return objRefType;
	}
	
	/*
	 * Object
	 */
	
	@Override
	public void setObjectRef(ObjectReferenceType objectRefType) {
		PrismReference objectRef;
		try {
			objectRef = taskPrism.findOrCreateReference(TaskType.F_OBJECT_REF);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
		objectRef.getValue().setOid(objectRefType.getOid());
		objectRef.getValue().setTargetType(objectRefType.getType());
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
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		
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
				throw new IllegalArgumentException("Requested object type "+type+", but the type of object in the task is "+object.getClass());
			}
		}
				
		OperationResult result = parentResult.createSubresult(Task.class.getName()+".getObject");
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
		
		try {
			// Note: storing this value in field, not local variable. It will be reused.
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
	
	private void setObjectTransient(PrismObject object) {
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
				throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
			}
			objectRef.getValue().setObject(object);
		}
	}

	/*
	 * Name
	 */

	@Override
	public String getName() {
		return taskPrism.asObjectable().getName();
	}

	@Override
	public void setName(String value) {
		processModificationBatched(setNameAndPrepareDelta(value));
	}
	
	@Override
	public void setNameImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setNameAndPrepareDelta(value), parentResult);
	}
	
	public void setNameTransient(String name) {
		taskPrism.asObjectable().setName(name);
	}
	
	private PropertyDelta<?> setNameAndPrepareDelta(String value) {
		setNameTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_NAME, value) : null;
	}

	/*
	 * Extension
	 */

	@Override
	public PrismContainer<?> getExtension() {
		return taskPrism.getExtension();
	}
	
	@Override
	public PrismProperty<?> getExtension(QName propertyName) {
		return getExtension().findProperty(propertyName);
	}

	@Override
	public void setExtensionProperty(PrismProperty<?> property) throws SchemaException {
		processModificationBatched(setExtensionPropertyAndPrepareDelta(property));
	}
	
	@Override
	public void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setExtensionPropertyAndPrepareDelta(property), parentResult);
	}
	
	public void setExtensionPropertyTransient(PrismProperty<?> property) throws SchemaException {
		setExtensionPropertyAndPrepareDelta(property);
	}
	
	private PropertyDelta<?> setExtensionPropertyAndPrepareDelta(PrismProperty<?> property) throws SchemaException {
		
        PropertyDelta delta = new PropertyDelta(new PropertyPath(TaskType.F_EXTENSION, property.getName()), property.getDefinition());
        delta.setValuesToReplace(property.getValues());
        
		Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>(1);
		modifications.add(delta);
        PropertyDelta.applyTo(modifications, taskPrism);		// i.e. here we apply changes only locally (in memory)
        
		return isPersistent() ? delta : null;
	}


	
	/*
	 * Last run start timestamp
	 */
	@Override
	public Long getLastRunStartTimestamp() {
		XMLGregorianCalendar gc = taskPrism.asObjectable().getLastRunStartTimestamp();
		return gc != null ? new Long(XmlTypeConverter.toMillis(gc)) : null;
	}
	
	public void setLastRunStartTimestamp(Long value) {
		processModificationBatched(setLastRunStartTimestampAndPrepareDelta(value));
	}

	public void setLastRunStartTimestampImmediate(Long value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setLastRunStartTimestampAndPrepareDelta(value), parentResult);
	}

	public void setLastRunStartTimestampTransient(Long value) {
		taskPrism.asObjectable().setLastRunStartTimestamp(
				XmlTypeConverter.createXMLGregorianCalendar(value));
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
		return gc != null ? new Long(XmlTypeConverter.toMillis(gc)) : null;
	}

	public void setLastRunFinishTimestamp(Long value) {
		processModificationBatched(setLastRunFinishTimestampAndPrepareDelta(value));
	}

	public void setLastRunFinishTimestampImmediate(Long value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setLastRunFinishTimestampAndPrepareDelta(value), parentResult);
	}

	public void setLastRunFinishTimestampTransient(Long value) {
		taskPrism.asObjectable().setLastRunFinishTimestamp(
				XmlTypeConverter.createXMLGregorianCalendar(value));
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
	 * Next run start time
	 */

	@Override
	public Long getNextRunStartTime() {
		return taskManager.getNextRunStartTime(getOid());
	}
	
	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("Task(");
		sb.append(TaskQuartzImpl.class.getName());
		sb.append(")\n");
		sb.append(taskPrism.debugDump(1));
		sb.append("\n  persistenceStatus: ");
		sb.append(persistenceStatus);
		sb.append("\n  result: ");
		if (result==null) {
			sb.append("null");
		} else {
			sb.append(result.dump());
		}
		return sb.toString();
	}

	public void recordRunStart(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		setLastRunStartTimestamp(System.currentTimeMillis());
		savePendingModifications(parentResult);
	}

	public void recordRunFinish(TaskRunResult runResult, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		setProgress(runResult.getProgress()); 
		setLastRunFinishTimestamp(System.currentTimeMillis());
		setResult(runResult.getOperationResult());
		
		savePendingModifications(parentResult);
		
		// TODO: Also save the OpResult (?)
	}
	
	
	@Override
	public void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(Task.class.getName()+".refresh");
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		if (!isPersistent()) {
			// Nothing to do for transient tasks
			result.recordSuccess();
			return;
		}
		
		PrismObject<TaskType> repoObj = null;
		try {
			repoObj = repositoryService.getObject(TaskType.class, getOid(), null, result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Object not found", ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordFatalError("Schema error", ex);
			throw ex;			
		}
		this.taskPrism = repoObj;
		initialize(result);
		result.recordSuccess();
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
	 * 
	 * BEWARE, this method has to be invoked on the same Task instance that is executing.
	 * If called e.g. on a Task just retrieved from the repository, it will have no effect whatsoever.
	 */

	public void signalShutdown() {
		LOGGER.trace("Shutdown signalled for task " + this + " (" + System.identityHashCode(this) + "), setting canRun to false");
		canRun = false;
	}

	@Override
	public boolean canRun() {
		return canRun;
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
		if (persistenceStatus != other.persistenceStatus)
			return false;
		if (result == null) {
			if (other.result != null)
				return false;
		} else if (!result.equals(other.result))
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




}
