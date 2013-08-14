/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.task.quartzimpl;

import java.text.ParseException;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.data.common.RTask;
import com.evolveum.midpoint.repo.sql.query.definition.PropertyDefinition;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.hibernate.Session;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.execution.ExecutionManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * Task Manager implementation using Quartz scheduler.
 *
 * Main classes:
 *  - TaskManagerQuartzImpl
 *  - TaskQuartzImpl
 *
 * Helper classes:
 *  - ExecutionManager: node-related functions (start, stop, query status), task-related functions (stop, query status)
 *    - LocalNodeManager and RemoteExecutionManager (specific methods for local node and remote nodes)
 *
 *  - TaskManagerConfiguration: access to config gathered from various places (midpoint config, sql repo config, system properties)
 *  - ClusterManager: keeps cluster nodes synchronized and verifies cluster configuration sanity
 *  - JmxClient: used to invoke remote JMX agents
 *  - JmxServer: provides a JMX agent for midPoint
 *  - TaskSynchronizer: synchronizes information about tasks between midPoint repo and Quartz Job Store
 *  - Initializer: contains TaskManager initialization code (quite complex)
 *
 * @author Pavol Mederly
 *
 */
@Service(value = "taskManager")
@DependsOn(value="repositoryService")
public class TaskManagerQuartzImpl implements TaskManager, BeanFactoryAware {

    private static final String DOT_INTERFACE = TaskManager.class.getName() + ".";
    private static final String DOT_IMPL_CLASS = TaskManagerQuartzImpl.class.getName() + ".";
    private static final String OPERATION_SUSPEND_TASKS = DOT_INTERFACE + "suspendTasks";
    private static final String OPERATION_DEACTIVATE_SERVICE_THREADS = DOT_INTERFACE + "deactivateServiceThreads";
    private static final String CLEANUP_TASKS = DOT_INTERFACE + "cleanupTasks";

    // instances of all the helper classes (see their definitions for their description)
    private TaskManagerConfiguration configuration = new TaskManagerConfiguration();
    private ExecutionManager executionManager = new ExecutionManager(this);
    private ClusterManager clusterManager = new ClusterManager(this);
    
    // task handlers (mapped from their URIs)
    private Map<String,TaskHandler> handlers = new HashMap<String, TaskHandler>();

    // cached task prism definition
	private PrismObjectDefinition<TaskType> taskPrismDefinition;

    // error status for this node (local Quartz scheduler is not allowed to be started if this status is not "OK")
    private NodeErrorStatus nodeErrorStatus = NodeErrorStatus.OK;

	private BeanFactory beanFactory;

    @Autowired(required=true)
    MidpointConfiguration midpointConfiguration;

    @Autowired(required=true)
	private RepositoryService repositoryService;

    @Autowired(required=true)
	private LightweightIdentifierGenerator lightweightIdentifierGenerator;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
    private static final transient Trace LOGGER = TraceManager.getTrace(TaskManagerQuartzImpl.class);

    // how long to wait after TaskManager shutdown, if using JDBC Job Store (in order to give the jdbc thread pool a chance
    // to close, before embedded H2 database server would be closed by the SQL repo shutdown procedure)
    //
    // the fact that H2 database is embedded is recorded in the 'databaseIsEmbedded' configuration flag
    // (see shutdown() method)
    private static final long WAIT_ON_SHUTDOWN = 2000;

    /*
    *  ********************* INITIALIZATION AND SHUTDOWN *********************
    */

    /**
     * Initialization.
     *
     * TaskManager can work in two modes:
     *  - "stop on initialization failure" - it means that if TaskManager initialization fails, the midPoint will
     *    not be started (implemented by throwing SystemException). This is a safe approach, however, midPoint could
     *    be used also without Task Manager, so it is perhaps too harsh to do it this way.
     *  - "continue on initialization failure" - after such a failure midPoint initialization simply continues;
     *    however, task manager is switched to "Error" state, in which the scheduler cannot be started;
     *    Moreover, actually almost none Task Manager methods can be invoked, to prevent a damage.
     *
     *    This second mode is EXPERIMENTAL, should not be used in production for now.
     *
     *  ---
     *  So,
     *
     *  (A) Generally, when not initialized, we refuse to execute almost all operations (knowing for sure that
     *  the scheduler is not running).
     *
     *  (B) When initialized, but in error state (typically because of cluster misconfiguration not related to this node),
     *  we refuse to start the scheduler on this node. Other methods are OK.
     *
     */

    @PostConstruct
    public void init() {

        OperationResult result = createOperationResult("init");

        try {
            new Initializer(this).init(result);
        } catch (TaskManagerInitializationException e) {
            LoggingUtils.logException(LOGGER, "Cannot initialize TaskManager due to the following exception: ", e);
            throw new SystemException("Cannot initialize TaskManager", e);
        }

        // if running in test mode, the postInit will not be executed... so we have to start scheduler here
        if (configuration.isTestMode()) {
            postInit(result);
        }
    }

    @Override
    public void postInit(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_IMPL_CLASS + "postInit");

        if (!configuration.isTestMode()) {
            clusterManager.startClusterManagerThread();
        }

        executionManager.startScheduler(getNodeId(), result);
        if (result.getLastSubresultStatus() != OperationResultStatus.SUCCESS) {
            throw new SystemException("Quartz task scheduler couldn't be started.");
        }
        
        result.computeStatus();
    }

    @PreDestroy
    public void shutdown() {

        OperationResult result = createOperationResult("shutdown");

        LOGGER.info("Task Manager shutdown starting");

        if (executionManager.getQuartzScheduler() != null) {

            executionManager.stopScheduler(getNodeId(), result);
            executionManager.stopAllTasksOnThisNodeAndWait(0L, result);

            if (configuration.isTestMode()) {
                LOGGER.info("Quartz scheduler will NOT be shutdown. It stays in paused mode.");
            } else {
                try {
                    executionManager.shutdownLocalScheduler();
                } catch (TaskManagerException e) {
                    LoggingUtils.logException(LOGGER, "Cannot shutdown Quartz scheduler, continuing with node shutdown", e);
                }
            }
        }

        clusterManager.stopClusterManagerThread(0L, result);
        clusterManager.recordNodeShutdown(result);

        if (configuration.isJdbcJobStore() && configuration.isDatabaseIsEmbedded()) {
            LOGGER.trace("Waiting {} msecs to give Quartz thread pool a chance to shutdown.", WAIT_ON_SHUTDOWN);
            try {
                Thread.sleep(WAIT_ON_SHUTDOWN);
            } catch (InterruptedException e) {
                // safe to ignore
            }
        }
        LOGGER.info("Task Manager shutdown finished");
    }

    /*
     *  ********************* STATE MANAGEMENT *********************
     */

    public boolean isInErrorState() {
        return nodeErrorStatus != NodeErrorStatus.OK;
    }

    /*
     *  ********************* OWN BUSINESS LOGIC *********************
     */

    /*
    * First here are TaskManager API methods implemented in this class,
    * then those, which are delegated to helper classes.
    */

    @Override
    public boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "deactivateServiceThreads");
        result.addParam("timeToWait", timeToWait);

        LOGGER.info("Deactivating Task Manager service threads (waiting time = " + timeToWait + ")");
        clusterManager.stopClusterManagerThread(timeToWait, result);
        boolean retval = executionManager.stopSchedulerAndTasksLocally(timeToWait, result);

        result.computeStatus();
        return retval;
    }

    @Override
    public void reactivateServiceThreads(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "reactivateServiceThreads");

        LOGGER.info("Reactivating Task Manager service threads.");
        clusterManager.startClusterManagerThread();
        executionManager.startScheduler(getNodeId(), result);

        result.computeStatus();
    }

    @Override
    public boolean getServiceThreadsActivationState() {

        try {
            Scheduler scheduler = executionManager.getQuartzScheduler();
            return scheduler != null && scheduler.isStarted() &&
                    !scheduler.isInStandbyMode() &&
                    !scheduler.isShutdown() &&
                    clusterManager.isClusterManagerThreadActive();
        } catch (SchedulerException e) {
            LoggingUtils.logException(LOGGER, "Cannot determine the state of the Quartz scheduler", e);
            return false;
        }
    }

    @Override
    public boolean suspendTask(Task task, long waitTime, OperationResult parentResult) {

        return suspendTasks(oneItemSet(task), waitTime, parentResult);
    }

    public boolean suspendTask(Task task, long waitTime, boolean doNotStop, OperationResult parentResult) {

        return suspendTasks(oneItemSet(task), waitTime, doNotStop, parentResult);
    }

    private<T> Set<T> oneItemSet(T item) {
        Set<T> set = new HashSet<T>();
        set.add(item);
        return set;
    }

    @Override
    public boolean suspendTasks(Collection<Task> tasks, long waitTime, OperationResult parentResult) {
        return suspendTasks(tasks, waitTime, false, parentResult);
    }

    @Override
    public boolean suspendTasks(Collection<Task> tasks, long waitTime, boolean doNotStop, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "suspendTasks");
        result.addArbitraryCollectionAsParam("tasks", tasks);
        result.addParam("waitTime", waitTime);
        result.addParam("doNotStop", doNotStop);

        LOGGER.info("Suspending tasks " + tasks + " (waiting " + waitTime + " msec)");

        for (Task task : tasks) {

            if (task.getOid() == null) {
                // this should not occur; so we can treat it in such a brutal way
                throw new IllegalArgumentException("Only persistent tasks can be suspended (for now); task " + task + " is transient.");
            } else {
                try {
                    ((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.SUSPENDED, result);
                } catch (ObjectNotFoundException e) {
                    String message = "Cannot suspend task because it does not exist; task = " + task;
                    LoggingUtils.logException(LOGGER, message, e);
                } catch (SchemaException e) {
                    String message = "Cannot suspend task because of schema exception; task = " + task;
                    LoggingUtils.logException(LOGGER, message, e);
                }

                executionManager.unscheduleTask(task, result);
                // even if this will not succeed, by setting the execution status to SUSPENDED we hope the task
                // thread will exit on next iteration (does not apply to single-run tasks, of course)
            }
        }

        boolean stopped = false;
        if (!doNotStop) {
            stopped = executionManager.stopTasksRunAndWait(tasks, null, waitTime, true, result);
        }
        result.computeStatus();
        return stopped;
    }

    // todo: better name for this method

    @Override
    public void pauseTask(Task task, TaskWaitingReason reason, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "pauseTask");
        result.addArbitraryObjectAsParam("task", task);

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            String message = "Attempted to pause a task that is not in the RUNNABLE state (task = " + task + ", state = " + task.getExecutionStatus();
            LOGGER.error(message);
            result.recordFatalError(message);
            return;
        }
        try {
            ((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.WAITING, result);
            ((TaskQuartzImpl) task).setWaitingReasonImmediate(reason, result);
        } catch (ObjectNotFoundException e) {
            String message = "A task cannot be paused, because it does not exist; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            throw e;
        } catch (SchemaException e) {
            String message = "A task cannot be paused due to schema exception; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            throw e;
        }

        // make the trigger as it should be
        executionManager.synchronizeTask((TaskQuartzImpl) task, result);

        if (result.isUnknown()) {
            result.computeStatus();
        }
    }

    // todo: better name for this method

    @Override
    public void unpauseTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "unpauseTask");
        result.addArbitraryObjectAsParam("task", task);

        if (task.getExecutionStatus() != TaskExecutionStatus.WAITING) {
            String message = "Attempted to unpause a task that is not in the WAITING state (task = " + task + ", state = " + task.getExecutionStatus();
            LOGGER.error(message);
            result.recordFatalError(message);
            return;
        }
        resumeOrUnpauseTask(task, result);
    }

    @Override
    public void resumeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "resumeTask");
        result.addArbitraryObjectAsParam("task", task);

        if (task.getExecutionStatus() != TaskExecutionStatus.SUSPENDED) {
            String message = "Attempted to resume a task that is not in the SUSPENDED state (task = " + task + ", state = " + task.getExecutionStatus();
            LOGGER.error(message);
            result.recordFatalError(message);
            return;
        }
        resumeOrUnpauseTask(task, result);
    }

    private void resumeOrUnpauseTask(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {

        try {
            ((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNABLE, result);
        } catch (ObjectNotFoundException e) {
            String message = "A task cannot be resumed/unpaused, because it does not exist; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            throw e;
        } catch (SchemaException e) {
            String message = "A task cannot be resumed/unpaused due to schema exception; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            throw e;
        }

        // make the trigger as it should be
        executionManager.synchronizeTask((TaskQuartzImpl) task, result);

        if (result.isUnknown()) {
            result.computeStatus();
        }
    }

    /*
     *  ********************* WORKING WITH TASK INSTANCES *********************
     */

	@Override
	public Task createTaskInstance() {
		return createTaskInstance(null);
	}
	
	@Override
	public Task createTaskInstance(String operationName) {
		LightweightIdentifier taskIdentifier = generateTaskIdentifier();
		TaskQuartzImpl taskImpl = new TaskQuartzImpl(this, taskIdentifier, operationName);
		return taskImpl;
	}
	
	private LightweightIdentifier generateTaskIdentifier() {
		return lightweightIdentifierGenerator.generate();
	}

    @Override
    public Task createTaskInstance(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException {
        return createTaskInstance(taskPrism, null, parentResult);
    }

    @Override
	public Task createTaskInstance(PrismObject<TaskType> taskPrism, String operationName, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "createTaskInstance");
        result.addParam("taskPrism", taskPrism);

		//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
		RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
		TaskQuartzImpl task = new TaskQuartzImpl(this, taskPrism, repoService, operationName);
		task.resolveOwnerRef(result);
        result.recordSuccessIfUnknown();
		return task;
	}

    void updateTaskInstance(Task task, PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "updateTaskInstance");
        result.addArbitraryObjectAsParam("task", task);
        result.addParam("taskPrism", taskPrism);

        TaskQuartzImpl taskQuartz = (TaskQuartzImpl) task;
        taskQuartz.replaceTaskPrism(taskPrism);
        taskQuartz.resolveOwnerRef(result);
        result.recordSuccessIfUnknown();
    }

	@Override
	public Task getTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getTask");
		result.addParam(OperationResult.PARAM_OID, taskOid);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);
		
		Task task;
		try {
			PrismObject<TaskType> taskPrism = repositoryService.getObject(TaskType.class, taskOid, null, result);
            task = createTaskInstance(taskPrism, result);
        } catch (ObjectNotFoundException e) {
			result.recordFatalError("Task not found", e);
			throw e;
		} catch (SchemaException e) {
			result.recordFatalError("Task schema error: "+e.getMessage(), e);
			throw e;
		}
		
		result.recordSuccess();
		return task;
	}

	@Override
	public void switchToBackground(final Task task, OperationResult parentResult) {

		parentResult.recordStatus(OperationResultStatus.IN_PROGRESS, "Task switched to background");
		OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "switchToBackground");

        // if the task result was unknown, we change it to 'in-progress'
        // (and roll back this change if storing into repo fails...)
        boolean wasUnknown = false;
		try {
            if (task.getResult().isUnknown()) {
                wasUnknown = true;
                task.getResult().recordInProgress();
            }
			persist(task, result);
            result.recordSuccess();
        } catch (RuntimeException ex) {
            if (wasUnknown) {
                task.getResult().recordUnknown();
            }
			result.recordFatalError("Unexpected problem: "+ex.getMessage(),ex);
			throw ex;
		}
	}

	private void persist(Task task, OperationResult parentResult) {
		if (task.getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT) {
			// Task already persistent. Nothing to do.
			return;
		}

        TaskQuartzImpl taskImpl = (TaskQuartzImpl) task;

        if (task.getName() == null) {
        	PolyStringType polyStringName = new PolyStringType("Task " + task.getTaskIdentifier());
            taskImpl.setNameTransient(polyStringName);
        }

        if (taskImpl.getOid() != null) {
			// We don't support user-specified OIDs
			throw new IllegalArgumentException("Transient task must not have OID (task:"+task+")");
		}

        // hack: set Category if it is not set yet
        if (taskImpl.getCategory() == null) {
            taskImpl.setCategoryTransient(taskImpl.getCategoryFromHandler());
        }
		
//		taskImpl.setPersistenceStatusTransient(TaskPersistenceStatus.PERSISTENT);

		// Make sure that the task has repository service instance, so it can fully work as "persistent"
    	if (taskImpl.getRepositoryService() == null) {
			RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
			taskImpl.setRepositoryService(repoService);
		}

		try {
			addTaskToRepositoryAndQuartz(taskImpl, parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			// This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
			throw new IllegalStateException("Got ObjectAlreadyExistsException while not expecting it (task:"+task+")",ex);
		} catch (SchemaException ex) {
			// This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
			throw new IllegalStateException("Got SchemaException while not expecting it (task:"+task+")",ex);
		}
	}
	
	@Override
	public String addTask(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "addTask");
		Task task = createTaskInstance(taskPrism, result);			// perhaps redundant, but it's more convenient to work with Task than with Task prism
        if (task.getTaskIdentifier() == null) {
            task.getTaskPrismObject().asObjectable().setTaskIdentifier(generateTaskIdentifier().toString());
        }
		String oid = addTaskToRepositoryAndQuartz(task, result);
        result.computeStatus();
        return oid;
	}

	private String addTaskToRepositoryAndQuartz(Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_IMPL_CLASS + "addTaskToRepositoryAndQuartz");
        result.addArbitraryObjectAsParam("task", task);

		PrismObject<TaskType> taskPrism = task.getTaskPrismObject();
        String oid;
        try {
		     oid = repositoryService.addObject(taskPrism, null, result);
        } catch (ObjectAlreadyExistsException e) {
            result.recordFatalError("Couldn't add task to repository: " + e.getMessage(), e);
            throw e;
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't add task to repository: " + e.getMessage(), e);
            throw e;
        }

		((TaskQuartzImpl) task).setOid(oid);
		
		synchronizeTaskWithQuartz((TaskQuartzImpl) task, result);

        result.computeStatus();
		return oid;
	}

    @Override
    public void modifyTask(String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "modifyTask");
        try {
		    repositoryService.modifyObject(TaskType.class, oid, modifications, result);
            TaskQuartzImpl task = (TaskQuartzImpl) getTask(oid, result);
            task.setRecreateQuartzTrigger(true);
            synchronizeTaskWithQuartz(task, result);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void suspendAndDeleteTasks(List<String> taskOidList, long suspendTimeout, boolean alsoSubtasks, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "suspendAndDeleteTasks");
        result.addCollectionOfSerializablesAsParam("taskOidList", taskOidList);

        List<Task> tasksToBeDeleted = new ArrayList<Task>();
        for (String oid : taskOidList) {
            try {
                Task task = getTask(oid, result);
                tasksToBeDeleted.add(task);
                if (alsoSubtasks) {
                    tasksToBeDeleted.addAll(task.listSubtasksDeeply(result));
                }
            } catch (ObjectNotFoundException e) {
                // just skip suspending/deleting this task. As for the error, it should be already put into result.
                LoggingUtils.logException(LOGGER, "Error when retrieving task {} or its subtasks before the deletion. Skipping the deletion for this task.", e, oid);
            } catch (SchemaException e) {
                // same as above
                LoggingUtils.logException(LOGGER, "Error when retrieving task {} or its subtasks before the deletion. Skipping the deletion for this task.", e, oid);
            } catch (RuntimeException e) {
                result.createSubresult(DOT_IMPL_CLASS + "getTaskTree").recordPartialError("Unexpected error when retrieving task tree for " + oid + " before deletion", e);
                LoggingUtils.logException(LOGGER, "Unexpected error when retrieving task {} or its subtasks before the deletion. Skipping the deletion for this task.", e, oid);
            }
        }

        // now suspend the tasks before deletion
        suspendTasks(tasksToBeDeleted, suspendTimeout, result);

        // delete them
        for (Task task : tasksToBeDeleted) {
            try {
                deleteTask(task.getOid(), result);
            } catch (ObjectNotFoundException e) {   // in all cases (even RuntimeException) the error is already put into result
                LoggingUtils.logException(LOGGER, "Error when deleting task {}", e, task);
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Error when deleting task {}", e, task);
            } catch (RuntimeException e) {
                LoggingUtils.logException(LOGGER, "Error when deleting task {}", e, task);
            }
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }
    }

    @Override
    public void deleteTask(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "deleteTask");
        result.addParam("oid", oid);
        try {
            Task task = getTask(oid, result);
            if (task.getNode() != null) {
                result.recordWarning("Deleting a task that seems to be currently executing on node " + task.getNode());
            }
            repositoryService.deleteObject(TaskType.class, oid, result);
            executionManager.removeTaskFromQuartz(oid, result);
            result.recordSuccessIfUnknown();
        } catch (ObjectNotFoundException e) {
            result.recordFatalError("Cannot delete the task because it does not exist.", e);
            throw e;
        } catch (SchemaException e) {
            result.recordFatalError("Cannot delete the task because of schema exception.", e);
            throw e;
        } catch (RuntimeException e) {
            result.recordFatalError("Cannot delete the task because of a runtime exception.", e);
            throw e;
        }
    }

    @Override
    public Long getNextRunStartTime(String oid, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getNextRunStartTime");
        result.addParam("oid", oid);
        return executionManager.getNextRunStartTime(oid, result);
    }

    /*
     *  ********************* SEARCHING TASKS AND NODES *********************
     */

    /*
     * Gets nodes from repository and adds runtime information to them (taken from ClusterStatusInformation).
     */
    @Override
    public int countNodes(ObjectQuery query, OperationResult result) throws SchemaException {
        return repositoryService.countObjects(NodeType.class, query, result);
    }

    @Override
    public List<Node> searchNodes(ObjectQuery query, ClusterStatusInformation clusterStatusInformation, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "searchNodes");
        result.addParam("query", query);
//        result.addParam("paging", paging);
        result.addParam("clusterStatusInformation", clusterStatusInformation);

        if (clusterStatusInformation == null) {
            clusterStatusInformation = executionManager.getClusterStatusInformation(true, result);
        }

        List<PrismObject<NodeType>> nodesInRepository;
        try {
            nodesInRepository = repositoryService.searchObjects(NodeType.class, query, null, result);
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't get nodes from repository: " + e.getMessage());
            throw e;
        }

        List<Node> retval = new ArrayList<Node>();
        for (PrismObject<NodeType> nodeInRepository : nodesInRepository) {
            Node node = new Node(nodeInRepository);
            Node nodeRuntimeInfo = clusterStatusInformation.findNodeById(node.getNodeIdentifier());
            if (nodeRuntimeInfo != null) {
                node.setNodeExecutionStatus(nodeRuntimeInfo.getNodeExecutionStatus());
                node.setNodeErrorStatus(nodeRuntimeInfo.getNodeErrorStatus());
                node.setConnectionError(nodeRuntimeInfo.getConnectionError());
            } else {
                // node is in repo, but no information on it is present in CSI
                // (should not occur except for some temporary conditions, because CSI contains info on all nodes from repo)
                node.setNodeExecutionStatus(NodeExecutionStatus.COMMUNICATION_ERROR);
                node.setConnectionError("Node not known at this moment");       // TODO localize this message
            }
            retval.add(node);
        }
        LOGGER.trace("searchNodes returning " + retval);
        result.computeStatus();
        return retval;
    }

    @Override
    public List<Task> listTasksRelatedToObject(String oid, ClusterStatusInformation clusterStatusInformation, OperationResult result) throws SchemaException {
        ObjectQuery query = new ObjectQuery();
        List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

        PrismPropertyDefinition oidDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(ObjectReferenceType.F_OID);
        PrismPropertyValue<String> oidValue = oidDefinition.instantiate().getValue();
        oidValue.setValue(oid);
        filters.add(EqualsFilter.createEqual(new ItemPath(TaskType.F_OBJECT_REF, ObjectReferenceType.F_OID), oidDefinition, oidValue));

        return searchTasks(query, clusterStatusInformation, result);
    }

   
    @Override
    public List<Task> searchTasks(ObjectQuery query, ClusterStatusInformation clusterStatusInformation, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "searchTasks");
        result.addParam("query", query);
//        result.addParam("paging", paging);
        result.addParam("clusterStatusInformation", clusterStatusInformation);

        if (clusterStatusInformation == null) {
            clusterStatusInformation = getExecutionManager().getClusterStatusInformation(true, result);
        }

        List<PrismObject<TaskType>> tasksInRepository;
        try {
            tasksInRepository = repositoryService.searchObjects(TaskType.class, query, null, result);
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't get tasks from repository: " + e.getMessage());
            throw e;
        }

        List<Task> retval = new ArrayList<Task>();
        for (PrismObject<TaskType> taskInRepository : tasksInRepository) {
            Task task = createTaskInstance(taskInRepository, result);
            Node runsAt = clusterStatusInformation.findNodeInfoForTask(task.getOid());
            if (runsAt != null) {
                ((TaskQuartzImpl) task).setCurrentlyExecutesAt(runsAt);
            } else {
                ((TaskQuartzImpl) task).setCurrentlyExecutesAt(null);
            }
            retval.add(task);
        }
        result.computeStatus();
        return retval;
    }

    
    @Override
    public int countTasks(ObjectQuery query, OperationResult result) throws SchemaException {
        return repositoryService.countObjects(TaskType.class, query, result);
    }

    /*
    *  ********************* MANAGING HANDLERS AND TASK CATEGORIES *********************
    */

	@Override
	public void registerHandler(String uri, TaskHandler handler) {
        LOGGER.trace("Registering task handler for URI " + uri);
		handlers.put(uri, handler);
	}
	
	public TaskHandler getHandler(String uri) {
		if (uri != null)
			return handlers.get(uri);
		else
			return null;
	}

    @Override
    public List<String> getAllTaskCategories() {

        Set<String> categories = new HashSet<String>();
        for (TaskHandler h : handlers.values()) {
            List<String> cat = h.getCategoryNames();
            if (cat != null) {
                categories.addAll(cat);
            } else {
                String catName = h.getCategoryName(null);
                if (catName != null) {
                    categories.add(catName);
                }
            }
        }
        return new ArrayList<String>(categories);
    }

    @Override
    public String getHandlerUriForCategory(String category) {
        for (Map.Entry<String,TaskHandler> h : handlers.entrySet()) {
            List<String> cats = h.getValue().getCategoryNames();
            if (cats != null && cats.contains(category)) {
                return h.getKey();
            } else {
                String cat = h.getValue().getCategoryName(null);
                if (category.equals(cat)) {
                    return h.getKey();
                }
            }
        }
        return null;
    }

    /*
    *  ********************* TASK CREATION/REMOVAL LISTENERS *********************
    */

    @Override
    public void onTaskCreate(String oid, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "onTaskCreate");
        result.addParam("oid", oid);

        LOGGER.trace("onTaskCreate called for oid = " + oid);

        Task task;
        try {
            task = getTask(oid, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Quartz shadow job cannot be created, because task in repository was not found; oid = {}", e, oid);
            result.computeStatus();
            return;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Quartz shadow job cannot be created, because task from repository could not be retrieved; oid = {}", e, oid);
            result.computeStatus();
            return;
        }

        ((TaskQuartzImpl) task).synchronizeWithQuartz(result);
        result.computeStatus();
    }

    @Override
    public void onTaskDelete(String oid, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "onTaskDelete");
        result.addParam("oid", oid);

        LOGGER.trace("onTaskDelete called for oid = " + oid);

        JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTaskOid(oid);

        try {
            if (executionManager.getQuartzScheduler().checkExists(jobKey)) {
                executionManager.getQuartzScheduler().deleteJob(jobKey);			// removes triggers as well
            }
        } catch (SchedulerException e) {
            String message = "Quartz shadow job cannot be removed; oid = " + oid;
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message);
        }

        result.recordSuccessIfUnknown();

    }

    /*
     *  ********************* OTHER METHODS + GETTERS AND SETTERS *********************
     */
	
    PrismObjectDefinition<TaskType> getTaskObjectDefinition() {
		if (taskPrismDefinition == null) {
			taskPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
		}
		return taskPrismDefinition;
	}

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(TaskManagerQuartzImpl.class.getName() + "." + methodName);
    }

    public TaskManagerConfiguration getConfiguration() {
        return configuration;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public NodeErrorStatus getLocalNodeErrorStatus() {
        return nodeErrorStatus;
    }

    public void setNodeErrorStatus(NodeErrorStatus nodeErrorStatus) {
        this.nodeErrorStatus = nodeErrorStatus;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public MidpointConfiguration getMidpointConfiguration() {
        return midpointConfiguration;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }
    
    public void setConfiguration(TaskManagerConfiguration configuration) {
        this.configuration = configuration;
    }

    public ExecutionManager getExecutionManager() {
        return executionManager;
    }


    /*
     *  ********************* DELEGATIONS *********************
     */

    void synchronizeTaskWithQuartz(TaskQuartzImpl task, OperationResult parentResult) {
        executionManager.synchronizeTask(task, parentResult);
    }

    @Override
    public void synchronizeTasks(OperationResult result) {
        executionManager.synchronizeJobStores(result);
    }

    @Override
    public String getNodeId() {
        return clusterManager.getNodeId();
    }

    @Override
    public Set<Task> getLocallyRunningTasks(OperationResult parentResult) throws TaskManagerException {
        return executionManager.getLocallyRunningTasks(parentResult);
    }

    @Override
    public void stopScheduler(String nodeIdentifier, OperationResult parentResult) {
        executionManager.stopScheduler(nodeIdentifier, parentResult);
    }

    @Override
    public void startScheduler(String nodeIdentifier, OperationResult parentResult) {
        executionManager.startScheduler(nodeIdentifier, parentResult);
    }

//    @Override
//    public boolean stopSchedulerAndTasks(String nodeIdentifier, long timeToWait) {
//        return executionManager.stopSchedulerAndTasks(nodeIdentifier, timeToWait);
//    }

    @Override
    public boolean stopSchedulersAndTasks(List<String> nodeList, long timeToWait, OperationResult result) {
        return executionManager.stopSchedulersAndTasks(nodeList, timeToWait, result);
    }

//    @Override
//    public boolean isTaskThreadActiveClusterwide(String oid, OperationResult parentResult) {
//        return executionManager.isTaskThreadActiveClusterwide(oid);
//    }

    private long lastRunningTasksClusterwideQuery = 0;
    private ClusterStatusInformation lastClusterStatusInformation = null;

    @Override
    public ClusterStatusInformation getRunningTasksClusterwide(OperationResult parentResult) {
        lastClusterStatusInformation = executionManager.getClusterStatusInformation(true, parentResult);
        lastRunningTasksClusterwideQuery = System.currentTimeMillis();
        return lastClusterStatusInformation;
    }

    @Override
    public ClusterStatusInformation getRunningTasksClusterwide(long allowedAge, OperationResult parentResult) {
        long age = System.currentTimeMillis() - lastRunningTasksClusterwideQuery;
        if (lastClusterStatusInformation != null && age < allowedAge) {
            LOGGER.trace("Using cached ClusterStatusInformation, age = " + age);
            parentResult.recordSuccess();
            return lastClusterStatusInformation;
        } else {
            LOGGER.trace("Cached ClusterStatusInformation too old, age = " + age);
            return getRunningTasksClusterwide(parentResult);
        }

    }

    @Override
    public boolean isCurrentNode(PrismObject<NodeType> node) {
        return clusterManager.isCurrentNode(node);
    }

    @Override
    public void deleteNode(String nodeIdentifier, OperationResult result) {
        clusterManager.deleteNode(nodeIdentifier, result);
    }

    @Override
    public void scheduleTaskNow(Task task, OperationResult parentResult) {
        executionManager.scheduleTaskNow(task, parentResult);
    }

    public void unscheduleTask(Task task, OperationResult parentResult) {
        executionManager.unscheduleTask(task, parentResult);
    }

    // use with care (e.g. w.r.t. dependent tasks)
    public void closeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        try {
            // todo do in one modify operation
            ((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.CLOSED, parentResult);
            ((TaskQuartzImpl) task).setCompletionTimestampImmediate(System.currentTimeMillis(), parentResult);
        } finally {
            executionManager.removeTaskFromQuartz(task.getOid(), parentResult);
        }
    }

    // do not forget to kick dependent tasks when closing this one (currently only done in finishHandler)
    public void closeTaskWithoutSavingState(Task task, OperationResult parentResult) {
        ((TaskQuartzImpl) task).setExecutionStatus(TaskExecutionStatus.CLOSED);
        ((TaskQuartzImpl) task).setCompletionTimestamp(System.currentTimeMillis());
        executionManager.removeTaskFromQuartz(task.getOid(), parentResult);
    }

    @Override
    public ParseException validateCronExpression(String cron) {
        return TaskQuartzImplUtil.validateCronExpression(cron);
    }

    // currently finds only persistent tasks
    @Override
    public Task getTaskByIdentifier(String identifier, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getTaskByIdentifier");
        result.addParam("identifier", identifier);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        ObjectFilter filter = null;
        try {
            filter = EqualsFilter.createEqual(TaskType.class, prismContext, TaskType.F_TASK_IDENTIFIER, identifier);
        } catch (SchemaException e) {
            throw new SystemException("Cannot create filter for identifier value due to schema exception", e);
        }
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        List<PrismObject<TaskType>> list = repositoryService.searchObjects(TaskType.class, query, null, result);
        if (list.isEmpty()) {
            throw new ObjectNotFoundException("Task with identifier " + identifier + " could not be found");
        } else if (list.size() > 1) {
            throw new IllegalStateException("Found more than one task with identifier " + identifier + " (" + list.size() + " of them)");
        }

        Task task = createTaskInstance(list.get(0), result);
        result.recordSuccessIfUnknown();
        return task;

    }

    public void checkWaitingTasks(OperationResult result) throws SchemaException {
        int count = 0;
        List<Task> tasks = listWaitingTasks(TaskWaitingReason.OTHER_TASKS, result);
        for (Task task : tasks) {
            try {
                ((TaskQuartzImpl) task).checkDependencies(result);
                count++;
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't check dependencies for task {}", e, task);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't check dependencies for task {}", e, task);
            }
        }
        LOGGER.trace("Check waiting tasks completed; {} tasks checked.", count);
    }

    private List<Task> listWaitingTasks(TaskWaitingReason reason, OperationResult result) throws SchemaException {

        ObjectFilter filter, filter1 = null, filter2 = null;
        try {
            filter1 = EqualsFilter.createEqual(TaskType.class, prismContext, TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.WAITING);
            if (reason != null) {
                filter2 = EqualsFilter.createEqual(TaskType.class, prismContext, TaskType.F_WAITING_REASON, reason.toTaskType());
            }
        } catch (SchemaException e) {
            throw new SystemException("Cannot create filter for listing waiting tasks due to schema exception", e);
        }
        filter = filter2 != null ? AndFilter.createAnd(filter1, filter2) : filter1;
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

//        query = new ObjectQuery();  // todo remove this hack when searching will work

        List<PrismObject<TaskType>> prisms = repositoryService.searchObjects(TaskType.class, query, null, result);
        List<Task> tasks = resolveTasksFromTaskTypes(prisms, result);

        result.recordSuccessIfUnknown();
        return tasks;

    }

    List<Task> resolveTasksFromTaskTypes(List<PrismObject<TaskType>> taskPrisms, OperationResult result) throws SchemaException {
        List<Task> tasks = new ArrayList<Task>(taskPrisms.size());
        for (PrismObject<TaskType> taskPrism : taskPrisms) {
            tasks.add(createTaskInstance(taskPrism, result));
        }

        result.recordSuccessIfUnknown();
        return tasks;
    }

    @Override
    public void cleanupTasks(CleanupPolicyType policy, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createSubresult(CLEANUP_TASKS);

        if (policy.getMaxAge() == null) {
            return;
        }

        Duration duration = policy.getMaxAge();
        if (duration.getSign() > 0) {
            duration = duration.negate();
        }
        Date deleteTasksClosedUpTo = new Date();
        duration.addTo(deleteTasksClosedUpTo);

        LOGGER.info("Starting cleanup for closed tasks deleting up to {} (duration '{}').",
                new Object[]{deleteTasksClosedUpTo, duration});

        XMLGregorianCalendar timeXml = XmlTypeConverter.createXMLGregorianCalendar(deleteTasksClosedUpTo.getTime());

        List<PrismObject<TaskType>> obsoleteTasks;
        try {
            ObjectQuery obsoleteTasksQuery = ObjectQuery.createObjectQuery(AndFilter.createAnd(
                    LessFilter.createLessFilter(TaskType.class, getPrismContext(), TaskType.F_COMPLETION_TIMESTAMP, timeXml, true),
                    EqualsFilter.createEqual(TaskType.class, getPrismContext(), TaskType.F_PARENT, null)));

            obsoleteTasks = repositoryService.searchObjects(TaskType.class, obsoleteTasksQuery, null, result);
        } catch (SchemaException e) {
            throw new SchemaException("Couldn't get the list of obsolete tasks: " + e.getMessage(), e);
        }

        LOGGER.debug("Found {} task tree(s) to be cleaned up", obsoleteTasks.size());

        int deleted = 0;
        int problems = 0;
        int bigProblems = 0;
        for (PrismObject<TaskType> rootTaskPrism : obsoleteTasks) {

            // get whole tree
            Task rootTask = createTaskInstance(rootTaskPrism, result);
            List<Task> taskTreeMembers = rootTask.listSubtasksDeeply(result);
            taskTreeMembers.add(rootTask);

            LOGGER.trace("Removing task {} along with its {} children.", rootTask, taskTreeMembers.size()-1);

            boolean problem = false;
            for (Task task : taskTreeMembers) {
                try {
                    deleteTask(task.getOid(), result);
                } catch (SchemaException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't delete obsolete task {} due to schema exception", e, task);
                    problem = true;
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't delete obsolete task {} due to object not found exception", e, task);
                    problem = true;
                } catch (RuntimeException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't delete obsolete task {} due to a runtime exception", e, task);
                    problem = true;
                }

                if (problem) {
                    problems++;
                    if (!task.getTaskIdentifier().equals(rootTask.getTaskIdentifier())) {
                        bigProblems++;
                    }
                } else {
                    deleted++;
                }
            }
        }
        result.recomputeStatus();

        LOGGER.info("Task cleanup procedure finished. Successfully deleted {} tasks; there were problems with deleting {} tasks.", deleted, problems);
        if (bigProblems > 0) {
            LOGGER.error("{} subtask(s) couldn't be deleted. Inspect that manually, otherwise they might reside in repo forever.", bigProblems);
        }
        if (problems == 0) {
            parentResult.createSubresult(CLEANUP_TASKS + ".statistics").recordStatus(OperationResultStatus.SUCCESS, "Successfully deleted " + deleted + " task(s).");
        } else {
            parentResult.createSubresult(CLEANUP_TASKS + ".statistics").recordPartialError("Successfully deleted " + deleted + " task(s), "
                    + "there was problems with deleting " + problems + " tasks."
                    + (bigProblems > 0 ? (" " + bigProblems + " subtask(s) couldn't be deleted, please see the log.") : ""));
        }

    }


}
