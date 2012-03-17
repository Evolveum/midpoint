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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 *
 */
public abstract class TaskRunner implements Runnable {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(TaskRunner.class);
	
	protected TaskHandler handler;			// TODO adapt to the situation when there can be more handlers (handler stack) 
	protected Task task;
	protected TaskManagerImpl taskManager;
	protected Thread thread;
	
	public TaskRunner(TaskHandler handler, Task task, TaskManagerImpl taskManager) {
		this.handler = handler;
		this.task = task;
		this.taskManager = taskManager;
	}
	
	void setThread(Thread thread) {
		this.thread = thread;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public abstract void run();
	
	protected void logRunStart() {
		LOGGER.info("Task run STARTING "+task+" ("+this.getClass().getSimpleName()+")");
	}

	protected void logRunFinish() {
		LOGGER.info("Task run FINISHED "+task+" ("+this.getClass().getSimpleName()+")");
	}

	public void signalShutdown() {
		LOGGER.info("Task SHUTDOWN requested for "+task+" ("+this.getClass().getSimpleName()+")");
		task.signalShutdown();
		// In case that the thread was sleeping ...
		thread.interrupt();
	}
	
	public void heartbeat(OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(TaskRunner.class.getName()+".heartbeat");
		result.addContext(OperationResult.CONTEXT_TASK, task);
		
		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNING) {
			LOGGER.warn("Attempt to heartbeat a task that is not running ("+task.getExecutionStatus()+") " + task);
		}
		
		Long progress = null;
		if (handler != null) {
			progress = handler.heartbeat(task);
		} else {
			LOGGER.warn("No handler for task (in heartbeat): {}",task);
		}
		
		
		if (progress != null) {
			try {
				
				task.setProgressImmediate(progress, result);
				
			} catch (ObjectNotFoundException e) {
				LOGGER.error("Error saving progress to task "+task+": Object not found: "+e.getMessage()+", the task will be stopped",e);
				// The task object in repo is gone. Therefore this task should not run any more.
				// Therefore commit sepukku
				taskManager.removeRunner(this);
			} catch (SchemaException e) {
				LOGGER.error("Error saving progress to task "+task+": Schema violation: "+e.getMessage(),e);
			}			
		}
	}

	public Task getTask() {
		return task;
	}
	
}
