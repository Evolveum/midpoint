/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.task.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType.F_TREE;

@Experimental
@Component
public class TaskActivityManager {

    private static final String OP_CLEAR_FAILED_ACTIVITY_STATE = TaskActivityManager.class.getName() + ".clearFailedActivityState";

    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;
    @Autowired @Qualifier("repositoryService") private RepositoryService plainRepositoryService;
    @Autowired private TaskManager taskManager;
    @Autowired private CommonTaskBeans beans;

    // TODO reconsider this
    //  How should we clear the "not executed" flag in the tree overview when using e.g. the tests?
    //  In production the flag is updated automatically when the task/activities start.
    public void clearFailedActivityState(String taskOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.subresult(OP_CLEAR_FAILED_ACTIVITY_STATE)
                .addParam("taskOid", taskOid)
                .build();
        try {
            plainRepositoryService.modifyObjectDynamically(TaskType.class, taskOid, null,
                    taskBean -> {
                        ActivityStateOverviewType stateOverview = ActivityStateOverviewUtil.getStateOverview(taskBean);
                        if (stateOverview != null) {
                            ActivityStateOverviewType updatedStateOverview = stateOverview.clone();
                            ActivityStateOverviewUtil.clearFailedState(updatedStateOverview);
                            return prismContext.deltaFor(TaskType.class)
                                    .item(TaskType.F_ACTIVITY_STATE, F_TREE, ActivityTreeStateType.F_ACTIVITY)
                                    .replace(updatedStateOverview)
                                    .asItemDeltas();
                        } else {
                            return List.of();
                        }
                    }, null, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // TODO reconsider the concept of resolver (as it is useless now - we have to fetch the subtasks manually!)
    public ActivityProgressInformation getProgressInformation(String rootTaskOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return ActivityProgressInformation.fromRootTask(
                getTaskWithSubtasks(rootTaskOid, result),
                createTaskResolver(result));
    }

    public TreeNode<ActivityPerformanceInformation> getPerformanceInformation(String rootTaskOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return ActivityTreeUtil.transformStates(
                getTaskWithSubtasks(rootTaskOid, result),
                createTaskResolver(result),
                (path, state, workerStates, task) -> {
                    if (workerStates != null) {
                        return ActivityPerformanceInformation.forCoordinator(path, workerStates);
                    } else {
                        ActivityItemProcessingStatisticsType itemStats = getItemStats(state);
                        if (itemStats != null) {
                            return ActivityPerformanceInformation.forRegularActivity(path, itemStats, state.getProgress());
                        } else {
                            return ActivityPerformanceInformation.notApplicable(path);
                        }
                    }
                });
    }

    private ActivityItemProcessingStatisticsType getItemStats(ActivityStateType state) {
        return state != null && state.getStatistics() != null ?
                state.getStatistics().getItemProcessing() : null;
    }

    private TaskResolver createTaskResolver(OperationResult result) {
        return oid -> getTaskWithSubtasks(oid, result);
    }

    @NotNull
    private TaskType getTaskWithSubtasks(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Collection<SelectorOptions<GetOperationOptions>> withChildren = schemaService.getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();

        return taskManager.getTask(oid, withChildren, result)
                .getUpdatedTaskObject()
                .asObjectable();
    }

    public @NotNull Activity<?, ?> getActivity(Task rootTask, ActivityPath activityPath)
            throws SchemaException {
        return ActivityTree.create(rootTask, beans)
                .getActivity(activityPath);
    }
}
