/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.createStatusType;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType.F_TREE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_ACTIVITY_STATE;

import java.util.Objects;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.LocalActivityExecution;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityStateOverviewUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ActivityTreeStateOverview {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityTreeStateOverview.class);

    @NotNull private final Task rootTask;
    @NotNull private final CommonTaskBeans beans;

    @NotNull private static final ItemPath PATH_REALIZATION_STATE
            = ItemPath.create(F_ACTIVITY_STATE, F_TREE, ActivityTreeStateType.F_REALIZATION_STATE);
    @NotNull private static final ItemPath PATH_ACTIVITY_STATE_TREE
            = ItemPath.create(F_ACTIVITY_STATE, F_TREE, ActivityTreeStateType.F_ACTIVITY);

    ActivityTreeStateOverview(@NotNull Task rootTask, @NotNull CommonTaskBeans beans) {
        this.rootTask = rootTask;
        this.beans = beans;
    }

    // TODO fix for multi-task (worker) executions
    public void recordExecutionStart(@NotNull LocalActivityExecution<?, ?, ?> execution, @NotNull OperationResult result)
            throws ActivityExecutionException {
        try {
            beans.plainRepositoryService.modifyObjectDynamically(TaskType.class, rootTask.getOid(), null,
                    taskBean -> {
                        ActivityStateOverviewType overview = ActivityStateOverviewUtil.getOrCreateStateOverview(taskBean);
                        ActivityStateOverviewType entry = ActivityStateOverviewUtil.findOrCreateEntry(overview, execution.getActivityPath())
                                .realizationState(ActivitySimplifiedRealizationStateType.IN_PROGRESS)
                                .executionState(ActivityExecutionStateType.EXECUTING)
                                .resultStatus(OperationResultStatusType.IN_PROGRESS);
                        entry.getTaskRef().clear(); // temporary implementation
                        entry.getTaskRef().add(execution.getRunningTask().getSelfReference());
                        return beans.prismContext.deltaFor(TaskType.class)
                                .item(F_ACTIVITY_STATE, F_TREE, ActivityTreeStateType.F_ACTIVITY).replace(overview)
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
                        ActivityStateOverviewType overview = ActivityStateOverviewUtil.getOrCreateStateOverview(taskBean);
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
                        ActivityStateOverviewType entry = ActivityStateOverviewUtil.findOrCreateEntry(overview, execution.getActivityPath())
                                .realizationState(realizationState)
                                .executionState(executionState)
                                .resultStatus(resultStatus);
                        entry.getTaskRef().clear();
                        entry.getTaskRef().add(execution.getRunningTask().getSelfReference());
                        return beans.prismContext.deltaFor(TaskType.class)
                                .item(F_ACTIVITY_STATE, F_TREE, ActivityTreeStateType.F_ACTIVITY).replace(overview)
                                .asItemDeltas();
                    }, null, result);
        } catch (Exception e) {
            throw new ActivityExecutionException("Couldn't record execution start in the activity tree",
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    public ActivityTreeRealizationStateType getRealizationState() {
        return rootTask.getPropertyRealValue(PATH_REALIZATION_STATE, ActivityTreeRealizationStateType.class);
    }

    /**
     * Updates the realization state (including writing to the repository).
     */
    public void updateRealizationState(ActivityTreeRealizationStateType value, OperationResult result)
            throws ActivityExecutionException {
        try {
            rootTask.setItemRealValues(PATH_REALIZATION_STATE, value);
            rootTask.flushPendingModifications(result);
        } catch (CommonException e) {
            throw new ActivityExecutionException("Couldn't update tree realization state", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    public ActivityStateOverviewType getActivityStateTree() {
        return rootTask.getPropertyRealValue(PATH_ACTIVITY_STATE_TREE, ActivityStateOverviewType.class);
    }

    /**
     * Updates the activity state tree (including writing to the repository).
     */
    private void updateActivityStateTree(ActivityStateOverviewType value, OperationResult result)
            throws ActivityExecutionException {
        try {
            rootTask.setItemRealValues(PATH_ACTIVITY_STATE_TREE, value);
            rootTask.flushPendingModifications(result);
        } catch (CommonException e) {
            throw new ActivityExecutionException("Couldn't update activity state tree", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    public void purge(OperationResult result) throws ActivityExecutionException {
        updateActivityStateTree(
                purgeStateRecursively(
                        getActivityStateTree()
                ),
                result);

        LOGGER.trace("State tree after purging: {}", lazy(this::getActivityStateTree));
    }

    private ActivityStateOverviewType purgeStateRecursively(ActivityStateOverviewType state) {
        if (state == null || isTransient(state)) {
            return null;
        }

        state.setRealizationState(null);
        state.setExecutionState(null);
        state.setResultStatus(null);
        // TODO taskref?

        state.getActivity().replaceAll(this::purgeStateRecursively);
        state.getActivity().removeIf(Objects::isNull);
        return state;
    }

    private boolean isTransient(@NotNull ActivityStateOverviewType state) {
        return state.getPersistence() == null ||
                state.getPersistence() == ActivityStatePersistenceType.SINGLE_REALIZATION;
    }
}
