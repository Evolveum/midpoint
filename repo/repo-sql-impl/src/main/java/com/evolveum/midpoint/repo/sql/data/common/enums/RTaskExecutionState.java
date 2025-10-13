/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;

/**
 * @author lazyman
 */
@JaxbType(type = TaskExecutionStateType.class)
public enum RTaskExecutionState implements SchemaEnum<TaskExecutionStateType> {

    RUNNABLE(TaskExecutionStateType.RUNNABLE),
    WAITING(TaskExecutionStateType.WAITING),
    SUSPENDED(TaskExecutionStateType.SUSPENDED),
    CLOSED(TaskExecutionStateType.CLOSED),
    RUNNING(TaskExecutionStateType.RUNNING);

    private TaskExecutionStateType status;

    RTaskExecutionState(TaskExecutionStateType status) {
        this.status = status;
        RUtil.register(this);
    }

    @Override
    public TaskExecutionStateType getSchemaValue() {
        return status;
    }
}
