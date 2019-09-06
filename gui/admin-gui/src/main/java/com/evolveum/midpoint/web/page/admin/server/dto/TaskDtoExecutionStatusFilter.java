/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Possible values that can be used to filter tasks by their execution status.
 *
 * @see com.evolveum.midpoint.task.api.TaskExecutionStatus
 * @author Pavol Mederly
 */
public enum TaskDtoExecutionStatusFilter {

    ALL,
    RUNNING_OR_RUNNABLE,
    WAITING,
    SUSPENDED_OR_SUSPENDING,
    CLOSED,
    NOT_CLOSED;

    public S_AtomicFilterEntry appendFilter(S_AtomicFilterEntry q) {
        switch(this) {
            case ALL: return q;
            case RUNNING_OR_RUNNABLE: return q.item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.RUNNABLE).and();
            case WAITING: return q.item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.WAITING).and();
            case SUSPENDED_OR_SUSPENDING: return q.item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.SUSPENDED).and();
            case CLOSED: return q.item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.CLOSED).and();
            case NOT_CLOSED: return q.block().not().item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.CLOSED).endBlock().and();
            default: throw new SystemException("Unknown value for TaskDtoExecutionStatusFilter: " + this);
        }
    }
}
