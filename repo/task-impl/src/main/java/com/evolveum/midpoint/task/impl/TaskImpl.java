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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyModification;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

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
	
	private TaskExecutionStatus executionStatus;
	private TaskExclusivityStatus exclusivityStatus;
	private TaskPersistenceStatus persistenceStatus;
	private String handlerUri;
	private ObjectType object;
	private String oid;
	private String name;
	private Long lastRunTimestamp;
	private List<Property> extension;

	public TaskImpl() {
		executionStatus = TaskExecutionStatus.RUNNING;
		exclusivityStatus = TaskExclusivityStatus.CLAIMED;
		persistenceStatus = TaskPersistenceStatus.TRANSIENT;
		extension = new ArrayList<Property>();
	}

	public TaskImpl(TaskType taskType) {
		executionStatus = TaskExecutionStatus.fromTaskType(taskType.getExecutionStatus());
		exclusivityStatus = TaskExclusivityStatus.fromTaskType(taskType.getExclusivityStatus());
		// If that is created from the TaskType, then this is persistent task
		persistenceStatus = TaskPersistenceStatus.PERSISTENT;
		oid = taskType.getOid();
		handlerUri = taskType.getHandlerUri();
		// TODO: object = 
		name = taskType.getName();
		if (taskType.getLastRunTimestamp()!=null) {
			lastRunTimestamp = new Long(taskType.getLastRunTimestamp().getMillisecond());
		}
		// TODO: extension
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#isAsynchronous()
	 */
	@Override
	public boolean isAsynchronous() {
		// This is very simple now. It may complicate later.
		return (persistenceStatus==TaskPersistenceStatus.PERSISTENT);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getObject()
	 */
	@Override
	public ObjectType getObject() {
		return object;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.Task#getResult()
	 */
	@Override
	public OperationResult getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHanderUri() {
		return handlerUri;
	}

	@Override
	public void setHanderUri(String handlerUri) {
		this.handlerUri = handlerUri;
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
	public List<Property> getExtension() {
		return extension;
	}

	@Override
	public void modifyExtension(PropertyModification modification) {
		throw new NotImplementedException();
	}
	
	@Override
	public TaskType getTaskTypeObject() {
		throw new NotImplementedException();
	}

	@Override
	public Long getLastRunTimestamp() {
		return lastRunTimestamp;
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
		sb.append("\n  object: ");
		sb.append(object);
		sb.append("\n  lastRunTimestamp: ");
		sb.append(lastRunTimestamp);
		sb.append("\n  extension: ");
		sb.append(extension);
		return sb.toString();
	}

}
