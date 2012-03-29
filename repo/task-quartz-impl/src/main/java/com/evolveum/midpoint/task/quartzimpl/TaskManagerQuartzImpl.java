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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.util.exception.ConcurrencyException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
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
	
	private Map<String,TaskHandler> handlers = new HashMap<String, TaskHandler>();
	private PrismObjectDefinition<TaskType> taskPrismDefinition;

	private BeanFactory beanFactory;	
	
	@Autowired(required=true)
	private RepositoryService repositoryService;

	@Autowired(required=true)
	private LightweightIdentifierGenerator lightweightIdentifierGenerator;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(TaskManagerQuartzImpl.class);
	private static final long WAIT_FOR_COMPLETION_INITIAL = 100;			// initial waiting time (for task or tasks to be finished); it is doubled at each step 
	private static final long WAIT_FOR_COMPLETION_MAX = 1600;				// max waiting time (in one step) for task(s) to be finished
	
	/*
	 * Whether to allow reusing quartz scheduler after task manager shutdown.
	 * 
	 * Concretely, if it is set to 'true', quartz scheduler will not be shut down, only paused.
	 * This allows for restarting it (scheduler cannot be started, if it was shut down: 
	 * http://quartz-scheduler.org/api/2.1.0/org/quartz/Scheduler.html#shutdown())
	 *
	 * By default, if run within TestNG (determined by seeing SUREFIRE_PRESENCE_PROPERTY set), we allow the reuse.
	 * If run within Tomcat, we do not, because pausing the scheduler does NOT stop the execution threads.
	 */
	private boolean reusableQuartzScheduler = false;
	
	private static final String SUREFIRE_PRESENCE_PROPERTY = "surefire.real.class.path";
	
	private static boolean jdbcJobStore = false;
	
	private static int START_DELAY_TIME = 1;								// delay time - how long to wait before starting the scheduler
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
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
		LOGGER.info("Task Manager initialization");
		NoOpTaskHandler.instantiateAndRegister(this);
		
		// hacks...
		JobExecutor.setTaskManagerQuartzImpl(this);
		
		StdSchedulerFactory sf = new StdSchedulerFactory();

		// TODO: take some of the properties from midpoint configuration 
		Properties qprops = new Properties();
		
		if (jdbcJobStore)
			qprops.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
		else
			qprops.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
		qprops.put("org.quartz.scheduler.instanceName", "midPointScheduler");
		qprops.put("org.quartz.scheduler.instanceId", "AUTO");
		qprops.put("org.quartz.scheduler.skipUpdateCheck", "true");
		qprops.put("org.quartz.threadPool.threadCount", "5");
		qprops.put("org.quartz.scheduler.idleWaitTime", "10000");

		Properties sp = System.getProperties();
		if (sp.containsKey(SUREFIRE_PRESENCE_PROPERTY)) {
			LOGGER.info("Determined to run in a test environment, setting reusableQuartzScheduler to 'true'.");
			reusableQuartzScheduler = true;
		}
		
		if (reusableQuartzScheduler) {
			LOGGER.info("ReusableQuartzScheduer is set: the task manager threads will NOT be stopped on shutdown. Also, scheduler threads will run as daemon ones.");
			qprops.put("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
			qprops.put("org.quartz.threadPool.makeThreadsDaemons", "true");
		}
		
		try {
			sf.initialize(qprops);
			quartzScheduler = sf.getScheduler();
			quartzScheduler.startDelayed(START_DELAY_TIME);
		} catch (SchedulerException e) {
			LoggingUtils.logException(LOGGER, "Cannot get or start Quartz scheduler", e);
			throw new SystemException("Cannot get or start Quartz scheduler", e);
		}
		
		if (!jdbcJobStore)
			importQuartzJobs();
		
		LOGGER.trace("Quartz scheduler initialized and started; it is " + quartzScheduler);
		
		LOGGER.info("Task Manager initialized");
	}
	
	
	// imports Quartz jobs, reading them from repository 
	private void importQuartzJobs() {
		
		OperationResult result = createOperationResult("importQuartzJobs");
		
		LOGGER.trace("Importing existing tasks into volatile Quartz job store.");
		
		PagingType paging = new PagingType();
		List<PrismObject<TaskType>> tasks = null;
		tasks = repositoryService.listObjects(TaskType.class, paging, result);

		if (tasks != null) {
			
			int errors = 0;
			for (PrismObject<TaskType> taskPrism : tasks) {
				TaskQuartzImpl task = null;
				try {
					task = (TaskQuartzImpl) createTaskInstance(taskPrism, result);
					task.addOrReplaceQuartzTask();
				} catch (SchemaException e) {
					LoggingUtils.logException(LOGGER, "Task Manager cannot create task instance from task stored in repository due to schema exception; OID = {}", e, taskPrism.getOid());
					errors++;
				} catch (Exception e) {		// FIXME: correct exception handling
					LoggingUtils.logException(LOGGER, "Cannot create quartz job for task {}", e, task);
					errors++;
				}
			}
		
			LOGGER.info("Import existing tasks into volatile Quartz job store finished: " + (tasks.size()-errors) + " task(s) successfully imported. Import of " + errors + " task(s) failed.");
		} else {
			LOGGER.info("No tasks to import.");
		}
	}

	@PreDestroy
	public void shutdown() {
		LOGGER.info("Task Manager shutdown");
		
		if (quartzScheduler != null) {
			
			LOGGER.info("Putting Quartz scheduler into standby mode");
			try {
				quartzScheduler.standby();
			} catch (SchedulerException e1) {
				LoggingUtils.logException(LOGGER, "Cannot put Quartz scheduler into standby mode", e1);
			}
			
			LOGGER.info("Interrupting all tasks");
			try {
				shutdownTasksAndWait(getRunningTasks(), 0);
			} catch(Exception e) {		// FIXME
				LoggingUtils.logException(LOGGER, "Cannot shutdown tasks", e);
			}

			if (reusableQuartzScheduler) {
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
		
		if (task.getOid()!=null) {
			// We don't support user-specified OIDs
			throw new IllegalArgumentException("Transient task must not have OID (task:"+task+")");
		}
		
		((TaskQuartzImpl) task).setPersistenceStatusTransient(TaskPersistenceStatus.PERSISTENT);

		// Make sure that the task has repository service instance, so it can fully work as "persistent"
		if (task instanceof TaskQuartzImpl) {
			TaskQuartzImpl taskImpl = (TaskQuartzImpl)task;
			if (taskImpl.getRepositoryService()==null) {
				RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
				taskImpl.setRepositoryService(repoService);
			}
		}

		try {
			addTaskToRepositoryAndQuartz(task, parentResult);
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
		
		((TaskQuartzImpl) task).addOrReplaceQuartzTask();
		
		return oid;
	}
	

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#registerHandler(java.lang.String, com.evolveum.midpoint.task.api.TaskHandler)
	 */
	@Override
	public void registerHandler(String uri, TaskHandler handler) {
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
			return shutdownTaskAndWait(task, waitTime);
			
		} catch (SchedulerException e1) {
			LoggingUtils.logException(LOGGER, "Cannot pause the Quartz job corresponding to task {}", e1, task);
			shutdownTaskAndWait(task, waitTime);			// if it is running...
			throw new SystemException("Cannot pause the Quartz job corresponding to task " + task, e1);
		}
		
	}
	
	private boolean shutdownTasksAndWait(Collection<Task> tasks, long waitTime) {

		if (tasks.isEmpty())
			return true;
		
		LOGGER.trace("Stopping tasks " + tasks + " (waiting " + waitTime + " msec)");
		
		for (Task task : tasks)
			signalShutdownToTask(task);
		
		return waitForTaskCompletion(tasks, waitTime);
	}
	
	private boolean shutdownTaskAndWait(Task task, long waitTime) {
		ArrayList<Task> list = new ArrayList<Task>(1);
		list.add(task);
		return shutdownTasksAndWait(list, waitTime);
	}

	
	private void signalShutdownToTask(Task task) {
		
		LOGGER.info("Signalling shutdown to task " + task + " (via quartz)");
		
		try {
			quartzScheduler.interrupt(TaskQuartzImplUtil.createJobKeyForTask(task));
		} catch (UnableToInterruptJobException e) {
			LoggingUtils.logException(LOGGER, "Unable to interrupt the task {}", e, task);			// however, we continue (to suspend the task)
		}
	}

	// returns true if tasks are down
	private boolean waitForTaskCompletion(Collection<Task> tasks, long maxWaitTime) {
		
		LOGGER.trace("Waiting for task(s) " + tasks + " to complete, at most for " + maxWaitTime + " ms.");
		
		Set<String> oids = new HashSet<String>();
		for (Task t : tasks)
			if (t.getOid() != null)
				oids.add(t.getOid());
		
		long singleWait = WAIT_FOR_COMPLETION_INITIAL;
		long started = System.currentTimeMillis();
		
		for(;;) {
			
			List<JobExecutionContext> jecs;
			try {
				jecs = quartzScheduler.getCurrentlyExecutingJobs();
			} catch (SchedulerException e1) {
				LoggingUtils.logException(LOGGER, "Cannot get the list of currently executing jobs. Finishing waiting for task(s) completion.", e1);
				return false;
			}

			boolean isAnythingExecuting = false;
			for (JobExecutionContext jec : jecs) {
				if (oids.contains(jec.getJobDetail().getKey().getName())) {
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
			
			LOGGER.trace("Tasks have not completed yet, waiting for " + singleWait + " ms (max: " + maxWaitTime + ")");
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
			
		((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNING, parentResult);
			
		JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTask(task);
		TriggerKey triggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(task);
		try {
			
			// if there is no trigger for this task, let us create one (there is no need to resume it in such a case)
			if (!quartzScheduler.checkExists(triggerKey))
				((TaskQuartzImpl) task).addOrReplaceQuartzTask();
			else
				quartzScheduler.resumeJob(jobKey);			// resumes existing trigger
			
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
	public void deactivateServiceThreads() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reactivateServiceThreads() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getServiceThreadsActivationState() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean isTaskThreadActive(String taskIdentifier) {
		// TODO Auto-generated method stub
		return false;
	}

    private OperationResult createOperationResult(String methodName) {
		return new OperationResult(TaskManagerQuartzImpl.class.getName() + "." + methodName);
	}

	Scheduler getQuartzScheduler() {
		return quartzScheduler;
	}

}
