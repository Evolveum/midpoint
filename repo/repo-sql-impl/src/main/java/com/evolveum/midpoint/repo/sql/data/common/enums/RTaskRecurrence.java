/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;

/**
 * @author lazyman
 */
@JaxbType(type = TaskRecurrenceType.class)
public enum RTaskRecurrence implements SchemaEnum<TaskRecurrenceType> {

    SINGLE(TaskRecurrenceType.SINGLE),
    RECURRING(TaskRecurrenceType.RECURRING);

    private TaskRecurrenceType recurrence;

    RTaskRecurrence(TaskRecurrenceType recurrence) {
        this.recurrence = recurrence;
        RUtil.register(this);
    }

    @Override
    public TaskRecurrenceType getSchemaValue() {
        return recurrence;
    }
}
