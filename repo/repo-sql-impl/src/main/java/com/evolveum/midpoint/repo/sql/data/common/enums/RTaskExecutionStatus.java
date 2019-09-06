/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;

/**
 * @author lazyman
 */
@JaxbType(type = TaskExecutionStatusType.class)
public enum RTaskExecutionStatus implements SchemaEnum<TaskExecutionStatusType> {

    RUNNABLE(TaskExecutionStatusType.RUNNABLE),
    WAITING(TaskExecutionStatusType.WAITING),
    SUSPENDED(TaskExecutionStatusType.SUSPENDED),
    CLOSED(TaskExecutionStatusType.CLOSED);

    private TaskExecutionStatusType status;

    private RTaskExecutionStatus(TaskExecutionStatusType status) {
        this.status = status;
    }

    @Override
    public TaskExecutionStatusType getSchemaValue() {
        return status;
    }
}
