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
package com.evolveum.midpoint.task.api;

import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;

/**
 * Task Manager - a component that controls (asynchronous) task execution.
 * 
 * The task manager can store the task for later execution, switch them to background
 * resume execution of a task from a different node, etc. Generally speaking, task
 * manager provides operation to manage tasks in the whole midPoint cluster of IDM nodes.
 * 
 * @author Radovan Semancik
 *
 */
public interface TaskManager {
	
	/**
	 * Creates new transient, running, claimed task instance.
	 * 
	 * This is fact creates usual "synchronous" task.
	 * 
	 * This is useful for normal day-to-day tasks that are either
	 * synchronous or start as a synchronous and are switched to
	 * asynchronous task later.
	 * 
	 * @return transient, running, claimed task instance
	 */
	// TODO: parameters
	public Task createTaskInstance();
	
	/**
	 * Returns a task with specified OID.
	 * 
	 * This operation will look up a task instance in the repository and return it in a form of Task object.
	 * 
	 * Works only on persistent tasks.
	 * 
	 * @param taskOid OID of the persistent task.
	 * @return Task instance
	 */
	public Task getTask(String taskOid);
	
	// TODO
	public void modifyTask(ObjectModificationType objectChange);
	
	/**
	 * Deletes a task from the repository.
	 * 
	 * @param taskOid OID of task to delete.
	 */
	public void deteleTask(String taskOid);
	
	/**
	 * Claim task exclusively for this node.
	 * 
	 * The operation will try to claim a task for this node. The operation will normally return, switching the
	 * task to a claimed state. Or it may throw exception if the task "claiming" failed.
	 * 
	 * Claiming will only work on released tasks. But even if this node considers task to be released, it might
	 * have been claimed by another node in the meantime. This operation guarantees atomicity. It will claim
	 * task only on a single node.
	 * 
	 * This method is in the TaskManager instead of Task, so the Task can
	 * stay free of RepositoryService dependency.
	 * 
	 * TODO: EXCEPTIONS
	 * 
	 * @param task task instance to claim
	 */
	public void claimTask(Task task);
	
	/**
	 * Release a claimed task.
	 * 
	 * The task is released for other nodes to work on it. The task state is saved to the repository
	 * before release, if necessary. If a transient task is provided as an argument, the task will be
	 * made persistent during this call. 
	 * 
	 * Release only works on claimed tasks.
	 * 
	 * This method is in the TaskManager instead of Task, so the Task can
	 * stay free of RepositoryService dependency.
	 * 
	 * @param task task instance to release
	 * @throws IllegalArgumentException attempt to release a task that is not claimed.
	 */
	public void releaseTask(Task task);
	
	/**
	 * Switches the provided task to background, making it asynchronous.
	 * 
	 * The provided task will be "released" to other nodes to execute. If the task execution state is "running" the
	 * method also tries to make sure that the task will be immediately execute (e.g. by allocating a thread). However,
	 * the nodes may compete for the tasks or there may be explicit limitations. Therefore there is no guarantee that
	 * the task will execute on the same node that called the switchToBackground() method.
	 * 
	 * @param task task to switch to background.
	 */
	public void switchToBackground(Task task);
	
	// TODO: signature
	public Set<Task> listTasks();
	
	// TODO: search
	/**
	 * Register a handler for a specified handler URI.
	 * 
	 */
	public void registerHandler(String uri, TaskHandler handler);
	
}
