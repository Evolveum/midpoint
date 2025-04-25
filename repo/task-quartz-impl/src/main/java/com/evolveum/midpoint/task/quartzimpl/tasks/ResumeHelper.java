/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import static com.evolveum.midpoint.task.quartzimpl.tasks.TaskStateManager.clearTaskOperationResult;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.exception.SystemException;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.quartz.TaskSynchronizer;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Helps with task resume.
 */
@Component
class ResumeHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ResumeHelper.class);

    @Autowired private TaskRetriever taskRetriever;
    @Autowired private PrismContext prismContext;
    @Autowired private TaskSynchronizer taskSynchronizer;

    public void resumeTaskTree(String rootTaskOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<String> oidsToResume = getOidsToResume(rootTaskOid, result);
        resumeTasks(oidsToResume, result);
    }

    @NotNull
    private List<String> getOidsToResume(String rootTaskOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        TaskQuartzImpl root = taskRetriever.getTaskPlain(rootTaskOid, result);
        List<TaskQuartzImpl> subtasks = root.listSubtasksDeeply(true, result);
        List<String> oidsToResume = new ArrayList<>(subtasks.size() + 1);
        if (root.getSchedulingState() == TaskSchedulingStateType.SUSPENDED) {
            oidsToResume.add(rootTaskOid);
        }
        for (Task subtask : subtasks) {
            if (subtask.getSchedulingState() == TaskSchedulingStateType.SUSPENDED) {
                oidsToResume.add(subtask.getOid());
            }
        }
        return oidsToResume;
    }

    private void resumeTasks(List<String> oids, OperationResult result) {
        for (String oid : oids) {
            try {
                resumeTask(oid, result);
            } catch (Exception e) {
                result.recordPartialError("Couldn't resume task " + oid + ": " + e.getMessage(), e);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resume task {}", e, oid);
            }
        }
    }

    public void resumeTask(String taskOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TaskQuartzImpl task = taskRetriever.getTaskPlain(taskOid, result);
        resumeTask(task, result);
    }

    public void resumeTask(TaskQuartzImpl task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (task.getSchedulingState() == TaskSchedulingStateType.SUSPENDED) {
            doResumeTask(task, result);
            return;
        }

        if (task.isRecurring()) {
            if (task.getSchedulingState() == TaskSchedulingStateType.CLOSED) {
                doResumeTask(task, result);
            } else {
                throw new IllegalStateException("Attempted to resume recurring task that is not suspended nor closed. "
                        + "Task = " + task + ", scheduling state = " + task.getSchedulingState());
            }
        } else {
            throw new IllegalStateException("Attempted to resume non-recurring task that was not suspended. "
                    + "Task = " + task + ", scheduling state = " + task.getSchedulingState());
        }
    }

    private void doResumeTask(TaskQuartzImpl task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        LOGGER.info("Resuming task {}.", task);
        try {
            clearTaskOperationResult(task, result); // see a note on scheduleTaskNow

            LOGGER.trace("Task scheduling state before suspend: {}", task.getSchedulingStateBeforeSuspend());
            if (task.getSchedulingStateBeforeSuspend() == TaskSchedulingStateType.WAITING) {
                List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_EXECUTION_STATE).replace(MoreObjects.firstNonNull(task.getStateBeforeSuspend(), TaskExecutionStateType.WAITING))
                        .item(TaskType.F_SCHEDULING_STATE).replace(TaskSchedulingStateType.WAITING)
                        .item(TaskType.F_STATE_BEFORE_SUSPEND).replace()
                        .item(TaskType.F_SCHEDULING_STATE_BEFORE_SUSPEND).replace()
                        .asItemDeltas();
                task.applyDeltasImmediate(itemDeltas, result);
            } else {
                List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_EXECUTION_STATE).replace(TaskExecutionStateType.RUNNABLE)
                        .item(TaskType.F_SCHEDULING_STATE).replace(TaskSchedulingStateType.READY)
                        .item(TaskType.F_STATE_BEFORE_SUSPEND).replace()
                        .item(TaskType.F_SCHEDULING_STATE_BEFORE_SUSPEND).replace()
                        .asItemDeltas();
                task.applyDeltasImmediate(itemDeltas, result);
                taskSynchronizer.synchronizeTask(task, result);
            }
        } catch (ObjectAlreadyExistsException t) {
            throw new SystemException("Unexpected exception while resuming task " + task + ": " + t.getMessage(), t);
        }
    }
}
