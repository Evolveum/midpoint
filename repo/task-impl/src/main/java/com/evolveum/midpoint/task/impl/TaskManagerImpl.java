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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

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
public class TaskManagerImpl implements TaskManager, BeanFactoryAware {
	
	private static final String THREAD_NAME = "midpoint-task-scanner";
	private long JOIN_TIMEOUT = 5000;
	
	private Map<String,TaskHandler> handlers = new HashMap<String, TaskHandler>();
	private Set<TaskRunner> runners = new HashSet<TaskRunner>();
	private TaskScanner scannerThread;
	/**
	 * True if the service threads are running.
	 * Is is true in a normal case. It is false is the threads were temporarily suspended.
	 */
	private boolean threadsRunning = true;

	private BeanFactory beanFactory;	
	
	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	private static final transient Trace logger = TraceManager.getTrace(TaskManagerImpl.class);
	private static final String TASK_THREAD_NAME_PREFIX = "midpoint-task-";
	
	private static long threadCounter = 0;
	
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		 this.beanFactory = beanFactory;
	}
	
	@PostConstruct
	public void init() {
		logger.info("Task Manager initialization");
		startScannerThread();
	}
	
	@PreDestroy
	public void shutdown() {
		logger.info("Task Manager shutdown");
		stopScannerThread();
		for (TaskRunner runner : runners) {
			runner.shutdown();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance()
	 */
	@Override
	public Task createTaskInstance() {
		return new TaskImpl(this);
	}
	
	@Override
	public Task createTaskInstance(TaskType taskType) {
		//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
		RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
		return new TaskImpl(this,taskType,repoService);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#getTask(java.lang.String)
	 */
	@Override
	public Task getTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".getTask");
		result.addParam(OperationResult.PARAM_OID, taskOid);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerImpl.class);
		
		return fetchTaskFromRepository(taskOid, result);
	}

	private Task fetchTaskFromRepository(String taskOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType object = repositoryService.getObject(taskOid, resolve, result);
		TaskType taskType = (TaskType) object;
		//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
		RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");	
		return new TaskImpl(this,taskType,repoService);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#claimTask(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	public void claimTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, ConcurrencyException, SchemaException {
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".claimTask");
		result.addParam(OperationResult.PARAM_OID, task.getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerImpl.class);
		
		try {
			repositoryService.claimTask(task.getOid(), result);
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
	@Override
	public void releaseTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".releaseTask");
		result.addParam(OperationResult.PARAM_OID, task.getOid());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerImpl.class);
		
		try {
			repositoryService.releaseTask(task.getOid(), result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Task not found", ex);
			throw ex;
		} catch (SchemaException ex) {
			result.recordPartialError("Schema error", ex);
			throw ex;
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#switchToBackground(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	public void switchToBackground(final Task task, OperationResult parentResult) {
		
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".switchToBackground");
		persist(task,result);
		
		// TODO: The task should be released and claimed again here - to let other nodes participate
		
		// No result is passed here ... as this is just a kind of async notification
		processRunnableTask(task);
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
		
		task.setPersistenceStatus(TaskPersistenceStatus.PERSISTENT);
		TaskType taskType = task.getTaskTypeObject();
		try {
			String oid = repositoryService.addObject(taskType, parentResult);
			task.setOid(oid);
		} catch (ObjectAlreadyExistsException ex) {
			// This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
			throw new IllegalStateException("Got ObjectAlreadyExistsException while not expecting it (task:"+task+")",ex);
		} catch (SchemaException ex) {
			// This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
			throw new IllegalStateException("Got SchemaException while not expecting it (task:"+task+")",ex);
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
		return handlers.get(uri);
	}
	
	private void startScannerThread() {
		if (scannerThread == null) {
			scannerThread = new TaskScanner();
			scannerThread.setName(THREAD_NAME);
			//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
			RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
			scannerThread.setRepositoryService(repoService);
			scannerThread.setTaskManagerImpl(this);
		}
		if (scannerThread.isAlive()) {
			logger.warn("Attempt to start task scanner thread that is already running");
		} else {
			scannerThread.start();
		}
	}

	private void stopScannerThread() {
		if (scannerThread == null) {
			logger.warn("Attempt to stop non-existing task scanner thread");
		} else {
			if (scannerThread.isAlive()) {
				scannerThread.disable();
				scannerThread.interrupt();
				try {
					scannerThread.join(JOIN_TIMEOUT);
				} catch (InterruptedException ex) {
					logger.warn("Wait to thread join in task manager was interrupted");
				}
			} else {
				logger.warn("Attempt to stop a task scanner thread that is not alive");
			}
		}
	}

	/**
	 * Process runnable task with TaskType XML object as an argument.
	 * 
	 * This is called by a task scanner or anyone that has a runnable task.
	 * 
	 * Precondition: claimed, runnable task
	 * As the task is claimed as it enters this methods, all we need is to execute it.
	 * 
	 * @param task XML TaskType object
	 */
	public void processRunnableTaskType(TaskType taskType) {
		//Note: we need to be Spring Bean Factory Aware, because some repo implementations are in scope prototype
		RepositoryService repoService = (RepositoryService) this.beanFactory.getBean("repositoryService");
		Task task = new TaskImpl(this,taskType,repoService);
		processRunnableTask(task);
	}

	
	public void processRunnableTask(Task task) {
		// We assume that all tasks are singles or cycles now.
		// TODO: support more task types

		TaskHandler handler = getHandler(task.getHandlerUri());
		
		if (handler==null) {
			logger.error("No handler for URI {}, task {}",task.getHandlerUri(),task);
			throw new IllegalStateException("No handler for URI "+task.getHandlerUri());
		}
		
		TaskRunner runner = null;
				
		if (task.isCycle()) {
			
			// CYCLE task
			runner = new CycleRunner(handler,task, this);
				
		} else if (task.isSingle()) {
			
			// SINGLE task
			
			runner = new SingleRunner(handler,task, this);
						
		} else {
			
			// Not supported yet
			logger.error("Tightly bound tasks (cycles) are the only supported reccuring tasks for now. Sorry.");
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
		releaseTask(task,parentResult);
		runners.remove(runner);
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

	@Override
	public String addTask(TaskType taskType, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {
		// TODO: result
		String oid = repositoryService.addObject(taskType, parentResult);
		// Wake up scanner thread. This may be a new runnable task
		scannerThread.scan();
		return oid;
	}

	@Override
	public void modifyTask(ObjectModificationType objectChange, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {
		// TODO: result
		repositoryService.modifyObject(objectChange, parentResult);
		// Wake up scanner thread. This may be runnable task now
		scannerThread.scan();
	}

	@Override
	public void deleteTask(String oid, OperationResult parentResult) throws ObjectNotFoundException {
		// TODO: result
		repositoryService.deleteObject(oid, parentResult);
	}

	@Override
	public void deactivateServiceThreads() {
		logger.warn("DEACTIVATING Task Manager service threads (RISK OF SYSTEM MALFUNCTION)");
		stopScannerThread();
		for (TaskRunner runner : runners) {
			runner.shutdown();
		}
		threadsRunning=false;
	}

	@Override
	public void reactivateServiceThreads() {
		logger.info("Reactivating Task Manager service threads");
		startScannerThread();
		// The scanner should find the runnable threads and reactivate runners
		threadsRunning=true;
	}

	@Override
	public boolean getServiceThreadsActivationState() {
		return threadsRunning;
	}

}
