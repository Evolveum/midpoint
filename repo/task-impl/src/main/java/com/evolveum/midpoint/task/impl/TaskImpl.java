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
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.processor.ExtensionProcessor;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyModification;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExecutionStatusType;
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
	
	private String taskIdentifier;
	private UserType owner;
	private TaskExecutionStatus executionStatus;
	private TaskExclusivityStatus exclusivityStatus;
	private TaskPersistenceStatus persistenceStatus;
	private TaskRecurrence recurrenceStatus;
	private TaskBinding binding;
	private String handlerUri;
	private UriStack otherHandlersUriStack;
	private ObjectType object;
	private ObjectReferenceType objectRef;
	private String oid;
	private String name;
	private Long lastRunStartTimestamp;
	private Long lastRunFinishTimestamp;
	private Long nextRunStartTime;
	private PropertyContainer extension;
	private long progress;
	private TaskManagerImpl taskManager;
	private RepositoryService repositoryService;
	private OperationResult result;
	private ScheduleType schedule;
	private boolean canRun;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(TaskImpl.class);

	/**
	 * Note: This constructor assumes that the task is transient.
	 * @param taskType
	 * @param repositoryService
	 */	
	TaskImpl(TaskManagerImpl taskManager, LightweightIdentifier taskIdentifier) {
		this.taskIdentifier = taskIdentifier.toString();
		this.taskManager = taskManager;
		executionStatus = TaskExecutionStatus.RUNNING;
		exclusivityStatus = TaskExclusivityStatus.CLAIMED;
		persistenceStatus = TaskPersistenceStatus.TRANSIENT;
		recurrenceStatus = TaskRecurrence.SINGLE;
		binding = DEFAULT_BINDING_TYPE;
		extension = new PropertyContainer();
		progress = 0;
		repositoryService = null;
		object = null;
		objectRef = null;
		// TODO: Is this OK?
		result = null;
		schedule = null;
		canRun = true;
	}

	/**
	 * Note: This constructor assumes that the task is persistent.
	 * @param taskType
	 * @param repositoryService
	 */
	TaskImpl(TaskManagerImpl taskManager, RepositoryService repositoryService) {
		this.taskManager = taskManager;
		this.repositoryService = repositoryService;
		canRun = true;
	}
		
	void initialize(TaskType taskType, OperationResult initResult) throws SchemaException {
		taskIdentifier = taskType.getTaskIdentifier();
		ObjectReferenceType ownerRef = taskType.getOwnerRef();
		if (ownerRef == null) {
			throw new SchemaException("Task "+taskType.getOid()+" does not have an owner (missing ownerRef)");
		}
		owner = resolveOwnerRef(taskType.getOwnerRef(), initResult);
		executionStatus = TaskExecutionStatus.fromTaskType(taskType.getExecutionStatus());
		exclusivityStatus = TaskExclusivityStatus.fromTaskType(taskType.getExclusivityStatus());
		recurrenceStatus = TaskRecurrence.fromTaskType(taskType.getRecurrence());
		binding = TaskBinding.fromTaskType(taskType.getBinding());
		if (binding == null)
			binding = DEFAULT_BINDING_TYPE;
		
		if (taskType.getOid()==null || taskType.getOid().isEmpty()) {
			persistenceStatus = TaskPersistenceStatus.TRANSIENT;
			oid = null;			
		} else {
			persistenceStatus = TaskPersistenceStatus.PERSISTENT;
			oid = taskType.getOid();
		}
		handlerUri = taskType.getHandlerUri();
		otherHandlersUriStack = taskType.getOtherHandlersUriStack();
		// TODO: object =
		objectRef = taskType.getObjectRef();
		name = taskType.getName();
		if (taskType.getLastRunStartTimestamp()!=null) {
			lastRunStartTimestamp = new Long(XsdTypeConverter.toMillis(taskType.getLastRunStartTimestamp()));
		}
		if (taskType.getLastRunFinishTimestamp()!=null) {
			lastRunFinishTimestamp = new Long(XsdTypeConverter.toMillis(taskType.getLastRunFinishTimestamp()));
		}
		if (taskType.getNextRunStartTime()!=null) {
			nextRunStartTime = new Long(XsdTypeConverter.toMillis(taskType.getNextRunStartTime()));
		}
		if (taskType.getProgress()!=null) {
			progress = taskType.getProgress().longValue();
		} else {
			progress = 0;
		}
		if (taskType.getResult()!=null) {
			result = OperationResult.createOperationResult(taskType.getResult());
		} else {
			result = null;
		}
		schedule = taskType.getSchedule();
		// Parse the extension
		LOGGER.trace("Parsing extension {}",ObjectTypeUtil.dump(taskType.getExtension()));
		extension = ExtensionProcessor.parseExtension(taskType.getExtension());
	}
	
	private UserType resolveOwnerRef(ObjectReferenceType ownerRef, OperationResult result) throws SchemaException {
		try {
			return repositoryService.getObject(UserType.class, ownerRef.getOid(), null, result);
		} catch (ObjectNotFoundException e) {
			throw new SystemException("The owner of task "+oid+" cannot be found (owner OID: "+ownerRef.getOid()+")",e);
		}
	}

	RepositoryService getRepositoryService() {
		return repositoryService;
	}
	
	void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}
	
	@Override
	public String getTaskIdentifier() {
		return taskIdentifier;
	}
	
	@Override
	public UserType getOwner() {
		return owner;
	}

	@Override
	public void setOwner(UserType owner) {
		this.owner = owner;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getExecutionStatus()
	 */
	@Override
	public TaskExecutionStatus getExecutionStatus() {
		return executionStatus;
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
		return exclusivityStatus;
	}

	@Override
	public TaskBinding getBinding() {
		return binding;
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
		return progress;
	}

	@Override
	public ObjectReferenceType getObjectRef() {
		return objectRef;
	}
	
	@Override
	public void setObjectRef(ObjectReferenceType objectRef) {
		this.objectRef = objectRef;
	}
	
	@Override
	public String getObjectOid() {
		if (objectRef!=null) {
			return objectRef.getOid();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getObject()
	 */
	@Override
	public <T extends ObjectType> T getObject(Class<T> type, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(Task.class.getName()+".getObject");
		result.addContext(OperationResult.CONTEXT_OID, oid);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskImpl.class);
		
		if ( object!=null ) {
			// There is an embedded object in the task
			if (type.isAssignableFrom(object.getClass())) {
				result.recordSuccess();
				return (T) object;
			} else {
				throw new IllegalArgumentException("Requested object type "+type+", but the type of object in the task is "+object.getClass());
			}
		}
		if (objectRef != null) {
			// There is object reference. Let's try to resolve it
			try {
				// Note: storing this value in field, not local variable. It will be reused.
				object = repositoryService.getObject(type, objectRef.getOid(), null, result);
				result.recordSuccess();
				return (T) object;
			} catch (ObjectNotFoundException ex) {
				result.recordFatalError("Object not found", ex);
				throw ex;
			} catch (SchemaException ex) {
				result.recordFatalError("Schema error", ex);
				throw ex;
			}
		}
		return null;
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
		return handlerUri;
	}

	@Override
	public void setHandlerUri(String handlerUri) {
		this.handlerUri = handlerUri;
	}
	
	@Override
	public UriStack getOtherHandlersUriStack() {
		return otherHandlersUriStack;
	}

	@Override
	public void setExecutionStatus(TaskExecutionStatus executionStatus) {
		this.executionStatus = executionStatus;
	}

	@Override
	public void setPersistenceStatus(TaskPersistenceStatus persistenceStatus) {
		this.persistenceStatus = persistenceStatus;
	}

	@Override
	public void setExclusivityStatus(TaskExclusivityStatus exclusivityStatus) {
		this.exclusivityStatus = exclusivityStatus;
	}

	@Override
	public String getOid() {
		return oid;
	}

	@Override
	public void setOid(String oid) {
		this.oid = oid;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public PropertyContainer getExtension() {
		return extension;
	}
	
	@Override
	public Property getExtension(QName propertyName) {
		return extension.findProperty(propertyName);
	}

	@Override
	public void modifyExtension(List<PropertyModification> modifications, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult opResult = parentResult.createSubresult(Task.class.getName()+".modifyExtension");
		opResult.addParam("modifications", modifications);
		opResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskImpl.class);
		opResult.addContext(OperationResult.CONTEXT_OID,oid);
		
		// Only works for persistent tasks
		if (persistenceStatus!=TaskPersistenceStatus.PERSISTENT) {
			// No need to call repository. Just apply the updates to the container
			extension.applyModifications(modifications);
			opResult.recordSuccess();
			return;
		}
		
		ObjectModificationType objectChange = new ObjectModificationType();
		objectChange.setOid(oid);
		
		for (PropertyModification modification : modifications) {
			// Extension is schema-less now. Therefore we need to also record the types (hence "true" for recordType)
			PropertyModificationType propertyModificationType = null;
			try {
				propertyModificationType = modification.toPropertyModificationType(SchemaConstants.C_EXTENSION,true);
			} catch (SchemaException e) {
				// This is unlikely now, almost impossible. But may happen in the future.
				SchemaException ex = new SchemaException("Error dealing with extension schema, task OID "+oid,e);
				opResult.recordFatalError("Error dealing with extension schema",e);
				throw ex;
			}			
			objectChange.getPropertyModification().add(propertyModificationType);
		}
		
		try {
			repositoryService.modifyObject(TaskType.class, objectChange, opResult);
		} catch (ObjectNotFoundException ex) {
			opResult.recordFatalError("Object not found", ex);
			throw ex;
		} catch (SchemaException ex) {
			opResult.recordFatalError("Schema error", ex);
			throw ex;
		} catch (RuntimeException ex) {
			opResult.recordFatalError("Internal error", ex);
			throw ex;
		}
		
		opResult.recordSuccess();
	}
	
	@Override
	public TaskType getTaskTypeObject() {
		TaskType taskType = new TaskType();
		
		taskType.setExecutionStatus(executionStatus.toTaskType());
		taskType.setExclusivityStatus(exclusivityStatus.toTaskType());
		taskType.setRecurrence(recurrenceStatus.toTaskType());
		taskType.setBinding(binding.toTaskType());
		
		if (persistenceStatus == TaskPersistenceStatus.PERSISTENT) {
			taskType.setOid(oid);
		} else {
			// TRANSIENT task
			// Nothing to do
		}

		taskType.setHandlerUri(handlerUri);
		taskType.setOtherHandlersUriStack(otherHandlersUriStack);
		taskType.setName(name);
		taskType.setProgress(BigInteger.valueOf(progress));
		
		if (objectRef!=null) {
			taskType.setObjectRef(objectRef);
		} else if (object!=null) {
			// TODO
		}

		if (result!=null) {
			taskType.setResult(result.createOperationResultType());
		}
		
		if (schedule!=null) {
			taskType.setSchedule(schedule);
		}

//		if (extension!=null && !extension.isEmpty()) {			if we do not create (empty) extension element, we'll not be able to add properties into it 
		if (extension!=null) {		
			Extension xmlExtension;
			xmlExtension = ExtensionProcessor.createExtension(extension);
			taskType.setExtension(xmlExtension);
		}
		
		return taskType;
	}

	@Override
	public Long getLastRunStartTimestamp() {
		return lastRunStartTimestamp;
	}

	@Override
	public Long getLastRunFinishTimestamp() {
		return lastRunFinishTimestamp;
	}

	@Override
	public Long getNextRunStartTime() {
		return nextRunStartTime;
	}
	
	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("Task(");
		sb.append(TaskImpl.class.getName());
		sb.append(")\n");
		sb.append("  OID: ");
		sb.append(oid);
		sb.append("\n  name: ");
		sb.append(name);
		sb.append("\n  executionStatus: ");
		sb.append(executionStatus);
		sb.append("\n  exclusivityStatus: ");
		sb.append(exclusivityStatus);
		sb.append("\n  persistenceStatus: ");
		sb.append(persistenceStatus);
		sb.append("\n  handlerUri: ");
		sb.append(handlerUri);
		sb.append("\n  otherHandlersUriStack: ");
		sb.append(otherHandlersUriStack);
		sb.append("\n  object: ");
		sb.append(ObjectTypeUtil.toShortString(object));
		sb.append("\n  objectRef: ");
		sb.append(ObjectTypeUtil.toShortString(objectRef));
		sb.append("\n  lastRunStartTimestamp: ");
		sb.append(lastRunStartTimestamp);
		sb.append("\n  lastRunFinishTimestamp: ");
		sb.append(lastRunFinishTimestamp);
		sb.append("\n  nextRunStartTime: ");
		sb.append(nextRunStartTime);
		sb.append("\n  progress: ");
		sb.append(progress);
		sb.append("\n  result: ");
		if (result==null) {
			sb.append("null");
		} else {
			sb.append(result.dump());
		}
		sb.append("\n  extension: ");
		sb.append(extension);
		return sb.toString();
	}

	@Override
	public void recordRunStart(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		// TODO 
		lastRunStartTimestamp = System.currentTimeMillis();
		nextRunStartTime = ScheduleEvaluator.determineNextRunStartTime(this);
		// This is all we need to do for transient tasks
		if (!isPersistent()) {
			return;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis(lastRunStartTimestamp);
		ObjectModificationType modification = ObjectTypeUtil.createModificationReplaceProperty(oid, SchemaConstants.C_TASK_LAST_RUN_START_TIMESTAMP, cal);

		// FIXME: if nextRunStartTime == 0 we should delete the corresponding element; however, this does not work as for now
		if (nextRunStartTime > 0)
			modification.getPropertyModification().add(TaskManagerImpl.createNextRunStartTimeModification(nextRunStartTime));
		
		repositoryService.modifyObject(TaskType.class, modification, parentResult);
	}

	@Override
	public void recordRunFinish(TaskRunResult runResult, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		progress = runResult.getProgress(); 
		lastRunFinishTimestamp = System.currentTimeMillis();
		// This is all we need to do for transient tasks
		if (!isPersistent()) {
			return;
		}

		ObjectModificationType modification = new ObjectModificationType();
		modification.setOid(oid);

		// last run time modification
		GregorianCalendar calLRFT = new GregorianCalendar();
		calLRFT.setTimeInMillis(lastRunFinishTimestamp);
		PropertyModificationType timestampModificationLRFT = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_LAST_RUN_FINISH_TIMESTAMP, calLRFT);

		modification.getPropertyModification().add(timestampModificationLRFT);
		
		// progress
		PropertyModificationType progressModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_PROGRESS, progress);
		modification.getPropertyModification().add(progressModification);
		
		// result
		PropertyModificationType resultModification = null;
		if (runResult.getOperationResult()!=null) {
			resultModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_RESULT, runResult.getOperationResult().createOperationResultType());
		} else {
			// Make sure we replace any stale result that may be stored there
			resultModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_RESULT, null);
		}
//		// temporary - Pavol Mederly - make changes only if the task run result contains some OperationResult
//		if (runResult.getOperationResult()!=null)
			modification.getPropertyModification().add(resultModification);
			
		// execute the modification
		repositoryService.modifyObject(TaskType.class, modification, parentResult);
		
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
		ObjectModificationType modification = new ObjectModificationType();
		modification.setOid(oid);
		PropertyModificationType progressModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_PROGRESS, progress);
		modification.getPropertyModification().add(progressModification);
		PropertyModificationType resultModification = null;
		if (result!=null) {
			resultModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_RESULT, result.createOperationResultType());
		} else {
			// Make sure we replace any stale result that may be stored there
			resultModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_RESULT, null);
		}
		modification.getPropertyModification().add(resultModification);
		repositoryService.modifyObject(TaskType.class, modification, parentResult);		
		
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
		
		ObjectType repoObj = null;
		try {
			repoObj = repositoryService.getObject(ObjectType.class, getOid(), null, result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Object not found", ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordFatalError("Schema error", ex);
			throw ex;			
		}
		TaskType taskType = (TaskType)repoObj;
		initialize(taskType, result);
		result.recordSuccess();
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
		ObjectModificationType modification = new ObjectModificationType();
		modification.setOid(oid);
		PropertyModificationType timestampModification = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_EXECUTION_STATUS, TaskExecutionStatusType.CLOSED.value());
		modification.getPropertyModification().add(timestampModification);
		try {
			repositoryService.modifyObject(TaskType.class, modification, result);
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
		return (recurrenceStatus == TaskRecurrence.SINGLE);
	}

	@Override
	public boolean isCycle() {
		// TODO: binding
		return (recurrenceStatus == TaskRecurrence.RECURRING);
	}

	@Override
	public boolean isTightlyBound() {
		return binding == TaskBinding.TIGHT;
	}
	
	@Override
	public boolean isLooselyBound() {
		return binding == TaskBinding.LOOSE;
	}

	@Override
	public ScheduleType getSchedule() {
		return schedule;
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
		return "Task(id:" + taskIdentifier + ", name:" + name + ", oid:" + oid + ")";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((oid == null) ? 0 : oid.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskImpl other = (TaskImpl) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (oid == null) {
			if (other.oid != null)
				return false;
		} else if (!oid.equals(other.oid))
			return false;
		return true;
	}

	public void makeRecurrent(long interval)
	{
		recurrenceStatus = TaskRecurrence.RECURRING;
		schedule = new ScheduleType();
		schedule.setInterval(BigInteger.valueOf(interval));
	}

	@Override
	public void finishHandler() {

		// let us drop the current handler URI and nominate the top of the other
		// handlers stack as the current one
		int stackSize;
		if (otherHandlersUriStack != null && !otherHandlersUriStack.getUri().isEmpty()) {
			stackSize = otherHandlersUriStack.getUri().size();
			handlerUri = otherHandlersUriStack.getUri().get(stackSize - 1);
			otherHandlersUriStack.getUri().remove(stackSize - 1);
		} else {
			handlerUri = null;
			stackSize = 0;
		}
		
		LOGGER.trace("finishHandler: new current handler uri = {}, stack size = {}", handlerUri, stackSize);
		
		// TODO: make changes in repository as well (really? this has to be thought out yet)
	}

}
