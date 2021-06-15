/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.LocalActivityExecution;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityTreeStateOverviewUtil;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.createStatusType;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

public class ActivityTreeStateOverview {

    @NotNull private final Task rootTask;
    @NotNull private final CommonTaskBeans beans;

    public ActivityTreeStateOverview(@NotNull Task rootTask, @NotNull CommonTaskBeans beans) {
        this.rootTask = rootTask;
        this.beans = beans;
    }

    // TODO fix for multi-task (worker) executions
    public void recordExecutionStart(@NotNull LocalActivityExecution<?, ?, ?> execution, @NotNull OperationResult result)
            throws ActivityExecutionException {
        try {
            beans.plainRepositoryService.modifyObjectDynamically(TaskType.class, rootTask.getOid(), null,
                    taskBean -> {
                        ActivityStateOverviewType overview = ActivityTreeStateOverviewUtil.getOrCreateTreeOverview(taskBean);
                        ActivityTreeStateOverviewUtil.findOrCreateEntry(overview, execution.getActivityPath())
                                .realizationState(ActivitySimplifiedRealizationStateType.IN_PROGRESS)
                                .executionState(ActivityExecutionStateType.EXECUTING)
                                .resultStatus(OperationResultStatusType.IN_PROGRESS)
                                .taskRef(execution.getRunningTask().getSelfReference());
                        return beans.prismContext.deltaFor(TaskType.class)
                                .item(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_TREE_OVERVIEW).replace(overview)
                                .asItemDeltas();
                    }, null, result);
        } catch (Exception e) {
            throw new ActivityExecutionException("Couldn't record execution start in the activity tree",
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    /**
     * Note that execution result can be null only in the case of (very rare) uncaught exception.
     */
    // TODO fix for multi-task (worker) executions
    public void recordExecutionFinish(@NotNull LocalActivityExecution<?, ?, ?> execution,
            @Nullable ActivityExecutionResult executionResult, @NotNull OperationResult result)
            throws ActivityExecutionException {
        try {
            beans.plainRepositoryService.modifyObjectDynamically(TaskType.class, rootTask.getOid(), null,
                    taskBean -> {
                        ActivityStateOverviewType overview = ActivityTreeStateOverviewUtil.getOrCreateTreeOverview(taskBean);
                        ActivitySimplifiedRealizationStateType realizationState;
                        ActivityExecutionStateType executionState;
                        OperationResultStatusType resultStatus;
                        if (executionResult != null) {
                            realizationState = executionResult.getSimplifiedRealizationState();
                            executionState = executionResult.getExecutionState();
                            resultStatus = createStatusType(executionResult.getOperationResultStatus());
                        } else {
                            realizationState = ActivitySimplifiedRealizationStateType.IN_PROGRESS;
                            executionState = ActivityExecutionStateType.NOT_EXECUTING;
                            resultStatus = OperationResultStatusType.FATAL_ERROR;
                        }
                        ActivityTreeStateOverviewUtil.findOrCreateEntry(overview, execution.getActivityPath())
                                .realizationState(realizationState)
                                .executionState(executionState)
                                .resultStatus(resultStatus)
                                .taskRef(execution.getRunningTask().getSelfReference());
                        return beans.prismContext.deltaFor(TaskType.class)
                                .item(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_TREE_OVERVIEW).replace(overview)
                                .asItemDeltas();
                    }, null, result);
        } catch (Exception e) {
            throw new ActivityExecutionException("Couldn't record execution start in the activity tree",
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }
}
