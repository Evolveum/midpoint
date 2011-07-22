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

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;

/**
 * @author Radovan Semancik
 *
 */
public abstract class TaskRunner implements Runnable {
	
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
	
}
