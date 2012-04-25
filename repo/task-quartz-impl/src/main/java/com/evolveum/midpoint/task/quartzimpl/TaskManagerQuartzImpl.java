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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
//import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
//import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * Task Manager implementation using quartz scheduler.
 * 
 * @author Pavol Mederly
 *
 */
@Service(value = "taskManager")
@DependsOn(value="repositoryService")
public class TaskManagerQuartzImpl implements TaskManager, BeanFactoryAware {

    private TaskManagerConfiguration configuration;

    private Map<String,TaskHandler> handlers = new HashMap<String, TaskHandler>();
	private PrismObjectDefinition<TaskType> taskPrismDefinition;
    private PrismObject<NodeType> nodePrism;

	private BeanFactory beanFactory;

    @Autowired(required=true)
    MidpointConfiguration midpointConfiguration;

    @Autowired(required=true)
	private RepositoryService repositoryService;

	@Autowired(required=true)
	private LightweightIdentifierGenerator lightweightIdentifierGenerator;
	
	@Autowired(required=true)
	private PrismContext prismContext;

    @Autowired(required=true)
    private ClusterManager clusterManager;

    private TaskSynchronizer taskSynchronizer;

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskManagerQuartzImpl.class);
	private static final long WAIT_FOR_COMPLETION_INITIAL = 100;			// initial waiting time (for task or tasks to be finished); it is doubled at each step 
	private static final long WAIT_FOR_COMPLETION_MAX = 1600;				// max waiting time (in one step) for task(s) to be finished
    private static final long INTERRUPT_TASK_THREAD_AFTER = 5000;           // how long to wait before interrupting task thread (if UseThreadInterrupt = 'whenNecessary')
	
    private static final int MAX_LOCKING_PROBLEMS_ATTEMPTS = 3;
    private static final long LOCKING_PROBLEM_TIMEOUT = 500;

    // how long to wait after TaskManager shutdown, if using JDBC Job Store (in order to give the jdbc thread pool a chance
    // to close, before embedded H2 database server would be closed by the SQL repo shutdown procedure)
    private static final long WAIT_ON_SHUTDOWN = 2000;

    private boolean databaseIsEmbedded;

    public PrismContext getPrismContext() {
		return prismContext;
	}
	
	@SuppressWarnings("unused")
    public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		 this.beanFactory = beanFactory;
	}
	
	private Scheduler quartzScheduler;
	
	@PostConstruct
	public void init() {

        OperationResult result = createOperationResult("init");

        // get the configuration
        configuration = new TaskManagerConfiguration(midpointConfiguration);
		LOGGER.info("Task Manager initialization. Job Store: "
                + (isJdbcJobStore() ? "JDBC":"in-memory") + ", "
                + (isClustered() ? "":"NOT ") + "clustered. Threads: " + configuration.getThreads());
        configuration.validate();

        // register node (if in cluster)
        nodePrism = clusterManager.createNodePrism(configuration.getNodeId(), configuration.getJmxPort());
        if (isClustered()) {
            clusterManager.registerNode(nodePrism, result);
        }

        NoOpTaskHandler.instantiateAndRegister(this);
		JobExecutor.setTaskManagerQuartzImpl(this);             // quite a hack...

        // prepare Quartz scheduler properties

		Properties quartzProperties = new Properties();

        if (isJdbcJobStore()) {

            // quartz properties related to database connection will be taken from SQL repository
            SqlRepositoryFactory sqlRepositoryFactory;
            try {
                sqlRepositoryFactory = (SqlRepositoryFactory) beanFactory.getBean("sqlRepositoryFactory");
            } catch(NoSuchBeanDefinitionException e) {
                throw new SystemException("Cannot initialize quartz task manager, because sqlRepositoryFactory is not available.", e);
            }

            SqlRepositoryConfiguration sqlConfig = sqlRepositoryFactory.getSqlConfiguration();
            configuration.setJdbcJobStoreInformation(midpointConfiguration, sqlConfig);
            configuration.validateJdbcConfig();

            databaseIsEmbedded = sqlConfig.isEmbedded();

            quartzProperties.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
            quartzProperties.put("org.quartz.jobStore.driverDelegateClass", configuration.getJdbcDriverDelegateClass());
            quartzProperties.put("org.quartz.jobStore.dataSource", "myDS");

            try {
                createQuartzDbSchema();
            } catch (SQLException e) {
                throw new SystemException("Could not create Quartz database schema", e);
            }

            quartzProperties.put("org.quartz.dataSource.myDS.driver", configuration.getJdbcDriver());
            quartzProperties.put("org.quartz.dataSource.myDS.URL", configuration.getJdbcUrl());
            quartzProperties.put("org.quartz.dataSource.myDS.user", configuration.getJdbcUser());
            quartzProperties.put("org.quartz.dataSource.myDS.password", configuration.getJdbcPassword());

            quartzProperties.put("org.quartz.jobStore.isClustered", isClustered() ? "true" : "false");

        } else {
			quartzProperties.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        }
		quartzProperties.put("org.quartz.scheduler.instanceName", "midPointScheduler");

        quartzProperties.put("org.quartz.scheduler.instanceId", getNodeId());

		quartzProperties.put("org.quartz.scheduler.skipUpdateCheck", "true");
		quartzProperties.put("org.quartz.threadPool.threadCount", Integer.toString(configuration.getThreads()));
		quartzProperties.put("org.quartz.scheduler.idleWaitTime", "10000");
		
		quartzProperties.put("org.quartz.scheduler.jmx.export", "true");

		if (configuration.isReusableQuartzScheduler()) {
			LOGGER.info("ReusableQuartzScheduler is set: the task manager threads will NOT be stopped on shutdown. Also, scheduler threads will run as daemon ones.");
			quartzProperties.put("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
			quartzProperties.put("org.quartz.threadPool.makeThreadsDaemons", "true");
		}

        // initialize the scheduler (without starting it)
		try {
            LOGGER.trace("Quartz scheduler properties: {}", quartzProperties);
            StdSchedulerFactory sf = new StdSchedulerFactory();
            sf.initialize(quartzProperties);
			quartzScheduler = sf.getScheduler();
		} catch (SchedulerException e) {
			LoggingUtils.logException(LOGGER, "Cannot initialize the Quartz scheduler", e);
			throw new SystemException("Cannot initialize the Quartz scheduler", e);
		}

        taskSynchronizer = new TaskSynchronizer(this);

        // populate the scheduler with jobs (if RAM-based), or synchronize with midPoint repo
        if (taskSynchronizer.synchronizeJobStores(result) == false) {
            if (!isJdbcJobStore()) {
                LOGGER.error("Some or all tasks could not be imported from midPoint repository to Quartz job store. They will therefore not be executed.");
            } else {
                LOGGER.warn("Some or all tasks could not be synchronized between midPoint repository and Quartz job store. They may not function correctly.");
            }
        }

        LOGGER.trace("Quartz scheduler initialized (not yet started, however); it is " + quartzScheduler);

        // FIXME: brutal hack -- if running in test mode, the postInit will not be executed... so we have to run it here
        if (configuration.isReusableQuartzScheduler()) {
            postInit(result);
        }
		
		LOGGER.info("Task Manager initialized");
	}

    private boolean isJdbcJobStore() {
        return configuration.isJdbcJobStore();
    }

    public boolean isClustered() {
        return configuration.isClustered();
    }


    private void createQuartzDbSchema() throws SQLException {
        try {
            Class.forName(configuration.getJdbcDriver());
        } catch (ClassNotFoundException e) {
            throw new SystemException("Could not locate database driver class " + configuration.getJdbcDriver(), e);
        }
        Connection connection = DriverManager.getConnection(configuration.getJdbcUrl(), configuration.getJdbcUser(), configuration.getJdbcPassword());
        try {
            try {
                connection.prepareStatement("SELECT count(*) FROM qrtz_job_details").executeQuery().close();
            } catch (SQLException ignored) {
                try {
                    connection.prepareStatement(getResource(configuration.getSqlSchemaFile())).executeUpdate();
                } catch (IOException ex) {
                    throw new SystemException("Could not read Quartz database schema file: " + configuration.getSqlSchemaFile(), ex);
                }
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private String getResource(String name) throws IOException {
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            throw new SystemException("Cannot initialize Quartz task manager, because its DB schema (" + name + ") cannot be found.");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuffer sb = new StringBuffer();
        int i;
        while ((i = br.read()) != -1) {
            sb.append((char) i);
        }
        return sb.toString();
    }

    private boolean putQuartzIntoStandby() {
        LOGGER.info("Putting Quartz scheduler into standby mode");
        try {
            quartzScheduler.standby();
            return true;
        } catch (SchedulerException e1) {
            LoggingUtils.logException(LOGGER, "Cannot put Quartz scheduler into standby mode", e1);
            return false;
        }
    }

	@PreDestroy
	public void shutdown() {

        OperationResult result = createOperationResult("shutdown");

		LOGGER.info("Task Manager shutdown");
		
		if (quartzScheduler != null) {
			
            putQuartzIntoStandby();
            shutdownAllTasksAndWait(0, false);

			if (configuration.isReusableQuartzScheduler()) {
				LOGGER.info("Quartz scheduler will NOT be shutdown. It stays in paused mode.");
			} else {
				LOGGER.info("Shutting down Quartz scheduler");
				try {
					quartzScheduler.shutdown(true);
				} catch (SchedulerException e) {
					LoggingUtils.logException(LOGGER, "Cannot shutdown Quartz scheduler", e);
				}
			}
		}

        if (isClustered()) {
            clusterManager.unregisterNode(result);
        }

        if (isJdbcJobStore() && databaseIsEmbedded) {
            LOGGER.trace("Waiting {} ms to give Quartz thread pool a chance to shutdown.", WAIT_ON_SHUTDOWN);
            try {
                Thread.sleep(WAIT_ON_SHUTDOWN);
            } catch (InterruptedException e) {
                // safe to ignore
            }
        }
		LOGGER.info("Task Manager shutdown finished");
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance()
	 */
	@Override
	public Task createTaskInstance() {
		LightweightIdentifier taskIdentifier = generateTaskIdentifier();
		return new TaskQuartzImpl(this, taskIdentifier);
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance(java.lang.String)
	 */
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
		//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
		RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
		TaskQuartzImpl task = new TaskQuartzImpl(this, taskPrism, repoService);
		task.initialize(parentResult);
		return task;
	}

    /* (non-Javadoc)
      * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance(com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType, java.lang.String)
      */
	@Override
	public Task createTaskInstance(PrismObject<TaskType> taskPrism, String operationName, OperationResult parentResult) throws SchemaException {
		TaskQuartzImpl taskImpl = (TaskQuartzImpl) createTaskInstance(taskPrism, parentResult);
		if (taskImpl.getResult()==null) {
			taskImpl.setResultTransient(new OperationResult(operationName));
		}
		return taskImpl;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#getTask(java.lang.String)
	 */
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
		
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		PrismObject<TaskType> task = repositoryService.getObject(TaskType.class, taskOid, resolve, result);
		return createTaskInstance(task, result);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#switchToBackground(com.evolveum.midpoint.task.api.Task)
	 */
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
		
		PrismObject<TaskType> taskPrism = task.getTaskPrismObject();		
		String oid = repositoryService.addObject(taskPrism, parentResult);
		((TaskQuartzImpl) task).setOid(oid);
		
		((TaskQuartzImpl) task).synchronizeWithQuartz();

		return oid;
	}
	

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#registerHandler(java.lang.String, com.evolveum.midpoint.task.api.TaskHandler)
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
	public Set<Task> getRunningTasks() {
		
		OperationResult result = createOperationResult("getRunningTasks");		// temporary, until moved to interface 
		
		Set<Task> retval = new HashSet<Task>();
		
		List<JobExecutionContext> jecs;
		try {
			jecs = quartzScheduler.getCurrentlyExecutingJobs();
		} catch (SchedulerException e1) {
			LoggingUtils.logException(LOGGER, "Cannot get the list of currently executing jobs.", e1);
			throw new SystemException("Cannot get the list of currently executing jobs.", e1);				// or return empty list?
		}
		
		for (JobExecutionContext jec : jecs) {
			String oid = jec.getJobDetail().getKey().getName();
			try {
				retval.add(getTask(oid, result));
			} catch (ObjectNotFoundException e) {
				LoggingUtils.logException(LOGGER, "Cannot get the task with OID {} as it no longer exists", e, oid);
			} catch (SchemaException e) {
				LoggingUtils.logException(LOGGER, "Cannot get the task with OID {} due to schema problems", e, oid);			
			}
		}
		
		return retval;
	}


	@Override
	@Deprecated			// specific task setters should be used instead
	public void modifyTask(String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {
		throw new UnsupportedOperationException("Generic modification of a task is not supported; please use specific setters instead. OID = " + oid);
//		repositoryService.modifyObject(TaskType.class, oid, modifications, parentResult);
	}

	@Override
	@Deprecated
	public void deleteTask(String oid, OperationResult parentResult) throws ObjectNotFoundException {
		repositoryService.deleteObject(TaskType.class, oid, parentResult);
//		throw new UnsupportedOperationException("Explicit deletion of a task is not supported. OID = " + oid);
	}

	PrismObjectDefinition<TaskType> getTaskObjectDefinition() {
		if (taskPrismDefinition == null) {
			taskPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
		}
		return taskPrismDefinition;
	}


	@Override
	public boolean suspendTask(Task task, long waitTime, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {

		LOGGER.info("Suspending task " + task + " (waiting " + waitTime + " msec)");

		if (task.getOid() == null)
			throw new IllegalArgumentException("Only persistent tasks can be suspended (for now).");
		
		((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.SUSPENDED, parentResult);

		JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTask(task);
		
		try {
			quartzScheduler.pauseJob(jobKey);
			// Stop current task execution
			return shutdownTaskAndWait(task, waitTime, true);
			
		} catch (SchedulerException e1) {
			LoggingUtils.logException(LOGGER, "Cannot pause the Quartz job corresponding to task {}", e1, task);
			shutdownTaskAndWait(task, waitTime, true);			// if it is running...
			throw new SystemException("Cannot pause the Quartz job corresponding to task " + task, e1);
		}
		
	}

    private boolean shutdownAllTasksAndWait(long timeToWait, boolean clusterwide) {
        LOGGER.info("Interrupting all tasks");
        try {
            return shutdownTasksAndWait(getRunningTasks(), timeToWait, clusterwide);
        } catch(Exception e) {		// FIXME
            LoggingUtils.logException(LOGGER, "Cannot shutdown tasks", e);
            return false;
        }
    }


    private boolean shutdownTasksAndWait(Collection<Task> tasks, long waitTime, boolean clusterwide) {

		if (tasks.isEmpty())
			return true;
		
		LOGGER.trace("Stopping tasks " + tasks + " (waiting " + waitTime + " msec); clusterwide = " + clusterwide);
		
		for (Task task : tasks)
			signalShutdownToTask(task, clusterwide);
		
		return waitForTaskCompletion(tasks, waitTime, clusterwide);
	}
	
	private boolean shutdownTaskAndWait(Task task, long waitTime, boolean clusterwide) {
		ArrayList<Task> list = new ArrayList<Task>(1);
		list.add(task);
		return shutdownTasksAndWait(list, waitTime, clusterwide);
	}

    void signalShutdownToTaskLocally(String oid) {
        try {
            quartzScheduler.interrupt(TaskQuartzImplUtil.createJobKeyForTaskOid(oid));
        } catch (UnableToInterruptJobException e) {
            LoggingUtils.logException(LOGGER, "Unable to interrupt the task {}", e, oid);			// however, we continue (e.g. to suspend the task)
        }
    }

    /**
     * Calls Thread.interrupt() on a local thread hosting task with a given OID.
     */
    private void interruptLocalTaskThread(String oid) {

        LOGGER.trace("Trying to find and interrupt a local execution thread for task {} (if it exists).", oid);
        try {
            List<JobExecutionContext> jecs = quartzScheduler.getCurrentlyExecutingJobs();
            for (JobExecutionContext jec : jecs) {
                String oid1 = jec.getJobDetail().getKey().getName();
                if (oid.equals(oid1)) {
                    Job job = jec.getJobInstance();
                    if (job instanceof JobExecutor) {
                        JobExecutor jobExecutor = (JobExecutor) job;
                        jobExecutor.sendThreadInterrupt();
                    }
                    break;
                }
            }

        } catch (SchedulerException e1) {
            LoggingUtils.logException(LOGGER, "Cannot find the currently executing job for the task {}", e1, oid);
            // ...and ignore it.
        }
    }

    private void signalShutdownToTaskClusterwide(String oid) {

        LOGGER.trace("Interrupting remote task {} - first finding where it currently runs", oid);

        RunningTasksInfo info = clusterManager.getRunningTasks();

        RunningTasksInfo.NodeInfo nodeInfo = info.findNodeInfoForTask(oid);
        if (nodeInfo == null) {
            LOGGER.info("Asked to interrupt task {} but did not find it running at any node.", oid);
            return;
        }

        clusterManager.interruptTaskClusterwide(oid, nodeInfo);

    }

    private void signalShutdownToTask(Task task, boolean clusterwide) {
		
		LOGGER.info("Signalling shutdown to task " + task + " (via quartz); clusterwide = " + clusterwide);

        // if the task runs locally or !clusterwide
        if (isTaskThreadActive(task.getOid()) || !clusterwide || !isClustered()) {
            signalShutdownToTaskLocally(task.getOid());
        } else {
            signalShutdownToTaskClusterwide(task.getOid());
        }
	}



	// returns true if tasks are down
	private boolean waitForTaskCompletion(Collection<Task> tasks, long maxWaitTime, boolean clusterwide) {

        boolean interruptExecuted = false;

		LOGGER.trace("Waiting for task(s) " + tasks + " to complete, at most for " + maxWaitTime + " ms.");
		
		Set<String> oids = new HashSet<String>();
		for (Task t : tasks)
			if (t.getOid() != null)
				oids.add(t.getOid());
		
		long singleWait = WAIT_FOR_COMPLETION_INITIAL;
		long started = System.currentTimeMillis();
		
		for(;;) {

            boolean isAnythingExecuting = false;
            RunningTasksInfo rtinfo = clusterManager.getRunningTasks(clusterwide);
            for (String oid : oids) {
                if (rtinfo.findNodeInfoForTask(oid) != null) {
                    isAnythingExecuting = true;
                    break;
                }
            }

			if (!isAnythingExecuting) {
				LOGGER.trace("The task(s), for which we have been waiting for, have finished.");
				return true;
			}

			if (maxWaitTime > 0 && System.currentTimeMillis() - started >= maxWaitTime) {
				LOGGER.trace("Wait time has elapsed without (some of) tasks being stopped. Finishing waiting for task(s) completion.");
				return false;
			}

            if (getUseThreadInterrupt() == UseThreadInterrupt.WHEN_NECESSARY && !interruptExecuted &&
                    System.currentTimeMillis() - started >= INTERRUPT_TASK_THREAD_AFTER) {

                LOGGER.info("Some tasks have not completed yet, sending their threads the 'interrupt' signal (if running locally).");
                for (String oid : oids) {
                    interruptLocalTaskThread(oid);
                }
                interruptExecuted = true;
            }
			
			LOGGER.trace("Some tasks have not completed yet, waiting for " + singleWait + " ms (max: " + maxWaitTime + ")");
			try {
				Thread.sleep(singleWait);
			} catch (InterruptedException e) {
				LOGGER.trace("Waiting interrupted" + e);
			}
			
			if (singleWait < WAIT_FOR_COMPLETION_MAX)
				singleWait *= 2;
		}
	}


	@Override
	public void resumeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException,
			ConcurrencyException, SchemaException {

		if (task.getExecutionStatus() != TaskExecutionStatus.SUSPENDED) {
			LOGGER.warn("Attempting to resume a task that is not in a SUSPENDED state (task = " + task + ", state = " + task.getExecutionStatus());
			return;
		}
			
		((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNABLE, parentResult);
			
//		JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTask(task);
//		TriggerKey triggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(task);
		try {
			
			// make the trigger as it should be
            taskSynchronizer.synchronizeTask((TaskQuartzImpl) task);

		} catch (SchedulerException e) {
			LoggingUtils.logException(LOGGER, "Cannot resume the Quartz job corresponding to task {}", e, task);
			throw new SystemException("Cannot resume the Quartz job corresponding to task " + task, e);			
		}
	}

	@Override
	public Set<Task> listTasks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deactivateServiceThreads(long timeToWait) {
        boolean result1 = putQuartzIntoStandby();
        boolean result2 = shutdownAllTasksAndWait(timeToWait, false);
        return result1 && result2;
	}

	@Override
	public boolean reactivateServiceThreads() {
        try {
            quartzScheduler.start();
            return true;
        } catch (SchedulerException e) {
            LoggingUtils.logException(LOGGER, "Cannot (re)start Quartz scheduler.", e);
            return false;
        }
    }

	@Override
	public boolean getServiceThreadsActivationState() {
        try {
            return quartzScheduler.isStarted() && !quartzScheduler.isInStandbyMode() && !quartzScheduler.isShutdown();
        } catch (SchedulerException e) {
            LoggingUtils.logException(LOGGER, "Cannot determine the state of the Quartz scheduler", e);
            return false;
        }
    }


	@Override
	public boolean isTaskThreadActive(String oid) {
        try {
            for (JobExecutionContext jec : quartzScheduler.getCurrentlyExecutingJobs()) {
                if (oid.equals(jec.getJobDetail().getKey().getName())) {
                    return true;
                }
            }
        } catch (SchedulerException e) {
            LoggingUtils.logException(LOGGER, "Cannot get the list of currently executing jobs", e);
            return false;
        }
        return false;
	}

    @Override
    public boolean isTaskThreadActiveClusterwide(String oid) {

        RunningTasksInfo info = clusterManager.getRunningTasks();
        return info.findNodeInfoForTask(oid) != null;
    }


    private OperationResult createOperationResult(String methodName) {
		return new OperationResult(TaskManagerQuartzImpl.class.getName() + "." + methodName);
	}

	Scheduler getQuartzScheduler() {
		return quartzScheduler;
	}

	@Override
	public void onTaskCreate(String oid) {

		OperationResult result = createOperationResult("onTaskCreate");
		
		LOGGER.trace("onTaskCreate called for oid = " + oid);
		
		Task task;
		try {
			task = getTask(oid, result);
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logException(LOGGER, "Quartz shadow job cannot be created, because task in repository was be found; oid = {}", e, oid);
			return;
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Quartz shadow job cannot be created, because task from repository could not be retrieved; oid = {}", e, oid);
			return;
		}

		((TaskQuartzImpl) task).synchronizeWithQuartz();
	}

	@Override
	public void onTaskDelete(String oid) {

		LOGGER.trace("onTaskDelete called for oid = " + oid);
		
		JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTaskOid(oid);
		
		try {
			if (quartzScheduler.checkExists(jobKey)) {
				quartzScheduler.deleteJob(jobKey);			// removes triggers as well
			}
		} catch (SchedulerException e) {
			LoggingUtils.logException(LOGGER, "Quartz shadow job cannot be removed; oid = {}", e, oid);
		}
		
	}

    @Override
    public Long getNextRunStartTime(String oid) {
        Trigger t;
        try {
            t = quartzScheduler.getTrigger(TaskQuartzImplUtil.createTriggerKeyForTaskOid(oid));
        } catch (SchedulerException e) {
            LoggingUtils.logException(LOGGER, "Cannot determine next run start time for task with OID {}", e, oid);
            return null;
        }
        if (t == null) {
            return null;
        }
        Date next = t.getNextFireTime();
        return next == null ? null : next.getTime();
    }

    public PrismObject<NodeType> getNodePrism() {
        return nodePrism;
    }

    @Override
    public String getNodeId() {
        return nodePrism.asObjectable().getNodeIdentifier();
    }

    @Override
    public boolean isCurrentNode(PrismObject<NodeType> node) {
        return getNodeId().equals(node.asObjectable().getNodeIdentifier());
    }

    @Override
    public RunningTasksInfo getRunningTasksClusterwide() {
        return clusterManager.getRunningTasks();
    }

    /**
     * Modifies a task in repo, checks for locking-related exceptions.
     */
    void modifyTaskChecked(String oid, Collection<PropertyDelta<?>> modifications, OperationResult parentResult, TaskQuartzImpl task) throws ObjectNotFoundException, SchemaException {

        int attempt = 1;

        while (true) {
            try {
                repositoryService.modifyObject(TaskType.class, oid, modifications, parentResult);
                return;
            } catch (ObjectNotFoundException e) {
                throw e;
            } catch (SchemaException e) {
                throw e;
            } catch (SystemException e) {
                String message = e.getMessage();
                if (message != null && (message.contains("Deadlock detected") || message.contains("Row was updated or deleted by another transaction"))) {

                    LOGGER.info("A locking-related problem occurred when updating task with oid " + oid + ", retrying after "
                            + LOCKING_PROBLEM_TIMEOUT + " ms (this was attempt " + attempt + " of " + MAX_LOCKING_PROBLEMS_ATTEMPTS
                            + "; message = " + message);

                    if (attempt == MAX_LOCKING_PROBLEMS_ATTEMPTS) {
                        throw e;
                    }

                    try {
                        Thread.sleep(LOCKING_PROBLEM_TIMEOUT);
                    } catch (InterruptedException e1) {
                        // ignore this
                    }
                    if (task != null) {
                        task.refresh(parentResult);
                    }
                    attempt++;
                } else {
                    throw e;
                }
            }
        }
    }

    public void synchronizeTaskWithQuartz(TaskQuartzImpl task) throws SchedulerException {
        taskSynchronizer.synchronizeTask(task);
    }

    public UseThreadInterrupt getUseThreadInterrupt() {
        return configuration.getUseThreadInterrupt();
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

    public TaskManagerConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void postInit(OperationResult result) {
        try {
            LOGGER.info("Starting the Quartz scheduler");
            quartzScheduler.start();
        } catch (SchedulerException e) {
            LoggingUtils.logException(LOGGER, "Cannot start the Quartz scheduler", e);
            throw new SystemException("Cannot start the Quartz scheduler", e);
        }
    }
}
