/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;

import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Migrates tasks from 4.2 to 4.3.
 */
@Component
public class TaskMigrator {

    public void migrateIfNeeded(TaskQuartzImpl task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {

        if (task.getSchedulingState() == null && task.getExecutionState() != null) {
            task.setSchedulingState(determineSchedulingState(task.getExecutionState()));
            task.flushPendingModifications(result);
        }
    }

    @NotNull
    static TaskSchedulingStateType determineSchedulingState(@NotNull TaskExecutionStateType executionState) {
        TaskSchedulingStateType schedulingState;
        switch (executionState) {
            case RUNNING:
                // Strange but we accept it.
            case RUNNABLE:
                schedulingState = TaskSchedulingStateType.READY;
                break;
            case SUSPENDED:
                schedulingState = TaskSchedulingStateType.SUSPENDED;
                break;
            case WAITING:
                schedulingState = TaskSchedulingStateType.WAITING;
                break;
            case CLOSED:
                schedulingState = TaskSchedulingStateType.CLOSED;
                break;
            default:
                throw new AssertionError(executionState);
        }
        return schedulingState;
    }
}
