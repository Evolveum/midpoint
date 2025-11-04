/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.PERMANENT_ERROR;

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

    /** Deletes all existing subtasks that are relevant for this activity. */
    void deleteRemainingWorkers(OperationResult result) throws SchemaException {
        LOGGER.trace("Deleting all existing workers for activity run {}", activityRun);
        List<? extends Task> relevantChildren = getRelevantChildren(result);
        for (Task relevantChild : relevantChildren) {
            try {
                getBeans().taskManager.deleteTask(relevantChild.getOid(), result);
                LOGGER.debug("Deleted worker {}", relevantChild);
            } catch (ObjectNotFoundException e) {
                LOGGER.trace("Worker {} already deleted: {}", relevantChild, e.getMessage(), e);
            }
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
