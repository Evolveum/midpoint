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

import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;

/**
 * @author Radovan Semancik
 *
 */
public class TaskManagerImpl implements TaskManager {
	
	private Map<String,TaskHandler> handlers;
	
	// Temporary HACK
	private Map<String,Task> tasks;

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#createTaskInstance()
	 */
	@Override
	public Task createTaskInstance() {
		return new TaskImpl();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#getTask(java.lang.String)
	 */
	@Override
	public Task getTask(String taskOid) {
		return tasks.get(taskOid);
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
	public void claimTask(Task task) {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskManager#releaseTask(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	public void releaseTask(Task task) {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
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

		tasks.put(task.getOid(), task);
		
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

}
