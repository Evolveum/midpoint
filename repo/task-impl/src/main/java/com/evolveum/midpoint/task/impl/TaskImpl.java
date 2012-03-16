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
package com.evolveum.midpoint.task.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExclusivityStatusType;
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
 * @see TaskManagerImpl
 * 
 * @author Radovan Semancik
 *
 */
public class TaskImpl implements Task {
	
	private TaskBinding DEFAULT_BINDING_TYPE = TaskBinding.TIGHT;
	
	private PrismObject<TaskType> taskPrism;
	
//	private String taskIdentifier;
//	private PrismObject<UserType> owner;
//	private TaskExecutionStatus executionStatus;
//	private TaskExclusivityStatus exclusivityStatus;
	private TaskPersistenceStatus persistenceStatus;
//	private TaskRecurrence recurrenceStatus;
//	private TaskBinding binding;
//	private String handlerUri;
//	private UriStack otherHandlersUriStack;
//	private PrismObject<ObjectType> object;
//	private ObjectReferenceType objectRef;
//	private String oid;
//	private String name;
//	private Long lastRunStartTimestamp;
//	private Long lastRunFinishTimestamp;
//	private Long nextRunStartTime;
//	private PrismContainer extension;
//	private long progress;
	private TaskManagerImpl taskManager;
	private RepositoryService repositoryService;
	private OperationResult result;
//	private ScheduleType schedule;
	private boolean canRun;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(TaskImpl.class);

	/**
	 * Note: This constructor assumes that the task is transient.
	 * @param taskType
	 * @param repositoryService
	 */	
	TaskImpl(TaskManagerImpl taskManager, LightweightIdentifier taskIdentifier) {
		this.taskManager = taskManager;
		this.repositoryService = null;
		this.taskPrism = createPrism();
		this.result = null;
		this.canRun = true;
		
		setTaskIdentifier(taskIdentifier.toString());
		setExecutionStatus(TaskExecutionStatus.RUNNING);
		setExclusivityStatus(TaskExclusivityStatus.CLAIMED);
		setPersistenceStatus(TaskPersistenceStatus.TRANSIENT);
		setRecurrenceStatus(TaskRecurrence.SINGLE);
		setBinding(DEFAULT_BINDING_TYPE);
		setProgress(0);
		setObject(null);
		
		setDefaults();
	}

	/**
	 * Assumes that the task is persistent
	 */
	TaskImpl(TaskManagerImpl taskManager, PrismObject<TaskType> taskPrism, RepositoryService repositoryService) {
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
			setBinding(DEFAULT_BINDING_TYPE);
		
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
		if (pendingModifications != null && !pendingModifications.isEmpty()) {
			repositoryService.modifyObject(TaskType.class, getOid(), pendingModifications, parentResult);
		}
	}

	
	private void processModificationNow(PropertyDelta<?> delta, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		if (delta != null) {
			Collection<PropertyDelta<?>> deltas = new ArrayList<PropertyDelta<?>>(1);
			deltas.add(delta);
			repositoryService.modifyObject(TaskType.class, getOid(), deltas, parentResult);
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
		Integer value = taskPrism.getPropertyRealValue(TaskType.F_PROGRESS, Integer.class);		// TODO: change to Long when xsd:long will work
		return value != null ? value : 0; 
	}

	@Override
	public void setProgress(long value) {
		taskPrism.setPropertyRealValue(TaskType.F_PROGRESS, Integer.valueOf((int) value));			// TODO: get rid of this cast when xsd:long will work
	}

	@Override
	public void setProgressPersistent(long value, OperationResult result) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setProgressAndPrepareDelta(value), result);
	}
	
	@Override
	public void setProgressPersistentBatched(long value) {
		processModificationBatched(setProgressAndPrepareDelta(value));
	}
	
	private PropertyDelta<?> setProgressAndPrepareDelta(long value) {
		setProgress(value);
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
		this.result = result;
	}

	@Override
	public void setResultPersistent(OperationResult result, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setResultAndPrepareDelta(result), parentResult);
	}
	
	@Override
	public void setResultPersistentBatched(OperationResult result) {
		processModificationBatched(setResultAndPrepareDelta(result));
	}
	
	private PropertyDelta<?> setResultAndPrepareDelta(OperationResult result) {
		setResult(result);
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

	@Override
	public void setHandlerUri(String handlerUri) {
		taskPrism.setPropertyRealValue(TaskType.F_HANDLER_URI, handlerUri);
	}

	@Override
	public void setHandlerUriPersistent(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setHandlerUriAndPrepareDelta(value), parentResult);
	}
	
	@Override
	public void setHandlerUriPersistentBatched(String value) {
		processModificationBatched(setHandlerUriAndPrepareDelta(value));
	}
	
	private PropertyDelta<?> setHandlerUriAndPrepareDelta(String value) {
		setHandlerUri(value);
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_HANDLER_URI, value) : null;
	}
	
	/*
	 * Other handlers URI stack
	 */

	@Override
	public UriStack getOtherHandlersUriStack() {
		return taskPrism.getPropertyRealValue(TaskType.F_OTHER_HANDLERS_URI_STACK, UriStack.class);
	}
	
	/*
	 * Persistence status
	 */

	@Override
	public TaskPersistenceStatus getPersistenceStatus() {
		return persistenceStatus;
	}
	
	@Override
	public void setPersistenceStatus(TaskPersistenceStatus persistenceStatus) {
		this.persistenceStatus = persistenceStatus;
	}
	
	// obviously, there are no "persistent" versions of setPersistenceStatus

	/*
	 * Oid
	 */
	
	@Override
	public String getOid() {
		return taskPrism.getOid();
	}

	@Override
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
		taskPrism.setPropertyRealValue(TaskType.F_TASK_IDENTIFIER, value);
	}
	
	/* 
	 * Execution status
	 */
	
	@Override
	public TaskExecutionStatus getExecutionStatus() {
		TaskExecutionStatusType xmlValue = taskPrism.getPropertyRealValue(TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.class);
		if (xmlValue == null) {
			return null;
		}
		return TaskExecutionStatus.fromTaskType(xmlValue);
	}
	
	@Override
	public void setExecutionStatus(TaskExecutionStatus executionStatus) {
		taskPrism.setPropertyRealValue(TaskType.F_EXECUTION_STATUS, executionStatus.toTaskType());
	}

	@Override
	public void setExecutionStatusPersistent(TaskExecutionStatus value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setExecutionStatusAndPrepareDelta(value), parentResult);
	}

	@Override
	public void setExecutionStatusPersistentBatched(TaskExecutionStatus value) {
		processModificationBatched(setExecutionStatusAndPrepareDelta(value));
	}

	private PropertyDelta<?> setExecutionStatusAndPrepareDelta(TaskExecutionStatus value) {
		setExecutionStatus(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_EXECUTION_STATUS, value.toTaskType()) : null;
	}

	/* 
	 * Exclusivity status
	 */
	
	@Override
	public TaskExclusivityStatus getExclusivityStatus() {
		TaskExclusivityStatusType xmlValue = taskPrism.getPropertyRealValue(TaskType.F_EXCLUSIVITY_STATUS, TaskExclusivityStatusType.class);
		if (xmlValue == null) {
			return null;
		}
		return TaskExclusivityStatus.fromTaskType(xmlValue);
	}
	
	@Override
	public void setExclusivityStatus(TaskExclusivityStatus exclusivityStatus) {
		taskPrism.setPropertyRealValue(TaskType.F_EXCLUSIVITY_STATUS, exclusivityStatus.toTaskType());
	}

	@Override
	public void setExclusivityStatusPersistent(TaskExclusivityStatus value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setExclusivityStatusAndPrepareDelta(value), parentResult);
	}
	
	@Override
	public void setExclusivityStatusPersistentBatched(TaskExclusivityStatus value) {
		processModificationBatched(setExclusivityStatusAndPrepareDelta(value));
	}
	
	private PropertyDelta<?> setExclusivityStatusAndPrepareDelta(TaskExclusivityStatus value) {
		setExclusivityStatus(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_EXCLUSIVITY_STATUS, value.toTaskType()) : null;
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
	public void setRecurrenceStatus(TaskRecurrence value) {
		taskPrism.setPropertyRealValue(TaskType.F_RECURRENCE, value.toTaskType());
	}
	
	@Override
	public void setRecurrenceStatusPersistent(TaskRecurrence value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setRecurrenceStatusAndPrepareDelta(value), parentResult);
	}
	
	@Override
	public void setRecurrenceStatusPersistentBatched(TaskRecurrence value) {
		processModificationBatched(setRecurrenceStatusAndPrepareDelta(value));
	}
	
	private PropertyDelta<?> setRecurrenceStatusAndPrepareDelta(TaskRecurrence value) {
		setRecurrenceStatus(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_RECURRENCE, value.toTaskType()) : null;
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
	public void setBinding(TaskBinding value) {
		taskPrism.setPropertyRealValue(TaskType.F_BINDING, value.toTaskType());
	}
	
	@Override
	public void setBindingPersistent(TaskBinding value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setBindingAndPrepareDelta(value), parentResult);
	}
	
	@Override
	public void setBindingPersistentBatched(TaskBinding value) {
		processModificationBatched(setBindingAndPrepareDelta(value));
	}
	
	private PropertyDelta<?> setBindingAndPrepareDelta(TaskBinding value) {
		setBinding(value);
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
		PrismReference ownerRef = taskPrism.findOrCreateReference(TaskType.F_OWNER_REF);
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
		PrismReference objectRef = taskPrism.findOrCreateReference(TaskType.F_OBJECT_REF);
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
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskImpl.class);
		
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
	
	private void setObject(PrismObject object) {
		if (object == null) {
			PrismReference objectRef = taskPrism.findReference(TaskType.F_OBJECT_REF);
			if (objectRef != null) {
				taskPrism.getValue().remove(objectRef);
			}
		} else {
			PrismReference objectRef = taskPrism.findOrCreateReference(TaskType.F_OBJECT_REF);
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
	public void setName(String name) {
		taskPrism.asObjectable().setName(name);
	}
	
	@Override
	public void setNamePersistent(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setNameAndPrepareDelta(value), parentResult);
	}
	
	@Override
	public void setNamePersistentBatched(String value) {
		processModificationBatched(setNameAndPrepareDelta(value));
	}
	
	private PropertyDelta<?> setNameAndPrepareDelta(String value) {
		setName(value);
		return isPersistent() ? PropertyDelta.createReplaceDelta(
					taskManager.getTaskObjectDefinition(), TaskType.F_NAME, value) : null;
	}

	/*
	 * Extension
	 */

	@Override
	public PrismContainer getExtension() {
		return taskPrism.getExtension();
	}
	
	@Override
	public PrismProperty getExtension(QName propertyName) {
		return getExtension().findProperty(propertyName);
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
		taskPrism.asObjectable().setLastRunStartTimestamp(
				XmlTypeConverter.createXMLGregorianCalendar(value));
	}
	
	public void setLastRunStartTimestampPersistent(Long value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setLastRunStartTimestampAndPrepareDelta(value), parentResult);
	}

	public void setLastRunStartTimestampPersistentBatched(Long value) {
		processModificationBatched(setLastRunStartTimestampAndPrepareDelta(value));
	}

	private PropertyDelta<?> setLastRunStartTimestampAndPrepareDelta(Long value) {
		setLastRunStartTimestamp(value);
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
		taskPrism.asObjectable().setLastRunFinishTimestamp(
				XmlTypeConverter.createXMLGregorianCalendar(value));
	}
	
	public void setLastRunFinishTimestampPersistent(Long value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setLastRunFinishTimestampAndPrepareDelta(value), parentResult);
	}

	public void setLastRunFinishTimestampPersistentBatched(Long value) {
		processModificationBatched(setLastRunFinishTimestampAndPrepareDelta(value));
	}

	private PropertyDelta<?> setLastRunFinishTimestampAndPrepareDelta(Long value) {
		setLastRunFinishTimestamp(value);
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
		XMLGregorianCalendar gc = taskPrism.asObjectable().getNextRunStartTime();
		return gc != null ? new Long(XmlTypeConverter.toMillis(gc)) : null;
	}
	
	public void setNextRunStartTime(Long value) {
		taskPrism.asObjectable().setNextRunStartTime(
				XmlTypeConverter.createXMLGregorianCalendar(value));
	}
	
	public void setNextRunStartTimePersistent(Long value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		processModificationNow(setNextRunStartTimeAndPrepareDelta(value), parentResult);
	}

	public void setNextRunStartTimePersistentBatched(Long value) {
		processModificationBatched(setNextRunStartTimeAndPrepareDelta(value));
	}

	private PropertyDelta<?> setNextRunStartTimeAndPrepareDelta(Long value) {
		setNextRunStartTime(value);
		
		return isPersistent() ? PropertyDelta.createReplaceDeltaOrEmptyDelta(
									taskManager.getTaskObjectDefinition(), 
									TaskType.F_NEXT_RUN_START_TIME, 
									taskPrism.asObjectable().getNextRunStartTime()) 
							  : null;
	}

	
	
	
	
	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("Task(");
		sb.append(TaskImpl.class.getName());
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

	@Override
	public void recordRunStart(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO 

		long currentTimestamp = System.currentTimeMillis();
		setLastRunStartTimestampPersistent(currentTimestamp, parentResult);
		setNextRunStartTimePersistent(ScheduleEvaluator.determineNextRunStartTime(this), parentResult);

//		// This is all we need to do for transient tasks
//		if (!isPersistent()) {
//			return;
//		}
//		GregorianCalendar cal = new GregorianCalendar();
//		cal.setTimeInMillis(currentTimestamp);
//		Collection<? extends ItemDelta> modifications = PropertyDelta.createModificationReplacePropertyCollection(
//				TaskType.F_LAST_RUN_START_TIMESTAMP, 
//				getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class),
//				cal);
//
//		Long nextRunStartTime = getNextRunStartTime();
//		// FIXME: if nextRunStartTime == 0 we should delete the corresponding element; however, this does not work as for now
//		if (nextRunStartTime > 0) {
//			((Collection)modifications).add(taskManager.createNextRunStartTimeModification(nextRunStartTime));
//		}
//		
//		repositoryService.modifyObject(TaskType.class, getOid(), modifications, parentResult);
	}

	@Override
	public void recordRunFinish(TaskRunResult runResult, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		setProgressPersistent(runResult.getProgress(), parentResult); 
		long currentTimestamp = System.currentTimeMillis();
		setLastRunFinishTimestampPersistent(currentTimestamp, parentResult);
		setResultPersistent(runResult.getOperationResult(), parentResult);
		
		// TODO: Also save the OpResult
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#recordProgress(long, com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public void recordProgress(long progress, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		setProgressPersistent(progress, parentResult);
	}
	
	@Override
	public void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(Task.class.getName()+".refresh");
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskImpl.class);
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
	
	@Override
	public void modify(Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		PropertyDelta.applyTo(modifications, taskPrism);
		if (isPersistent()) {
			getRepositoryService().modifyObject(TaskType.class, getOid(), modifications, parentResult);
		}
	}

	public void modify(ItemDelta<?> modification, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>(1);
		modifications.add(modification);
		modify(modifications, parentResult);
	}

	private boolean isPersistent() {
		return persistenceStatus == TaskPersistenceStatus.PERSISTENT;
	}

	@Override
	public void close(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(Task.class.getName()+".close");
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskImpl.class);
		result.addContext(OperationResult.CONTEXT_OID, getOid());
		
		// Close the task
		Collection<? extends ItemDelta> modifications = PropertyDelta.createModificationReplacePropertyCollection(
				TaskType.F_EXECUTION_STATUS, 
				getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class),
				TaskExecutionStatusType.CLOSED.value());
		try {
			repositoryService.modifyObject(TaskType.class, getOid(), modifications, result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Object not found", ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordFatalError("Schema error", ex);
			throw ex;
		}		
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

	@Override
	public boolean isTightlyBound() {
		return getBinding() == TaskBinding.TIGHT;
	}
	
	@Override
	public boolean isLooselyBound() {
		return getBinding() == TaskBinding.LOOSE;
	}

	@Override
	public ScheduleType getSchedule() {
		return taskPrism.asObjectable().getSchedule();
	}
	
	private void setSchedule(ScheduleType schedule) {
		taskPrism.asObjectable().setSchedule(schedule);
	}

	@Override
	public void shutdown() {
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
		TaskImpl other = (TaskImpl) obj;
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

	public void makeRecurrent(long interval)
	{
		setRecurrenceStatus(TaskRecurrence.RECURRING);
		ScheduleType schedule = new ScheduleType();
		schedule.setInterval(BigInteger.valueOf(interval));
		setSchedule(schedule);
	}

	@Override
	public void finishHandler() {

		// let us drop the current handler URI and nominate the top of the other
		// handlers stack as the current one
		int stackSize;
		UriStack otherHandlersUriStack = getOtherHandlersUriStack();
		if (otherHandlersUriStack != null && !otherHandlersUriStack.getUri().isEmpty()) {
			stackSize = otherHandlersUriStack.getUri().size();
			setHandlerUri(otherHandlersUriStack.getUri().get(stackSize - 1));
			otherHandlersUriStack.getUri().remove(stackSize - 1);
		} else {
			setHandlerUri(null);
			stackSize = 0;
		}
		
		LOGGER.trace("finishHandler: new current handler uri = {}, stack size = {}", getHandlerUri(), stackSize);
		
		// TODO: make changes in repository as well (really? this has to be thought out yet)
	}
	
	private PrismContext getPrismContext() {
		return taskManager.getPrismContext();
	}

}
