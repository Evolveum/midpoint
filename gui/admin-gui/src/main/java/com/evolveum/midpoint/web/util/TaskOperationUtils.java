/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;

public class TaskOperationUtils {

	private static final String DOT_CLASS = TaskOperationUtils.class.getName() + ".";
    private static final String OPERATION_SUSPEND_TASKS = DOT_CLASS + "suspendTask";
    private static final String OPERATION_RESUME_TASK = DOT_CLASS + "resumeTask";
    private static final String OPERATION_RUN_NOW_TASK = DOT_CLASS + "runNowTask";

	public static OperationResult suspendPerformed(TaskService taskService, Collection<String> oids, PageBase pageBase) {
		Task opTask = pageBase.createSimpleTask(OPERATION_SUSPEND_TASKS);
        OperationResult result = opTask.getResult();
        try {
            boolean suspended = taskService.suspendTasks(oids,
                    PageTasks.WAIT_FOR_TASK_STOP, opTask, result);

            result.computeStatus();
            if (result.isSuccess()) {
                if (suspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS, pageBase.createStringResource("TaskOperationUtils.message.suspendPerformed.success").getString()); 
                } else {
                    result.recordWarning(pageBase.createStringResource("TaskOperationUtils.message.suspendPerformed.warning").getString());
                }
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(pageBase.createStringResource("TaskOperationUtils.message.suspendPerformed.fatalError").getString(), e);
        }

        return result;

    }

	public static OperationResult resumePerformed(TaskService taskService, List<String> oids, PageBase pageBase) {
		Task opTask = pageBase.createSimpleTask(OPERATION_RESUME_TASK);
        OperationResult result = opTask.getResult();
        try {
            taskService.resumeTasks(oids, opTask, result);
            result.computeStatus();

            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, pageBase.createStringResource("TaskOperationUtils.message.resumePerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(pageBase.createStringResource("TaskOperationUtils.message.resumePerformed.fatalError").getString(), e);
        }

        return result;
    }

	public static OperationResult runNowPerformed(TaskService taskService, List<String> oids, PageBase pageBase) {
		Task opTask = pageBase.createSimpleTask(OPERATION_RUN_NOW_TASK);
        OperationResult result = opTask.getResult();
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

}
