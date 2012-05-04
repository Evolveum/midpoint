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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

/**
 * Task Manager implementation using Quartz scheduler.
 *
 * Main classes:
 *  - TaskManagerQuartzImpl
 *  - TaskQuartzImpl
 *
 * Helper classes:
 *  - GlobalExecutionManager: node-related functions (start, stop, query status), task-related functions (stop, query status)
 *    - LocalExecutionManager and RemoteExecutionManager (specific methods for local node and remote nodes)
 *
 *  - TaskManagerConfiguration: access to config gathered from various places (midpoint config, sql repo config, system properties)
 *  - ClusterManager: keeps cluster nodes synchronized and verifies cluster configuration sanity
 *  - JmxClient: used to invoke remote JMX agents
 *  - JmxServer: provides a JMX agent for midPoint
 *  - TaskSynchronizer: synchronizes information about tasks between midPoint repo and Quartz Job Store
 *  - Initializer: contains TaskManager initialization code (quite complex)
 *
 * TODO: check initialized/error-state flag on public methods
 *
 * @author Pavol Mederly
 *
 */
@Service(value = "taskManager")
@DependsOn(value="repositoryService")
public class TaskManagerQuartzImpl implements TaskManager, BeanFactoryAware {

    // instances of all the helper classes (see their definitions for their description)
    private TaskManagerConfiguration configuration = new TaskManagerConfiguration();
    private TaskSynchronizer taskSynchronizer = new TaskSynchronizer(this);
    private NodeRegistrar nodeRegistrar = new NodeRegistrar(this);
    private LocalExecutionManager localExecutionManager = new LocalExecutionManager(this);
    private RemoteNodesManager remoteNodesManager = new RemoteNodesManager(this);
    private GlobalExecutionManager globalExecutionManager = new GlobalExecutionManager(this);
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

    // whether initialization was successfully carried out (see description of init() method below)
    // if initialized = false, it is sure that no tasks will be executed on this node
    private boolean initialized = false;


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
            initialized = true;
        } catch (TaskManagerInitializationException e) {
            processInitializationException(e);
            initialized = false;
        }

        // if running in test mode, the postInit will not be executed... so we have to start scheduler here
        if (initialized && configuration.isReusableQuartzScheduler()) {
            postInit(result);
        }
    }

    @Override
    public void postInit(OperationResult result) {

        if (!initialized || getLocalNodeErrorStatus() != NodeErrorStatus.OK) {
            LOGGER.error("Task Manager is not initialized or is in error state, therefore we will NOT start the scheduler.");
            return;
        }

        try {
            localExecutionManager.startScheduler();
        } catch (TaskManagerException e) {
            processInitializationException(e);
        }
    }

    /**
     * Throws an exception or just returns, depending on "stopOnInitializationFailure" parameter.
     * In both cases, logs the exception and sets nodeErrorStatus.
     *
     * @see com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl#init()
     * @param e An exception that occurred during task manager initialization (or post initialization)
     */
    private void processInitializationException(TaskManagerException e) {
        LoggingUtils.logException(LOGGER, "Cannot initialize TaskManager due to the following exception: ", e);
        if (e instanceof TaskManagerConfigurationException) {       // TODO make this information part of TaskManagerException
            nodeErrorStatus = NodeErrorStatus.LOCAL_CONFIGURATION_ERROR;
        } else {
            nodeErrorStatus = NodeErrorStatus.LOCAL_INITIALIZATION_ERROR;
        }
        if (configuration.isStopOnInitializationFailure()) {
            throw new SystemException("Cannot initialize TaskManager", e);
        }
    }

    @PreDestroy
    public void shutdown() {

        OperationResult result = createOperationResult("shutdown");

        LOGGER.info("Task Manager shutdown starting");

        if (!initialized) {
            // deregistration of this node in repo is skipped, but that is not a problem (other nodes will see it is not alive)
            LOGGER.info("Task Manager shutdown skipped, because Task Manager was not initialized successfully on node startup.");
            return;
        }

        if (getQuartzScheduler() != null) {

            try {
                localExecutionManager.pauseScheduler();
            } catch (TaskManagerException e) {
                LoggingUtils.logException(LOGGER, "Cannot pause local Quartz scheduler, continuing with shutdown", e);
            }

            try {
                globalExecutionManager.stopAllTasksOnThisNodeAndWait(0L, result);
            } catch (TaskManagerException e) {
                LoggingUtils.logException(LOGGER, "Cannot stop locally running tasks, continuing with shutdown", e);
            }

            if (configuration.isReusableQuartzScheduler()) {
                LOGGER.info("Quartz scheduler will NOT be shutdown. It stays in paused mode.");
            } else {
                try {
                    localExecutionManager.shutdownScheduler();
                } catch (TaskManagerException e) {
                    LoggingUtils.logException(LOGGER, "Cannot shutdown Quartz scheduler, continuing with node shutdown", e);
                }
            }
        }

        nodeRegistrar.recordNodeShutdown(result);

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

    boolean isInErrorState() {
        return !initialized || nodeErrorStatus != NodeErrorStatus.OK;
    }

    boolean isInitialized() {
        return initialized;
    }

    private boolean isNotInitialized(OperationResult result) {
        if (!initialized) {
            result.recordFatalError("Task Manager is not initialized.");
            return true;
        } else {
            return false;
        }
    }

    void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Task Manager is not initialized");
        }
    }

    public NodeExecutionStatus getLocalNodeExecutionStatus() {
        return localExecutionManager.getLocalNodeExecutionStatus();
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

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".deactivateServiceThreads");
        result.addParam("timeToWait", timeToWait);

        if (isNotInitialized(result)) {
            return false;
        }

        LOGGER.info("Deactivating Task Manager service threads (waiting time = " + timeToWait + ")");
        clusterManager.stopClusterManagerThread(timeToWait, result);
        boolean retval = localExecutionManager.stopSchedulerAndTasksLocally(timeToWait, result);
        result.recordSuccessIfUnknown();
        return retval;
    }

    @Override
    public void reactivateServiceThreads(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".reactivateServiceThreads");

        if (!initialized) {
            result.recordFatalError("Local Task Manager is not initialized.");
        } else {
            LOGGER.info("Reactivating Task Manager service threads.");
            clusterManager.startClusterManagerThread();
            localExecutionManager.startSchedulerLocally(result);
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public boolean getServiceThreadsActivationState() {
        try {
            return getQuartzScheduler() != null && getQuartzScheduler().isStarted() &&
                    !getQuartzScheduler().isInStandbyMode() &&
                    !getQuartzScheduler().isShutdown() &&
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

    private<T> Set<T> oneItemSet(T item) {
        Set<T> set = new HashSet<T>();
        set.add(item);
        return set;
    }

    @Override
    public boolean suspendTasks(Collection<Task> tasks, long waitTime, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".suspendTasks");
        result.addParam("tasks", tasks);
        result.addParam("waitTime", waitTime);

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
                    result.recordPartialError(message, e);
                } catch (SchemaException e) {
                    String message = "Cannot suspend task because of schema exception; task = " + task;
                    LoggingUtils.logException(LOGGER, message, e);
                    result.recordPartialError(message, e);
                }
                try {
                    globalExecutionManager.unscheduleTask(task);
                } catch (TaskManagerException e) {
                    LoggingUtils.logException(LOGGER, "Cannot unschedule task {} while suspending it", e, task);
                    result.recordPartialError("Cannot unschedule task " + task + " while suspending it", e);
                    // but by setting the execution status to SUSPENDED we hope the task thread will exit on next iteration
                    // (does not apply to single-run tasks, of course)
                }
            }
        }

        boolean stopped = globalExecutionManager.stopTasksAndWait(tasks, waitTime, true, result);
        result.recordSuccessIfUnknown();
        return stopped;
    }

    @Override
    public void resumeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {

        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".resumeTask");
        result.addParam("task", task);

        if (task.getExecutionStatus() != TaskExecutionStatus.SUSPENDED) {
            String message = "Attempted to resume a task that is not in the SUSPENDED state (task = " + task + ", state = " + task.getExecutionStatus();
            LOGGER.error(message);
            result.recordFatalError(message);
            return;
        }

        try {
            ((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNABLE, parentResult);
        } catch (ObjectNotFoundException e) {
            String message = "A task cannot be resumed, because it does not exist; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message);
            throw e;
        } catch (SchemaException e) {
            String message = "A task cannot be resumed due to schema exception; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message);
            throw e;
        }

        try {
            // make the trigger as it should be
            taskSynchronizer.synchronizeTask((TaskQuartzImpl) task, result);
            result.recordSuccessIfUnknown();

        } catch (SchedulerException e) {
            String message = "Cannot resume the Quartz job corresponding to task " + task;
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
        }
    }

    /*
     *  ********************* WORKING WITH TASK INSTANCES *********************
     */

	@Override
	public Task createTaskInstance() {
		LightweightIdentifier taskIdentifier = generateTaskIdentifier();
		return new TaskQuartzImpl(this, taskIdentifier);
	}
	
	@Override
	public Task createTaskInstance(String operationName) {
		LightweightIdentifier taskIdentifier = generateTaskIdentifier();
		TaskQuartzImpl taskImpl = new TaskQuartzImpl(this, taskIdentifier);
		taskImpl.setResult(new OperationResult(operationName));
		return taskImpl;
	}
	
	private LightweightIdentifier generateTaskIdentifier() {
		return lightweightIdentifierGenerator.generate();
	}

	@Override
	public Task createTaskInstance(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".createTaskInstance");
        result.addParam("taskPrism", taskPrism);

		//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
		RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
		TaskQuartzImpl task = new TaskQuartzImpl(this, taskPrism, repoService);
		task.initialize(result);
        result.recordSuccessIfUnknown();
		return task;
	}

	@Override
	public Task createTaskInstance(PrismObject<TaskType> taskPrism, String operationName, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".createTaskInstance");
        result.addParam("taskPrism", taskPrism);

        TaskQuartzImpl taskImpl = (TaskQuartzImpl) createTaskInstance(taskPrism, result);
		if (taskImpl.getResult()==null) {
			taskImpl.setResultTransient(new OperationResult(operationName));
		}
        result.recordSuccessIfUnknown();
		return taskImpl;
	}

	@Override
	public Task getTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".getTask");
		result.addParam(OperationResult.PARAM_OID, taskOid);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);
		
		Task task;
		try {
			task = fetchTaskFromRepository(taskOid, result);
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

	private Task fetchTaskFromRepository(String taskOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
		
		PrismObject<TaskType> task = repositoryService.getObject(TaskType.class, taskOid, result);
		return createTaskInstance(task, result);
	}

	@Override
	public void switchToBackground(final Task task, OperationResult parentResult) {
		
		parentResult.recordStatus(OperationResultStatus.IN_PROGRESS, "Task switched to background");
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".switchToBackground");
		// Kind of hack. We want success to be persisted. In case that the persist fails, we will switch it back
		
		try {
			
			result.recordSuccess();
			persist(task, result);
			
		} catch (RuntimeException ex) {
			result.recordFatalError("Unexpected problem: "+ex.getMessage(),ex);
			throw ex;
		}
	}

	private void persist(Task task, OperationResult parentResult)  {
		if (task.getPersistenceStatus()==TaskPersistenceStatus.PERSISTENT) {
			// Task already persistent. Nothing to do.
			return;
		}

        TaskQuartzImpl taskImpl = (TaskQuartzImpl) task;
		
		if (taskImpl.getOid() != null) {
			// We don't support user-specified OIDs
			throw new IllegalArgumentException("Transient task must not have OID (task:"+task+")");
		}

        // hack: set Category if it is not set yet
        if (taskImpl.getCategory() == null) {
            taskImpl.setCategoryTransient(taskImpl.getCategoryFromHandler());
        }
		
		taskImpl.setPersistenceStatusTransient(TaskPersistenceStatus.PERSISTENT);

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
		Task task = createTaskInstance(taskPrism, parentResult);			// perhaps redundant, but it's more convenient to work with Task than with Task prism 
		return addTaskToRepositoryAndQuartz(task, parentResult);
	}

	private String addTaskToRepositoryAndQuartz(Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".addTaskToRepositoryAndQuartz");
        result.addParam("task", task);

		PrismObject<TaskType> taskPrism = task.getTaskPrismObject();		
		String oid = repositoryService.addObject(taskPrism, result);
		((TaskQuartzImpl) task).setOid(oid);
		
		((TaskQuartzImpl) task).synchronizeWithQuartz(result);

        result.recordSuccessIfUnknown();

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
        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".deleteTask");
        result.addParam("oid", oid);
        try {
            Task task = getTask(oid, result);
            if (task.getNode() != null) {
                result.recordWarning("Deleting a task that seems to be currently executing on node " + task.getNode());
            }
            repositoryService.deleteObject(TaskType.class, oid, result);
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
        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".getNexxtRunStartTime");
        result.addParam("oid", oid);

        Trigger t;
        try {
            t = getQuartzScheduler().getTrigger(TaskQuartzImplUtil.createTriggerKeyForTaskOid(oid));
            result.recordSuccess();
        } catch (SchedulerException e) {
            String message = "Cannot determine next run start time for task with OID " + oid;
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
            return null;
        }
        if (t == null) {
            return null;
        }
        Date next = t.getNextFireTime();
        return next == null ? null : next.getTime();
    }

    /*
     *  ********************* SEARCHING TASKS AND NODES *********************
     */

    /*
     * Gets nodes from repository and adds runtime information to them (taken from ClusterStatusInformation).
     */
    @Override
    public List<Node> searchNodes(QueryType query, PagingType paging, ClusterStatusInformation clusterStatusInformation, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".searchNodes");
        result.addParam("query", query);
        result.addParam("paging", paging);
        result.addParam("clusterStatusInformation", clusterStatusInformation);

        if (clusterStatusInformation == null) {
            clusterStatusInformation = getGlobalExecutionManager().getClusterStatusInformation(true, result);
        }

        List<PrismObject<NodeType>> nodesInRepository = repositoryService.searchObjects(NodeType.class, query, paging, result);

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
        result.recordSuccessIfUnknown();
        return retval;
    }

    @Override
    public int countNodes(QueryType query, OperationResult result) throws SchemaException {
        return repositoryService.countObjects(NodeType.class, query, result);
    }


    @Override
    public List<Task> searchTasks(QueryType query, PagingType paging, ClusterStatusInformation clusterStatusInformation, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".searchTasks");
        result.addParam("query", query);
        result.addParam("paging", paging);
        result.addParam("clusterStatusInformation", clusterStatusInformation);

        if (clusterStatusInformation == null) {
            clusterStatusInformation = getGlobalExecutionManager().getClusterStatusInformation(true, result);
        }

        List<PrismObject<TaskType>> tasksInRepository = repositoryService.searchObjects(TaskType.class, query, paging, result);

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
        result.recordSuccessIfUnknown();
        return retval;
    }


    @Override
    public int countTasks(QueryType query, OperationResult result) throws SchemaException {
        return repositoryService.countObjects(TaskType.class, query, result);
    }

    List<PrismObject<NodeType>> getAllNodes(OperationResult result) {
        try {
            return getRepositoryService().searchObjects(NodeType.class, QueryUtil.createAllObjectsQuery(), new PagingType(), result);
        } catch (SchemaException e) {       // should not occur
            throw new SystemException("Cannot get the list of nodes from the repository", e);
        }
    }

    PrismObject<NodeType> getNodeById(String nodeIdentifier, OperationResult result) throws ObjectNotFoundException {
        try {
            QueryType q = QueryUtil.createNameQuery(nodeIdentifier);        // TODO change to query-by-node-id
            List<PrismObject<NodeType>> nodes = repositoryService.searchObjects(NodeType.class, q, new PagingType(), result);
            if (nodes.isEmpty()) {
//                result.recordFatalError("A node with identifier " + nodeIdentifier + " does not exist.");
                throw new ObjectNotFoundException("A node with identifier " + nodeIdentifier + " does not exist.");
            } else if (nodes.size() > 1) {
                throw new SystemException("Multiple nodes with the same identifier '" + nodeIdentifier + "' in the repository.");
            } else {
                return nodes.get(0);
            }
        } catch (SchemaException e) {       // should not occur
            throw new SystemException("Cannot get the list of nodes from the repository", e);
        }
    }

    /*
    *  ********************* MANAGING HANDLERS AND TASK CATEGORIES *********************
    */

	@Override
	public void registerHandler(String uri, TaskHandler handler) {
        LOGGER.trace("Registering task handler for URI " + uri);
		handlers.put(uri, handler);
	}
	
	TaskHandler getHandler(String uri) {
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
                retval.add(h.getCategoryName(null));
            }
        }
        return retval;
    }

    /*
     *  ********************* TASK CREATION/REMOVAL LISTENERS *********************
     */

    @Override
    public void onTaskCreate(String oid, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".onTaskCreate");
        result.addParam("oid", oid);

        LOGGER.trace("onTaskCreate called for oid = " + oid);

        Task task;
        try {
            task = getTask(oid, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Quartz shadow job cannot be created, because task in repository was not found; oid = {}", e, oid);
            return;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Quartz shadow job cannot be created, because task from repository could not be retrieved; oid = {}", e, oid);
            return;
        }

        ((TaskQuartzImpl) task).synchronizeWithQuartz(result);
        result.recordSuccessIfUnknown();
    }

    @Override
    public void onTaskDelete(String oid, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(TaskManagerQuartzImpl.class.getName() + ".onTaskCreate");
        result.addParam("oid", oid);

        LOGGER.trace("onTaskDelete called for oid = " + oid);

        JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTaskOid(oid);

        try {
            if (getQuartzScheduler().checkExists(jobKey)) {
                getQuartzScheduler().deleteJob(jobKey);			// removes triggers as well
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

    Scheduler getQuartzScheduler() {
        return globalExecutionManager.getQuartzScheduler();
    }

    public PrismObject<NodeType> getNodePrism() {
        return nodeRegistrar.getNodePrism();
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

    public NodeRegistrar getNodeRegistrar() {
        return nodeRegistrar;
    }

    public void setConfiguration(TaskManagerConfiguration configuration) {
        this.configuration = configuration;
    }

    public LocalExecutionManager getLocalExecutionManager() {
        return localExecutionManager;
    }

    public RemoteNodesManager getRemoteNodesManager() {
        return remoteNodesManager;
    }

    public GlobalExecutionManager getGlobalExecutionManager() {
        return globalExecutionManager;
    }


    /*
     *  ********************* DELEGATIONS *********************
     */

    void synchronizeTaskWithQuartz(TaskQuartzImpl task, OperationResult parentResult) throws SchedulerException {
        taskSynchronizer.synchronizeTask(task, parentResult);
    }

    @Override
    public void synchronizeTasks(OperationResult result) {
        taskSynchronizer.synchronizeJobStores(result);
    }

    @Override
    public String getNodeId() {
        return nodeRegistrar.getNodeId();
    }

    @Override
    @Deprecated
    public Set<Task> getRunningTasks() throws TaskManagerException {
        return localExecutionManager.getLocallyRunningTasks(createOperationResult("getRunningTasks"));
    }

    @Override
    public void stopScheduler(String nodeIdentifier, OperationResult parentResult) {
        globalExecutionManager.stopScheduler(nodeIdentifier, parentResult);
    }

    @Override
    public void startScheduler(String nodeIdentifier, OperationResult parentResult) {
        globalExecutionManager.startScheduler(nodeIdentifier, parentResult);
    }

//    @Override
//    public boolean stopSchedulerAndTasks(String nodeIdentifier, long timeToWait) {
//        return globalExecutionManager.stopSchedulerAndTasks(nodeIdentifier, timeToWait);
//    }

    @Override
    public boolean stopSchedulersAndTasks(List<String> nodeList, long timeToWait, OperationResult result) {
        return globalExecutionManager.stopSchedulersAndTasks(nodeList, timeToWait, result);
    }

    @Override
    public boolean isTaskThreadActiveLocally(String oid) {
        return localExecutionManager.isTaskThreadActiveLocally(oid);
    }

//    @Override
//    public boolean isTaskThreadActiveClusterwide(String oid, OperationResult parentResult) {
//        return globalExecutionManager.isTaskThreadActiveClusterwide(oid);
//    }

    private long lastRunningTasksClusterwideQuery = 0;
    private ClusterStatusInformation lastClusterStatusInformation = null;

    @Override
    public ClusterStatusInformation getRunningTasksClusterwide(OperationResult parentResult) {
        lastClusterStatusInformation = globalExecutionManager.getClusterStatusInformation(true, parentResult);
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
        return nodeRegistrar.isCurrentNode(node);
    }

    @Override
    public void deleteNode(String nodeIdentifier, OperationResult result) {
        nodeRegistrar.deleteNode(nodeIdentifier, result);
    }
}
