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
package com.evolveum.midpoint.task.quartzimpl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskWaitingReasonType;
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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
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
    private static final String DOT_IMPL_CLASS = TaskManagerQuartzImpl.class.getName() + ".";;
    private static final String OPERATION_SUSPEND_TASKS = DOT_INTERFACE + "suspendTasks";
    private static final String OPERATION_DEACTIVATE_SERVICE_THREADS = DOT_INTERFACE + "deactivateServiceThreads";

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
        result.addParam("tasks", tasks);
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
        result.addParam("task", task);

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
        result.addParam("task", task);

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
        result.addParam("task", task);

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
        result.addParam("task", task);
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
			PrismObject<TaskType> taskPrism = repositoryService.getObject(TaskType.class, taskOid, result);
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
        result.addParam("task", task);

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
    @Deprecated			// specific task setters should be used instead
    public void modifyTask(String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {
        throw new UnsupportedOperationException("Generic modification of a task is not supported; please use specific setters instead. OID = " + oid);
//		repositoryService.modifyObject(TaskType.class, oid, modifications, parentResult);
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
            nodesInRepository = repositoryService.searchObjects(NodeType.class, query, result);
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
            tasksInRepository = repositoryService.searchObjects(TaskType.class, query, result);
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

        List<String> retval = new ArrayList<String>();
        for (TaskHandler h : handlers.values()) {
            List<String> cat = h.getCategoryNames();
            if (cat != null) {
                retval.addAll(cat);
            } else {
                String catName = h.getCategoryName(null);
                if (catName != null) {
                    retval.add(catName);
                }
            }
        }
        return retval;
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
    @Deprecated
    public Set<Task> getRunningTasks() throws TaskManagerException {
        return executionManager.getLocallyRunningTasks();
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

    public void closeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        try {
            ((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.CLOSED, parentResult);
        } finally {
            executionManager.removeTaskFromQuartz(task.getOid(), parentResult);
        }
    }

    // do not forget to kick dependent tasks when closing this one (currently only done in finishHandler)
    public void closeTaskWithoutSavingState(Task task, OperationResult parentResult) {
        ((TaskQuartzImpl) task).setExecutionStatus(TaskExecutionStatus.CLOSED);
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

        List<PrismObject<TaskType>> list = repositoryService.searchObjects(TaskType.class, query, result);
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

        List<PrismObject<TaskType>> prisms = repositoryService.searchObjects(TaskType.class, query, result);
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

}
