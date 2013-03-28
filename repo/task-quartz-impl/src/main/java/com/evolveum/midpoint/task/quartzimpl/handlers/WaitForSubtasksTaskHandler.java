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

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;

/**
 * @author Pavol Mederly
 *
 */
public class WaitForSubtasksTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(WaitForSubtasksTaskHandler.class);
	public static final String HANDLER_URI = "http://midpoint.evolveum.com/repo/subtasks-handler-1";

	private static WaitForSubtasksTaskHandler instance = null;
	private TaskManagerQuartzImpl taskManagerImpl;

	private WaitForSubtasksTaskHandler() {}
	
	public static void instantiateAndRegister(TaskManager taskManager) {
		if (instance == null)
			instance = new WaitForSubtasksTaskHandler();
		taskManager.registerHandler(HANDLER_URI, instance);
		instance.taskManagerImpl = (TaskManagerQuartzImpl) taskManager;
	}

	@Override
	public TaskRunResult run(Task task) {

		OperationResult opResult = new OperationResult(WaitForSubtasksTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();

        LOGGER.info("WaitForSubtasksTaskHandler run starting; in task " + task.getName());

        List<PrismObject<TaskType>> subtasks = null;
        try {
            subtasks = task.listSubtasksRaw(opResult);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't list subtasks of " + task + " due to schema exception", e);
        }

        LOGGER.info("Number of subtasks found: " + subtasks.size() + "; task = {}", task);
        boolean allClosed = true;
        for (PrismObject<TaskType> t : subtasks) {
            if (t.asObjectable().getExecutionStatus() != TaskExecutionStatusType.CLOSED) {
                LOGGER.info("Subtask " + t.getOid() + "/" + t.asObjectable().getName() + " is not closed, it is " + t.asObjectable().getExecutionStatus() + ", for task {}", task);
                allClosed = false;
                break;
            }
        }

        TaskRunResultStatus status = TaskRunResultStatus.FINISHED;
        if (allClosed) {
            LOGGER.info("All subtasks are closed, finishing waiting for them; task = {}", task);
            try {
                ((TaskQuartzImpl) task).finishHandler(opResult);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Task handler cannot be finished because the task does not exist anymore", e);
                status = TaskRunResultStatus.PERMANENT_ERROR;
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Task handler cannot be finished due to schema exception", e);
                status = TaskRunResultStatus.PERMANENT_ERROR;
            }
        }

        runResult.setOperationResult(null);                             // not to overwrite task's result
        runResult.setProgress(task.getProgress());                      // not to overwrite task's progress
        runResult.setRunResultStatus(status);
		LOGGER.info("WaitForSubtasksTaskHandler run finishing; in task " + task.getName());
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
