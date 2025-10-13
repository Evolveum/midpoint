/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RTaskAutoScaling;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAutoScalingType;

public class TaskAutoScalingMapper implements Mapper<TaskAutoScalingType, RTaskAutoScaling> {

    @Override
    public RTaskAutoScaling map(TaskAutoScalingType input, MapperContext context) {
        try {
            RTaskAutoScaling taskAutoScaling = new RTaskAutoScaling();
            RTaskAutoScaling.fromJaxb(input, taskAutoScaling);

            return taskAutoScaling;
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate TaskAutoScaling to entity", ex);
        }
    }
}
