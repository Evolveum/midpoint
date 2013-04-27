/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;

/**
 * Adds "RUNNING" state to the TaskExecutionStatus (meaning the task is currently executing at a node).
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
    CLOSED;

    public static TaskDtoExecutionStatus fromTaskExecutionStatus(TaskExecutionStatus executionStatus, boolean running) {
        if (running) {
            return TaskDtoExecutionStatus.RUNNING;
        } else {
            switch (executionStatus) {
                case RUNNABLE: return RUNNABLE;
                case WAITING: return WAITING;
                case SUSPENDED: return SUSPENDED;
                case CLOSED: return CLOSED;
                default: throw new IllegalArgumentException("executionStatus = " + executionStatus);
            }
        }
    }
}
