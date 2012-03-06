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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ExtensionProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
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

	RepositoryService getRepositoryService() {
		return repositoryService;
	}
	
	void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}
	
	@Override
	public String getTaskIdentifier() {
		return taskPrism.getPropertyRealValue(TaskType.F_TASK_IDENTIFIER, String.class);
	}
	
	private void setTaskIdentifier(String value) {
		taskPrism.setPropertyRealValue(TaskType.F_TASK_IDENTIFIER, value);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getExecutionStatus()
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getPersistenceStatus()
	 */
	@Override
	public TaskPersistenceStatus getPersistenceStatus() {
		return persistenceStatus;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getExclusivityStatus()
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
	
	public TaskRecurrence getRecurrenceStatus() {
		TaskRecurrenceType xmlValue = taskPrism.getPropertyRealValue(TaskType.F_RECURRENCE, TaskRecurrenceType.class);
		if (xmlValue == null) {
			return null;
		}
		return TaskRecurrence.fromTaskType(xmlValue);
	}
	
	private void setRecurrenceStatus(TaskRecurrence value) {
		taskPrism.setPropertyRealValue(TaskType.F_RECURRENCE, value.toTaskType());
	}

	@Override
	public TaskBinding getBinding() {
		TaskBindingType xmlValue = taskPrism.getPropertyRealValue(TaskType.F_BINDING, TaskBindingType.class);
		if (xmlValue == null) {
			return null;
		}
		return TaskBinding.fromTaskType(xmlValue);
	}
	
	private void setBinding(TaskBinding value) {
		taskPrism.setPropertyRealValue(TaskType.F_BINDING, value.toTaskType());
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#isAsynchronous()
	 */
	@Override
	public boolean isAsynchronous() {
		// This is very simple now. It may complicate later.
		return (persistenceStatus==TaskPersistenceStatus.PERSISTENT);
	}
	
	@Override
	public long getProgress() {
		return taskPrism.getPropertyRealValue(TaskType.F_PROGRESS, Long.class);
	}

	private void setProgress(long value) {
		taskPrism.setPropertyRealValue(TaskType.F_PROGRESS, value);
	}
	
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getObject()
	 */
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


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getResult()
	 */
	@Override
	public OperationResult getResult() {
		return result;
	}

	public void setResult(OperationResult result) {
		this.result = result;
	}

	@Override
	public String getHandlerUri() {
		return taskPrism.getPropertyRealValue(TaskType.F_HANDLER_URI, String.class);
	}

	@Override
	public void setHandlerUri(String handlerUri) {
		taskPrism.setPropertyRealValue(TaskType.F_HANDLER_URI, handlerUri);
	}
	
	@Override
	public UriStack getOtherHandlersUriStack() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPersistenceStatus(TaskPersistenceStatus persistenceStatus) {
		this.persistenceStatus = persistenceStatus;
	}

	@Override
	public String getOid() {
		return taskPrism.getOid();
	}

	@Override
	public void setOid(String oid) {
		taskPrism.setOid(oid);
	}

	@Override
	public String getName() {
		return taskPrism.asObjectable().getName();
	}

	@Override
	public void setName(String name) {
		taskPrism.asObjectable().setName(name);
	}

	@Override
	public PrismContainer getExtension() {
		return taskPrism.getExtension();
	}
	
	@Override
	public PrismProperty getExtension(QName propertyName) {
		return getExtension().findProperty(propertyName);
	}
	
	@Override
	public PrismObject<TaskType> getTaskPrismObject() {
				
		if (result != null) {
			taskPrism.asObjectable().setResult(result.createOperationResultType());
		}				
		
		return taskPrism;
	}

	@Override
	public Long getLastRunStartTimestamp() {
		return new Long(XmlTypeConverter.toMillis(taskPrism.asObjectable().getLastRunStartTimestamp()));
	}
	
	private void setLastRunStartTimestamp(Long value) {
		taskPrism.asObjectable().setLastRunStartTimestamp(
				XmlTypeConverter.createXMLGregorianCalendar(value));
	}

	@Override
	public Long getLastRunFinishTimestamp() {
		return new Long(XmlTypeConverter.toMillis(taskPrism.asObjectable().getLastRunFinishTimestamp()));
	}
	
	private void setLastRunFinishTimestamp(Long value) {
		taskPrism.asObjectable().setLastRunFinishTimestamp(
				XmlTypeConverter.createXMLGregorianCalendar(value));
	}

	@Override
	public Long getNextRunStartTime() {
		return new Long(XmlTypeConverter.toMillis(taskPrism.asObjectable().getNextRunStartTime()));
	}
	
	private void setNextRunStartTime(Long value) {
		taskPrism.asObjectable().setNextRunStartTime(
				XmlTypeConverter.createXMLGregorianCalendar(value));
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
		setLastRunStartTimestamp(currentTimestamp);
		
		setNextRunStartTime(ScheduleEvaluator.determineNextRunStartTime(this));
		// This is all we need to do for transient tasks
		if (!isPersistent()) {
			return;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis(currentTimestamp);
		Collection<? extends ItemDelta> modifications = PropertyDelta.createModificationReplacePropertyCollection(
				TaskType.F_LAST_RUN_START_TIMESTAMP, 
				getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class),
				cal);

		Long nextRunStartTime = getNextRunStartTime();
		// FIXME: if nextRunStartTime == 0 we should delete the corresponding element; however, this does not work as for now
		if (nextRunStartTime > 0) {
			((Collection)modifications).add(taskManager.createNextRunStartTimeModification(nextRunStartTime));
		}
		
		repositoryService.modifyObject(TaskType.class, getOid(), modifications, parentResult);
	}

	@Override
	public void recordRunFinish(TaskRunResult runResult, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		setProgress(runResult.getProgress()); 
		long currentTimestamp = System.currentTimeMillis();
		setLastRunFinishTimestamp(currentTimestamp);
		// This is all we need to do for transient tasks
		if (!isPersistent()) {
			return;
		}

		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>();

		// last run time modification
		GregorianCalendar calLRFT = new GregorianCalendar();
		calLRFT.setTimeInMillis(currentTimestamp);
		PropertyDelta timestampModificationLRFT = PropertyDelta.createReplaceDelta(taskManager.getTaskObjectDefinition(), 
				TaskType.F_LAST_RUN_FINISH_TIMESTAMP, calLRFT);
		modifications.add(timestampModificationLRFT);
		
		// progress
		PropertyDelta progressModification = PropertyDelta.createReplaceDelta(taskManager.getTaskObjectDefinition(), 
				TaskType.F_PROGRESS, getProgress());
		modifications.add(progressModification);
		
		// result
		PropertyDelta resultModification = null;
		if (runResult.getOperationResult() != null) {
			resultModification = PropertyDelta.createReplaceDelta(taskManager.getTaskObjectDefinition(), 
					TaskType.F_RESULT, runResult.getOperationResult().createOperationResultType());
		} else {
			resultModification = PropertyDelta.createReplaceEmptyDelta(taskManager.getTaskObjectDefinition(), TaskType.F_RESULT);
		}
//		// temporary - Pavol Mederly - make changes only if the task run result contains some OperationResult
//		if (runResult.getOperationResult()!=null)
		modifications.add(resultModification);
			
		// execute the modification
		repositoryService.modifyObject(TaskType.class, getOid(), modifications, parentResult);
		
		// TODO: Also save the OpResult
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#recordProgress(long, com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public void recordProgress(long progress, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// This is all we need to do for transient tasks
		if (!isPersistent()) {
			return;
		}
		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
		PropertyDelta progressModification = PropertyDelta.createReplaceDelta(taskManager.getTaskObjectDefinition(), 
				TaskType.F_PROGRESS, progress);
		modifications.add(progressModification);
		PropertyDelta resultModification = null;
		if (result!=null) {
			resultModification = PropertyDelta.createReplaceDelta(taskManager.getTaskObjectDefinition(), 
				TaskType.F_RESULT, result.createOperationResultType());
		} else {
			// Make sure we replace any stale result that may be stored there
			resultModification = PropertyDelta.createReplaceEmptyDelta(taskManager.getTaskObjectDefinition(), TaskType.F_RESULT);
		}
		modifications.add(resultModification);
		repositoryService.modifyObject(TaskType.class, getOid(), modifications, parentResult);		
		
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
