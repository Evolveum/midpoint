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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

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
 * API for modifying task properties works like this: 
 * 
 * - A getter (get<property-name>) reads data from the in-memory representation of a task.
 * - A setter (set<property-name>) writes data to the in-memory representation, and prepares a PropertyDelta to be
 *   written into repository later (of course, only for persistent tasks).
 *   
 * PropertyDeltas should be then written by calling savePendingModifications method.
 * 
 * In case you want to write property change into the repository immediately, you have to use
 * set<property-name>Immediate method. In that case, the property change does not go into
 * the list of pending modifications, but it is instantly written into the repository
 * (so the method uses OperationResult as parameter, and can throw relevant exceptions as well).  
 * 
 * @author Radovan Semancik
 *
 */
public interface Task extends Dumpable {

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

	/*
	 * Standard getters and setters.
	 */
	
	/**
	 * Returns execution status.
	 * 
	 * @see TaskExecutionStatus
	 * 
	 * @return execution status.
	 */
	public TaskExecutionStatus getExecutionStatus();

    /**
     * Status-changing method. It changes task's execution status to WAITING.
     * Use with care, currently only on transient tasks.
     */

    public void makeWaiting();

    /**
     * Status-changing method. It changes task's execution status to RUNNABLE.
     * Use with care, currently only on transient tasks.
     */

    public void makeRunnable();

    /**
     * Returns the node the task is currently executing at, based on real run-time information.
     *
     * BEWARE, this information is valid only when returned from searchTasks
     * (not e.g. when got via getTask).
     *
     * @return
     */
    public Node currentlyExecutesAt();
	
	/**
	 * Sets task execution status. Can be used only for transient tasks (for safety reasons).
     *
	 * @see TaskExecutionStatus
	 * 
	 * @param value new task execution status.
	 */
	public void setInitialExecutionStatus(TaskExecutionStatus value);

	/**
	 * Sets task execution status, in memory as well as in repository (for persistent tasks). 
	 * 
	 * @see TaskExecutionStatus
	 * 
	 * @param executionStatus new task execution status.
	 */
//	public void setExecutionStatusImmediate(TaskExecutionStatus value, OperationResult result) throws ObjectNotFoundException, SchemaException;

	/**
	 * Returns task persistence status.
	 * 
	 * @see TaskPersistenceStatus
	 * 
	 * @return task persistence status.
	 */
	public TaskPersistenceStatus getPersistenceStatus();

    boolean isTransient();

    boolean isPersistent();
	
	/**
	 * Returns task recurrence status.
	 * 
	 * @return task recurrence status
	 */
	public TaskRecurrence getRecurrenceStatus();

	/**
	 * Checks whether the task is single-run.
	 */
	public boolean isSingle();

	/**
	 * Checks whether the task is a cyclic (recurrent) one.
	 */
	public boolean isCycle();

//	void setRecurrenceStatus(TaskRecurrence value);
//
//	void setRecurrenceStatusImmediate(TaskRecurrence value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;
	
	/**
	 * Returns the schedule. Note that if the task is 'single-run' (not recurrent), the schedule is ignored.
	 * And the other way around, if the task is recurrent, the schedule must be present (otherwise the results
	 * are unpredictable).
	 */
	
	public ScheduleType getSchedule();
	
	public TaskBinding getBinding();
	
	public boolean isTightlyBound();
	
	public boolean isLooselyBound();

	void setBinding(TaskBinding value);

	void setBindingImmediate(TaskBinding value, OperationResult parentResult)
		throws ObjectNotFoundException, SchemaException;

	/**
	 * Returns handler URI.
	 * 
	 * Handler URI indirectly specifies which class is responsible to handle the task. The handler will execute reaction to a task lifecycle events such as executing the task, task heartbeat, etc.
	 * 
	 * @return handler URI
	 */
	public String getHandlerUri();

	/**
	 * Sets handler URI.
	 * 
	 * Handler URI indirectly specifies which class is responsible to handle the task. The handler will execute reaction to a task lifecycle events such as executing the task, task heartbeat, etc.
	 * 
	 * @param value new handler URI
	 */
	void setHandlerUri(String value);
	
	void setHandlerUriImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException,	SchemaException;

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
	
	// TODO: sets owner in-memory only (for now)
	public void setOwner(PrismObject<UserType> owner);

    /**
     * Works with the 'requestee', i.e. the user about who is the request.
     * Currently implemented via task extension. TODO decide how to do that seriously.
     *
     * TODO FIXME: xxxRequesteeRef methods currently do not work because of problem in RAnyConverter, use xxxRequesteeOid instead
     */
    public PrismReference getRequesteeRef();
    public void setRequesteeRef(PrismReferenceValue reference) throws SchemaException;
    public void setRequesteeRef(PrismObject<UserType> requestee) throws SchemaException;

    public String getRequesteeOid();
    public void setRequesteeOid(String oid) throws SchemaException;
    public void setRequesteeOidImmediate(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
	 * Returns change channel URI.
	 */
	public String getChannel();
	
	/**
	 * Sets change channel URI.
	 */
	public void setChannel(String channelUri);
	
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
	
	// TODO: provide "persistent version" of this method
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

	public void setResult(OperationResult result);

	public void setResultImmediate(OperationResult result, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;
	
	/**
	 * Returns the time when the task last run was started (or null if the task was never started).
	 */
	public Long getLastRunStartTimestamp();
	
	/**
	 * Returns the time when the task last run was finished (or null if the task was not finished yet).
	 */
	public Long getLastRunFinishTimestamp();
	
	/**
	 * Returns the time when the task should start again. (For non-recurrent tasks it is ignored. For recurrent tasks,
	 * null value means 'start immediately', if missed schedule tolerance does not prevent it.)  
	 */
	public Long getNextRunStartTime(OperationResult parentResult);

	/**
	 * Returns human-readable name of the task.
	 * 
	 * @return human-readable name of the task.
	 */
	public PolyStringType getName();
	
	/**
	 * Sets the human-readable name of the task.
	 * 
	 * @param value new human-readable name of the task.
	 */
	public void setName(PolyStringType value);
    public void setName(String value);
	
	public void setNameImmediate(PolyStringType value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

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
	 * @return task extension
	 */
	public <C extends Containerable> PrismContainer<C> getExtension();
	
	public <T> PrismProperty<T> getExtension(QName propertyName);

    public <T extends PrismValue> Item<T> getExtensionItem(QName propertyName);

    public <C extends Containerable> void setExtensionContainer(PrismContainer<C> item) throws SchemaException;

    public void setExtensionReference(PrismReference reference) throws SchemaException;

    public void setExtensionProperty(PrismProperty<?> property) throws SchemaException;
	
	public void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

	// TODO
	public long getProgress();

	/**
	 * Record progress of the task, storing it persistently if needed.
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 */
	public void setProgress(long value);

	public void setProgressImmediate(long progress, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

	/*
	 * Other methods
	 */
	
	// TODO
	public PrismObject<TaskType> getTaskPrismObject();

	/**
	 * Re-reads the task state from the persistent storage.
	 * 
	 * The task state may be synchronized with the repository all the time. But the specified timing is implementation-specific.
	 * Call to this method will make sure that the task contains fresh data.
	 * 
	 * This has no effect on transient tasks.
	 * @param parentResult
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 */
	public void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

	/*
	 * Please do not use this method; use specific setters (e.g. setName, setExtensionProperty, and so on) instead.
	 */
//	@Deprecated
//	public void modify(Collection<? extends ItemDelta> modifications, OperationResult parentResult) 
//			throws ObjectNotFoundException, SchemaException;

	/**
	 * Record finish of the last "run" of the task
	 * 
	 * TODO: better documentation
	 * 
	 * @param runResult result of the run to record
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 */
//	public void recordRunFinish(TaskRunResult runResult,OperationResult parentResult) throws ObjectNotFoundException, SchemaException;
	
//	public void recordRunStart(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

	
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
//	public void close(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;


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
//	public void finishHandler(OperationResult parentResult) throws ObjectNotFoundException,
//	SchemaException;
	
	// TODO
	void savePendingModifications(OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException, ObjectAlreadyExistsException;

    /**
     * Categories are treated in a special way. They can be set directly. But if not set directly, they
     * are set on first task execution - determined based on task handler URI.
     *
     * List of categories is in the
     * @see
     * @return
     */
    String getCategory();

    void makeRecurrentSimple(int interval);

    void makeRecurrentCron(String cronLikeSpecification);

    void makeSingle();

    String getNode();

    OperationResultStatusType getResultStatus();

    ThreadStopActionType getThreadStopAction();

    /**
     * Resilient tasks are those that survive node shutdown.
     * I.e. their ThreadStopAction is either 'restart' or 'reschedule'.
     * @return
     */

    boolean isResilient();

    //void pushHandlerUri(String uri, ScheduleType scheduleType);

    void setCategory(String category);


    void setDescriptionImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    void setDescription(String value);

    String getDescription();

    void addExtensionProperty(PrismProperty<?> property) throws SchemaException;

    <T> void setExtensionPropertyValue(QName propertyName, T value) throws SchemaException;

    void modifyExtension(ItemDelta itemDelta) throws SchemaException;

    /**
     * Removes specified VALUES of this extension property (not all of its values).
     *
     * @param property
     * @throws SchemaException
     */
    void deleteExtensionProperty(PrismProperty<?> property) throws SchemaException;

//    void replaceCurrentHandlerUri(String newUri, ScheduleType scheduleType);

    void setThreadStopAction(ThreadStopActionType value);

    void makeRecurrent(ScheduleType schedule);

    void makeSingle(ScheduleType schedule);

    /**
     * Creates a subtask
     *
     * @return
     */

    Task createSubtask();

    /**
     * Waits for subtasks to finish. Executes a special task handler which periodically tests for the completion
     * of this tasks' children.
     *
     * SHOULD BE USED ONLY FROM A TASK HANDLER.
     *
     * Returns a TaskRunResult that should the task handler immediately return (in order to activate newly created 'waiting' task handler).
     */
    TaskRunResult waitForSubtasks(Integer interval, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    TaskRunResult waitForSubtasks(Integer interval, Collection<ItemDelta<?>> extensionDeltas, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    String getParent();

    //void pushHandlerUri(String uri);

    void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding);

    void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, Collection<ItemDelta<?>> extensionDeltas);

    ItemDelta<?> createExtensionDelta(PrismPropertyDefinition definition, Object realValue);

    void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, ItemDelta<?> delta);

    void finishHandler(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    List<PrismObject<TaskType>> listSubtasksRaw(OperationResult parentResult) throws SchemaException;

    List<Task> listSubtasks(OperationResult parentResult) throws SchemaException;

    List<PrismObject<TaskType>> listPrerequisiteTasksRaw(OperationResult parentResult) throws SchemaException;

    List<Task> listPrerequisiteTasks(OperationResult parentResult) throws SchemaException;

    void addDependent(String taskIdentifier);

    void startWaitingForTasksImmediate(OperationResult result) throws SchemaException, ObjectNotFoundException;


    List<String> getDependents();

    void deleteDependent(String value);

    List<Task> listDependents(OperationResult result) throws SchemaException, ObjectNotFoundException;

    Task getParentTask(OperationResult result) throws SchemaException, ObjectNotFoundException;

    TaskWaitingReason getWaitingReason();

    boolean isClosed();

    void makeWaiting(TaskWaitingReason reason);

    void pushWaitForTasksHandlerUri();
}
