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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskRecurrenceType;
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
public class TaskManagerImpl implements TaskManager {
	
	private static final String THREAD_NAME = "midpoint-task-scanner";
	private long JOIN_TIMEOUT = 5000;
	
	private Map<String,TaskHandler> handlers = new HashMap<String, TaskHandler>();

	private TaskScanner scannerThread;
	
	// Temporary HACK
	private Map<String,Task> claimedTasks = new HashMap<String, Task>();

	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	private static final transient Trace logger = TraceManager.getTrace(TaskManagerImpl.class);
	
	@PostConstruct
	public void init() {
		logger.info("Task Manager initialization");
		startThread();
	}
	
	@PreDestroy
	public void shutdown() {
		logger.info("Task Manager shutdown");
		stopThread();
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance()
	 */
	@Override
	public Task createTaskInstance() {
		return new TaskImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#getTask(java.lang.String)
	 */
	@Override
	public Task getTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(TaskManager.class.getName()+".getTask");
		result.addParam(OperationResult.PARAM_OID, taskOid);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerImpl.class);
		
		// Look through the claimed tasks first. This is fast and provides a live java instance.
		Task claimedTask = claimedTasks.get(taskOid);
		if (claimedTask != null) {
			return claimedTask;
		}
		
		// Otherwise we need to fetch the task from repository
		return fetchTaskFromRepository(taskOid, result);
	}

	private Task fetchTaskFromRepository(String taskOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType object = repositoryService.getObject(taskOid, resolve, result);
		TaskType taskType = (TaskType) object;
		return new TaskImpl(this,taskType,repositoryService);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#modifyTask(com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType)
	 */
	@Override
	public void modifyTask(ObjectModificationType objectChange) {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#deteleTask(java.lang.String)
	 */
	@Override
	public void deteleTask(String taskOid) {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
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
	public void switchToBackground(final Task task) {
		
		final TaskHandler handler = handlers.get(task.getHanderUri());

		// No thread pool here now. Just do the dumbest thing to execute a new
		// thread.
		
		Thread thread = new Thread(task.getName()) {
			@Override
			public void run() {
				handler.run(task);
			}
		};
		
		// formally switching to persistent, althouhg we are not going to persiste it now
		// this is needed so the task will be considered asynchronous
		
		task.setPersistenceStatus(TaskPersistenceStatus.PERSISTENT);
		task.setOid(""+new Random().nextInt());

		claimedTasks.put(task.getOid(), task);
		
		thread.start();

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
	
	private void startThread() {
		if (scannerThread == null) {
			scannerThread = new TaskScanner();
			scannerThread.setName(THREAD_NAME);
			scannerThread.setRepositoryService(repositoryService);
			scannerThread.setTaskManagerImpl(this);
		}
		if (scannerThread.isAlive()) {
			logger.warn("Attempt to start task scanner thread that is already running");
		} else {
			scannerThread.start();
		}
	}

	private void stopThread() {
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
		// We assume that all tasks are singles or cycles now.
		// TODO: support more task types
		
		Task task = new TaskImpl(this,taskType,repositoryService);
		TaskHandler handler = getHandler(task.getHanderUri());
		
		if (handler==null) {
			logger.error("No handler for URI "+task.getHanderUri()+" "+DebugUtil.prettyPrint(taskType));
			throw new IllegalStateException("No handler for URI "+task.getHanderUri());
		}
		
		Thread taskThread = null;
		
		if (taskType.getRecurrence() == TaskRecurrenceType.RECURRING) {
		
			if (taskType.getBinding() == null || taskType.getBinding() == TaskBindingType.TIGHT) {
				// This is CYCLE
				
				CycleRunner cycleRunner = new CycleRunner(handler,task, this);
				
				taskThread = allocateThread(task, cycleRunner);
				taskThread.start();
				
			} else {
				
				// Not supported yet
				logger.error("Tightly bound tasks (cycles) are the only supported reccuring tasks for now. Sorry.");
				// Ignore otherwise. Nothing else to do.
				
			}
			
			// TODO: heartbeat, etc.
		} else {
			
			// SINGLE task
			
			SingleRunner singleRunner = new SingleRunner(handler,task, this);
			
			taskThread = allocateThread(task, singleRunner);
			taskThread.start();
			
		}
		
	}
	
	void finishRunnableTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		
		if (task.isSingle()) {	
			// We need to release the task here.
			releaseTask(task,parentResult);
			
		} // We don't care about other types
		
	}
	
	private Thread allocateThread(Task task, Runnable target) {
		// TODO: thread pooling (later)
		return new Thread(target);
		// TODO: set thread name, etc.
	}

}
