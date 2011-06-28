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
 * @author Radovan Semancik
 *
 */
public interface TaskManager {
	
	/**
	 * Creates new transient, running, claimed task instance.
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
	 * Works only on persistent tasks.
	 * 
	 * @param taskOid
	 * @return
	 */
	public Task getTask(String taskOid);
	
	public void modifyTask(ObjectModificationType objectChange);
	
	public void deteleTask(String taskOid);
	
	/**
	 * Claim task exclusively for this node.
	 * 
	 * This method is in the TaskManager instead of Task, so the Task can
	 * stay free of RepositoryService dependency.
	 * 
	 * @param task
	 */
	public void claimTask(Task task);
	
	/**
	 * Release a claimed task.
	 * 
	 * This method is in the TaskManager instead of Task, so the Task can
	 * stay free of RepositoryService dependency.
	 * 
	 * @param task
	 */
	public void releaseTask(Task task);
	
	public void switchToBackground(Task task);
	
	// TODO: signature
	public Set<Task> listTasks();
	
	// TODO: search
	
	public void registerHandler(String uri, TaskHandler handler);
	
}
