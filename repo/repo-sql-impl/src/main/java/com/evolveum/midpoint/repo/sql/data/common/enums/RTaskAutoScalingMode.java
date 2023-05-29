/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAutoScalingModeType;

@JaxbType(type = TaskAutoScalingModeType.class)
public enum RTaskAutoScalingMode implements SchemaEnum<TaskAutoScalingModeType> {

    DISABLED(TaskAutoScalingModeType.DISABLED),
    DEFAULT(TaskAutoScalingModeType.DEFAULT);

    private final TaskAutoScalingModeType mode;

    RTaskAutoScalingMode(TaskAutoScalingModeType mode) {
        this.mode = mode;
        RUtil.register(this);
    }

    @Override
    public TaskAutoScalingModeType getSchemaValue() {
        return mode;
    }
}
