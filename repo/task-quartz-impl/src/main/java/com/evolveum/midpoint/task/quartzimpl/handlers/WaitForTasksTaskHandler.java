/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * This is very simple task handler that causes the process to enter WAITING for OTHER_TASKS state.
 *
 * @author Pavol Mederly
 */
public class WaitForTasksTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(WaitForTasksTaskHandler.class);
    public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/wait-for-tasks/handler-3";

    private static WaitForTasksTaskHandler instance = null;
    private TaskManagerQuartzImpl taskManagerImpl;

    private WaitForTasksTaskHandler() {}

    public static void instantiateAndRegister(TaskManagerQuartzImpl taskManager) {
        if (instance == null) {
            instance = new WaitForTasksTaskHandler();
        }
        taskManager.registerHandler(HANDLER_URI, instance);
        instance.taskManagerImpl = taskManager;
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {

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
