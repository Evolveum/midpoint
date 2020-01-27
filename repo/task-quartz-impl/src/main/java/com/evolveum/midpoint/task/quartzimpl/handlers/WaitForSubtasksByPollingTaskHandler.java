/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.handlers;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.InternalTaskInterface;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author Pavol Mederly
 */
@Deprecated
public class WaitForSubtasksByPollingTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(WaitForSubtasksByPollingTaskHandler.class);
    public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/wait-for-subtasks-polling/handler-3";

    private static WaitForSubtasksByPollingTaskHandler instance = null;
    private TaskManagerQuartzImpl taskManagerImpl;

    private WaitForSubtasksByPollingTaskHandler() {}

    public static void instantiateAndRegister(TaskManagerQuartzImpl taskManager) {
        if (instance == null)
            instance = new WaitForSubtasksByPollingTaskHandler();
        taskManager.registerHandler(HANDLER_URI, instance);
        instance.taskManagerImpl = taskManager;
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {

        OperationResult opResult = new OperationResult(WaitForSubtasksByPollingTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();

        LOGGER.info("WaitForSubtasksByPollingTaskHandler run starting; in task " + task.getName());

        List<PrismObject<TaskType>> subtasks;
        try {
            subtasks = ((InternalTaskInterface) task).listPersistentSubtasksRaw(opResult);
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

        TaskRunResultStatus status;
        if (allClosed) {
            LOGGER.info("All subtasks are closed, finishing waiting for them; task = {}", task);
            status = TaskRunResultStatus.FINISHED_HANDLER;
        } else {
            status = TaskRunResultStatus.FINISHED;
        }

        runResult.setRunResultStatus(status);
        LOGGER.info("WaitForSubtasksByPollingTaskHandler run finishing; in task " + task.getName());
        return runResult;
    }

    @Override
    public Long heartbeat(Task task) {
        return null;        // not to overwrite progress information!
    }

    @Override
    public void refreshStatus(Task task) {
    }

    @Override
    public String getCategoryName(Task task) {
        return null;        // hopefully we will never need to derive category from this handler! (category is filled-in when persisting tasks)
    }
}
