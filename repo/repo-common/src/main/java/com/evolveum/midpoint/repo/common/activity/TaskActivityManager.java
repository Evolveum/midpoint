/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import java.util.List;

import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityStateOverviewUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateOverviewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@Experimental
@Component
public class TaskActivityManager {

    private static final String OP_CLEAR_FAILED_ACTIVITY_STATE = TaskActivityManager.class.getName() + ".clearFailedActivityState";

    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("repositoryService") private RepositoryService plainRepositoryService;

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
                        ActivityStateOverviewType treeOverview = ActivityStateOverviewUtil.getTreeOverview(taskBean);
                        if (treeOverview != null) {
                            ActivityStateOverviewType updatedTreeOverview = treeOverview.clone();
                            ActivityStateOverviewUtil.clearFailedState(updatedTreeOverview);
                            return prismContext.deltaFor(TaskType.class)
                                    .item(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_TREE_OVERVIEW)
                                    .replace(updatedTreeOverview)
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
}
