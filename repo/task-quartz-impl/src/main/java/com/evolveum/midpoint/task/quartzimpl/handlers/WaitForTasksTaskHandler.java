/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.task.quartzimpl.handlers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;

import java.util.List;

/**
 * This is very simple task handler that causes the process to enter WAITING for OTHER_TASKS state.
 *
 * @author Pavol Mederly
 */
public class WaitForTasksTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(WaitForTasksTaskHandler.class);
	public static final String HANDLER_URI = "http://midpoint.evolveum.com/repo/wait-for-tasks-handler-1";

	private static WaitForTasksTaskHandler instance = null;
	private TaskManagerQuartzImpl taskManagerImpl;

	private WaitForTasksTaskHandler() {}
	
	public static void instantiateAndRegister(TaskManager taskManager) {
		if (instance == null) {
			instance = new WaitForTasksTaskHandler();
        }
		taskManager.registerHandler(HANDLER_URI, instance);
		instance.taskManagerImpl = (TaskManagerQuartzImpl) taskManager;
	}

	@Override
	public TaskRunResult run(Task task) {

		OperationResult result = task.getResult().createSubresult(WaitForTasksTaskHandler.class.getName()+".run");

        LOGGER.info("WaitForTasksTaskHandler run starting; in task " + task.getName());
        try {
            // todo resolve this brutal hack
            taskManagerImpl.pauseTask(task, TaskWaitingReason.OTHER, result);
            task.startWaitingForTasksImmediate(result);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't mark task as waiting for prerequisite tasks", e);       // should not occur; will be handled by task runner
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Couldn't mark task as waiting for prerequisite tasks", e);       // should not occur; will be handled by task runner
        }
        LOGGER.info("WaitForSubtasksTaskHandler run finishing; in task " + task.getName());

        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(result);
        runResult.setProgress(task.getProgress());                      // not to overwrite task's progress
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		return null;		// not to overwrite progress information!
	}

	@Override
	public void refreshStatus(Task task) {
	}

    @Override
    public String getCategoryName(Task task) {
        return null;        // hopefully we will never need to derive category from this handler! (category is filled-in when persisting tasks)
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
