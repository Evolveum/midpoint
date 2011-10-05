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

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 *
 */
public abstract class TaskRunner implements Runnable {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(TaskRunner.class);
	
	protected TaskHandler handler;
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

	public void shutdown() {
		task.shutdown();
		// In case that the thread was sleeping ...
		thread.interrupt();
	}
	
	public void heartbeat(OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(TaskRunner.class.getName()+".heartbeat");
		result.addContext(OperationResult.CONTEXT_TASK, task);
		
		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNING) {
			LOGGER.warn("Attempt to heartbeat a task that is not running ("+task.getExecutionStatus()+") " + task);
		}
		
		long progress = handler.heartbeat(task);
		
		try {
			task.recordProgress(progress, result);
		} catch (ObjectNotFoundException e) {
			LOGGER.error("Error saving progress to task "+task+": Object not found: "+e.getMessage(),e);
		} catch (SchemaException e) {
			LOGGER.error("Error saving progress to task "+task+": Schema violation: "+e.getMessage(),e);
		}
	}

	public Task getTask() {
		return task;
	}
	
}
