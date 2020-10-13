/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

public class TaskOperationUtils {

    private static final String DOT_CLASS = TaskOperationUtils.class.getName() + ".";
    private static final String OPERATION_SUSPEND_TASKS = DOT_CLASS + "suspendTasks";
    private static final String OPERATION_RESUME_TASKS = DOT_CLASS + "resumeTasks";
    private static final String OPERATION_RUN_NOW_TASKS = DOT_CLASS + "runNowTasks";

    /**
     * Suspends tasks "intelligently" i.e. tries to recognize whether to suspend a single task,
     * or to suspend the whole tree. (Maybe this differentiation should be done by the task manager itself.)
     *
     * It is also questionable whether we should create the task here or it should be done by the caller.
     * For the time being it is done here.
     */
    public static OperationResult suspendTasks(List<TaskType> selectedTasks, PageBase pageBase) {
        Task opTask = pageBase.createSimpleTask(TaskOperationUtils.OPERATION_SUSPEND_TASKS);
        OperationResult result = opTask.getResult();

        try {
            TaskService taskService = pageBase.getTaskService();

            List<TaskType> plainTasks = getPlainTasks(selectedTasks);
            List<TaskType> treeRoots = getTreeRoots(selectedTasks);
            boolean allPlainTasksSuspended = suspendPlainTasks(taskService, plainTasks, result, opTask);
            boolean allTreesSuspended = suspendTrees(taskService, treeRoots, result, opTask);

            result.computeStatus();
            if (result.isSuccess()) {
                if (allPlainTasksSuspended && allTreesSuspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS,
                            pageBase.createStringResource("TaskOperationUtils.message.suspendPerformed.success").getString());
                } else {
                    result.recordWarning(pageBase.createStringResource("TaskOperationUtils.message.suspendPerformed.warning").getString());
                }
            }
        } catch (Throwable t) {
            result.recordFatalError(pageBase.createStringResource("pageTasks.message.suspendTasksPerformed.fatalError").getString(), t);
        }

        return result;
    }

    private static boolean suspendPlainTasks(TaskService taskService, List<TaskType> plainTasks, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        //noinspection SimplifiableIfStatement
        if (!plainTasks.isEmpty()) {
            return taskService.suspendTasks(ObjectTypeUtil.getOids(plainTasks), PageTasks.WAIT_FOR_TASK_STOP, opTask, result);
        } else {
            return true;
        }
    }

    private static boolean suspendTrees(TaskService taskService, List<TaskType> roots, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        boolean suspended = true;
        if (!roots.isEmpty()) {
            for (TaskType root : roots) {
                boolean s = taskService.suspendTaskTree(root.getOid(), PageTasks.WAIT_FOR_TASK_STOP, opTask, result);
                suspended = suspended && s;
            }
        }
        return suspended;
    }

    /**
     * Resumes tasks "intelligently" i.e. tries to recognize whether to resume a single task,
     * or to resume the whole tree. See {@link #suspendTasks(List, PageBase)}.
     */
    public static OperationResult resumeTasks(List<TaskType> selectedTasks, PageBase pageBase) {
        Task opTask = pageBase.createSimpleTask(OPERATION_RESUME_TASKS);
        OperationResult result = opTask.getResult();

        try {
            TaskService taskService = pageBase.getTaskService();

            List<TaskType> plainTasks = getPlainTasks(selectedTasks);
            List<TaskType> treeRoots = getTreeRoots(selectedTasks);
            taskService.resumeTasks(ObjectTypeUtil.getOids(plainTasks), opTask, result);
            for (TaskType treeRoot : treeRoots) {
                taskService.resumeTaskTree(treeRoot.getOid(), opTask, result);
            }
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        pageBase.createStringResource("TaskOperationUtils.message.resumePerformed.success").getString());
            }
        } catch (Throwable t) {
            result.recordFatalError(pageBase.createStringResource("TaskOperationUtils.message.resumePerformed.fatalError").getString(), t);
        }
        return result;
    }

    /**
     * Schedules the tasks for immediate execution.
     *
     * TODO should we distinguish between plain task and task tree roots here?
     */
    public static OperationResult runNowPerformed(List<String> oids, PageBase pageBase) {
        Task opTask = pageBase.createSimpleTask(OPERATION_RUN_NOW_TASKS);
        OperationResult result = opTask.getResult();
        TaskService taskService = pageBase.getTaskService();

        try {
            taskService.scheduleTasksNow(oids, opTask, result);
            result.computeStatus();

            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, pageBase.createStringResource("TaskOperationUtils.message.runNowPerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(pageBase.createStringResource("TaskOperationUtils.message.runNowPerformed.fatalError").getString(), e);
        }

        return result;
    }

    @NotNull
    private static List<TaskType> getPlainTasks(List<TaskType> selectedTasks) {
        return selectedTasks.stream()
                .filter(task -> !TaskTypeUtil.isManageableTreeRoot(task))
                .collect(Collectors.toList());
    }

    @NotNull
    private static List<TaskType> getTreeRoots(List<TaskType> selectedTasks) {
        return selectedTasks.stream()
                .filter(TaskTypeUtil::isManageableTreeRoot)
                .collect(Collectors.toList());
    }
}
