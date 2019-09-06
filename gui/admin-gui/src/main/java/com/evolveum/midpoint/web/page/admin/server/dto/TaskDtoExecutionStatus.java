/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;

/**
 * Adds "RUNNING" state to the TaskExecutionStatus (meaning the task is currently executing at a node).
 * And also "SUSPENDING" if it is running, but marked as suspended.
 *
 * @see TaskExecutionStatus
 * @author Pavol Mederly
 */
public enum TaskDtoExecutionStatus {

    RUNNING_OR_RUNNABLE,
    RUNNING,
    RUNNABLE,
    WAITING,
    SUSPENDED,
    SUSPENDING,
    CLOSED;

    public static TaskDtoExecutionStatus fromTaskExecutionStatus(TaskExecutionStatusType executionStatus, boolean running) {
        if (running) {
            if (executionStatus == TaskExecutionStatusType.SUSPENDED) {
                return SUSPENDING;
            } else {
                return TaskDtoExecutionStatus.RUNNING;
            }
        } else {
            if (executionStatus != null) {
                switch (executionStatus) {
                    case RUNNABLE: return RUNNABLE;
                    case WAITING: return WAITING;
                    case SUSPENDED: return SUSPENDED;
                    case CLOSED: return CLOSED;
                    default: throw new IllegalArgumentException("executionStatus = " + executionStatus);
                }
            } else {
                return null;
            }
        }
    }
}
