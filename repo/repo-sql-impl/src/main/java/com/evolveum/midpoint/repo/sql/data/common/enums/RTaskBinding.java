/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskBindingType;

/**
 * @author lazyman
 */
@JaxbType(type = TaskBindingType.class)
public enum RTaskBinding implements SchemaEnum<TaskBindingType> {

    LOOSE(TaskBindingType.LOOSE),
    TIGHT(TaskBindingType.TIGHT);

    private TaskBindingType binding;

    RTaskBinding(TaskBindingType binding) {
        this.binding = binding;
        RUtil.register(this);
    }

    @Override
    public TaskBindingType getSchemaValue() {
        return binding;
    }
}
