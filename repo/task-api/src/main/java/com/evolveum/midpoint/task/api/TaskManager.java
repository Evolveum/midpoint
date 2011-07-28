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

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

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
	 * TODO:
	 * 
	 * Creates task instance from the XML task representation.
	 * 
	 * @param taskType
	 * @return
	 */
	public Task createTaskInstance(TaskType taskType);
	
	/**
	 * Returns a task with specified OID.
	 * 
	 * This operation will look up a task instance in the repository and return it in a form of Task object.
	 * 
	 * Works only on persistent tasks.
	 * 
	 * @param taskOid OID of the persistent task.
	 * @return Task instance
	 * @throws SchemaException error dealing with resource schema
	 * @throws ObjectNotFoundException wrong OID format, etc.
	 */
	public Task getTask(String taskOid,OperationResult parentResult) throws ObjectNotFoundException, SchemaException;
	
	/**
	 * Add new task.
	 * 
	 * The OID provided in the task may be empty. In that case the OID
	 * will be assigned by the implementation of this method and it will be
	 * provided as return value.
	 * 
	 * This operation should fail if such object already exists (if object with
	 * the provided OID already exists).
	 * 
	 * The operation may fail if provided OID is in an unusable format for the
	 * storage. Generating own OIDs and providing them to this method is not
	 * recommended for normal operation.
	 * 
	 * Should be atomic. Should not allow creation of two objects with the same
	 * OID (even if created in parallel).
	 * 
	 * The operation may fail if the object to be created does not conform to
	 * the underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * 
	 * @param object
	 *            object to create
	 * @param scripts
	 *            scripts to execute before/after the operation
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return OID assigned to the created object
	 * 
	 * @throws ObjectAlreadyExistsException
	 *             object with specified identifiers already exists, cannot add
	 * @throws SchemaException
	 *             error dealing with storage schema, e.g. schema violation
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 */
	public String addTask(TaskType taskType, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException;

	
	/**
	 * Modifies task using relative change description. Must fail if object with
	 * provided OID does not exists. Must fail if any of the described changes
	 * cannot be applied. Should be atomic.
	 * 
	 * If two or more modify operations are executed in parallel, the operations
	 * should be merged. In case that the operations are in conflict (e.g. one
	 * operation adding a value and the other removing the same value), the
	 * result is not deterministic.
	 * 
	 * The operation may fail if the modified object does not conform to the
	 * underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * 
	 * TODO: optimistic locking
	 * 
	 * @param objectChange
	 *            specification of object changes
	 * @param scripts
	 *            scripts that should be executed before of after operation
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws SchemaException
	 *             resulting object would violate the schema
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 */
	public void modifyTask(ObjectModificationType objectChange, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;

	/**
	 * Deletes task with provided OID. Must fail if object with specified OID
	 * does not exists. Should be atomic.
	 * 
	 * @param oid
	 *            OID of object to delete
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 */
	public void deleteTask(String oid, OperationResult parentResult) throws ObjectNotFoundException;

		
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
	 * @throws SchemaException 
	 * @throws ConcurrencyException 
	 * @throws ObjectNotFoundException 
	 */
	public void claimTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, ConcurrencyException, SchemaException;
	
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
	 * @throws ObjectNotFoundException 
	 * @throws SchemaException 
	 * @throws IllegalArgumentException attempt to release a task that is not claimed.
	 */
	public void releaseTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;
	
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
	public void switchToBackground(Task task, OperationResult parentResult);
	
	// TODO: signature
	public Set<Task> listTasks();
	
	// TODO: search
	/**
	 * Register a handler for a specified handler URI.
	 * 
	 */
	public void registerHandler(String uri, TaskHandler handler);
	
	/**
	 * Make sure all processes are stopped properly.
	 * Will block until all processes are shut down.
	 */
	public void shutdown();
	
	/**
	 * Returns tasks that currently run on this node.
	 * E.g. tasks that have allocated threads.
	 * 
	 * It should be a different view than looking for a claimed tasks in the repository, although normally it should
	 * return the same data. This should look at the real situation (e.g. threads) and should be used to troubleshoot
	 * task management problems.
	 * 
	 * @return
	 */
	public Set<Task> getRunningTasks();
	
}
