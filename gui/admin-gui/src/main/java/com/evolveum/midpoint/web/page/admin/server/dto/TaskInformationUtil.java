/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Attaches {@link TaskInformation} to {@link SelectableBean} for `TaskType`.
 *
 * FIXME Temporary solution. It would be better to provide a specialized subclass of {@link SelectableBeanImpl} to cover
 *  this use case.
 */
public class TaskInformationUtil implements Serializable {

    /**
     * Installs {@link TaskInformationUtil} into given {@link SelectableBean} (or returns information already being there).
     *
     * @param bean The bean representing task in question. We either update it with the information, or just get it from there.
     * @param rootTask The root task. Should not be null if a task in bean is non-root. (If it is null, then information will be
     * most probably displayed wrongly.)
     */
    public static @NotNull TaskInformation getOrCreateInfo(@NotNull SelectableBean<TaskType> bean,
            @Nullable TaskType rootTask) {
        if (!(bean.getCustomData() instanceof TaskInformation)) {
            bean.setCustomData(
                    TaskInformation.createForTask(bean.getValue(), rootTask));
        }
        return (TaskInformation) bean.getCustomData();
    }
}
