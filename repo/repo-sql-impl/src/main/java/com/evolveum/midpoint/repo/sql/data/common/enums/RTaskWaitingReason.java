/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWaitingReasonType;

/**
 * @author lazyman
 */
@JaxbType(type = TaskWaitingReasonType.class)
public enum RTaskWaitingReason implements SchemaEnum<TaskWaitingReasonType> {

    OTHER_TASKS(TaskWaitingReasonType.OTHER_TASKS),

    // See MID-6117.
    PLACEHOLDER(null),

    OTHER(TaskWaitingReasonType.OTHER);

    private TaskWaitingReasonType reason;

    RTaskWaitingReason(TaskWaitingReasonType reason) {
        this.reason = reason;
        RUtil.register(this);
    }

    @Override
    public TaskWaitingReasonType getSchemaValue() {
        return reason;
    }
}
