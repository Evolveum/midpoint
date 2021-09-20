/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;

/**
 * TODO TODO TODO update this doc
 *
 * Original description:
 *  Adds "RUNNING" state to the TaskExecutionState (meaning the task is currently executing at a node).
 *  And also "SUSPENDING" if it is running, but marked as suspended.
 *
 * New meaning:
 *  Probably not needed any more. The RUNNING_OR_RUNNABLE is needed to query for the task state, and should not be here.
 */
public enum TaskDtoExecutionState {

    RUNNING_OR_RUNNABLE,
    RUNNING,
    RUNNABLE,
    WAITING,
    SUSPENDED,
    SUSPENDING,
    CLOSED;

    // TODO MID-6783
    public static TaskDtoExecutionState fromTaskExecutionState(TaskExecutionStateType executionState, boolean running) {
        if (running) {
            if (executionState == TaskExecutionStateType.SUSPENDED) {
                return SUSPENDING;
            } else {
                return TaskDtoExecutionState.RUNNING;
            }
        } else {
            if (executionState != null) {
                switch (executionState) {
                    case RUNNABLE: return RUNNABLE;
                    case RUNNING: return RUNNING;
                    case WAITING: return WAITING;
                    case SUSPENDED: return SUSPENDED;
                    case CLOSED: return CLOSED;
                    default: throw new IllegalArgumentException("executionState = " + executionState);
                }
            } else {
                return null;
            }
        }
    }
}
