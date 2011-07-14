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
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * @author Radovan Semancik
 * 
 */
public class TaskScanner extends Thread {

	private RepositoryService repositoryService;
	private TaskManagerImpl taskManagerImpl;
	private int sleepInterval = 5000;
	private boolean enabled = true;
	private long lastLoopRun = 0;

	private static final transient Trace logger = TraceManager.getTrace(TaskScanner.class);

	/**
	 * @return the repositoryService
	 */
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	/**
	 * @param repositoryService
	 *            the repositoryService to set
	 */
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	/**
	 * @return the taskManagerImpl
	 */
	public TaskManagerImpl getTaskManagerImpl() {
		return taskManagerImpl;
	}

	/**
	 * @param taskManagerImpl
	 *            the taskManagerImpl to set
	 */
	public void setTaskManagerImpl(TaskManagerImpl taskManagerImpl) {
		this.taskManagerImpl = taskManagerImpl;
	}

	@Override
	public void run() {
		try {
			logger.info("Task scanner starting (enabled:{})", enabled);
			while (enabled) {
				logger.trace("Task scanner loop: start");
				lastLoopRun = System.currentTimeMillis();

				OperationResult loopResult = new OperationResult(TaskScanner.class.getName() + ".run");
				PagingType paging = new PagingType();
				QueryType query = createQuery();
				ObjectListType tasks = null;
				try {
					tasks = repositoryService.searchObjects(query, paging, loopResult);
				} catch (SchemaException e) {
					logger.error("Task scanner cannot search for tasks", e);
					// TODO: better error handling
				}

				if (tasks != null) {
					logger.trace("Task scanner found {} runnable tasks", tasks.getObject().size());
					List<ObjectType> objectList = tasks.getObject();
					for (Object o : objectList) {
						if (o instanceof TaskType) {
							TaskType task = (TaskType) o;
							logger.trace("Task scanner: Start processing task " + task.getName() + " (OID: " + task.getOid()
									+ ")");

							if (canHandle(task)) {
								if (ScheduleEvaluator.shouldRun(task)) {
									long startTime = System.currentTimeMillis();

									boolean claimed = false;
									try {
										repositoryService.claimTask(task.getOid(), loopResult);
										claimed = true;
									} catch (ConcurrencyException ex) {
										// Claim failed. This means that the
										// task
										// was claimed by another
										// host in the meantime. We don't really
										// need to care. It will
										// get executed by the host that
										// succeeded
										// in claiming the
										// task.
										// Just log warning for now. This can be
										// switched to DEBUG later.
										logger.warn(
												"Task scanner: Claiming of task {} failed due to concurrency exception \"{}\", skipping it.",
												DebugUtil.prettyPrint(task), ex.getMessage());

									}

									if (claimed) {

										try {

											logger.debug("Task scanner is passing task to task manager:  "
													+ DebugUtil.prettyPrint(task));

											taskManagerImpl.processRunnableTaskType(task);

											// TODO: Remember the start time
											// only if
											// the
											// call is successful

											// We do not release the task here.
											// Task
											// manager should do it.
											// We don't know the state of the
											// task.
											// The task manage may have
											// allocated the thread for the task
											// and
											// the task may be still running
											// releasing it now may be an error.

										} catch (RuntimeException ex) {
											// Runtime exceptions are used from
											// time
											// to time, although all the
											// exceptions that could be
											// reasonably
											// caught should be transformed to
											// Faults, obvious not all of them
											// are.
											// Do not cause this thread to die
											// because of bug in the synchronize
											// method.

											// TODO: Better error reporting
											logger.error(
													"Task scanner got runtime exception (processRunnableTaskType): {} : {}",
													new Object[] { ex.getClass().getSimpleName(), ex.getMessage(), ex });
										}
									} // claimed
								} else {
									logger.trace("Task scanner: skipping task " + DebugUtil.prettyPrint(task)
											+ " because it should not run yet");
								}
							} else {
								logger.trace("Task scanner: skipping task " + DebugUtil.prettyPrint(task)
										+ " because there is no handler for it on this node");
							}

							logger.trace("Task scanner: End processing task " + DebugUtil.prettyPrint(task));
						} else {
							logger.error("Task scanner got unexpected object type in listObjects: " + o.getClass().getName());
							// skip it
						}
					}
				}

				if (lastLoopRun + sleepInterval > System.currentTimeMillis()) {

					// Let's sleep a while to slow down the synch, to avoid
					// overloading the system with sync polling

					logger.trace("Synchronization thread loop: going to sleep");

					try {
						Thread.sleep(sleepInterval - (System.currentTimeMillis() - lastLoopRun));
					} catch (InterruptedException ex) {
						logger.trace("Task scanner got InterruptedException: " + ex);
						// Safe to ignore
					}
				}
				logger.trace("Task scanner loop: end");
			}
			logger.info("Task scanner stopping");
		} catch (Throwable t) {
			logger.error("Task scanner: Critical error: {}: {}", new Object[] { t, t.getMessage(), t });
		}
	}

	private boolean canHandle(TaskType task) {
		if (taskManagerImpl.getHandler(task.getHandlerUri()) != null) {
			return true;
		}
		return false;
	}

	// Look for runnable tasks that are not claimed
	private QueryType createQuery() {

		Document doc = DOMUtil.getDocument();

		Element executionStatusElement = doc.createElementNS(SchemaConstants.C_TASK_EXECUTION_STATUS.getNamespaceURI(),
				SchemaConstants.C_TASK_EXECUTION_STATUS.getLocalPart());
		executionStatusElement.setTextContent(TaskExecutionStatusType.RUNNING.value());
		Element exclusivityStatusElement = doc.createElementNS(SchemaConstants.C_TASK_EXECLUSIVITY_STATUS.getNamespaceURI(),
				SchemaConstants.C_TASK_EXECLUSIVITY_STATUS.getLocalPart());
		exclusivityStatusElement.setTextContent(TaskExclusivityStatusType.RELEASED.value());

		// We have all the data, we can construct the filter now
		Element filter = QueryUtil.createAndFilter(
				doc,
				// No path needed. The default is OK.
				QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.C_TASK_TYPE)),
				QueryUtil.createEqualFilter(doc, null, executionStatusElement),
				QueryUtil.createEqualFilter(doc, null, exclusivityStatusElement));

		QueryType query = new QueryType();
		query.setFilter(filter);
		return query;
	}

	public void disable() {
		enabled = false;
	}

	public void enable() {
		enabled = true;
	}
}
