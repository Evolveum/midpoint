/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;

/**
 * @author lazyman
 */
@JaxbType(type = TaskExecutionStateType.class)
public enum RTaskExecutionStatus implements SchemaEnum<TaskExecutionStateType> {

    RUNNABLE(TaskExecutionStateType.RUNNABLE),
    WAITING(TaskExecutionStateType.WAITING),
    SUSPENDED(TaskExecutionStateType.SUSPENDED),
    CLOSED(TaskExecutionStateType.CLOSED),
    RUNNING(TaskExecutionStateType.RUNNING),
    SUSPENDING(TaskExecutionStateType.SUSPENDING);

    private TaskExecutionStateType status;

    RTaskExecutionStatus(TaskExecutionStateType status) {
        this.status = status;
    }

    @Override
    public TaskExecutionStateType getSchemaValue() {
        return status;
    }
}
