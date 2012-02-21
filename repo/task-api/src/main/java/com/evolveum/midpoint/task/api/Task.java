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
package com.evolveum.midpoint.task.api;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UriStack;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * Task instance - a logical unit of work that is either done synchronously, asynchronously, it is deferred, scheduled, etc.
 * 
 * The classes that implement this interface hold a "java" task state. They represent the in-memory task data structure.
 * The instances must be able to serialize the state to the repository object (TaskType) when needed. This usually happens
 * on task "release".
 * 
 * The task implementation should be simple Java objects (POJOs). They are created also for a synchronous tasks, which means
 * they are created frequently. We want a low overhead for task management until the task is made asynchronous.
 * 
 * TODO: change model. How to propagate task changes down to the repository. attached or detached?
 * 
 * @author Radovan Semancik
 *
 */
public interface Task extends Dumpable {
	
	/**
	 * Returns execution status.
	 * 
	 * @see TaskExecutionStatus
	 * 
	 * @return execution status.
	 */
	public TaskExecutionStatus getExecutionStatus();
	
	/**
	 * Sets task execution status.
	 * 
	 * @see TaskExecutionStatus
	 * 
	 * @param executionStatus new task execution status.
	 */
	public void setExecutionStatus(TaskExecutionStatus executionStatus);
	
	/**
	 * Returns task persistence status.
	 * 
	 * @see TaskPersistenceStatus
	 * 
	 * @return task persistence status.
	 */
	public TaskPersistenceStatus getPersistenceStatus();
	
	/**
	 * Sets task persistence status.
	 * 
	 * @see TaskPersistenceStatus
	 * 
	 * @param persistenceStatus new task persistence status.
	 */
	public void setPersistenceStatus(TaskPersistenceStatus persistenceStatus);
	
	/**
	 * Returns task exclusivity status.
	 * 
	 * @see TaskExclusivityStatus
	 * 
	 * @return task exclusivity status.
	 */
	public TaskExclusivityStatus getExclusivityStatus();
	
	/**
	 * Sets task exclusivity status.
	 * 
	 * @see TaskExclusivityStatus
	 * 
	 * @param exclusivityStatus new task exclusivity status.
	 */
	public void setExclusivityStatus(TaskExclusivityStatus exclusivityStatus);
	
	/**
	 * Returns handler URI.
	 * 
	 * Handler URI indirectly specifies which class is responsible to handle the task. The handler will execute reaction to a task lifecycle events such as executing the task, task heartbeat, etc.
	 * 
	 * @return handler URI
	 */
	public String getHandlerUri();
	
	/**
	 * Returns the stack of other handlers URIs.
	 * 
	 * The idea is that a task may have a chain of handlers, forming a stack. After a handler at the top
	 * of the stack finishes its processing, TaskManager will remove it from the stack and invoke
	 * the then-current handler. After that finishes, the next handler will be called, and so on,
	 * until the stack is empty.
	 *   
	 * @return
	 */
	public UriStack getOtherHandlersUriStack();
	
	/**
	 * Sets handler URI.
	 * 
	 * Handler URI indirectly specifies which class is responsible to handle the task. The handler will execute reaction to a task lifecycle events such as executing the task, task heartbeat, etc.
	 * 
	 * @param handlerUri new handler URI
	 */
	public void setHandlerUri(String handlerUri);
	
	/**
	 * Returns true if the task is asynchronous.
	 * 
	 * The asynchronous task is not executing in foreground. Therefore any thread that is not explicitly
	 * allocated for the task can be discarded. E.g. if a GUI thread detects that the task is asynchronous
	 * it knows that there is no point in waiting for the task result. It can just display appropriate
	 * message to the user (e.g. "please come back later") and return control back to the web container.
	 * 
	 * @return true if the task is asynchronous.
	 */
	public boolean isAsynchronous();
	
	// TODO
	public PrismObject<TaskType> getTaskPrismObject();
	
	/**
	 * Returns task (lightweight) identifier. This is an unique identification of any task,
	 * regardless whether it is persistent or transient (cf. OID). Therefore this can be used
	 * to identify all tasks, e.g. for the purposes of auditing and logging.
	 *  
	 * Task identifier is assigned automatically when the task is created. It is immutable.
	 *  
	 * @return task (lightweight) identifier
	 */
	public String getTaskIdentifier();
	
	/**
	 * Returns user that owns this task. It usually means the user that started the task
     * or a system used that is used to execute the task. The owner will be used to
     * determine access rights of the task, will be used for auditing, etc.
     * 
	 * @return task owner
	 */
	public PrismObject<UserType> getOwner();
	
	public void setOwner(PrismObject<UserType> owner);
	
	/**
	 * Returns task OID.
	 * 
	 * Only persistent tasks have OID. This returns null if the task is not persistent.
	 * 
	 * @return task OID
	 * 
	 */
	public String getOid();
	
	/**
	 * Sets task OID.
	 * 
	 * This method should not be used outside task manager. The OID should be considered read-only.
	 * 
	 * TODO: be stricter and maybe do not publish this method at all.
	 * 
	 * @param oid new task OID.
	 */
	public void setOid(String oid);
	
	/**
	 * Returns object that the task is associated with.
	 * 
	 * Tasks may be associated with a particular objects. For example a "import from resource" task is associated with the resource definition object that it imports from. Similarly for synchronization and reconciliation tasks (cycles). This is an optional property.
	 * 
	 * The object will only be returned if the task really contains an object without OID (e.g. unfinished account shadow). In all other cases this method may return null. Use getObjectRef instead.
	 * 
	 * Optional. May return null.
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 */
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;
	
	/**
	 * Returns reference to the object that the task is associated with.
	 * 
	 * Tasks may be associated with a particular objects. For example a "import from resource" task is associated with the resource definition object that it imports from. Similarly for synchronization and reconciliation tasks (cycles). This is an optional property.
	 * 
	 * @return
	 */
	public ObjectReferenceType getObjectRef();
	
	public void setObjectRef(ObjectReferenceType objectRef);
	
	/**
	 * Returns OID of the object that the task is associated with.
	 * 
	 * Convenience method. This will get the OID from the objectRef.
	 * 
	 */
	public String getObjectOid();

	/**
	 * Returns a top-level OperationResult stored in the task.
	 * 
	 * @return task operation result. 
	 */
	public OperationResult getResult();
		
	public Long getLastRunStartTimestamp();
	public Long getLastRunFinishTimestamp();
	public Long getNextRunStartTime();
	
	/**
	 * Returns human-readable name of the task.
	 * 
	 * @return human-readable name of the task.
	 */
	public String getName();
	
	/**
	 * Sets the human-readable name of the task.
	 * 
	 * @param name new human-readable name of the task.
	 */
	public void setName(String name);
	
	public long getProgress();
	
	/**
	 * Re-reads the task state from the persistent storage.
	 * 
	 * The task state may be synchronized with the repository all the time. But the specified timing is implementation-specific.
	 * Call to this method will make sure that the task contains fresh data.
	 * 
	 * This has no effect on transient tasks.
	 * @param result
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 */
	public void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;
	
	/**
	 * Returns task extension.
	 * 
	 * The extension is a part of task that can store arbitrary data.
	 * It usually holds data specific to a task type, internal task state,
	 * business state or similar data that are out of scope of this
	 * interface definition.
	 * 
	 * Although this methods returns list, it should be rather regarded as
	 * set. The list is used to avoid unnecessary reordering of properties
	 * in the storage (in case store is ordering-sensitive).
	 * 
	 * Returned list should be regarded as immutable. In case that the client
	 * does any change, weird things may happen.
	 * 
	 * @return task extension
	 */
	public PrismContainer getExtension();
	
	public PrismProperty getExtension(QName propertyName);
		
	/**
	 * Record finish of the last "run" of the task
	 * 
	 * TODO: better documentation
	 * 
	 * @param runResult result of the run to record
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 */
	public void recordRunFinish(TaskRunResult runResult,OperationResult parentResult) throws ObjectNotFoundException, SchemaException;
	
	public void recordRunStart(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

	/**
	 * Record progress of the task, storing it persistently if needed.
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 */
	public void recordProgress(long progress, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

	/**
	 * Return human-readable representation of the task content.
	 * Useful for diagnostics. May return multi-line string.
	 * @return human-readable representation of the task content
	 */
	public String dump();

	/**
	 * Close the task.
	 * 
	 * This will NOT release the task.
	 * 
	 * TODO
	 * 
	 * @param runnerRunOpResult
	 * @throws ObjectNotFoundException 
	 * @throws SchemaException 
	 */
	public void close(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

	public boolean isSingle();

	public boolean isCycle();

	public TaskBinding getBinding();
	
	public boolean isTightlyBound();
	
	public boolean isLooselyBound();

	public ScheduleType getSchedule();
	
	public void modify(Collection<? extends ItemDelta> modifications, OperationResult parentResult) 
			throws ObjectNotFoundException, SchemaException;

	/**
	 * Signal the task to shut down.
	 * It may not stop immediately, but it should stop eventually.
	 */
	public void shutdown();

	/**
	 * Returns true if the task can run (was not interrupted).
	 * 
	 * Will return false e.g. if shutdown was signaled.
	 * 
	 * @return true if the task can run
	 */
	public boolean canRun();

	/**
	 * Marks current handler as finished, and removes it from the handler stack.
	 * 
	 * This method *probably* should be called either implicitly by SingleRunner (after a handler 
	 * returns from run() method) or explicitly by task handler, in case of CycleRunner.
	 * TODO this has to be thought out a bit. 
	 */
	public void finishHandler();
}
