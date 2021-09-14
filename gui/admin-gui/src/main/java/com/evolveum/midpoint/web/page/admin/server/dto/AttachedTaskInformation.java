/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

/**
 * An object attached to {@link SelectableBean} for `TaskType`. It contains pre-processed information needed
 * to display columns not readily available in the `TaskType` itself, like progress, nodes, errors, and overall status.
 *
 * FIXME Temporary solution. It would be better to provide a specialized subclass of {@link SelectableBeanImpl} to cover
 *  this use case.
 */
public class AttachedTaskInformation implements Serializable {

    @NotNull private final TaskInformation information;

    private AttachedTaskInformation(@NotNull TaskType task, @Nullable TaskType rootTask) {
        information = TaskInformation.createForTask(task, rootTask);
    }

    /**
     * Installs {@link AttachedTaskInformation} into given {@link SelectableBean} (or returns information already being there).
     *
     * @param bean The bean representing task in question. We either update it with the information, or just get it from there.
     * @param rootTask The root task. Should not be null if a task in bean is non-root. (If it is null, then information will be
     * most probably displayed wrongly.)
     */
    public static @NotNull AttachedTaskInformation getOrCreate(@NotNull SelectableBean<TaskType> bean,
            @Nullable TaskType rootTask) {
        if (!(bean.getCustomData() instanceof AttachedTaskInformation)) {
            bean.setCustomData(new AttachedTaskInformation(bean.getValue(), rootTask));
        }
        return (AttachedTaskInformation) bean.getCustomData();
    }

    public String getProgressDescriptionShort() {
        return information.getProgressDescriptionShort();
    }

    public Integer getAllErrors() {
        return information.getAllErrors();
    }

    public String getNodesDescription() {
        return information.getNodesDescription();
    }

    public @NotNull OperationResultStatusType getResultStatus() {
        return information.getResultStatus();
    }

    public @Nullable XMLGregorianCalendar getCompletelyStalledSince() {
        return information.getCompletelyStalledSince();
    }
}
