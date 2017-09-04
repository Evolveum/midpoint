package com.evolveum.midpoint.web.util;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;

public class TaskOperationUtils {

	private static final String DOT_CLASS = TaskOperationUtils.class.getName() + ".";
    private static final String OPERATION_SUSPEND_TASKS = DOT_CLASS + "suspendTask";
    private static final String OPERATION_RESUME_TASK = DOT_CLASS + "resumeTask";
    private static final String OPERATION_RUN_NOW_TASK = DOT_CLASS + "runNowTask";

	public static OperationResult suspendPerformed(TaskService taskService, Collection<String> oids) {
        OperationResult result = new OperationResult(OPERATION_SUSPEND_TASKS);
        try {
            boolean suspended = taskService.suspendTasks(oids,
                    PageTasks.WAIT_FOR_TASK_STOP, result);

            result.computeStatus();
            if (result.isSuccess()) {
                if (suspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "The task have been successfully suspended.");
                } else {
                    result.recordWarning("Task suspension has been successfully requested; please check for its completion using task list.");
                }
            }
        } catch (ObjectNotFoundException|SchemaException|SecurityViolationException|RuntimeException e) {
            result.recordFatalError("Couldn't suspend the task", e);
        }

        return result;

    }

	public static OperationResult resumePerformed(TaskService taskService, List<String> oids) {
        OperationResult result = new OperationResult(OPERATION_RESUME_TASK);
        try {
            taskService.resumeTasks(oids, result);
            result.computeStatus();

            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task has been successfully resumed.");
            }
        } catch (ObjectNotFoundException|SchemaException|SecurityViolationException|RuntimeException e) {
            result.recordFatalError("Couldn't resume the task", e);
        }

        return result;
    }

	public static OperationResult runNowPerformed(TaskService taskService, List<String> oids) {
        OperationResult result = new OperationResult(OPERATION_RUN_NOW_TASK);
        try {
            taskService.scheduleTasksNow(oids, result);
            result.computeStatus();

            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task has been successfully scheduled to run.");
            }
        } catch (ObjectNotFoundException|SchemaException|SecurityViolationException|RuntimeException e) {
            result.recordFatalError("Couldn't schedule the task", e);
        }

        return result;
    }

}
