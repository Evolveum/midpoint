/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;

@JaxbType(type = TaskSchedulingStateType.class)
public enum RTaskSchedulingState implements SchemaEnum<TaskSchedulingStateType> {

    READY(TaskSchedulingStateType.READY),
    WAITING(TaskSchedulingStateType.WAITING),
    SUSPENDED(TaskSchedulingStateType.SUSPENDED),
    CLOSED(TaskSchedulingStateType.CLOSED);

    private final TaskSchedulingStateType stateType;

    RTaskSchedulingState(TaskSchedulingStateType stateType) {
        this.stateType = stateType;
        RUtil.register(this);
    }

    @Override
    public TaskSchedulingStateType getSchemaValue() {
        return stateType;
    }
}
