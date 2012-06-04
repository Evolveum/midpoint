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
package com.evolveum.midpoint.task.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ConcurrencyException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.TaskType;

/**
 * Task Manager implementation.
 * 
 * VERY SIMPLISTIC. This needs to be updated later.
 * 
 * It assumes only a single host. No cluster, in fact not even a repository access.
 * So the tasks will not survive restarts.
 * 
 * @author Radovan Semancik
 *
 */
@Service(value = "taskManager")
@DependsOn(value="repositoryService")
public class TaskManagerImpl implements TaskManager, BeanFactoryAware {
	
	private static final String SCANNER_THREAD_NAME = "midpoint-task-scanner";
	private static final String HEARTBEAT_THREAD_NAME = "midpoint-heartbeat";
	private long JOIN_TIMEOUT = 5000;
	
	private Map<String,TaskHandler> handlers = new HashMap<String, TaskHandler>();
	private Set<TaskRunner> runners = new CopyOnWriteArraySet<TaskRunner>();
	private TaskScanner scannerThread;
	private HeartbeatThread heartbeatThread;
	private PrismObjectDefinition<TaskType> taskPrismDefinition;
	/**
	 * True if the service threads are running.
	 * Is is true in a normal case. It is false is the threads were temporarily suspended.
	 */
	private boolean threadsRunning = true;

	private BeanFactory beanFactory;	
	
	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	@Autowired(required=true)
	private LightweightIdentifierGenerator lightweightIdentifierGenerator;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(TaskManagerImpl.class);
	private static final String TASK_THREAD_NAME_PREFIX = "midpoint-task-";
	
	private static long threadCounter = 0;
	
	PrismContext getPrismContext() {
		return prismContext;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		 this.beanFactory = beanFactory;
	}
	
	@PostConstruct
	public void init() {
		LOGGER.info("Task Manager initialization");
		NoOpTaskHandler.instantiateAndRegister(this);
		releaseClaimedTasks();
		startInternalThreads();
		LOGGER.info("Task Manager initialized");
	}
	
	@PreDestroy
	public void shutdown() {
		LOGGER.info("Task Manager shutdown");
		stopInternalThreads();
		finishAllTasks();
		LOGGER.info("Task Manager shutdown finished");
	}

	private void finishAllTasks() {
		//we will wait for all tasks to finish correctly till we proceed with shutdown procedure
		LOGGER.info("Wait for Task Manager's tasks finish");
		for (TaskRunner runner : runners) {
			shutdownRunnerAndWait(runner, 0);
			removeRunner(runner);
		}
		LOGGER.info("All Task Manager's tasks finished");
	}
	
	/*
	 * Shutting down runner works like this:
	 * 
	 * TaskManagerImpl.shutdownRunnerAndWait:
	 *  -> runner.signalShutdown()
	 *     -> (log)
	 *     -> task.signalShutdown() [sets canRun:=false, expects that the task will finish (*)] + thread.interrupt()
	 *  -> runner.thead.join()
	 *  
	 *  (*) meanwhile, the concrete task handler will finish its processing (for longer-running handlers
	 *  it's their responsibility to watch canRun flag)
	 *  
	 *  SingleRunner.run, after getting control back from handler.run(task):
	 *   -> task.finishHandler (removes the handler from stack)
	 *   -> task.recordRunFinish (stores progress, last run finish time, operation result; on ObjectNotFound error it explicitly calls removeRunner)
	 *   -> taskManager.finishRunnableTask (releases task, removes the runner)
	 *   -> logRunFinish (logs the 'FINISH' event)
	 *  
	 *  CycleRunner.run, after getting control back from handler.run(task):
	 *   -> task.recordRunFinish
	 *   -> taskManager.finishRunnableTask (releases task, removes the runner)
	 *   -> logRunFinish (logs the 'FINISH' event)
	 *      
	 *   
	 */

	void shutdownRunnerAndWait(TaskRunner runner, long waitTime) {
		runner.signalShutdown();
		try {
			runner.thread.join(waitTime);			// note: removal from runners is done by finishRunnableTask
		} catch (InterruptedException e) {
			// Safe to ignore. 
			LOGGER.trace("TaskManager waiting for join task threads got InterruptedException: " + e);
		}
	}

//	void shutdownRunner(TaskRunner runner) {
//		shutdownRunner(runner, 0);
//	}
	
	void removeRunner(TaskRunner runner) {
		runners.remove(runner);
	}
	

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance()
	 */
	@Override
	public Task createTaskInstance() {
		LightweightIdentifier taskIdentifier = generateTaskIdentifier();
		return new TaskImpl(this, taskIdentifier);
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance(java.lang.String)
	 */
	@Override
	public Task createTaskInstance(String operationName) {
		LightweightIdentifier taskIdentifier = generateTaskIdentifier();
		TaskImpl taskImpl = new TaskImpl(this, taskIdentifier);
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
		TaskImpl task = new TaskImpl(this, taskPrism, repoService);
		task.initialize(parentResult);
		return task;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance(com.evolveum.midpoint.xml.ns._public.common.common_2.TaskType, java.lang.String)
	 */
	@Override
	public Task createTaskInstance(PrismObject<TaskType> taskPrism, String operationName, OperationResult parentResult) throws SchemaException {
		RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
		TaskImpl taskImpl = (TaskImpl)createTaskInstance(taskPrism, parentResult);
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
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerImpl.class);
		
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

		//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
		return createTaskInstance(task, result);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#claimTask(com.evolveum.midpoint.task.api.Task)
	 */
	public void claimTask(Task task0, OperationResult parentResult) throws ObjectNotFoundException, ConcurrencyException, SchemaException {
		TaskImpl task = (TaskImpl) task0;
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".claimTask");
		result.addParam(OperationResult.PARAM_OID, task.getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerImpl.class);
		
		try {
			
			task.refresh(parentResult);

			// Check whether the task is claimed
			
			if (task.getExclusivityStatus() != TaskExclusivityStatus.RELEASED) {
				// TODO: check whether the claim is not expired yet
				throw new ConcurrencyException("Attempt to claim already claimed task (OID:" + task.getOid() + ")");
			}

			// Modify the status to claim the task.
			// TODO: mark node identifier and claim expiration (later)
			
			task.setExclusivityStatus(TaskExclusivityStatus.CLAIMED);
			task.savePendingModifications(parentResult);

		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Task not found", ex);
			throw ex;
		} catch (ConcurrencyException ex) {
			result.recordPartialError("Concurrency problem while claiming task (race condition?)", ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordPartialError("Schema error", ex);
			throw ex;
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#releaseTask(com.evolveum.midpoint.task.api.Task)
	 */
	public void releaseTask(Task task0, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		TaskImpl task = (TaskImpl) task0;
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".releaseTask");
		result.addParam(OperationResult.PARAM_OID, task.getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerImpl.class);
		
		task.refresh(parentResult);
		task.setExclusivityStatus(TaskExclusivityStatus.RELEASED);
		task.savePendingModifications(parentResult);
	}
	
//	private void releaseTaskByOid(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
//		try {
//			repositoryService.releaseTask(oid, result);
//		} catch (ObjectNotFoundException ex) {
//			result.recordFatalError("Cannot release task, as it was not found", ex);
//			throw ex;
//		} catch (SchemaException ex) {
//			result.recordPartialError("Cannot release task due to schema error", ex);
//			throw ex;
//		}
//	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#switchToBackground(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	public void switchToBackground(final Task task0, OperationResult parentResult) {
		
		TaskImpl task = (TaskImpl) task0;
		
		parentResult.recordStatus(OperationResultStatus.IN_PROGRESS, "Task switched to background");
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".switchToBackground");
		// Kind of hack. We want success to be persisted. In case that the persist fails, we will switch it back
		
		try {
			
			result.recordSuccess();
			task.setExclusivityStatus(TaskExclusivityStatus.RELEASED);
			persist(task, result);
			
			scannerThread.scan();			// or perhaps not, to give other nodes a chance to get the task :)
			
		} catch (RuntimeException ex) {
			result.recordFatalError("Unexpected problem: "+ex.getMessage(),ex);
			throw ex;
		}
	}

	private void persist(Task task,OperationResult parentResult)  {
		if (task.getPersistenceStatus()==TaskPersistenceStatus.PERSISTENT) {
			// Task already persistent. Nothing to do.
			return;
		}
		
		if (task.getOid()!=null) {
			// We don't support user-specified OIDs
			throw new IllegalArgumentException("Transient task must not have OID (task:"+task+")");
		}
		
		((TaskImpl) task).setPersistenceStatusTransient(TaskPersistenceStatus.PERSISTENT);
		PrismObject<TaskType> taskPrism = task.getTaskPrismObject();
		try {
			String oid = repositoryService.addObject(taskPrism, parentResult);
			((TaskImpl) task).setOid(oid);
		} catch (ObjectAlreadyExistsException ex) {
			// This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
			throw new IllegalStateException("Got ObjectAlreadyExistsException while not expecting it (task:"+task+")",ex);
		} catch (SchemaException ex) {
			// This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
			throw new IllegalStateException("Got SchemaException while not expecting it (task:"+task+")",ex);
		}
		
		// Make sure that the task has repository service instance, so it can fully work as "persistent"
		if (task instanceof TaskImpl) {
			TaskImpl taskImpl = (TaskImpl)task;
			if (taskImpl.getRepositoryService()==null) {
				RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
				taskImpl.setRepositoryService(repoService);
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#listTasks()
	 */
	@Override
	public Set<Task> listTasks() {
		// TODO Auto-generated method stub
		return null;
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
	
	private void startInternalThreads() {
		// Scanner thread
		if (scannerThread == null) {
			scannerThread = new TaskScanner();
			scannerThread.setName(SCANNER_THREAD_NAME);
			//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
			RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
			scannerThread.setRepositoryService(repoService);
			scannerThread.setTaskManagerImpl(this);
		}
		if (scannerThread.isAlive()) {
			LOGGER.warn("Attempt to start task scanner thread that is already running");
		} else {
			scannerThread.start();
		}

		// Heartbeat thread
		if (heartbeatThread == null) {
			heartbeatThread = new HeartbeatThread(runners);
			heartbeatThread.setName(HEARTBEAT_THREAD_NAME);
		}
		if (heartbeatThread.isAlive()) {
			LOGGER.warn("Attempt to start heartbeat thread that is already running");
		} else {
			heartbeatThread.start();
		}
	}

	private void stopInternalThreads() {
		// Scanner thread
		if (scannerThread == null) {
			LOGGER.warn("Attempt to stop non-existing task scanner thread");
		} else {
			if (scannerThread.isAlive()) {
				scannerThread.disable();
				scannerThread.interrupt();
				try {
					scannerThread.join(JOIN_TIMEOUT);
				} catch (InterruptedException ex) {
					LOGGER.warn("Wait to thread join in task manager was interrupted");
				}
			} else {
				LOGGER.warn("Attempt to stop a task scanner thread that is not alive");
			}
			// Stopped thread cannot be started again. Therefore set it to null.
			// New thread will be created on the next start attempt.
			scannerThread = null;
		}

		// Heartbeat thread
		if (heartbeatThread == null) {
			LOGGER.warn("Attempt to stop non-existing heartbeat thread");
		} else {
			if (heartbeatThread.isAlive()) {
				heartbeatThread.disable();
				heartbeatThread.interrupt();
				try {
					heartbeatThread.join(JOIN_TIMEOUT);
				} catch (InterruptedException ex) {
					LOGGER.warn("Wait to thread join in heartbeat was interrupted");
				}
			} else {
				LOGGER.warn("Attempt to stop a heartbeat thread that is not alive");
			}
			// Stopped thread cannot be started again. Therefore set it to null.
			// New thread will be created on the next start attempt.
			heartbeatThread = null;
		}
	}

	/**
	 * Process runnable task with Task object as an argument.
	 * 
	 * This is called by a task scanner or anyone that has a runnable task.
	 * 
	 * Precondition: claimed, runnable task
	 * As the task is claimed as it enters this methods, all we need is to execute it.
	 * 
	 * @param task Task object 
	 */
//	public void processRunnableTaskType(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException {
//		Task task = createTaskInstance(taskPrism, parentResult);
//		processRunnableTask(task);
//	}

	
	public void processRunnableTask(Task task) {
		// We assume that all tasks are singles or cycles now.
		// TODO: support more task types

		TaskHandler handler = getHandler(task.getHandlerUri());
		
		if (handler==null) {
			LOGGER.error("No handler for URI {}, task {}",task.getHandlerUri(),task);
			throw new IllegalStateException("No handler for URI "+task.getHandlerUri());
		}
		
		TaskRunner runner = null;
				
		if (task.isCycle()) {
			
			if (((TaskImpl) task).getHandlersCount() > 1) {
				LOGGER.error("Recurrent tasks cannot have more than one task handler; task = {}", task);
				throw new IllegalStateException("Recurrent tasks cannot have more than one task handler; task = " + task);
			}
			
			// CYCLE task
			runner = new CycleRunner(handler,task, this);
				
		} else if (task.isSingle()) {
			
			// SINGLE task
			
			runner = new SingleRunner(handler,task, this);
						
		} else {
			
			// Not supported yet
			//LOGGER.error("Tightly bound tasks (cycles) are the only supported for reccuring tasks for now. Sorry.");
			LOGGER.error("Tasks must be either recurrent or single-run. This one is neither. Sorry.");
			// Ignore otherwise. Nothing else to do.
			
		}
		
		// TODO: thread pooling, etc.
		
		runners.add(runner);
		
		Thread taskThread = allocateThread(task, runner);
		runner.setThread(taskThread);
		taskThread.start();		
		
		// TODO: heartbeat, etc.

		
	}
	
	void finishRunnableTask(TaskRunner runner, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		
		// We have claimed the task before, therefore we need to release the task here.
		try {
			releaseTask(task, parentResult);
			task.refresh(parentResult);
		}
		finally {
			removeRunner(runner);
		}
	}
	
	private Thread allocateThread(Task task, Runnable target) {
		// TODO: thread pooling (later)
		Thread thread = new Thread(target);
		// TODO: set thread name, etc.
		thread.setName(TASK_THREAD_NAME_PREFIX + (++threadCounter));
		
		return thread;
	}

	@Override
	public Set<Task> getRunningTasks() {
		Set<Task> tasks = new HashSet<Task>();
		for (TaskRunner runner: runners) {
			tasks.add(runner.getTask());
		}
		return tasks;
	}

	private TaskRunner findRunner(String taskIdentifier) {
		
		if (taskIdentifier == null)
			return null;
		
		Set<Task> tasks = new HashSet<Task>();
		for (TaskRunner runner: runners) {
			if (taskIdentifier.equals(runner.getTask().getTaskIdentifier())) {
				return runner; 
			}
		}
		return null;
	}

	@Override
	public String addTask(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {
		// TODO: result
		String oid = repositoryService.addObject(taskPrism, parentResult);
		// Wake up scanner thread. This may be a new runnable task
		scannerThread.scan();
		return oid;
	}

	@Override
	@Deprecated			// specific task setters should be used instead
	public void modifyTask(String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {
		// TODO: result
		repositoryService.modifyObject(TaskType.class, oid, modifications, parentResult);
		// Wake up scanner thread. This may be runnable task now
		scannerThread.scan();
	}

	@Override
	@Deprecated
	public void deleteTask(String oid, OperationResult parentResult) throws ObjectNotFoundException {
		// TODO: result
		repositoryService.deleteObject(TaskType.class, oid, parentResult);
	}

	@Override
	public void deactivateServiceThreads() {
		LOGGER.warn("DEACTIVATING Task Manager service threads (RISK OF SYSTEM MALFUNCTION)");
		stopInternalThreads();
		for (TaskRunner runner : runners) {
			runner.signalShutdown();
		}
		threadsRunning=false;
	}

	@Override
	public void reactivateServiceThreads() {
		LOGGER.info("Reactivating Task Manager service threads");
		startInternalThreads();
		// The scanner should find the runnable threads and reactivate runners
		threadsRunning=true;
	}

	@Override
	public boolean getServiceThreadsActivationState() {
		return threadsRunning;
	}


	/**
	 * Releases all tasks that are errorneously claimed at the time of system startup.
	 * (Works only as long as there is only one node executing tasks ... but that's ok for now.) 
	 */
	private void releaseClaimedTasks() {
		
		OperationResult result = new OperationResult(TaskManagerImpl.class.getName() + ".releaseClaimedTasks");
		PagingType paging = new PagingType();
		QueryType query;
		List<PrismObject<TaskType>> tasks = null;
		try {
			query = createQueryForClaimedTasks();
			tasks = repositoryService.searchObjects(TaskType.class, query, paging, result);
		} catch (SchemaException e) {
			LOGGER.error("Task manager cannot search for tasks that were left claimed", e);
			return;
		}

		if (tasks != null && tasks.size() > 0) {
			LOGGER.info("Task manager found {} task(s) left in CLAIMED state, and is about to release them.", tasks.size());
			for (PrismObject<TaskType> taskPrism : tasks) {
				TaskType taskType = taskPrism.asObjectable();
				LOGGER.info("Releasing task " + taskType.getName() + " (OID: " + taskPrism.getOid() + ")");
				try {
					releaseTask(createTaskInstance(taskPrism, result), result);
				} catch (Exception e) {
					LOGGER.error("Task manager cannot release a task that was left claimed; OID = " + taskPrism.getOid(), e);
				}
			}
		} else
			LOGGER.info("Task manager found no tasks left in CLAIMED state.");
	}
	
	private QueryType createQueryForClaimedTasks() throws SchemaException { // Look for claimed tasks

		Document doc = DOMUtil.getDocument();

		Element exclusivityStatusElement = doc.createElementNS(SchemaConstants.C_TASK_EXECLUSIVITY_STATUS.getNamespaceURI(),
				SchemaConstants.C_TASK_EXECLUSIVITY_STATUS.getLocalPart());
		exclusivityStatusElement.setTextContent(TaskExclusivityStatusType.CLAIMED.value());

		// We have all the data, we can construct the filter now
		Element filter = QueryUtil.createEqualFilter(doc, null, exclusivityStatusElement);

		QueryType query = new QueryType();
		query.setFilter(filter);
		return query;
	}

	public Long determineNextRunStartTime(TaskType taskType) {
		return ScheduleEvaluator.determineNextRunStartTime(taskType);
	}

	/**
	 * Draft implementation.
	 * Works only for persistent tasks.
	 * Returns true if the runner is down, false if it is still running.
	 */
	@Override
	public boolean suspendTask(Task task, long waitTime, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {
		
		LOGGER.info("Suspending task " + task + " (waiting " + waitTime + " msec)");

		if (task.getOid() == null)
			throw new IllegalArgumentException("Only persistent tasks can be suspended (for now).");

		((TaskImpl) task).setExecutionStatus(TaskExecutionStatus.SUSPENDED);
		task.savePendingModifications(parentResult);
		
		TaskRunner runner = findRunner(task.getTaskIdentifier());
		LOGGER.trace("Suspending task " + task + ", runner = " + runner);
		if (runner != null) {
			shutdownRunnerAndWait(runner, waitTime);
			LOGGER.trace("Suspending task " + task + ", runner.thread = " + runner.thread + ", isAlive: " + runner.thread.isAlive());
		}
		
		boolean retval = runner == null || !runner.thread.isAlive();
		LOGGER.info("Suspending task " + task + ": done (is down = " + retval + ")");
		return retval;
	}

//	private void suspendTaskByOid(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
//		try {
//			Collection<? extends ItemDelta> modifications = PropertyDelta.createModificationReplacePropertyCollection(
//					SchemaConstants.C_TASK_EXECUTION_STATUS, 
//					prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class),
//					TaskExecutionStatusType.SUSPENDED.value());
//			repositoryService.modifyObject(TaskType.class, oid, modifications, result);
//		} catch (ObjectNotFoundException ex) {
//			result.recordFatalError("Cannot suspend task, as it was not found", ex);
//			throw ex;
//		} catch (SchemaException ex) {
//			result.recordPartialError("Cannot suspend task due to schema error", ex);
//			throw ex;
//		}
//	}

	@Override
	public void resumeTask(Task task, OperationResult result) throws ObjectNotFoundException,
			ConcurrencyException, SchemaException {
		LOGGER.info("Resuming task " + task);

		String oid = task.getOid(); 
		if (oid == null)
			throw new IllegalArgumentException("Only persistent tasks can be resumed (for now).");
		
		// TODO: check whether task is suspended
		// TODO: recompute next running time
				
		((TaskImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNING, result);
		
		// Wake up scanner thread, this task may need to be processed
		scannerThread.scan();
	}

//	private PropertyDelta createNextRunStartTimeModification(long time) {
//		PrismPropertyDefinition nextRunStartTimePropDef = getTaskObjectDefinition().findPropertyDefinition(TaskType.F_NEXT_RUN_START_TIME);
//		if (time != 0) {
//			GregorianCalendar cal = new GregorianCalendar();
//			cal.setTimeInMillis(time);
//			PropertyDelta delta = new PropertyDelta(TaskType.F_NEXT_RUN_START_TIME, nextRunStartTimePropDef);
//			delta.setValueToReplace(new PrismPropertyValue<Object>(cal));
//			return delta;
//			//return ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace, null, SchemaConstants.C_TASK_NEXT_RUN_START_TIME, cal);
//		} else {
//			PropertyDelta delta = new PropertyDelta(TaskType.F_NEXT_RUN_START_TIME, nextRunStartTimePropDef);
//			// replace delta with no value, this will clear the property
//			return delta;
//			//return ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.delete, null, SchemaConstants.C_TASK_NEXT_RUN_START_TIME);
//		}
//	}
	
//	public void recordNextRunStartTime(String oid, long time, OperationResult result) throws ObjectNotFoundException, SchemaException {
//		
//		// FIXME: if nextRunStartTime == 0 we should delete the corresponding element; however, this does not work as for now
//		// so we just exit here, leaving nextRunStartTime as it is
//		if (time == 0)
//			return;
//		
//		try {
//			Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>(1);
//			modifications.add(createNextRunStartTimeModification(time));
//			repositoryService.modifyObject(TaskType.class, oid, modifications, result);
//		} catch (ObjectNotFoundException ex) {
//			result.recordFatalError("Cannot record next run start time, as the task object was not found", ex);
//			throw ex;
//		} catch (SchemaException ex) {
//			result.recordPartialError("Cannot record next run start time due to schema error", ex);
//			throw ex;
//		}
//
//	}

	@Override
	public boolean isTaskThreadActive(String taskIdentifier) {
		TaskRunner runner = findRunner(taskIdentifier);
		return runner != null && runner.thread.isAlive();
	}

	PrismObjectDefinition<TaskType> getTaskObjectDefinition() {
		if (taskPrismDefinition == null) {
			taskPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
		}
		return taskPrismDefinition;
	}
	
}
