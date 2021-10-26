/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.task;

import com.evolveum.midpoint.repo.common.activity.ActivityTreeStateOverview;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Cleans-up everything for a task when the task's node is found to be down.
 *
 * Currently this means updating the activity tree state overview,
 * and releasing buckets delegated to the worker (if applicable).
 */
class NodeDownCleaner {

    private static final Trace LOGGER = TraceManager.getTrace(NodeDownCleaner.class);

    @NotNull private final Task task;
    @Nullable private final Task parent;
    @NotNull private final Task root;
    @NotNull private final CommonTaskBeans beans;

    NodeDownCleaner(@NotNull Task task, @Nullable Task parent, @NotNull Task root, @NotNull CommonTaskBeans beans) {
        this.task = task;
        this.parent = parent;
        this.root = root;
        this.beans = beans;
    }

    public void execute(OperationResult result) throws SchemaException, ObjectNotFoundException {
        updateActivityTreeOverview(result);
        releaseAllBucketsOfWorker(result);
    }

    private void updateActivityTreeOverview(OperationResult result) throws SchemaException, ObjectNotFoundException {
        new ActivityTreeStateOverview(root, beans)
                .recordTaskDead(task, result);
    }

    /**
     * We don't want this task to hold its buckets forever. So let's release them.
     *
     * (There's a slight risk if the task is restarted in the meanwhile, i.e. before learning that node is down and this
     * moment. But let us live with this risk for now.)
     */
    private void releaseAllBucketsOfWorker(OperationResult result) throws ObjectNotFoundException, SchemaException {
        TaskActivityStateType state = task.getActivitiesStateOrClone();
        if (state == null || state.getTaskRole() != TaskRoleType.WORKER) {
            return;
        }

        if (parent == null) {
            LOGGER.warn("No parent for worker {} (root {}) ?", task, root);
            return;
        }

        ActivityPath activityPath =
                ActivityPath.fromBean(
                        Objects.requireNonNull(
                                state.getLocalRoot(), "No local root in " + task));

        LOGGER.info("Returning all buckets from {} (coordinator {})", task, parent);
        beans.bucketingManager.releaseAllWorkBucketsFromWorker(parent.getOid(), task.getOid(), activityPath, null, result);
    }
}
