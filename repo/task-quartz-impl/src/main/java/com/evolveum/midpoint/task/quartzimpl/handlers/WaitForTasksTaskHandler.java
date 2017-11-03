/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.handlers;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.List;

/**
 * This is very simple task handler that causes the process to enter WAITING for OTHER_TASKS state.
 *
 * @author Pavol Mederly
 */
public class WaitForTasksTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(WaitForTasksTaskHandler.class);
	public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/wait-for-tasks/handler-3";

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
        result.recordInProgress();

        LOGGER.debug("WaitForTasksTaskHandler run starting; in task " + task.getName());
        try {
            // todo resolve this brutal hack
            taskManagerImpl.pauseTask(task, TaskWaitingReason.OTHER, result);
            task.startWaitingForTasksImmediate(result);
        } catch (SchemaException | ObjectNotFoundException e) {
            throw new SystemException("Couldn't mark task as waiting for prerequisite tasks", e);       // should not occur; will be handled by task runner
        }
		LOGGER.debug("WaitForTasksTaskHandler run finishing; in task " + task.getName());

        result.computeStatus();

        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(result);
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
