/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.api;

import java.util.Collection;
import java.util.function.Predicate;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * <p>Task Manager Interface.</p>
 * <p>
 * Status: public
 * Stability: DRAFT
 *
 * @author Radovan Semancik
 * </p>
 * <p>
 * Task manager provides controls task execution, coordination, distribution and failover between nodes, etc.
 * </p><p>
 * This interface is just a basic framework for task management now. Although we hope that this is roughly almost final
 * shape of the interface, the implementation is not complete and some changes may happen.
 * </p>
 * <p>
 * This definition specifies interface of Task Manager - a component that controls (asynchronous) task execution.
 * </p><p>
 * The task manager can store the task for later execution, switch them to background
 * resume execution of a task from a different node, etc. Generally speaking, task
 * manager provides operation to manage tasks in the whole midPoint cluster of IDM nodes.
 * </p><p>
 * This interface partially adheres to [Common Interface Concepts], but the goals are slightly
 * different. This interface should be conveniently used also for tasks that are not persistent
 * (synchronous short tasks). Therefore some methods are made much more user-friendly while
 * tolerating some redundancy in the interface.
 * </p>
 * @version 0.1
 */
public interface TaskManager {

    long WAIT_INDEFINITELY = 0L;
    long DO_NOT_WAIT = -1L;
    long DO_NOT_STOP = -2L;

    //region Generic operations delegated from the model
    /**
     * Notes: Implemented options are:
     *
     * - noFetch: it causes task manager NOT to ask remote nodes about node/task status.
     * - (for tasks) TaskType.F_NEXT_RUN_START_TIMESTAMP: it can be used to disable asking Quartz for next run start time
     * - other options that are passed down to repository
     */
    @NotNull <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult)
            throws SchemaException;

    <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ResultHandler<T> handler,
            OperationResult parentResult)
            throws SchemaException;

    /**
     * Counts the number of objects.
     */
    <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult parentResult) throws SchemaException;

    /**
     * TODO
     */
    @NotNull <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Add new task.
     *
     * The OID provided in the task may be empty. In that case the OID
     * will be assigned by the implementation of this method and it will be
     * provided as return value.
     *
     * This operation should fail if such object already exists (if object with
     * the provided OID already exists).
     *
     * The operation may fail if provided OID is in an unusable format for the
     * storage. Generating own OIDs and providing them to this method is not
     * recommended for normal operation.
     *
     * Should be atomic. Should not allow creation of two objects with the same
     * OID (even if created in parallel).
     *
     * The operation may fail if the object to be created does not conform to
     * the underlying schema of the storage system or the schema enforced by the
     * implementation.
     *
     * @param taskPrism object to create
     * @param parentResult parent OperationResult (in/out)
     * @return OID assigned to the created object
     * @throws ObjectAlreadyExistsException object with specified identifiers already exists, cannot add
     * @throws SchemaException error dealing with storage schema, e.g. schema violation
     * @throws IllegalArgumentException wrong OID format, etc.
     */
    default String addTask(PrismObject<TaskType> taskPrism, OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException {
        return addTask(taskPrism, null, parentResult);
    }

    String addTask(PrismObject<TaskType> taskPrism, RepoAddOptions options, OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException;

    /**
     * Modifies task using relative change description. Must fail if object with
     * provided OID does not exist. Must fail if any of the described changes
     * cannot be applied. Should be atomic.
     *
     * If two or more modify operations are executed in parallel, the operations
     * should be merged. In case that the operations are in conflict (e.g. one
     * operation adding a value and the other removing the same value), the
     * result is not deterministic.
     *
     * The operation may fail if the modified object does not conform to the
     * underlying schema of the storage system or the schema enforced by the
     * implementation.
     *
     * HOWEVER, the preferred way of modifying tasks is to use methods in Task interface.
     *
     * @param oid OID of the task to be changed
     * @param modifications specification of object changes
     * @param parentResult parent OperationResult (in/out)
     * @throws ObjectNotFoundException specified object does not exist
     * @throws SchemaException resulting object would violate the schema
     * @throws IllegalArgumentException wrong OID format, described change is not applicable
     */
    void modifyTask(String oid, Collection<? extends ItemDelta<?, ?>> modifications, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    /**
     * Deletes task with provided OID. Must fail if object with specified OID
     * does not exist. Should be atomic.
     *
     * BEWARE: call this method only if you are pretty sure the task is not running.
     * Otherwise the running thread will complain when it will try to store task result into repo.
     * (I.e. it is a good practice to suspend the task before deleting.)
     *
     * @param oid OID of object to delete
     * @param parentResult parent OperationResult (in/out)
     * @throws ObjectNotFoundException specified object does not exist
     * @throws IllegalArgumentException wrong OID format, described change is not applicable
     */
    void deleteTask(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /** TODO */
    void deleteTaskTree(String rootTaskOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;
    //endregion

    //region Basic working with tasks (create instance, get, modify, delete)

    /**
     * Creates new transient, running task instance.
     *
     * This is fact creates usual "synchronous" task.
     *
     * This is useful for normal day-to-day tasks that are either
     * synchronous or start as a synchronous and are switched to
     * asynchronous task later.
     *
     * @return transient, running task instance
     */
    default Task createTaskInstance() {
        return createTaskInstance(null);
    }

    /**
     * Creates task instance from the XML task representation.
     *
     * @param taskPrism JAXB (XML) representation of the task
     * @return new Java representation of the task
     * @throws SchemaException The provided taskType is not compliant to schema
     */
    @NotNull
    Task createTaskInstance(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException;

    /**
     * Creates new transient, running task instance.
     *
     * This in fact creates usual "synchronous" task.
     *
     * This is useful for normal day-to-day tasks that are either
     * synchronous or start as a synchronous and are switched to
     * asynchronous task later.
     *
     * The result inside the task will be initialized with
     * specified operation name.
     *
     * @param operationName operation name to use as a root for new result in task
     * @return new Java representation of the task
     */
    Task createTaskInstance(String operationName);

    /**
     * Creates task instance from the XML task representation.
     *
     * If there is not a result inside the task, it will create the
     * result with specified operation name.
     *
     * @param taskPrism Prism representation of the task
     * @param operationName operation name to use as a root for new result in task -- IGNORED
     * @return new Java representation of the task
     * @throws SchemaException The provided taskType is not compliant to schema
     */
    @NotNull
    Task createTaskInstance(PrismObject<TaskType> taskPrism, @Deprecated String operationName, OperationResult parentResult)
            throws SchemaException;

    /**
     * Returns a task with specified OID.
     *
     * This operation will look up a task instance in the repository and return it in a form of Task object.
     *
     * Works only on persistent tasks.
     *
     * Gets the task simply by fetching it from repository. No attempts to augment it with the live data nor Quartz scheduling
     * information nor subtasks is done. TODO can we use options (noFetch? raw?) to achieve this?
     *
     * @param taskOid OID of the persistent task.
     * @return Task instance
     * @throws SchemaException error dealing with resource schema
     * @throws ObjectNotFoundException wrong OID format, etc.
     */
    @NotNull
    Task getTaskPlain(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Gets the task simply by fetching it from repository. No attempts to augment it with the live data nor Quartz scheduling
     * information nor subtasks is done. TODO can we use options (noFetch? raw?) to achieve this?
     */
    @NotNull
    Task getTaskPlain(String taskOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    /**
     * TODO
     */
    @NotNull
    Task getTask(String taskOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    default Task getTaskTree(String rootTaskOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        return getTask(
                rootTaskOid,
                SchemaService.get().getOperationOptionsBuilder()
                        .item(TaskType.F_SUBTASK_REF).retrieve()
                        .build(),
                parentResult);
    }

    /**
     * Gets the task (as in getTaskPlain) but with its operation result.
     */
    @NotNull
    Task getTaskWithResult(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    @VisibleForTesting
        // TODO
    void closeTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Returns a task with a given identifier.
     *
     * (NOTE: Currently finds only persistent tasks. In the future, we plan to support searching for transient tasks as well.)
     *
     * @param identifier task identifier to search for
     */
    @NotNull
    Task getTaskByIdentifier(String identifier, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;

    /**
     * TODO
     */
    @NotNull
    PrismObject<TaskType> getTaskTypeByIdentifier(String identifier,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException;

    /**
     * @return true if the task is orphaned i.e. it has a parent declared but not existing
     */
    boolean isOrphaned(PrismObject<TaskType> task, OperationResult parentResult) throws SchemaException;

    /**
     * Deletes obsolete tasks, as specified in the policy.
     *
     * This method removes whole task trees, i.e. not single tasks. A task tree is deleted if the root task is closed
     * (assuming all tasks in the tree are closed) and was closed before at least specified time.
     *
     * @param closedTasksPolicy specifies which tasks are to be deleted, e.g. how old they have to be
     * @param selector If returns false, the respective task will not be removed. In a task tree, the selector must return
     * true for every task for the tree to be deleted.
     * @param task task, within which context the cleanup executes (used to test for interruptions)
     */
    void cleanupTasks(@NotNull CleanupPolicyType closedTasksPolicy, @NotNull Predicate<TaskType> selector,
            @NotNull RunningTask task, @NotNull OperationResult opResult)
            throws SchemaException, ObjectNotFoundException;

    /**
     * Deletes dead nodes, i.e. ones that are not checking-in, and the last checkin time is older than specified time period.
     *
     * @param selector If returns false, the respective node will not be removed.
     */
    void cleanupNodes(@NotNull DeadNodeCleanupPolicyType deadNodesPolicy, @NotNull Predicate<NodeType> selector,
            @NotNull RunningTask task, @NotNull OperationResult opResult) throws SchemaException, ObjectNotFoundException;
    //endregion

    //region Remotely invokable methods
    /**
     * Returns the local scheduler information. To be called from the task manager on other nodes.
     */
    SchedulerInformationType getLocalSchedulerInformation(OperationResult parentResult);

    /**
     * Stops the local scheduler. To be called from the task manager on other nodes.
     */
    void stopLocalScheduler(OperationResult parentResult);

    /**
     * Starts the local scheduler. To be called from the task manager on other nodes.
     */
    void startLocalScheduler(OperationResult parentResult);

    /**
     * Stops the local task. To be called from the task manager on other nodes.
     */
    void stopLocalTaskRunInStandardWay(String oid, OperationResult result);
    //endregion

    //region Suspending, resuming and scheduling the tasks
    /**
     * Suspends a set of tasks. Sets their execution status to SUSPENDED. Stops their execution (unless doNotStop is set).
     *
     * @param taskOids a collection of OIDs of tasks that have to be suspended
     * @param waitForStop how long (in milliseconds) to wait for stopping the execution of tasks;
     * {@link #WAIT_INDEFINITELY} means wait indefinitely,
     * {@link #DO_NOT_WAIT} means stop the tasks, but do not wait for finishing their execution,
     * {@link #DO_NOT_STOP} means do not try to stop the task execution. Tasks will only be put into SUSPENDED state, and
     * their executions (if any) will be left as they are. Use this option only when you know what you're doing.
     * @return true if all the tasks were stopped, false if some tasks continue to run or if stopping was not requested (DO_NOT_STOP option)
     *
     * On error conditions does NOT throw an exception.
     */
    boolean suspendTasks(Collection<String> taskOids, long waitForStop, OperationResult parentResult);

    /**
     * Suspends a task. The same as above except that on error condition it DOES throw appropriate exception.
     */
    boolean suspendTask(Task task, long waitTime, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Suspends a task. The same as above except that on error condition it DOES throw appropriate exception.
     */
    boolean suspendTask(String taskOid, long waitTime, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException;

    /**
     * Brings a closed task to suspended state. This is to allow later resumption of the task.
     *
     * Not very clean solution, so don't use unless really needed.
     */
    @Experimental
    void markClosedTaskSuspended(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Suspends tasks and deletes them.
     *
     * @param taskOidList List of task OIDs to be suspended and deleted.
     * @param suspendTimeout How long (in milliseconds) to wait for task suspension before proceeding with deletion.
     * @param alsoSubtasks Should also subtasks be deleted?
     */
    void suspendAndDeleteTasks(Collection<String> taskOidList, long suspendTimeout, boolean alsoSubtasks, OperationResult parentResult);

    /**
     * The same as above, but limited to a single task and throws the first exception encountered.
     */
    void suspendAndDeleteTask(String taskOid, long suspendTimeout, boolean alsoSubtasks, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException;

    /**
     * TODO
     */
    void suspendAndCloseTaskNoException(Task task, long suspendTimeout, OperationResult parentResult);
    /**
     * Resume suspended task.
     *
     * @param task task instance to be resumed.
     */
    void resumeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    void resumeTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
     * Resume suspended tasks.
     *
     * @param taskOids a collection of OIDs of tasks that have to be resumed
     */
    void resumeTasks(Collection<String> taskOids, OperationResult parentResult);

    boolean suspendTaskTree(String coordinatorOid, long waitTime, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException;

    void resumeTaskTree(String coordinatorOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException;

    /**
     * Puts a WAITING task back into RUNNABLE state.
     *
     * @throws PreconditionViolationException If there is a conflict during unpausing, i.e. the task is originally in
     * the waiting state, but (independently) changes the state during execution of the method.
     */
    void unpauseTask(Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException;

    /**
     * Switches the provided task to background, making it asynchronous.
     *
     * The provided task will be "released" to other nodes to execute. There is no guarantee that
     * the task will execute on the same node that called the switchToBackground() method.
     *
     * @param task task to switch to background.
     */
    void switchToBackground(Task task, OperationResult parentResult);

    /**
     * Schedules RUNNABLE/CLOSED tasks to be run immediately. (If a task will really start immediately,
     * depends e.g. on whether a scheduler is started, whether there are available threads, and so on.)
     *
     * @param taskOids a collection of OIDs of tasks that have to be scheduled
     *
     * Proceeds quietly - i.e. on exception it simply logs it.
     */
    void scheduleTasksNow(Collection<String> taskOids, OperationResult parentResult);

    /**
     * Schedules a RUNNABLE task or CLOSED single-run task to be run immediately. (If the task will really start immediately,
     * depends e.g. on whether a scheduler is started, whether there are available threads, and so on.)
     *
     * Throws appropriate exceptions.
     */
    void scheduleTaskNow(Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;

    /**
     * The same as above.
     */
    void scheduleTaskNow(String taskOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;
    //endregion

    //region Working with nodes
    /**
     * Returns identifier for current node.
     */
    @NotNull String getNodeId();

    /**
     * Checks whether supplied node is the current node.
     *
     * @return true if node is the current node
     */
    boolean isCurrentNode(PrismObject<NodeType> node);

    /**
     * Deletes a node from the repository.
     * (Checks whether the node is not up before deleting it.)
     */
    void deleteNode(String nodeOid, OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Registers current node as "up". Normally this is done after midPoint starting up;
     * but should be done explicitly in tests.
     */
    @VisibleForTesting
    void registerNodeUp(OperationResult result);

    /** Retrieves the cluster state needed e.g. for workers reconciliation. */
    @Experimental
    @NotNull ClusterStateType determineClusterState(OperationResult result) throws SchemaException;
    //endregion

    //region Managing state of the scheduler(s)

    /**
     * Deactivates service threads (temporarily).
     *
     * This will suspend all background activity such as scanning threads, heartbeats and similar mechanisms.
     *
     * Note: The threads are normally activated after task manager implementation starts. This methods should not be used
     * in a normal case.
     *
     * WARNING: this feature is intended for development-time diagnostics and should not be used on production environments.
     * Suspending the threads may affect correct behavior of the system (such as timeouts on heartbeats). Use this feature
     * only if you really know what you are doing.
     *
     * timeToWait is only for orientation = it may be so that the implementation would wait 2 or 3 times this value
     * (if it waits separately for several threads completion)
     */
    boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) throws SchemaException;

    /**
     * Re-activates the service threads after they have been deactivated.
     */
    void reactivateServiceThreads(OperationResult parentResult);

    /**
     * Returns true if the service threads are running.
     *
     * This method returns true in a normal case. It returns false is the threads were temporarily suspended.
     *
     * @return true if the service threads are running.
     */
    boolean getServiceThreadsActivationState();

    /**
     * Stops the scheduler on a given node. This means that at that node no tasks will be started.
     *
     * @param nodeIdentifier Node on which the scheduler should be stopped. Null means current node.
     */
    void stopScheduler(String nodeIdentifier, OperationResult parentResult);

    void stopSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult);

    /**
     * Stops a set of schedulers (on their nodes) and tasks that are executing on these nodes.
     *
     * @param nodeIdentifiers collection of node identifiers
     * @param waitTime how long to wait for task shutdown, in milliseconds;
     * {@link TaskManager#WAIT_INDEFINITELY} means wait indefinitely,
     * {@link TaskManager#DO_NOT_WAIT} means stop the tasks, but do not wait for finishing their execution.
     */
    boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long waitTime, OperationResult parentResult)
            throws SchemaException;

    /**
     * Starts the scheduler on a given node. A prerequisite is that the node is running and its
     * TaskManager is not in an error state.
     *
     * @param nodeIdentifier Node on which the scheduler should be started. Null means current node.
     */
    void startScheduler(String nodeIdentifier, OperationResult parentResult);

    void startSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult);
    //endregion

    //region Notifications
    /**
     * Registers a task listener that will be notified on task-related events.
     *
     * @param taskListener listener to be registered
     */
    void registerTaskListener(TaskListener taskListener);

    /**
     * Unregisters a task listener.
     *
     * @param taskListener listener to be unregistered
     */
    void unregisterTaskListener(TaskListener taskListener);

    void registerTaskUpdatedListener(TaskUpdatedListener taskListener);

    void unregisterTaskUpdatedListener(TaskUpdatedListener taskListener);

    //endregion

    //region Miscellaneous methods
    // ==================================================== Miscellaneous methods

    /**
     * Synchronizes information in midPoint repository and task scheduling database.
     */
    void synchronizeTasks(OperationResult parentResult);

    /**
     * Gets next scheduled execution time for a given task.
     *
     * @param oid OID of the task
     * @return null if there's no next scheduled execution for a given task or if a task with given OID does not exist
     */
    Long getNextRunStartTime(String oid, OperationResult result);

    /**
     * Registers a handler for a specified handler URI.
     *
     * @param uri URI of the handler, e.g. http://midpoint.evolveum.com/xml/ns/public/model/cleanup/handler-3
     * @param handler instance of the handler
     */
    void registerHandler(@NotNull String uri, @NotNull TaskHandler handler);

    /**
     * Unregisters a handler URI (registered either as "standard", additional or deprecated handler URI).
     */
    void unregisterHandler(String uri);

    void registerTaskDeletionListener(TaskDeletionListener listener);

    void setDefaultHandlerUri(String uri);

    //endregion

    //region TODO
    /**
     * EXPERIMENTAL. Relaxes some assumptions on cluster structure e.g. that IP addresses of cluster members must be different.
     * To be used for demonstration/testing only. Avoid using in production environments.
     */
    boolean isLocalNodeClusteringEnabled();

    /**
     * EXPERIMENTAL. Used to provide midPoint URL path (typically "/midpoint") when determined by the web layer.
     */
    void setWebContextPath(String path);

    String getRunningTasksThreadsDump(OperationResult parentResult);

    String recordRunningTasksThreadsDump(String cause, OperationResult parentResult) throws ObjectAlreadyExistsException;

    String getTaskThreadsDump(String taskOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;

    /**
     * Use only for tests. (Even in that case it is an ugly hack.)
     */
    @VisibleForTesting
    RunningTask createFakeRunningTask(Task task);

    TaskHandler getHandler(String handlerUri);

    /** Returns the local node object (immutable). */
    @NotNull NodeType getLocalNode();

    /** Returns the local node object OID. */
    @NotNull String getLocalNodeOid();

    // A little bit of hack as well
    CacheConfigurationManager getCacheConfigurationManager();

    boolean isDynamicProfilingEnabled();

    boolean isClustered();

    // EXPERIMENTAL
    boolean isTracingOverridden();

    // EXPERIMENTAL
    @NotNull
    Collection<TracingRootType> getGlobalTracingRequestedFor();

    // EXPERIMENTAL
    TracingProfileType getGlobalTracingProfile();

    // EXPERIMENTAL
    void setGlobalTracingOverride(@NotNull Collection<TracingRootType> roots, @NotNull TracingProfileType profile);

    // EXPERIMENTAL
    void unsetGlobalTracingOverride();

    /**
     * @return true if we consider this node to be "up" (alive). This is determined by looking at operational state
     * (should be UP) and last check-in information (should not be more than nodeTimeout ago).
     */
    boolean isUpAndAlive(NodeType node);

    /**
     * @return true if this node has recently checked in. It might be starting or up.
     */
    boolean isCheckingIn(NodeType node);

    /**
     * @return Collection of node groups that the current cluster node belongs to.
     *
     * The collection is unmodifiable. Groups are represented by abstract roles.
     * (Current implementation uses node archetypes to keep this information.)
     */
    Collection<ObjectReferenceType> getLocalNodeGroups();

    /**
     * Returns locally-run task by identifier. Returned instance is the same as is being used to carrying out
     * operations. SO USE WITH CARE.
     *
     * EXPERIMENTAL. Should be replaced by something like "get operational information".
     */
    @VisibleForTesting
    RunningTask getLocallyRunningTaskByIdentifier(String lightweightIdentifier);

    /**
     * Should be called only from the thread that created the children - to avoid race conditions.
     */
    void waitForTransientChildrenAndCloseThem(RunningTask task, OperationResult result);

    /**
     * Returns hikari pool statistics (active, idle, waiting, total, max number of DB connections)
     * Return null if pool is unavailable.
     *
     * TODO move to more appropriate place
     */
    Number[] getDBPoolStats();

    //endregion
}
