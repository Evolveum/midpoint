/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.util.*;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.handlers.WaitForSubtasksByPollingTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.handlers.WaitForTasksTaskHandler;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang.Validate;

/**
 * Implementation of a Task.
 * 
 * @see TaskManagerQuartzImpl
 * 
 * @author Radovan Semancik
 * @author Pavol Mederly
 *
 */
public class TaskQuartzImpl implements Task {

    public static final String DOT_INTERFACE = Task.class.getName() + ".";

    private TaskBinding DEFAULT_BINDING_TYPE = TaskBinding.TIGHT;
    private static final Integer DEFAULT_SUBTASKS_WAIT_INTERVAL = 30;
    private static final int TIGHT_BINDING_INTERVAL_LIMIT = 10;

    private PrismObject<TaskType> taskPrism;

    private PrismObject<UserType> requestee;                                  // temporary information

    // private Set<Task> subtasks = new HashSet<Task>();          // relevant only for transient tasks, currently not used

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

	private volatile boolean canRun;

    private TaskManagerQuartzImpl taskManager;
    private RepositoryService repositoryService;

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskQuartzImpl.class);


    /**
	 * Note: This constructor assumes that the task is transient.
	 * @param taskManager
	 * @param taskIdentifier
     * @param operationName if null, default op. name will be used
	 */	
	TaskQuartzImpl(TaskManagerQuartzImpl taskManager, LightweightIdentifier taskIdentifier, String operationName) {
		this.taskManager = taskManager;
		this.repositoryService = null;
		this.taskPrism = createPrism();
		this.canRun = true;
		
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
     * @param operationName if null, default op. name will be used
     */
	TaskQuartzImpl(TaskManagerQuartzImpl taskManager, PrismObject<TaskType> taskPrism, RepositoryService repositoryService, String operationName) {
		this.taskManager = taskManager;
		this.repositoryService = repositoryService;
		this.taskPrism = taskPrism;
		canRun = true;
        createOrUpdateTaskResult(operationName);

        setDefaults();
    }

    /**
     * Analogous to the previous constructor.
     *
     * @param taskPrism
     */
    void replaceTaskPrism(PrismObject<TaskType> taskPrism) {
        this.taskPrism = taskPrism;
        updateTaskResult();
        setDefaults();
    }

	private PrismObject<TaskType> createPrism() {
		PrismObjectDefinition<TaskType> taskTypeDef = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
		PrismObject<TaskType> taskPrism = taskTypeDef.instantiate();
		return taskPrism;
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
        taskResult = OperationResult.createOperationResult(resultInPrism);
    }

    // called after getObject (within getTask or refresh)
//	void initializeFromRepo(OperationResult initResult) throws SchemaException {
//		resolveOwnerRef(initResult);
//	}
	
	@Override
	public PrismObject<TaskType> getTaskPrismObject() {
				
		if (taskResult != null) {
			taskPrism.asObjectable().setResult(taskResult.createOperationResultType());
            taskPrism.asObjectable().setResultStatus(taskResult.getStatus().createStatusType());
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
		return (getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT);
	}

	private boolean recreateQuartzTrigger = false;          // whether to recreate quartz trigger on next savePendingModifications and/or synchronizeWithQuartz

    public boolean isRecreateQuartzTrigger() {
        return recreateQuartzTrigger;
    }

    public void setRecreateQuartzTrigger(boolean recreateQuartzTrigger) {
        this.recreateQuartzTrigger = recreateQuartzTrigger;
    }

    private Collection<ItemDelta<?>> pendingModifications = null;
	
	public void addPendingModification(ItemDelta<?> delta) {
		if (pendingModifications == null) {
			pendingModifications = new ArrayList<ItemDelta<?>>();
        }
		pendingModifications.add(delta);
	}
	
	@Override
	public void savePendingModifications(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		if (pendingModifications != null) {
			synchronized (pendingModifications) {		// todo perhaps we should put something like this at more places here...
				if (!pendingModifications.isEmpty()) {

                    try {
					    repositoryService.modifyObject(TaskType.class, getOid(), pendingModifications, parentResult);
                    } finally {     // todo reconsider this (it's not ideal but we need at least to reset pendingModifications to stop repeating applying this change)
					    synchronizeWithQuartzIfNeeded(pendingModifications, parentResult);
					    pendingModifications.clear();
                    }
				}
			}
		}
        if (isRecreateQuartzTrigger()) {
            synchronizeWithQuartz(parentResult);
        }
	}

    @Override
    public Collection<ItemDelta<?>> getPendingModifications() {
        return pendingModifications;
    }

    public void synchronizeWithQuartz(OperationResult parentResult) {
        taskManager.synchronizeTaskWithQuartz(this, parentResult);
        setRecreateQuartzTrigger(false);
    }
	
	private static Set<QName> quartzRelatedProperties = new HashSet<QName>();
	static {
		quartzRelatedProperties.add(TaskType.F_BINDING);
		quartzRelatedProperties.add(TaskType.F_RECURRENCE);
		quartzRelatedProperties.add(TaskType.F_SCHEDULE);
        quartzRelatedProperties.add(TaskType.F_HANDLER_URI);
	}
	
	private void synchronizeWithQuartzIfNeeded(Collection<ItemDelta<?>> deltas, OperationResult parentResult) {
        if (isRecreateQuartzTrigger()) {
            synchronizeWithQuartz(parentResult);
            return;
        }
        for (ItemDelta<?> delta : deltas) {
			if (delta.getParentPath().isEmpty() && quartzRelatedProperties.contains(delta.getElementName())) {
				synchronizeWithQuartz(parentResult);
				return;
			}
		}
	}

	private void processModificationNow(ItemDelta<?> delta, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		if (delta != null) {
			Collection<ItemDelta<?>> deltas = new ArrayList<ItemDelta<?>>(1);
			deltas.add(delta);
			repositoryService.modifyObject(TaskType.class, getOid(), deltas, parentResult);
			synchronizeWithQuartzIfNeeded(deltas, parentResult);
		}
	}

	private void processModificationBatched(ItemDelta<?> delta) {
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

    @Override
    public Long getExpectedTotal() {
        Long value = taskPrism.getPropertyRealValue(TaskType.F_EXPECTED_TOTAL, Long.class);
        return value != null ? value : 0;
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
            throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
        }
    }

    private PropertyDelta<?> setExpectedTotalAndPrepareDelta(Long value) {
        setExpectedTotalTransient(value);
        return isPersistent() ? PropertyDelta.createReplaceDelta(
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
	
	public void setResultTransient(OperationResult result) {
		this.taskResult = result;
        this.taskPrism.asObjectable().setResult(result.createOperationResultType());
        setResultStatusTypeTransient(result != null ? result.getStatus().createStatusType() : null);
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
            PropertyDelta<?> d = PropertyDelta.createReplaceDeltaOrEmptyDelta(taskManager.getTaskObjectDefinition(),
                    TaskType.F_RESULT_STATUS, value);
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
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
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
		return isPersistent() ? PropertyDelta.createReplaceDelta(
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
        pushHandlerUri(uri, schedule, binding, (Collection<ItemDelta<?>>) null);
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, ItemDelta<?> delta) {
        Collection<ItemDelta<?>> deltas = null;
        if (delta != null) {
            deltas = new ArrayList<ItemDelta<?>>();
            deltas.add(delta);
        }
        pushHandlerUri(uri, schedule, binding, deltas);
    }

    /**
     * Makes (uri, schedule, binding) the current task properties, and pushes current (uri, schedule, binding, extensionChange)
     * onto the stack.
     *
     * @param uri New Handler URI
     * @param schedule New schedule
     * @param binding New binding
     */
    @Override
	public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, Collection<ItemDelta<?>> extensionDeltas) {

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

    public ItemDelta<?> createExtensionDelta(PrismPropertyDefinition definition, Object realValue) {
        PrismProperty<?> property = (PrismProperty<?>) definition.instantiate();
        property.setRealValue(realValue);
        PropertyDelta propertyDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(TaskType.F_EXTENSION, property.getElementName()), definition, realValue);
//        PropertyDelta propertyDelta = new PropertyDelta(new ItemPath(TaskType.F_EXTENSION, property.getElementName()), definition);
//        propertyDelta.setValuesToReplace(PrismValue.cloneCollection(property.getValues()));
        return propertyDelta;
    }

    private void storeExtensionDeltas(List<ItemDeltaType> result, Collection<ItemDelta<?>> extensionDeltas) {

        for (ItemDelta itemDelta : extensionDeltas) {
            Collection<ItemDeltaType> deltaTypes = null;
            try {
                deltaTypes = DeltaConvertor.toPropertyModificationTypes(itemDelta);
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
            setRecurrenceStatus(use.getRecurrence() != null ? TaskRecurrence.fromTaskType(use.getRecurrence()) : recurrenceFromSchedule(use.getSchedule()));
            setSchedule(use.getSchedule());
            if (use.getBinding() != null) {
                setBinding(TaskBinding.fromTaskType(use.getBinding()));
            } else {
                setBinding(bindingFromSchedule(use.getSchedule()));
            }
            for (ItemDeltaType itemDeltaType : use.getExtensionDelta()) {
                ItemDelta itemDelta = DeltaConvertor.createItemDelta(itemDeltaType, TaskType.class, taskManager.getPrismContext());
                LOGGER.trace("Applying ItemDelta to task extension; task = {}; itemDelta = {}", this, itemDelta.debugDump());
                this.modifyExtension(itemDelta);
            }
            this.setRecreateQuartzTrigger(true);
		} else {
			//setHandlerUri(null);                                                  // we want the last handler to remain set so the task can be revived
			taskManager.closeTaskWithoutSavingState(this, parentResult);			// if there are no more handlers, let us close this task
		}
        try {
		    savePendingModifications(parentResult);
            checkDependentTasksOnClose(parentResult);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
		
		LOGGER.trace("finishHandler: new current handler uri = {}, new number of handlers = {}", getHandlerUri(), getHandlersCount());
	}

    private void checkDependentTasksOnClose(OperationResult result) throws SchemaException, ObjectNotFoundException {

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
                    LOGGER.trace("Dependency {} of {} is not closed (status = {})", new Object[] { dependency, this, dependency.getExecutionStatus() });
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
			throw new IllegalStateException("Handler URI is null but there is at least one 'other' handler (otherHandlerUriStack size = " + getOtherHandlersUriStack().getUriStackEntry().size() + ")");
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
		return isPersistent() ? PropertyDelta.createReplaceDelta(
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
            throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
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
        return isPersistent() ? PropertyDelta.createReplaceDelta(
                taskManager.getTaskObjectDefinition(), TaskType.F_WAITING_REASON, value.toTaskType()) : null;
    }

    // "safe" method
    @Override
    public void startWaitingForTasksImmediate(OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (getExecutionStatus() != TaskExecutionStatus.WAITING) {
            throw new IllegalStateException("Task that has to start waiting for tasks should be in WAITING state (it is in " + getExecutionStatus() + " now)");
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
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}
	
	private PropertyDelta<?> setRecurrenceStatusAndPrepareDelta(TaskRecurrence value) {
		setRecurrenceStatusTransient(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
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
    public void makeRecurring(ScheduleType schedule)
    {
        setRecurrenceStatus(TaskRecurrence.RECURRING);
        setSchedule(schedule);
    }

    @Override
	public void makeRecurringSimple(int interval)
	{
		setRecurrenceStatus(TaskRecurrence.RECURRING);

		ScheduleType schedule = new ScheduleType();
		schedule.setInterval(interval);
		
		setSchedule(schedule);
	}

    @Override
    public void makeRecurringCron(String cronLikeSpecification)
    {
        setRecurrenceStatus(TaskRecurrence.RECURRING);

        ScheduleType schedule = new ScheduleType();
        schedule.setCronLikePattern(cronLikeSpecification);

        setSchedule(schedule);
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
        if (isPersistent()) {
            throw new IllegalStateException("setOwner method can be called only on transient tasks!");
        }

		PrismReference ownerRef;
		try {
			ownerRef = taskPrism.findOrCreateReference(TaskType.F_OWNER_REF);
		} catch (SchemaException e) {
			// This should not happen
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
		ownerRef.getValue().setObject(owner);
	}
	
	PrismObject<UserType> resolveOwnerRef(OperationResult result) throws SchemaException {
		PrismReference ownerRef = taskPrism.findReference(TaskType.F_OWNER_REF);
		if (ownerRef == null) {
			throw new SchemaException("Task "+getOid()+" does not have an owner (missing ownerRef)");
		}
		try {
			PrismObject<UserType> owner = repositoryService.getObject(UserType.class, ownerRef.getOid(), null, result);
            ownerRef.getValue().setObject(owner);
            return owner;
		} catch (ObjectNotFoundException e) {
			LOGGER.warn("The owner of task "+getOid()+" cannot be found (owner OID: "+ownerRef.getOid()+")",e);
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

//    @Override
//    public void setDescriptionImmediate(String value, OperationResult parentResult)
//            throws ObjectNotFoundException, SchemaException {
//        try {
//            processModificationNow(setDescriptionAndPrepareDelta(value), parentResult);
//        } catch (ObjectAlreadyExistsException ex) {
//            throw new SystemException(ex);
//        }
//    }

    public void setChannelTransient(String name) {
        taskPrism.asObjectable().setChannel(name);
    }

    private PropertyDelta<?> setChannelAndPrepareDelta(String value) {
        setChannelTransient(value);
        return isPersistent() ? PropertyDelta.createReplaceDelta(
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
        PrismReference objectRef = taskPrism.findReference(TaskType.F_OBJECT_REF);
        if (objectRef == null) {
            return null;
        }
        ObjectReferenceType objRefType = new ObjectReferenceType();
        objRefType.setOid(objectRef.getOid());
        objRefType.setType(objectRef.getValue().getTargetType());
        return objRefType;
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
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
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
				
		OperationResult result = parentResult.createSubresult(DOT_INTERFACE+"getObject");
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
				throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
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
		return isPersistent() ? PropertyDelta.createReplaceDelta(
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
        return isPersistent() ? PropertyDelta.createReplaceDelta(
                taskManager.getTaskObjectDefinition(), TaskType.F_DESCRIPTION, value) : null;
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
        return isPersistent() ? PropertyDelta.createReplaceDelta(
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

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "listDependents");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        List<Task> dependents = new ArrayList<Task>(getDependents().size());
        for (String dependentId : getDependents()) {
            try {
                Task dependent = taskManager.getTaskByIdentifier(dependentId, result);
                dependents.add(dependent);
            } catch (ObjectNotFoundException e) {
                LOGGER.trace("Dependent task {} was not found. Probably it was not yet stored to repo; we just ignore it.", dependentId);
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
      * Extension
      */

	@Override
	public PrismContainer<?> getExtension() {
		return taskPrism.getExtension();
	}
	
	@Override
	public PrismProperty<?> getExtensionProperty(QName propertyName) {
        if (getExtension() != null) {
		    return getExtension().findProperty(propertyName);
        } else {
            return null;
        }
	}

    @Override
    public Item<?> getExtensionItem(QName propertyName) {
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
		processModificationBatched(setExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(), PrismValue.cloneCollection(property.getValues())));
	}

    @Override
    public void setExtensionReference(PrismReference reference) throws SchemaException {
        processModificationBatched(setExtensionReferenceAndPrepareDelta(reference.getElementName(), reference.getDefinition(), PrismValue.cloneCollection(reference.getValues())));
    }

    @Override
    public void addExtensionReference(PrismReference reference) throws SchemaException {
        processModificationBatched(addExtensionReferenceAndPrepareDelta(reference.getElementName(), reference.getDefinition(), PrismValue.cloneCollection(reference.getValues())));
    }

    @Override
    public <C extends Containerable> void setExtensionContainer(PrismContainer<C> container) throws SchemaException {
        processModificationBatched(setExtensionContainerAndPrepareDelta(container.getElementName(), container.getDefinition(), PrismValue.cloneCollection(container.getValues())));
    }

    // use this method to avoid cloning the value
    @Override
    public <T> void setExtensionPropertyValue(QName propertyName, T value) throws SchemaException {
        PrismPropertyDefinition propertyDef = getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(propertyName);
        if (propertyDef == null) {
            throw new SchemaException("Unknown property " + propertyName);
        }

        ArrayList<PrismPropertyValue<T>> values = new ArrayList(1);
        values.add(new PrismPropertyValue<T>(value));
        processModificationBatched(setExtensionPropertyAndPrepareDelta(propertyName, propertyDef, values));
    }

    // use this method to avoid cloning the value
    @Override
    public <T extends Containerable> void setExtensionContainerValue(QName containerName, T value) throws SchemaException {
        PrismContainerDefinition containerDef = getPrismContext().getSchemaRegistry().findContainerDefinitionByElementName(containerName);
        if (containerDef == null) {
            throw new SchemaException("Unknown container item " + containerName);
        }

        ArrayList<PrismContainerValue<T>> values = new ArrayList(1);
        values.add(value.asPrismContainerValue());
        processModificationBatched(setExtensionContainerAndPrepareDelta(containerName, containerDef, values));
    }

    @Override
    public void addExtensionProperty(PrismProperty<?> property) throws SchemaException {
        processModificationBatched(addExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(), PrismValue.cloneCollection(property.getValues())));
    }

    @Override
    public void deleteExtensionProperty(PrismProperty<?> property) throws SchemaException {
        processModificationBatched(deleteExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(), PrismValue.cloneCollection(property.getValues())));
    }

    @Override
    public void modifyExtension(ItemDelta itemDelta) throws SchemaException {
        if (itemDelta.getPath() == null ||
                itemDelta.getPath().first() == null ||
                !TaskType.F_EXTENSION.equals(ItemPath.getName(itemDelta.getPath().first()))) {
            throw new IllegalArgumentException("modifyExtension must modify the Task extension element; however, the path is " + itemDelta.getPath());
        }
        processModificationBatched(modifyExtensionAndPrepareDelta(itemDelta));
    }

    @Override
	public void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        try {
		    processModificationNow(setExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(), PrismValue.cloneCollection(property.getValues())), parentResult);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
	}
	
    private ItemDelta<?> setExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition, Collection<? extends PrismPropertyValue> values) throws SchemaException {
        ItemDelta delta = new PropertyDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?> setExtensionReferenceAndPrepareDelta(QName itemName, PrismReferenceDefinition definition, Collection<? extends PrismReferenceValue> values) throws SchemaException {
        ItemDelta delta = new ReferenceDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?> addExtensionReferenceAndPrepareDelta(QName itemName, PrismReferenceDefinition definition, Collection<? extends PrismReferenceValue> values) throws SchemaException {
        ItemDelta delta = new ReferenceDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());
        return addExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?> setExtensionContainerAndPrepareDelta(QName itemName, PrismContainerDefinition definition, Collection<? extends PrismContainerValue> values) throws SchemaException {
        ItemDelta delta = new ContainerDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private <V extends PrismValue> ItemDelta<?> setExtensionItemAndPrepareDeltaCommon(ItemDelta delta, Collection<V> values) throws SchemaException {

        // these values should have no parent, otherwise the following will fail
        delta.setValuesToReplace(values);

        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>(1);
        modifications.add(delta);
        PropertyDelta.applyTo(modifications, taskPrism);		// i.e. here we apply changes only locally (in memory)

        return isPersistent() ? delta : null;
    }

    private <V extends PrismValue> ItemDelta<?> addExtensionItemAndPrepareDeltaCommon(ItemDelta delta, Collection<V> values) throws SchemaException {

        // these values should have no parent, otherwise the following will fail
        delta.addValuesToAdd(values);

        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>(1);
        modifications.add(delta);
        PropertyDelta.applyTo(modifications, taskPrism);		// i.e. here we apply changes only locally (in memory)

        return isPersistent() ? delta : null;
    }

    private ItemDelta<?> modifyExtensionAndPrepareDelta(ItemDelta<?> delta) throws SchemaException {

        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>(1);
        modifications.add(delta);
        PropertyDelta.applyTo(modifications, taskPrism);		// i.e. here we apply changes only locally (in memory)

        return isPersistent() ? delta : null;
    }

    private ItemDelta<?> addExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition, Collection<? extends PrismPropertyValue> values) throws SchemaException {
        ItemDelta delta = new PropertyDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());

        delta.addValuesToAdd(values);

        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>(1);
        modifications.add(delta);
        PropertyDelta.applyTo(modifications, taskPrism);		// i.e. here we apply changes only locally (in memory)

        return isPersistent() ? delta : null;
    }

    private ItemDelta<?> deleteExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition, Collection<? extends PrismPropertyValue> values) throws SchemaException {
        ItemDelta delta = new PropertyDelta(new ItemPath(TaskType.F_EXTENSION, itemName), definition, getPrismContext());

        delta.addValuesToDelete(values);

        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>(1);
        modifications.add(delta);
        PropertyDelta.applyTo(modifications, taskPrism);		// i.e. here we apply changes only locally (in memory)

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
        if (value != null) {
	        return isPersistent() ? PropertyDelta.createReplaceDelta(
	                taskManager.getTaskObjectDefinition(), TaskType.F_NODE, value) : null;
        } else {
        	return isPersistent() ? PropertyDelta.createReplaceDelta(
	                taskManager.getTaskObjectDefinition(), TaskType.F_NODE) : null;
        }
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
	 * Completion timestamp
	 */

    @Override
    public Long getCompletionTimestamp() {
        XMLGregorianCalendar gc = taskPrism.asObjectable().getCompletionTimestamp();
        return gc != null ? new Long(XmlTypeConverter.toMillis(gc)) : null;
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
                XmlTypeConverter.createXMLGregorianCalendar(value));
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
		if (taskResult ==null) {
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
            throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
        }
    }

    private PropertyDelta<?> setCategoryAndPrepareDelta(String value) {
        setCategoryTransient(value);
        return isPersistent() ? PropertyDelta.createReplaceDelta(
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
		OperationResult result = parentResult.createSubresult(DOT_INTERFACE+"refresh");
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
        taskManager.updateTaskInstance(this, repoObj, result);
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
		LOGGER.trace("canRun set to false for task " + this + " (" + System.identityHashCode(this) + ")");
		canRun = false;
	}

	@Override
	public boolean canRun() {
		return canRun;
	}

    // checks latest start time (useful for recurring tightly coupled tasks
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

        if (isTransient()) {
            throw new IllegalStateException("Only persistent tasks can have subtasks (as for now)");
        }
        TaskQuartzImpl sub = (TaskQuartzImpl) taskManager.createTaskInstance();
        sub.setParent(this.getTaskIdentifier());
        sub.setOwner(this.getOwner());

        LOGGER.trace("New subtask " + sub.getTaskIdentifier() + " has been created.");
        return sub;
    }

    @Deprecated
    @Override
    public TaskRunResult waitForSubtasks(Integer interval, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return waitForSubtasks(interval, null, parentResult);
    }

    @Deprecated
    @Override
    public TaskRunResult waitForSubtasks(Integer interval, Collection<ItemDelta<?>> extensionDeltas, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "waitForSubtasks");
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        result.addContext(OperationResult.CONTEXT_OID, getOid());

        TaskRunResult trr = new TaskRunResult();
        trr.setProgress(this.getProgress());
        trr.setRunResultStatus(TaskRunResult.TaskRunResultStatus.RESTART_REQUESTED);
        trr.setOperationResult(null);

        ScheduleType schedule = new ScheduleType();
        if (interval != null) {
            schedule.setInterval(interval);
        } else {
            schedule.setInterval(DEFAULT_SUBTASKS_WAIT_INTERVAL);
        }
        pushHandlerUri(WaitForSubtasksByPollingTaskHandler.HANDLER_URI, schedule, null, extensionDeltas);
        setBinding(TaskBinding.LOOSE);
        savePendingModifications(result);

        return trr;
    }

    public List<PrismObject<TaskType>> listSubtasksRaw(OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "listSubtasksRaw");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        ObjectFilter filter = null;
//        try {
            filter = EqualFilter.createEqual(TaskType.F_PARENT, TaskType.class, taskManager.getPrismContext(), null, getTaskIdentifier());
//        } catch (SchemaException e) {
//            throw new SystemException("Cannot create filter for 'parent equals task identifier' due to schema exception", e);
//        }
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        List<PrismObject<TaskType>> list = taskManager.getRepositoryService().searchObjects(TaskType.class, query, null, result);
        result.recordSuccessIfUnknown();
        return list;
    }

    public List<PrismObject<TaskType>> listPrerequisiteTasksRaw(OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "listPrerequisiteTasksRaw");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        ObjectFilter filter = null;
//        try {
            filter = EqualFilter.createEqual(TaskType.F_DEPENDENT, TaskType.class, taskManager.getPrismContext(), null, getTaskIdentifier());
//        } catch (SchemaException e) {
//            throw new SystemException("Cannot create filter for 'dependent contains task identifier' due to schema exception", e);
//        }
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        List<PrismObject<TaskType>> list = taskManager.getRepositoryService().searchObjects(TaskType.class, query, null, result);
        result.recordSuccessIfUnknown();
        return list;
    }

    @Override
    public List<Task> listSubtasks(OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "listSubtasks");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        return listSubtasksInternal(result);
    }

    private List<Task> listSubtasksInternal(OperationResult result) throws SchemaException {
        return taskManager.resolveTasksFromTaskTypes(listSubtasksRaw(result), result);
    }

    @Override
    public List<Task> listSubtasksDeeply(OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "listSubtasksDeeply");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        ArrayList<Task> retval = new ArrayList<Task>();
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

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "listPrerequisiteTasks");
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
}
