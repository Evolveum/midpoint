/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.StatisticsCollector;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Task instance - a logical unit of work that is either done synchronously, asynchronously, it is deferred, scheduled, etc.
 *
 * The classes that implement this interface hold a "java" task state. They represent the in-memory task data structure.
 * The instances must be able to serialize the state to the repository object (TaskType) when needed.
 *
 * The task implementation should be simple Java objects (POJOs). They are created also for a synchronous tasks, which means
 * they are created frequently. We want a low overhead for task management until the task is made persistent.
 *
 * API for modifying task properties works like this:
 *
 * - A getter (get<property-name>) reads data from the in-memory representation of a task.
 * - A setter (set<property-name>) writes data to the in-memory representation, and prepares a PropertyDelta to be
 *   written into repository later (of course, only for persistent tasks).
 *
 * PropertyDeltas should be then written by calling flushPendingModifications method.
 *
 * In case you want to write property change into the repository immediately, you have to use
 * set<property-name>Immediate method. In that case, the property change does not go into
 * the list of pending modifications, but it is instantly written into the repository
 * (so the method uses OperationResult as parameter, and can throw relevant exceptions as well).
 *
 * @author Radovan Semancik
 * @author Pavol Mederly
 *
 */
public interface Task extends DebugDumpable, StatisticsCollector {

    String DOT_INTERFACE = Task.class.getName() + ".";

    // =================================================================== Basic information (ID, owner)

    /**
     * Returns task (lightweight) identifier. This is an unique identification of any task,
     * regardless whether it is persistent or transient (cf. OID). Therefore this can be used
     * to identify all tasks, e.g. for the purposes of auditing and logging.
     *
     * Task identifier is assigned automatically when the task is created. It is immutable.
     *
     * @return task (lightweight) identifier
     */
    String getTaskIdentifier();

    /**
     * Returns task OID.
     *
     * Only persistent tasks have OID. This returns null if the task is not persistent.
     *
     * @return task OID
     *
     */
    String getOid();

    /**
     * Returns user that owns this task. It usually means the user that started the task
     * or a system used that is used to execute the task. The owner will be used to
     * determine access rights of the task, will be used for auditing, etc.
     *
     * @return task owner
     */
    PrismObject<? extends FocusType> getOwner();

    /**
     * Sets the task owner.
     *
     * BEWARE: sets the owner only for in-memory information. So do not call this method for persistent tasks!
     * (until fixed)
     */
    void setOwner(PrismObject<? extends FocusType> owner);

    /**
     * Returns human-readable name of the task.
     */
    PolyStringType getName();

    /**
     * Sets the human-readable name of the task.
     */
    void setName(PolyStringType value);

    /**
     * Sets the human-readable name of the task.
     */
    void setName(String value);

    /**
     * Sets the human-readable name of the task, immediately into repository.
     */
    @SuppressWarnings("unused")
    void setNameImmediate(PolyStringType value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    /**
     * Returns task description.
     */
    String getDescription();

    /**
     * Sets task description.
     */
    void setDescription(String value);

    /**
     * Sets task description, immediately storing it into the repo.
     */
    @SuppressWarnings("unused")
    void setDescriptionImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Gets the policy rule defined for the task (for running task the returned value is a clone).
     */
    PolicyRuleType getPolicyRule();

    // =================================================================== Execution status

    /**
     * Returns execution status.
     *
     * @see TaskExecutionStatus
     *
     * @return execution status.
     */
    TaskExecutionStatus getExecutionStatus();

    /**
     * Status-changing method. It changes task's execution status to WAITING.
     * Currently use only on transient tasks, on suspended tasks or from within task handler.
     */
    void makeWaiting();

    /**
     * Changes exec status to WAITING, with a given waiting reason.
     * Currently use only on transient tasks or from within task handler.
     */
    void makeWaiting(TaskWaitingReason reason);

    void makeWaiting(TaskWaitingReason reason, TaskUnpauseActionType unpauseAction);

    /**
     * Status-changing method. It changes task's execution status to RUNNABLE.
     * Currently use ONLY on transient tasks.
     */
    @SuppressWarnings("unused")
    void makeRunnable();

    /**
     * Sets task execution status. Can be used only for transient tasks (for safety reasons).
     * However, it is better to use specific state-changing methods (makeWaiting, makeRunnable, ...).
     *
     * @see TaskExecutionStatus
     *
     * @param value new task execution status.
     */
    void setInitialExecutionStatus(TaskExecutionStatus value);

    /**
     * Returns true if the task is closed.
     */
    boolean isClosed();

    /**
     * Returns the completion timestamp - time when the task was closed (or null if it is not closed).
     */
    Long getCompletionTimestamp();

    /**
     * Returns the task waiting reason for a WAITING task.
     */
    TaskWaitingReason getWaitingReason();

    /**
     * Returns the node the task is currently executing at, based on repository information.
     * This is present in all cases, however, it might be out-of-date, e.g. when node crashes.
     */
    String getNode();

    String getNodeAsObserved();

    // =================================================================== Persistence and asynchrony

    /**
     * Returns task persistence status.
     *
     * @see TaskPersistenceStatus
     *
     * @return task persistence status.
     */
    TaskPersistenceStatus getPersistenceStatus();

    /**
     * Returns true if task is transient (i.e. not stored in repository).
     */
    boolean isTransient();

    /**
     * Returns true if task is persistent (i.e. stored in repository).
     */
    boolean isPersistent();

    /**
     * Returns true if the task is asynchronous.
     *
     * The asynchronous task is not executing in foreground. Therefore any thread that is not explicitly
     * allocated for the task can be discarded. E.g. if a GUI thread detects that the task is asynchronous
     * it knows that there is no point in waiting for the task result. It can just display appropriate
     * message to the user (e.g. "please come back later") and return control back to the web container.
     *
     * Usually, asynchronous means the same as persistent. However, there can are lightweight tasks
     * that are asynchronous but not stored in repository.
     *
     * @return true if the task is asynchronous.
     */
    boolean isAsynchronous();

    // ============================================================================================ Scheduling

    /**
     * Returns task recurrence status.
     *
     * @return task recurrence status
     */
    TaskRecurrence getRecurrenceStatus();

    /**
     * Checks whether the task is single-run.
     */
    boolean isSingle();

    /**
     * Checks whether the task is a cyclic (recurrent) one.
     */
    boolean isRecurring();

    /**
     * Makes a task recurring, with a given schedule.
     */
    @SuppressWarnings("unused")
    void makeRecurring(ScheduleType schedule);

    /**
     * Makes a task recurring, running in a fixed time intervals.
     * @param interval interval to run the task (in seconds)
     */
    @SuppressWarnings("unused")
    void makeRecurringSimple(int interval);

    /**
     * Makes a task recurring, running according to a cron-like schedule.
     * @param cronLikeSpecification schedule specification
     */
    @SuppressWarnings("unused")
    void makeRecurringCron(String cronLikeSpecification);

    /**
     * Makes a task single-run, with no particular schedule.
     */
    void makeSingle();

    /**
     * Makes a task single-run, with a given schedule.
     */
    void makeSingle(ScheduleType schedule);

    TaskExecutionConstraintsType getExecutionConstraints();

    void setExecutionConstraints(TaskExecutionConstraintsType value);

    String getGroup();

    @NotNull
    Collection<String> getGroups();

    @NotNull
    Map<String, Integer> getGroupsWithLimits();

    /**
     * Returns the schedule.
     */
    ScheduleType getSchedule();

    Integer getScheduleInterval();

    boolean hasScheduleInterval();

    /**
     * Returns the time when the task last run was started (or null if the task was never started).
     */
    Long getLastRunStartTimestamp();

    /**
     * Returns the time when the task last run was finished (or null if the task was not finished yet).
     */
    Long getLastRunFinishTimestamp();

    /**
     * Returns the time when the task should start again.
     */
    Long getNextRunStartTime(OperationResult parentResult);

    /**
     * Returns thread stop action (what happens when the task thread is stopped e.g. because of node going down).
     */
    ThreadStopActionType getThreadStopAction();

    /**
     * Sets the thread stop action for this task.
     */
    void setThreadStopAction(ThreadStopActionType value);

    /**
     * Resilient tasks are those that survive node shutdown.
     * I.e. their ThreadStopAction is either 'restart' or 'reschedule'.
     */
    @SuppressWarnings("unused")
    boolean isResilient();

    // ============================================================================================ Binding

    /**
     * Returns task binding.
     */
    TaskBinding getBinding();

    /**
     * Returns true if the task is tightly bound.
     */
    boolean isTightlyBound();

    /**
     * Returns true if the task is loosely bound.
     */
    boolean isLooselyBound();

    /**
     * Sets the binding for this task.
     */
    void setBinding(TaskBinding value);

    /**
     * Sets the binding (immediately through to the repo).
     */
    void setBindingImmediate(TaskBinding value, OperationResult parentResult)
        throws ObjectNotFoundException, SchemaException;


    // ============================================================================================ Handler(s)

    /**
     * Returns handler URI.
     *
     * Handler URI indirectly specifies which class is responsible to handle the task. The handler will execute
     * reaction to a task lifecycle events such as executing the task, task heartbeat, etc.
     *
     * @return handler URI
     */
    String getHandlerUri();

    /**
     * Sets handler URI.
     *
     * Handler URI indirectly specifies which class is responsible to handle the task. The handler will execute
     * reaction to a task lifecycle events such as executing the task, task heartbeat, etc.
     *
     * @param value new handler URI
     */
    void setHandlerUri(String value);

    /**
     * Sets handler URI, also immediately in the repository.
     */
    @SuppressWarnings("unused")
    void setHandlerUriImmediate(String value, OperationResult parentResult) throws ObjectNotFoundException,    SchemaException;

    /**
     * Returns the stack of other handlers URIs.
     *
     * The idea is that a task may have a chain of handlers, forming a stack. After a handler at the top
     * of the stack finishes its processing, TaskManager will remove it from the stack and invoke
     * the then-current handler. After that finishes, the next handler will be called, and so on,
     * until the stack is empty.
     */
    UriStack getOtherHandlersUriStack();

    /**
     * Pushes a new handler URI onto the stack of handlers. This means that the provided handler URI becomes the
     * current one. Current one becomes the first one on the stack of other handlers, etc.
     *
     * So the newly added handler will be started FIRST.
     *
     * Care must be taken not to interfere with the execution of a task handler. It is recommended to call this
     * method when it is sure that no handler is executing.
     *
     * Alongwith URI, other information are set, namely schedule, binding, and parameters that will be put into
     * task extension when the handler URI will be invoked.
     *
     * @param uri Handler URI to be put onto the stack.
     * @param schedule Schedule to be used to run the handler.
     * @param binding Binding to be used to run the handler.
     * @param extensionDeltas The feature is EXPERIMENTAL, do not use if not absolutely necessary.
     */
    void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, Collection<ItemDelta<?,?>> extensionDeltas);

    /**
     * Same as above, with one extension delta (not a collection of them).
     * @param delta EXPERIMENTAL, do not use if not absolutely necessary.
     */
    void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, ItemDelta<?,?> delta);

    /**
     * Same as above, with no extension deltas.
     */
    void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding);

    /**
     * Removes current handler from the handlers stack. Closes task if that was the last handler.
     *
     * USE WITH CARE. Normally, this is used implicitly in the task execution routine and there's no need for you
     * to call this from your code.
     */
    void finishHandler(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Task category is a user-oriented term, hinting on what 'kind' of task is the one being considered
     * (system task, workflow, live sync, ...). In most cases, category can be derived from the task handler.
     *
     * Category can be set directly; but if not set directly, it is set automatically on first task execution,
     * determined based on task handler URI.
     *
     * List of categories is in the TaskCategory class.
     */
    String getCategory();

    /**
     * Sets the task category.
     */
    void setCategory(String category);


    // ============================================================================================ Task extension.
    // -------------------------------------------------------------------------------- Task extension - GET

    /**
     * Returns task extension.
     *
     * The extension is a part of task that can store arbitrary data.
     * It usually holds data specific to a task type, internal task state,
     * business state or similar data that are out of scope of this
     * interface definition.
     *
     * To maintain thread safety, for RunningTask this method returns extension clone.
     * (So don't use the return value to modify the task extension if not sure whether the task is running.)
     *
     * @return task extension
     */
    PrismContainer<? extends ExtensionType> getExtensionOrClone();
    @NotNull PrismContainer<? extends ExtensionType> getOrCreateExtension() throws SchemaException;
    PrismContainer<? extends ExtensionType> getExtensionClone();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean hasExtension();

    /**
     * Returns specified property from the extension
     * @return null if extension or property does not exist.
     */
    <T> PrismProperty<T> getExtensionPropertyOrClone(ItemName propertyName);

    /**
     * Returns specified single-valued property real value from the extension
     * @return null if extension or property does not exist.
     */
    <T> T getExtensionPropertyRealValue(ItemName propertyName);

    /**
     * Returns specified single-valued container real value from the extension
     * To ensure thread safety, in the case of running tasks the returned value is a clone of the live one.
     *
     * @return null if extension or container does not exist.
     */
    <T extends Containerable> T getExtensionContainerRealValueOrClone(ItemName containerName);

    /**
     * Returns specified reference from the extension.
     * @return null if extension or reference does not exist.
     */
    @SuppressWarnings("unused")
    PrismReference getExtensionReferenceOrClone(ItemName name);

    /**
     * Returns specified item (property, reference or container) from the extension.
     * @return null if extension or item does not exist
     *
     * To maintain thread safety, for running tasks returns a clone of the original item.
     */
    <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> getExtensionItemOrClone(ItemName itemName);

    // -------------------------------------------------------------------------- Task extension - SET (replace values)

    /**
     * Sets a property in the extension - replaces existing value(s), if any, by the one(s) provided.
     */
    void setExtensionProperty(PrismProperty<?> property) throws SchemaException;

    /**
     * "Immediate" version of the above method.
     */
    void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Sets (i.e., replaces) the value of the given property in task extension.
     * @param propertyName name of the property
     * @param value value of the property
     */
    <T> void setExtensionPropertyValue(QName propertyName, T value) throws SchemaException;

    /**
     * Sets (i.e., replaces) the value of the given property in task extension - without writing to repo.
     * @param propertyName name of the property
     * @param value value of the property
     */
    @SuppressWarnings("unused")
    <T> void setExtensionPropertyValueTransient(QName propertyName, T value) throws SchemaException;

    /**
     * Sets a reference in the extension - replaces existing value(s), if any, by the one(s) provided.
     */
    void setExtensionReference(PrismReference reference) throws SchemaException;

    /**
     * Sets a container in the extension - replaces existing value(s), if any, by the one(s) provided.
     * @param item Container with value(s) to be put into task extension.
     */
    <C extends Containerable> void setExtensionContainer(PrismContainer<C> item) throws SchemaException;

    /**
     * Sets a container value in the extension - replaces existing value(s), if any, by the one provided.
     * @param containerName name of the container
     * @param value value to be put into extension
     */
    <T extends Containerable> void setExtensionContainerValue(QName containerName, T value) throws SchemaException;

    /**
     * Puts generic item into extension.
     */
    @SuppressWarnings("unused")
    void setExtensionItem(Item item) throws SchemaException;

    // ---------------------------------------------------------------------------- Task extension - ADD (add values)

    /**
     * Adds value(s) to a given extension property.
     * @param property holder of the value(s) to be added into task extension property
     */
    void addExtensionProperty(PrismProperty<?> property) throws SchemaException;

    /**
     * Adds value(s) to a given extension reference.
     * @param reference holder of the value(s) to be added into task extension reference
     */
    @SuppressWarnings("unused")
    void addExtensionReference(PrismReference reference) throws SchemaException;

    // ---------------------------------------------------------------------- Task extension - DELETE (delete values)

    /**
     * Removes specified VALUES of this extension property (not all of its values).
     */
    @SuppressWarnings("unused")
    void deleteExtensionProperty(PrismProperty<?> property) throws SchemaException;

    // --------------------------------------------------------------------------- Task extension - OTHER

    /**
     * Modifies task extension using given delta.
     */
    void modifyExtension(ItemDelta itemDelta) throws SchemaException;


    // ============================================================================================ Task object

    /**
     * Returns object that the task is associated with.
     *
     * Tasks may be associated with a particular objects. For example a "import from resource" task is associated
     * with the resource definition object that it imports from. Similarly for synchronization and reconciliation
     * tasks (cycles). User approval and modification task may be associated with that user.
     *
     * This is an optional property.
     *
     * The object will only be returned if the task really contains an object without OID (e.g. unfinished
     * account shadow). In all other cases this method may return null. Use getObjectRefOrClone instead.
     *
     * Optional. May return null.
     */
    <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Returns reference to the object that the task is associated with.
     *
     * Tasks may be associated with a particular objects. For example a "import from resource" task is associated
     * with the resource definition object that it imports from. Similarly for synchronization and reconciliation
     * tasks (cycles). This is an optional property.
     */
    ObjectReferenceType getObjectRefOrClone();

    /**
     * Sets the object reference.
     */
    void setObjectRef(ObjectReferenceType objectRef);

    /**
     * Sets the object reference.
     */
    void setObjectRef(String oid, QName type);

    /**
     * "Immediate" version of the previous method.
     */
    @SuppressWarnings("unused")
    void setObjectRefImmediate(ObjectReferenceType value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    /**
     * Sets the "task object" in the in-memory task representation (i.e. not in the repo).
     */
    void setObjectTransient(PrismObject object);

    /**
     * Returns OID of the object that the task is associated with.
     *
     * Convenience method. This will get the OID from the objectRef.
     */
    String getObjectOid();

    // ====================================================================================== Task result and progress

    /**
     * Returns a top-level OperationResult stored in the task.
     * Beware of thread safety. This is a live object!
     * @return task operation result.
     */
    OperationResult getResult();

    void setResultTransient(OperationResult result);

    /**
     * Returns the status of top-level OperationResult stored in the task.
     *
     * @return task operation result status
     */
    OperationResultStatusType getResultStatus();

    /**
     * Sets the top-level OperationResult stored in the task.
     */
    void setResult(OperationResult result);

    /**
     * "Immediate" version of above method.
     */

    void setResultImmediate(OperationResult result, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Returns task progress, as reported by task handler.
     */
    long getProgress();

    /**
     * Record progress of the task, storing it persistently if needed.
     */
    void setProgress(Long value);

    /**
     * "Immediate" version of the above method.
     */
    void setProgressImmediate(Long progress, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    void setProgressTransient(Long value);

    OperationStatsType getStoredOperationStats();

    /**
     * Returns expected total progress.
     */
    @Nullable
    Long getExpectedTotal();

    /**
     * Stores expected total progress of the task, storing it persistently if needed.
     */
    void setExpectedTotal(Long value);

    /**
     * "Immediate" version of the above method.
     */
    void setExpectedTotalImmediate(Long value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    // ===================================================================== Working with subtasks and dependent tasks

    /**
     * Creates a transient subtask.
     *
     * Owner is inherited from parent task to subtask.
     */
    Task createSubtask();

    /**
     * Returns the identifier of the task's parent (or null of there is no parent task).
     */
    String getParent();

    /**
     * Returns the parent task, if any.
     */
    Task getParentTask(OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Lists the (direct) subtasks of a given task.
     */
    @NotNull
    default List<Task> listSubtasks(OperationResult parentResult) throws SchemaException {
        return listSubtasks(false, parentResult);
    }

    @NotNull
    List<Task> listSubtasks(boolean persistentOnly, OperationResult parentResult) throws SchemaException;

    /**
     * List all the subtasks of a given task, i.e. whole task tree rooted at the current task.
     * Current task is not contained in the returned list.
     */
    default List<Task> listSubtasksDeeply(OperationResult result) throws SchemaException {
        return listSubtasksDeeply(false, result);
    }

    List<Task> listSubtasksDeeply(boolean persistentOnly, OperationResult result) throws SchemaException;

    /**
     * Lists all explicit dependents, i.e. tasks that wait for the completion of this tasks (that depend on it).
     * Implicit dependents, i.e. task's parent, grandparent, etc are NOT listed here.
     */
    List<Task> listDependents(OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Lists all explicit dependents' identifiers.
     */
    List<String> getDependents();

    /**
     * Add a task as this task's dependent, i.e. the task denoted by taskIdentifier DEPENDS ON (waits for completion of)
     * this task.
     */
    void addDependent(String taskIdentifier);

    /**
     * Deletes a task from the list of dependents of this task.
     */
    @SuppressWarnings("unused")
    void deleteDependent(String taskIdentifier);

    /**
     * List all prerequisite tasks for the current tasks, i.e. tasks that must complete before this one can proceed.
     * If A is on the list of prerequisities of B (THIS), it means that B is on list of dependents of A (i.e.
     * B waits for A to complete).
     *
     * Again, implicit prerequisities (children) are not listed here.
     */

    List<Task> listPrerequisiteTasks(OperationResult parentResult) throws SchemaException;

    /**
     * Starts "passive" waiting for other tasks.
     *
     * Precondition: The task must already be in WAITING state.
     * Postcondition: If there are any tasks to wait for, task remains in WAITING/OTHER_TASKS state.
     * However, if there are no tasks to wait for, task is either unpaused (if there is any handler) or closed (if there is not).
     *
     * Passive waiting consists of putting the task into WAITING/OTHER_TASKS state. Unpausing it is the responsibility
     * of task manager - it does it when any of prerequisite tasks closes. At that moment, task manager checks all
     * dependent tasks (explicit or implicit) of the closing task, and unpauses these, which can be unpaused.
     */
    void startWaitingForTasksImmediate(OperationResult result) throws SchemaException, ObjectNotFoundException;

    // ====================================================================================== Supplementary information

    /**
     * Returns change channel URI.
     */
    String getChannel();

    /**
     * Sets change channel URI.
     */
    void setChannel(String channelUri);

    /**
     * Sets change channel URI.
     */
    @SuppressWarnings("unused")
    void setChannelImmediate(String channelUri, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Gets the requestee OID - typically an identification of account owner (for notifications).
     * Serves for communication between model and provisioning.
     * It is a temporary feature - will be removed in midPoint 2.3.
     */

    PrismObject<UserType> getRequestee();

    void setRequesteeTransient(PrismObject<UserType> user);

    // not thread-safe!
    LensContextType getModelOperationContext();

    void setModelOperationContext(LensContextType modelOperationContext) throws SchemaException;

    // not thread-safe!
    TaskExecutionEnvironmentType getExecutionEnvironment();

    void setExecutionEnvironment(TaskExecutionEnvironmentType value);

    @SuppressWarnings("unused")
    void setExecutionEnvironmentTransient(TaskExecutionEnvironmentType value);

    // ====================================================================================== Other methods

    /**
     * Returns backing task prism object.
     * AVOID use of this method if possible.
     * - for regular tasks it has to update operation result in the prism object (might be costly)
     * - for running tasks it provides a clone of the actual prism object (even more costly and leads to lost changes
     *   if the returned value is changed)
     */
    @NotNull
    PrismObject<TaskType> getUpdatedOrClonedTaskObject();

    /**
     * Returns backing task prism object, provided that task is not running.
     * Beware that the task operation result is updated (might be costly).
     * @throws IllegalStateException if task is running
     */
    @NotNull
    PrismObject<TaskType> getUpdatedTaskObject();

    /**
     * Returns cloned task object.
     */
    @NotNull
    PrismObject<TaskType> getClonedTaskObject();

    /**
     * Re-reads the task state from the persistent storage.
     *
     * The task state may be synchronized with the repository all the time. But the specified timing is implementation-specific.
     * Call to this method will make sure that the task contains fresh data.
     *
     * This has no effect on transient tasks.
     */
    void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Changes in-memory representation immediately and schedules a corresponding batched modification.
     */
    void modify(ItemDelta<?, ?> delta) throws SchemaException;
    void modify(Collection<ItemDelta<?, ?>> deltas) throws SchemaException;

    /**
     * Changes in-memory and in-repo representations immediately.
     */
    void modifyAndFlush(ItemDelta<?, ?> delta, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException;

    /**
     * Saves modifications done against the in-memory version of the task into the repository.
     */
    void flushPendingModifications(OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, ObjectAlreadyExistsException;

    /**
     * Returns a list of pending modifications for this task.
     */
    @SuppressWarnings("unused")
    Collection<ItemDelta<?,?>> getPendingModifications();

    // TODO move into RunningTask?
    void close(OperationResult taskResult, boolean saveState, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    // not thread-safe!
    TaskWorkManagementType getWorkManagement();

    // not thread-safe!
    TaskWorkStateType getWorkState();

    TaskKindType getKind();

    TaskUnpauseActionType getUnpauseAction();

    TaskExecutionStatusType getStateBeforeSuspend();

    @SuppressWarnings("unused")
    boolean isPartitionedMaster();

    String getExecutionGroup();

    /**
     * Gets information from the current task and - for running task - its transient subtasks (aka worker threads).
     */
    OperationStatsType getAggregatedLiveOperationStats();

    ObjectReferenceType getSelfReference();

    String getVersion();

    /**
     * NEVER modify objects returned in multithreaded environments!
     */
    Collection<? extends TriggerType> getTriggers();

    /**
     * NEVER modify objects returned in multithreaded environments!
     */
    Collection<? extends AssignmentType> getAssignments();

    Collection<Task> getPathToRootTask(OperationResult parentResult) throws SchemaException;

    String getTaskTreeId(OperationResult result) throws SchemaException;

    default boolean hasAssignments() {
        return !getAssignments().isEmpty();
    }

    ObjectReferenceType getOwnerRef();

    // Returns immutable collection of caching profiles
    @NotNull
    Collection<String> getCachingProfiles();

    String getOperationResultHandlingStrategyName();

    boolean isScavenger();

    @NotNull
    Collection<TracingRootType> getTracingRequestedFor();

    void addTracingRequest(TracingRootType point);

    void removeTracingRequests();

    // Not thread safe.
    TracingProfileType getTracingProfile();

    void setTracingProfile(TracingProfileType tracingProfile);
}
