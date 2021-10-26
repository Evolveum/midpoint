/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskUnpauseActionType;

/**
 * Helps with the management of subtasks for {@link DelegatingActivityRun} and {@link DistributingActivityRun}.
 */
class SubtaskHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SubtaskHelper.class);

    @NotNull private final AbstractActivityRun<?, ?, ?> activityRun;

    SubtaskHelper(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    /**
     * Checks that there are no relevant subtasks existing. (They should have been deleted during state purging process,
     * unless the state is persistent.)
     */
    void checkNoRelevantSubtasksDoExist(OperationResult result) throws ActivityRunException {
        LOGGER.debug("Going to check for existing subtasks");
        try {
            List<? extends Task> relevantChildren = getRelevantChildren(result);
            if (!relevantChildren.isEmpty()) {
                // The error may be permanent or transient. But reporting it as permanent is more safe, as it causes
                // the parent task to always suspend, catching the attention of the administrators.
                throw new ActivityRunException("Couldn't (re)create activity subtask(s) because there are existing one(s): "
                        + "that are not closed: " + relevantChildren, FATAL_ERROR, PERMANENT_ERROR);
            }
        } catch (Exception e) {
            throw new ActivityRunException("Couldn't delete activity subtask(s)", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    // TODO deduplicate
    @NotNull List<? extends Task> getRelevantChildren(OperationResult result) throws SchemaException {
        List<? extends Task> allChildren = getRunningTask().listSubtasks(true, result);
        List<? extends Task> relevantChildren = allChildren.stream()
                .filter(this::isRelevantWorker)
                .collect(Collectors.toList());
        LOGGER.debug("Found {} relevant workers out of {} children: {}",
                relevantChildren.size(), allChildren.size(), relevantChildren);
        return relevantChildren;
    }

    void switchExecutionToChildren(Collection<Task> children, OperationResult result) throws ActivityRunException {
        try {
            RunningTask runningTask = getRunningTask();
            runningTask.makeWaitingForOtherTasks(TaskUnpauseActionType.EXECUTE_IMMEDIATELY);
            runningTask.flushPendingModifications(result);
            for (Task child : children) {
                if (child.isSuspended()) {
                    getBeans().taskManager.resumeTask(child.getOid(), result);
                    LOGGER.debug("Started prepared child {}", child);
                }
            }
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
            throw new ActivityRunException("Couldn't switch execution to activity subtask",
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    private boolean isRelevantWorker(Task worker) {
        return getActivityPath().equalsBean(worker.getWorkState().getLocalRoot());
    }

    private @NotNull RunningTask getRunningTask() {
        return activityRun.getRunningTask();
    }

    private @NotNull ActivityPath getActivityPath() {
        return activityRun.getActivityPath();
    }

    private @NotNull CommonTaskBeans getBeans() {
        return activityRun.getBeans();
    }
}
