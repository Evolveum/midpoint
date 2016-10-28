package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.Collection;
import java.util.List;

/**
 * Interface of the Model subsystem that provides task-specific operations.
 *
 * @author mederly
 */
public interface TaskService {

    //region Task-level operations

    long WAIT_INDEFINITELY = 0L;
    long DO_NOT_WAIT = -1L;
    long DO_NOT_STOP = -2L;

    /**
     * Suspends a set of tasks. Sets their execution status to SUSPENDED. Stops their execution (unless doNotStop is set).
     *
     * @param taskOids a collection of OIDs of tasks that have to be suspended
     * @param waitForStop how long (in milliseconds) to wait for stopping the execution of tasks;
     *                      WAIT_INDEFINITELY means wait indefinitely
     *                      DO_NOT_WAIT means stop the tasks, but do not wait for finishing their execution
     *                      DO_NOT_STOP means do not try to stop the task execution. Tasks will only be put into SUSPENDED state, and
     *                                  their executions (if any) will be left as they are. Use this option only when you know what you're doing.
     * @param parentResult
     * @return true if all the tasks were stopped, false if some tasks continue to run or if stopping was not requested (DO_NOT_STOP option)
     */
    boolean suspendTasks(Collection<String> taskOids, long waitForStop, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException;

    /**
     * Suspends tasks and deletes them.
     *
     * @param taskOids Collection of task OIDs to be suspended and deleted.
     * @param waitForStop How long (in milliseconds) to wait for task stop before proceeding with deletion.
     *                      WAIT_INDEFINITELY means wait indefinitely
     *                      DO_NOT_WAIT means stop the tasks, but do not wait for finishing their execution
     *                      DO_NOT_STOP means do not try to stop the task execution. Tasks will only be put into SUSPENDED state, and
     *                                  their executions (if any) will be left as they are. Use this option only when you know what you're doing.
     * @param alsoSubtasks Should also subtasks be deleted?
     * @param parentResult
     */
    void suspendAndDeleteTasks(Collection<String> taskOids, long waitForStop, boolean alsoSubtasks, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException;

    /**
     * Resume suspended tasks.
     *
     * @param taskOids a collection of OIDs of tasks that have to be resumed
     * @throws SchemaException
     * @throws com.evolveum.midpoint.util.exception.ObjectNotFoundException
     */
    void resumeTasks(Collection<String> taskOids, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException;

    /**
     * Schedules a RUNNABLE/CLOSED tasks to be run immediately. (If a task will really start immediately,
     * depends e.g. on whether a scheduler is started, whether there are available threads, and so on.)
     *
     * @param taskOids a collection of OIDs of tasks that have to be scheduled
     * @param parentResult
     */
    void scheduleTasksNow(Collection<String> taskOids, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException;

    /**
     * Returns information about task, given its identifier.
     * @param identifier
     * @param options
     * @param parentResult
     * @return
     */
    PrismObject<TaskType> getTaskByIdentifier(String identifier, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ConfigurationException, SecurityViolationException;
    //endregion

    //region Node-level operations
    /**
     * Deactivates service threads (temporarily).
     *
     * This will suspend all background activity such as scanning threads, heartbeats and similar mechanisms.
     *
     * Note: The threads are normally activated after task manager implementation starts. This methods should not be used
     * in a normal case.
     *
     *  WARNING: this feature is intended for development-time diagnostics and should not be used on production environments.
     *  Suspending the threads may affect correct behavior of the system (such as timeouts on heartbeats). Use this feature
     *  only if you really know what you are doing.
     *
     *  timeToWait is only for orientation = it may be so that the implementation would wait 2 or 3 times this value
     *  (if it waits separately for several threads completion)
     */
    boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) throws SchemaException, SecurityViolationException;

    /**
     * Re-activates the service threads after they have been deactivated.
     */
    void reactivateServiceThreads(OperationResult parentResult) throws SchemaException, SecurityViolationException;

    /**
     * Returns true if the service threads are running.
     *
     * This method returns true in a normal case. It returns false is the threads were temporarily suspended.
     *
     * @return true if the service threads are running.
     */
    boolean getServiceThreadsActivationState();

    /**
     * Stops the schedulers on a given nodes. This means that at that nodes no tasks will be started.
     *
     * @param nodeIdentifiers Nodes on which the schedulers should be stopped.
     */
    void stopSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException;

    /**
     * Stops a set of schedulers (on their nodes) and tasks that are executing on these nodes.
     *
     * @param nodeIdentifiers collection of node identifiers
     * @param waitTime how long to wait for task shutdown, in milliseconds
     *                 WAIT_INDEFINITELY means wait indefinitely
     *                 DO_NOT_WAIT means stop the tasks, but do not wait for finishing their execution
     * @param parentResult
     * @return
     */
    boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long waitTime, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException;

    /**
     * Starts the scheduler on a given nodes. A prerequisite is that nodes are running and their
     * TaskManagers are not in an error state.
     *
     * @param nodeIdentifiers Nodes on which the scheduler should be started.
     * @return true if the operation succeeded; false otherwise.
     */
    void startSchedulers(Collection<String> nodeIdentifiers, OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException;
    //endregion

    //region Miscellaneous
    /**
     * Synchronizes information in midPoint repository and task scheduling database.
     * Not needed to use during normal operation (only when problems occur).
     *
     * @param parentResult
     */
    void synchronizeTasks(OperationResult parentResult) throws SchemaException, SecurityViolationException;

	/**
	 * Synchronizes information in midPoint repository and activiti database.
	 * Not needed to use during normal operation (only when problems occur).
	 *
	 * @param parentResult
	 */
	void synchronizeWorkflowRequests(OperationResult parentResult) throws SchemaException, SecurityViolationException;

	/**
     * Gets a list of all task categories.
     * TODO consider removing this method
     *
     * @return
     */
    List<String> getAllTaskCategories();

    /**
     * Returns a default handler URI for a given task category.
     * TODO consider removing this method (currently used for task creation via GUI)
     *
     * @param category
     * @return
     */
    String getHandlerUriForCategory(String category);
    //endregion
}
