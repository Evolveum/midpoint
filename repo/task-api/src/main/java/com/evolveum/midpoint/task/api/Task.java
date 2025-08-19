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

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;
import com.evolveum.midpoint.schema.statistics.StatisticsCollector;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.util.task.ActivityStateOverviewUtil.ACTIVITY_TREE_STATE_OVERVIEW_PATH;

/**
 * Task instance - a logical unit of work.
 *
 * It is executed either synchronously or asynchronously.
 * When running asynchronously, it can be scheduled, suspended, or resumed.
 *
 * The classes that implement this interface hold a "java" task state. They represent the in-memory task data structure.
 * The instances must be able to serialize the state to the repository object (TaskType) when needed.
 *
 * The task implementation should be simple Java objects (POJOs). They are created also for a synchronous tasks, which means
 * they are created frequently. We want a low overhead for task management until the task is made persistent.
 *
 * API for modifying task properties works like this:
 *
 * - A getter (getItemName) reads data from the in-memory representation of a task.
 * - A setter (setItemName) writes data to the in-memory representation, and (for persistent tasks)
 * prepares a delta to be written into repository later.
 *
 * The deltas should be then written by calling {@link #flushPendingModifications(OperationResult)} method.
 *
 * In case you want to write property change into the repository immediately, you have to use
 * setItemNameImmediate method. In that case, the change does not go into
 * the list of pending modifications, but it is instantly written into the repository
 * (so the method uses OperationResult as parameter, and can throw relevant exceptions as well).
 *
 * @author Radovan Semancik
 */
public interface Task extends DebugDumpable, StatisticsCollector, ConnIdOperationsListener, ExecutionModeProvider {

    String DOT_INTERFACE = Task.class.getName() + ".";

    //region Basic information (ID, owner, name, description)

    /**
     * Returns task (lightweight) identifier. This is an unique identification of any task,
     * regardless whether it is persistent or transient (cf. OID). Therefore this can be used
     * to identify all tasks, e.g. for the purposes of auditing and logging.
     *
     * Task identifier is assigned automatically when the task is created. It is immutable.
     */
    String getTaskIdentifier();

    /**
     * Return identifier that is used to identify a particular run of the task.
     *
     * This is a unique identifier, which is assigned to each run of the task.
     */
    String getTaskRunIdentifier();

    /**
     * Returns task OID.
     *
     * Only persistent tasks have OID. So this method returns null if the task is not persistent.
     */
    String getOid();

    /**
     * Returns object that owns this task. It usually means the user that started the task
     * or a system used that is used to execute the task. The owner will be used to
     * determine access rights of the task, will be used for auditing, etc.
     *
     * The object is lazily fetched from repository.
     */
    PrismObject<? extends FocusType> getOwner(OperationResult result);

    /** Returns a reference to the task owner. (Cloned if the task is running.) */
    ObjectReferenceType getOwnerRef();

    /**
     * Sets the task owner.
     *
     * Precondition: Task is transient.
     */
    void setOwner(PrismObject<? extends FocusType> owner);

    /**
     * Sets the task owner reference.
     *
     * Precondition: Task is transient.
     */
    void setOwnerRef(ObjectReferenceType ownerRef);

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
    void setNameImmediate(PolyStringType value, OperationResult result)
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
    void setDescriptionImmediate(String value, OperationResult result) throws ObjectNotFoundException, SchemaException;
    //endregion

    //region Execution and scheduling state

    /**
     * Returns high-level execution state.
     */
    TaskExecutionStateType getExecutionState();

    default boolean isRunnable() {
        return getExecutionState() == TaskExecutionStateType.RUNNABLE;
    }

    default boolean isRunning() {
        return getExecutionState() == TaskExecutionStateType.RUNNING;
    }

    /**
     * Returns low-level scheduling state.
     */
    TaskSchedulingStateType getSchedulingState();

    /** Returns true if the task is closed. (Refers to the scheduling state.) */
    default boolean isClosed() {
        return getSchedulingState() == TaskSchedulingStateType.CLOSED;
    }

    /** Returns true if the task is ready. (Refers to the scheduling state.) */
    default boolean isReady() {
        return getSchedulingState() == TaskSchedulingStateType.READY;
    }

    /** Returns true if the task is waiting. (BEWARE: Refers to the scheduling state.) */
    default boolean isWaiting() {
        return getSchedulingState() == TaskSchedulingStateType.WAITING;
    }

    /** Returns true if the task is suspended. (BEWARE: Refers to the scheduling state.) */
    default boolean isSuspended() {
        return getSchedulingState() == TaskSchedulingStateType.SUSPENDED;
    }

    /**
     * Returns the task waiting reason for a WAITING task.
     */
    TaskWaitingReasonType getWaitingReason();

    /**
     * Returns the node the task is currently executing at, based on repository information.
     * This is present in all cases, however, it might be out-of-date, e.g. when node crashes.
     */
    String getNode();

    /**
     * Returns the node as really observed. This is a transient information that has to be requested explicitly.
     */
    String getNodeAsObserved();

    /**
     * Sets initial task execution and scheduled state. It will be used when the task is made persistent.
     *
     * Precondition: Task is transient.
     */
    void setInitialExecutionAndScheduledState(TaskExecutionStateType executionState, TaskSchedulingStateType schedulingState);

    /**
     * Sets the initial task execution and scheduling state to allow the task to run.
     */
    default void setInitiallyRunnable() {
        setInitialExecutionAndScheduledState(TaskExecutionStateType.RUNNABLE, TaskSchedulingStateType.READY);
    }

    /**
     * Sets the initial task execution and scheduling state to make task suspended.
     */
    default void setInitiallySuspended() {
        setInitialExecutionAndScheduledState(TaskExecutionStateType.SUSPENDED, TaskSchedulingStateType.SUSPENDED);
    }

    /**
     * Sets the initial task execution and scheduling state to make task waiting (for prerequisite tasks).
     */
    void setInitiallyWaitingForPrerequisites();

    //endregion

    //region Persistence and asynchrony
    /**
     * Returns task persistence status.
     */
    @NotNull TaskPersistenceStatus getPersistenceStatus();

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
    //endregion

    //region Scheduling and binding
    /**
     * Returns task recurrence status.
     */
    @NotNull TaskRecurrenceType getRecurrence();

    /**
     * Checks whether the task is a cyclic (recurring) one.
     */
    default boolean isRecurring() {
        return getRecurrence() == TaskRecurrenceType.RECURRING;
    }

    /** Checks whether the task is single-run. */
    default boolean isSingle() {
        return !isRecurring();
    }

    /** Makes a task single-run, with no particular schedule. */
    default void makeSingle() {
        setSchedule(null);
    }

    /** Sets the schedule for the task. Removes also the legacy recurrence flag. */
    void setSchedule(ScheduleType schedule);

    /** Returns task execution constraints */
    TaskExecutionConstraintsType getExecutionConstraints();

    /** Sets task execution constraints. */
    void setExecutionConstraints(TaskExecutionConstraintsType value);

    /** Gets the execution group name (i.e. executionConstraints/group). */
    String getGroup();

    /** Returns names of all groups (primary plus all secondary ones). */
    @NotNull
    Collection<String> getGroups();

    /** Returns all groups (primary plus all secondary ones) along with task count limits. */
    @NotNull
    Map<String, Integer> getGroupsWithLimits();

    /** Returns the schedule. */
    ScheduleType getSchedule();

    /** Returns the schedule interval. */
    Integer getScheduleInterval();

    /** Returns true if the schedule interval is set. */
    boolean hasScheduleInterval();

    /**
     * Returns thread stop action (what happens when the task thread is stopped e.g. because of node going down).
     */
    ThreadStopActionType getThreadStopAction();

    /**
     * Sets the thread stop action for this task.
     */
    void setThreadStopAction(ThreadStopActionType value);

    /**
     * Returns the time when the task last run was started (or null if the task was never started).
     * This is set only for asynchronously executing tasks. And it is set also for the current execution.
     */
    Long getLastRunStartTimestamp();

    /**
     * Returns the time when the task last run was finished (or null if the task was not finished yet).
     * This is set only for asynchronously executing tasks.
     */
    Long getLastRunFinishTimestamp();

    /**
     * Returns the time when the task should start again.
     * This is transient property, present only if explicitly requested.
     */
    Long getNextRunStartTime(OperationResult result);

    /**
     * Returns the completion timestamp - time when the task was closed (or null if it is not closed).
     */
    Long getCompletionTimestamp();

    /**
     * Returns task binding.
     */
    TaskBindingType getBinding();

    /**
     * Returns true if the task is loosely bound.
     */
    default boolean isLooselyBound() {
        return getBinding() == TaskBindingType.LOOSE;
    }

    /** Returns true if the task is tightly bound. */
    default boolean isTightlyBound() {
        return !isLooselyBound();
    }
    //endregion

    //region Handler URI, category, archetype

    /**
     * Returns handler URI. It indirectly specifies which class is responsible to handle the task. The handler will execute
     * reaction to a task lifecycle events such as executing the task, task heartbeat, etc.
     */
    String getHandlerUri();

    /**
     * Sets handler URI.
     */
    void setHandlerUri(String value);

    // TODO Rework all three following archetype-setting methods

    /**
     * Adds an archetype for the task. Assumes that the task will NOT undergo full model processing,
     * so this method will do everything by itself: creates an assignment, roleMembershipRef and archetypeRef.
     *
     * Throws an exception if an archetype is already assigned.
     *
     * This is temporary/experimental implementation. It was not tested for persistent tasks
     * (although it should work also for them).
     */
    @Experimental
    void addArchetypeInformation(@NotNull String archetypeOid);

    /**
     * As {@link #addArchetypeInformation(String)} but executed only if there's no archetype currently set.
     */
    @Experimental
    void addArchetypeInformationIfMissing(@NotNull String archetypeOid);

    //endregion

    //region Task extension "get" operations + also arbitrary "get" operations
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
     */
    PrismContainer<? extends ExtensionType> getExtensionOrClone();

    /**
     * Gets or creates an extension. Just like {@link PrismObject#getOrCreateExtension()}.
     * Cloned if the task is running.
     */
    @NotNull PrismContainer<? extends ExtensionType> getOrCreateExtension() throws SchemaException;

    /** Gets extension clone, or null if there is no extension. */
    PrismContainer<? extends ExtensionType> getExtensionClone();

    /**
     * Returns specified property from the extension; or null if extension or property does not exist.
     * Cloned if task is running.
     */
    <T> PrismProperty<T> getExtensionPropertyOrClone(ItemName propertyName);

    /**
     * Returns specified single-valued property real value from the extension
     * (null if extension or property does not exist).
     */
    default <T> T getExtensionPropertyRealValue(ItemName propertyName) {
        //noinspection unchecked
        return (T) getPropertyRealValue(ItemPath.create(TaskType.F_EXTENSION, propertyName), Object.class);
    }

    <T extends Containerable> T getContainerRealValue(ItemPath path, Class<T> expectedType);

    <T> T getPropertyRealValue(ItemPath path, Class<T> expectedType);

    <T> T getPropertyRealValueOrClone(ItemPath path, Class<T> expectedType);

    <T> T getItemRealValueOrClone(ItemPath path, Class<T> expectedType);

    /** TODO what about thread safety? */
    ObjectReferenceType getReferenceRealValue(ItemPath path);

    /** TODO what about thread safety? */
    Collection<ObjectReferenceType> getReferenceRealValues(ItemPath path);

    /**
     * Returns specified single-valued container real value from the extension
     * To ensure thread safety, in the case of running tasks the returned value is a clone of the live one.
     * Returns null if extension or container does not exist.
     */
    <T extends Containerable> T getExtensionContainerRealValueOrClone(ItemName containerName);

    /**
     * Returns specified reference from the extension. Cloned if running task.
     * Null if extension or reference does not exist.
     */
    @SuppressWarnings("unused")
    PrismReference getExtensionReferenceOrClone(ItemName name);

    /**
     * Returns specified item (property, reference or container) from the extension. Cloned if running task.
     * Null if extension or item does not exist.
     */
    <IV extends PrismValue,ID extends ItemDefinition<?>> Item<IV,ID> getExtensionItemOrClone(ItemName itemName);
    //endregion

    //region Task extension "set" operations + also arbitrary "set" operations
    /**
     * Sets a property in the extension - replaces existing value(s), if any, by the one(s) provided.
     */
    void setExtensionProperty(PrismProperty<?> property) throws SchemaException;

    /**
     * "Immediate" version of the above method.
     */
    void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult result)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Sets (i.e., replaces) the value of the given property in task extension.
     * @param propertyName name of the property
     * @param value value of the property
     */
    default <T> void setExtensionPropertyValue(QName propertyName, T value) throws SchemaException {
        setPropertyRealValue(ItemPath.create(TaskType.F_EXTENSION, propertyName), value);
    }

    <T> void setPropertyRealValue(ItemPath path, T value) throws SchemaException;

    default void setItemRealValues(ItemPath path, Object... value) throws SchemaException {
        setItemRealValuesCollection(path, MiscUtil.asListTreatingNull(value));
    }

    default void setItemRealValuesCollection(ItemPath path, Collection<?> values) throws SchemaException {
        modify(
                PrismContext.get().deltaFor(TaskType.class)
                        .item(path)
                        .replaceRealValues(values)
                        .asItemDelta());
    }

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
    void setExtensionItem(Item<?, ?> item) throws SchemaException;
    //endregion
    //region Task extension (adding and deleting items)

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

    /**
     * Removes specified VALUES of this extension property (not all of its values).
     */
    @SuppressWarnings("unused")
    void deleteExtensionProperty(PrismProperty<?> property) throws SchemaException;
    //endregion

    //region Associated object
    /**
     * Returns object that the task is associated with.
     *
     * Tasks may be associated with a particular objects. For example a "import from resource" task is associated
     * with the resource definition object that it imports from. Similarly for synchronization and reconciliation
     * tasks (cycles). User approval and modification task may be associated with that user.
     *
     * If the task contains a reference without full object, the object is fetched from the repository.
     * (Authorizations are *not* checked.)
     */
    <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult result)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Returns reference to the object that the task is associated with.
     *
     * Tasks may be associated with a particular objects. For example a "import from resource" task is associated
     * with the resource definition object that it imports from. Similarly for synchronization and reconciliation
     * tasks (cycles). This is an optional property.
     */
    ObjectReferenceType getObjectRefOrClone();

    /**
     * Returns OID of the object that the task is associated with.
     *
     * Convenience method. This will get the OID from the objectRef.
     */
    String getObjectOid();

    /**
     * Sets the object reference.
     */
    void setObjectRef(ObjectReferenceType objectRef);

    /**
     * Sets the object reference.
     */
    void setObjectRef(String oid, QName type);
    //endregion

    //region Task result and progress

    /**
     * Returns a top-level OperationResult stored in the task.
     * Beware of thread safety. This is a live object!
     */
    OperationResult getResult();

    /**
     * Returns the status of top-level OperationResult stored in the task.
     * If "live" operation result is present, the status it taken from it.
     * Otherwise, resultStatus from task prism is fetched.
     *
     * TODO reconsider this method
     */
    OperationResultStatusType getResultStatus();

    /**
     * Sets the top-level OperationResult stored in the task.
     * Use with care!
     */
    void setResult(OperationResult result);

    // TODO
    void applyDeltasImmediate(Collection<ItemDelta<?, ?>> itemDeltas, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException;

    // TODO
    void applyModificationsTransient(Collection<ItemDelta<?, ?>> modifications) throws SchemaException;
    /**
     * Returns task progress, as reported by task handler.
     */
    long getLegacyProgress();

    /**
     * Records _legacy_ progress of the task, storing it persistently if needed.
     */
    void setLegacyProgress(Long value);

    /**
     * Increments legacy progress without creating a pending modification.
     */
    void incrementLegacyProgressTransient();

    /**
     * "Immediate" version of {@link #setLegacyProgress(Long)}.
     *
     * BEWARE: this method can take quite a long time to execute, if invoked in a cycle.
     */
    @SuppressWarnings("unused") // may be used e.g. from scripts
    void setLegacyProgressImmediate(Long progress, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Returns operation statistics from the task prism object (i.e. not the live ones).
     * Clones if running task.
     */
    OperationStatsType getStoredOperationStatsOrClone();

    /**
     * Gets information from the current task and - for running task - its transient subtasks (aka worker threads).
     *
     * Clients beware: Update thread-local statistics before! They are not updated inside this method.
     */
    OperationStatsType getAggregatedLiveOperationStats();

    /**
     * Returns expected total progress.
     */
    @Nullable
    Long getExpectedTotal();

    /**
     * Stores expected total progress of the task, storing it persistently if needed.
     */
    void setExpectedTotal(Long value);
    //endregion

    //region Subtasks and dependent tasks
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
    default List<? extends Task> listSubtasks(OperationResult parentResult) throws SchemaException {
        return listSubtasks(false, parentResult);
    }

    @NotNull
    List<? extends Task> listSubtasks(boolean persistentOnly, OperationResult parentResult) throws SchemaException;

    /**
     * List all the subtasks of a given task, i.e. whole task tree rooted at the current task.
     * Current task is not contained in the returned list.
     */
    List<? extends Task> listSubtasksDeeply(OperationResult result) throws SchemaException;

    /**
     * Lists all tasks in subtasks tree.
     *
     * @param persistentOnly If true, transient subtasks (i.e. lightweight asynchronous tasks) are ignored.
     */
    List<? extends Task> listSubtasksDeeply(boolean persistentOnly, OperationResult result) throws SchemaException;

    /**
     * Lists all explicit dependents, i.e. tasks that wait for the completion of this tasks (that depend on it).
     *
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
     * List all prerequisite tasks for the current tasks, i.e. tasks that must complete before this one can proceed.
     * If A is on the list of prerequisites of B (THIS), it means that B is on list of dependents of A (i.e.
     * B waits for A to complete).
     *
     * Again, implicit prerequisites (children) are not listed here.
     */

    List<? extends Task> listPrerequisiteTasks(OperationResult parentResult) throws SchemaException;
    //endregion

    //region Task object as a whole, plus generic path-based access

    /**
     * Returns backing task prism object without updating with current operation result.
     * If the task is running, a clone is returned.
     */
    @NotNull
    PrismObject<TaskType> getRawTaskObjectClonedIfNecessary();

    /**
     * Returns CLONE of backing task prism object without updating with current operation result.
     */
    @NotNull
    PrismObject<TaskType> getRawTaskObjectClone();

    /**
     * Returns backing task prism object UPDATED with current operation result.
     *
     * Assumes that task is not running. (Otherwise IllegalStateException is thrown.)
     */
    @NotNull
    PrismObject<TaskType> getUpdatedTaskObject();

    /**
     * Returns a reference to the task prism.
     *
     * Precondition: Task must be persistent.
     */
    @NotNull ObjectReferenceType getSelfReference();

    /**
     * Returns a full (object-bearing) reference to the task prism.
     *
     * Precondition: Task must be persistent.
     */
    @NotNull ObjectReferenceType getSelfReferenceFull();

    /** Returns the version of underlying prism object. */
    String getVersion();

    /**
     * Re-reads the task state from the persistent storage.
     *
     * The task state may be synchronized with the repository all the time. But the specified timing is implementation-specific.
     * Call to this method will make sure that the task contains fresh data.
     *
     * This has no effect on transient tasks.
     */
    void refresh(OperationResult result) throws ObjectNotFoundException, SchemaException;

    /**
     * Changes in-memory representation immediately and schedules a corresponding batched modification.
     */
    void modify(@NotNull ItemDelta<?, ?> delta) throws SchemaException;

    /**
     * Applies given collection of deltas.
     */
    default void modify(Collection<ItemDelta<?, ?>> deltas) throws SchemaException {
        for (ItemDelta<?, ?> delta : deltas) {
            modify(delta);
        }
    }

    /**
     * Saves modifications done against the in-memory version of the task into the repository.
     */
    void flushPendingModifications(OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, ObjectAlreadyExistsException;

    /** TODO */
    <C extends Containerable> C getContainerableOrClone(ItemPath path, Class<C> type);

    /** TODO */
    boolean doesItemExist(ItemPath path);

    /** TODO */
    ActivityStateType getActivityStateOrClone(ItemPath path);

    /** Returns the activity state for given activity path. Assumes local execution! */
    default @Nullable ActivityStateType getActivityStateOrClone(@NotNull ActivityPath activityPath) {
        // We may improve the efficiency by cloning only the relevant part of activities state.
        return ActivityStateUtil.getActivityState(getActivitiesStateOrClone(), activityPath);
    }

    /** Returns the completion timestamp of the root activity. Assumes being executed on the root task. */
    default @Nullable XMLGregorianCalendar getRootActivityCompletionTimestamp() {
        ActivityStateType rootActivityState = getActivityStateOrClone(ActivityPath.empty());
        return rootActivityState != null ? rootActivityState.getRealizationEndTimestamp() : null;
    }
    //endregion

    //region Activities
    /**
     * Retrieves the definition of the [root] activity.
     */
    default ActivityDefinitionType getRootActivityDefinitionOrClone() {
        return getContainerableOrClone(TaskType.F_ACTIVITY, ActivityDefinitionType.class);
    }

    default void setRootActivityDefinition(ActivityDefinitionType activityDefinition) throws SchemaException {
        setItemRealValues(TaskType.F_ACTIVITY, activityDefinition);
    }

    /**
     * Gets task work state. NOT THREAD SAFE!
     *
     * TODO throw exception for RunningTask. (After revising of all uses.)
     */
    TaskActivityStateType getWorkState();

    /**
     * Gets task work state or its clone (for running tasks). TODO better name
     */
    TaskActivityStateType getActivitiesStateOrClone();

    default @Nullable ActivityStateOverviewType getActivityTreeStateOverviewOrClone() {
        return getPropertyRealValueOrClone(ACTIVITY_TREE_STATE_OVERVIEW_PATH, ActivityStateOverviewType.class);
    }
    //endregion

    //region Task tree related methods
    /**
     * Looks for OID of the parent and the root of the task tree for this task.
     *
     * PRE: task is either persistent or is a {@link RunningTask}.
     */
    @NotNull ParentAndRoot getParentAndRoot(OperationResult result)
            throws SchemaException, ObjectNotFoundException;

    /**
     * Returns the root of the task tree for this task.
     *
     * PRE: task is either persistent or is a {@link RunningTask}.
     */
    default @NotNull Task getRoot(OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getParentAndRoot(result).root;
    }

    /**
     * Returns the path from this task to the task tree root. (Starts with this task, ends with the root.)
     */
    List<Task> getPathToRootTask(OperationResult result) throws SchemaException;
    //endregion

    //region Tracing
    @Experimental
    boolean isTracingRequestedFor(@NotNull TracingRootType point);

    /**
     * Get points for which the tracing is requested (within this task).
     */
    @Experimental
    @NotNull Collection<TracingRootType> getTracingRequestedFor();

    @Experimental
    void setTracingRequestedFor(@NotNull Collection<TracingRootType> points);

    /** Requests (future) tracing for given tracing point - for this task. */
    @Experimental
    void addTracingRequest(TracingRootType point);

    @Experimental
    void removeTracingRequest(TracingRootType point);

    /** Removes all tracing requests for this task. */
    @Experimental
    void removeTracingRequests();

    /** Returns (reference to tracing profile) that was defined for the tracing started by this task. NOT THREAD SAFE! */
    @Experimental
    TracingProfileType getTracingProfile();

    /** Sets the profile to be used for future tracing within this task. */
    @Experimental
    void setTracingProfile(TracingProfileType tracingProfile);

    //endregion

    //region Reporting

    /** Registers a {@link ConnIdOperationsListener}. */
    @Experimental
    void registerConnIdOperationsListener(@NotNull ConnIdOperationsListener listener);

    /** Unregisters a {@link ConnIdOperationsListener}. */

    @Experimental
    void unregisterConnIdOperationsListener(@NotNull ConnIdOperationsListener listener);

    //endregion

    //region Other task items
    /** Returns channel URI associated with this task. */
    String getChannel();

    /** Sets channel URI. */
    void setChannel(String channelUri);

    /**
     * Gets the requestee OID - typically an identification of account owner (for notifications).
     * Serves for communication between model and provisioning.
     * It is a temporary feature - will be removed in midPoint 2.3.
     */
    PrismObject<UserType> getRequestee();

    void setRequesteeTransient(PrismObject<UserType> user);

    /** Gets the execution environment configuration. Cloned if running task. */
    TaskExecutionEnvironmentType getExecutionEnvironment();

    /** Returns an immutable collection of caching profiles. (From execution environment.) */
    @NotNull Collection<String> getCachingProfiles();

    /** Sets the execution environment configuration. */
    void setExecutionEnvironment(TaskExecutionEnvironmentType value);

    /** Returns true if the task has any assignments. */
    boolean hasAssignments();

    Duration getCleanupAfterCompletion();

    void setCleanupAfterCompletion(Duration duration);

    List<TaskRunRecordType> getTaskRunRecords();
    //endregion

    //region Misc

    default boolean isRoot() {
        return getParent() == null;
    }

    default ExecutionSupport getExecutionSupport() {
        return null;
    }

    default boolean isIndestructible() {
        return Boolean.TRUE.equals(getPropertyRealValue(TaskType.F_INDESTRUCTIBLE, Boolean.class));
    }

    /**
     * Sets the execution mode of this task. Use with care - preferably only for new tasks.
     * Returns the original value.
     */
    @NotNull TaskExecutionMode setExecutionMode(@NotNull TaskExecutionMode mode);

    default void assertPersistentExecution(String message) {
        TaskExecutionMode executionMode = getExecutionMode();
        if (!executionMode.isFullyPersistent()) {
            throw new UnsupportedOperationException(message + " (mode: " + executionMode + ")");
        }
    }

    /** Sets the current simulation transaction object. */
    SimulationTransaction setSimulationTransaction(SimulationTransaction context);

    /** Returns the current simulation transaction, if there is any. */
    @Nullable SimulationTransaction getSimulationTransaction();
    //endregion
}
