/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.RunningLightweightTaskImpl;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

@Component
public class TaskInstantiator {

    private static final Trace LOGGER = TraceManager.getTrace(TaskInstantiator.class);

    @Autowired private TaskManagerQuartzImpl taskManager;

    public TaskQuartzImpl createTaskInstance(String operationName) {
        return TaskQuartzImpl.createNew(taskManager, operationName);
    }

    @NotNull
    public TaskQuartzImpl createTaskInstance(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(Task.DOT_INTERFACE + "createTaskInstance");
        result.addParam("taskPrism", taskPrism);

        TaskQuartzImpl task = TaskQuartzImpl.createFromPrismObject(taskManager, taskPrism);
        task.checkOwnerRefPresent();
        result.recordSuccessIfUnknown();
        return task;
    }

    /**
     * If necessary, converts a task into running task instance. Does not change the prism data.
     */
    public RunningTaskQuartzImpl toRunningTaskInstance(@NotNull Task task, @NotNull String rootTaskOid) {
        if (task instanceof RunningTask) {
            LOGGER.warn("Task {} is already a RunningTask", task);
            return (RunningTaskQuartzImpl) task;
        } else {
            PrismObject<TaskType> taskPrismObject = task.getUpdatedTaskObject();
            return new RunningTaskQuartzImpl(taskManager, taskPrismObject, rootTaskOid);
        }
    }

    /**
     * If necessary, converts a task into running task instance. Does not change the prism data.
     */
    public RunningLightweightTaskImpl toRunningLightweightTaskInstance(@NotNull Task task, @NotNull String rootTaskOid,
            @NotNull RunningTaskQuartzImpl parent, @NotNull LightweightTaskHandler handler) {
        argCheck(!(task instanceof RunningTask), "Task is already a running task: %s", task);
        PrismObject<TaskType> taskPrismObject = task.getUpdatedTaskObject();

        return new RunningLightweightTaskImpl(taskManager, taskPrismObject, rootTaskOid, parent, handler);
    }
}
