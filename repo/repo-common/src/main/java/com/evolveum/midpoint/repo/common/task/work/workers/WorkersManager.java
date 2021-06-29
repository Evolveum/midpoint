/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task.work.workers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Manages worker tasks for a distributed activity.
 */
@Component
public class WorkersManager {

    private static final Trace LOGGER = TraceManager.getTrace(WorkersManager.class);

    @Autowired private TaskManager taskManager;
    @Autowired private CommonTaskBeans beans;

    public void reconcileWorkers(Task rootTask, Task coordinatorTask, ActivityPath distributedActivityPath,
            WorkersReconciliationOptions options, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        new WorkersReconciliation(rootTask, coordinatorTask, distributedActivityPath, options, beans)
                .execute(result);
    }

    public void deleteWorkersAndWorkState(String rootTaskOid, boolean deleteWorkers, long subtasksWaitTime,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        boolean suspended = taskManager.suspendTaskTree(rootTaskOid, subtasksWaitTime, result);
        if (!suspended) {
            // TODO less harsh handling
            throw new IllegalStateException("Not all tasks could be suspended. Please retry to operation.");
        }
        Task rootTask = taskManager.getTaskPlain(rootTaskOid, result);
        deleteWorkersAndWorkState(rootTask, deleteWorkers, result);
    }

    private void deleteWorkersAndWorkState(Task rootTask, boolean deleteWorkers, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
//        TaskKindType kind = rootTask.getKind();
//        List<? extends Task> subtasks = rootTask.listSubtasks(true, result);
//        if (deleteWorkers && kind == TaskKindType.COORDINATOR) {
//            taskManager.suspendAndDeleteTasks(TaskUtil.tasksToOids(subtasks), TaskManager.DO_NOT_WAIT, true, result);
//        } else {
//            for (Task subtask : subtasks) {
//                deleteWorkersAndWorkState(subtask, deleteWorkers, result);
//            }
//        }
//        deleteWorkState(rootTask.getOid(), result);
    }

    private void deleteWorkState(String taskOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
//        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
//                .item(TaskType.F_WORK_STATE).replace()
//                .item(TaskType.F_PROGRESS).replace()
//                .item(TaskType.F_EXPECTED_TOTAL).replace()
//                .item(TaskType.F_OPERATION_STATS).replace()
//                .item(TaskType.F_RESULT).replace()
//                .item(TaskType.F_RESULT_STATUS).replace()
//                .asItemDeltas();
//        try {
//            taskManager.modifyTask(taskOid, itemDeltas, result);
//        } catch (ObjectAlreadyExistsException e) {
//            throw new IllegalStateException("Unexpected exception: " + e.getMessage(), e);
//        }
    }
}
