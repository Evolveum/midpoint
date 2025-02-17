/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.util.LocalizableMessage;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.Nullable;

/**
 * The {@link TaskInformation} based on a legacy task structure. Very limited.
 */
public class LegacyTaskInformation extends TaskInformation {

    @NotNull private final ItemsProgressInformation itemsProgressInformation;

    private LegacyTaskInformation(
            @NotNull TaskType task, @NotNull ActivityWorkersInformation workersInformation,
            @NotNull OperationResultStatusType overallStatus,
            @NotNull ItemsProgressInformation itemsProgressInformation) {
        super(task, workersInformation, overallStatus);
        this.itemsProgressInformation = itemsProgressInformation;
    }

    public static @NotNull TaskInformation fromLegacyTaskOrNoTask(@Nullable TaskType task) {
        return task != null ?
                fromLegacyTask(task) : fromNoTask();
    }

    /**
     * Note: the task may or may not have children. But currently these are ignored here.
     */
    static @NotNull TaskInformation fromLegacyTask(@NotNull TaskType task) {
        return new LegacyTaskInformation(
                task,
                ActivityWorkersInformation.fromLegacyTask(task),
                notNull(task.getResultStatus()),
                ItemsProgressInformation.fromLegacyTask(task)
        );
    }

    private static @NotNull TaskInformation fromNoTask() {
        TaskType emptyTask = new TaskType();
        return new LegacyTaskInformation(
                emptyTask,
                ActivityWorkersInformation.empty(),
                OperationResultStatusType.UNKNOWN,
                ItemsProgressInformation.fromLegacyTask(emptyTask)
        );
    }

    @Override
    public String getProgressDescription(boolean longForm) {
        return itemsProgressInformation.toHumanReadableString(longForm);
    }

    @Override
    public double getProgress() {
        return -1; // It couldn't be determined from the legacy task, there are no activities.
    }

    @Override
    public boolean isComplete() {
        return false; // It couldn't be determined from the legacy task.
    }

    @Override
    public LocalizableMessage getTaskStatusDescription() {
        return null; // It couldn't be determined from the legacy task.
    }

    @Override
    public Integer getAllErrors() {
        return null; // This counter is not supported for legacy (non-activity-based) tasks.
    }

    @Override
    public XMLGregorianCalendar getStartTimestamp() {
        return task.getLastRunStartTimestamp();
    }

    @Override
    public XMLGregorianCalendar getEndTimestamp() {
        if (task.getLastRunStartTimestamp() == null ||
                task.getLastRunFinishTimestamp() == null ||
                task.getLastRunFinishTimestamp().compare(task.getLastRunStartTimestamp()) == DatatypeConstants.LESSER) {
            return null;
        } else {
            return task.getLastRunFinishTimestamp();
        }
    }

    @Override
    public Object getLiveSyncToken() {
        return ObjectTypeUtil.getExtensionItemRealValue(task.asPrismContainerValue(), SchemaConstants.SYNC_TOKEN);
    }
}
