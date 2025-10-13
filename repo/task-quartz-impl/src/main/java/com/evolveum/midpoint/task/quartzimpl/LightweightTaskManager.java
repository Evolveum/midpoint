/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.quartzimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;

/**
 * Manages lightweight asynchronous tasks.
 *
 * TODO finish revision of this class
 */
@Component
public class LightweightTaskManager {

    @Autowired private LocalNodeState localNodeState;

    private final ExecutorService lightweightHandlersExecutor = Executors.newCachedThreadPool();

    Future<?> submit(Runnable r) {
        return lightweightHandlersExecutor.submit(r);
    }

    public void waitForTransientChildrenAndCloseThem(RunningTask task, OperationResult result) {
        for (RunningLightweightTaskImpl subtask : ((RunningTaskQuartzImpl) task).getRunnableOrRunningLightweightAsynchronousSubtasks()) {
            subtask.waitForCompletion(result);
        }
    }

    public Collection<TaskQuartzImpl> getTransientSubtasks(String identifier) {
        if (identifier == null) {
            return List.of();
        }
        RunningTaskQuartzImpl runningInstance = localNodeState.getLocallyRunningTaskByIdentifier(identifier);
        if (runningInstance != null) {
            List<TaskQuartzImpl> subtasks = new ArrayList<>();
            for (RunningTaskQuartzImpl subtask : runningInstance.getLightweightAsynchronousSubtasks()) {
                subtasks.add(subtask.cloneAsStaticTask()); // Beware, does not update operation result in task prism
            }
            return subtasks;
        } else {
            return Collections.emptyList();
        }
    }
}
