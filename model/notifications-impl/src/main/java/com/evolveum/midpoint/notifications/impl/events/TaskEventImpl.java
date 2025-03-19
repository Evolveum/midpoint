/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

public class TaskEventImpl extends BaseEventImpl implements TaskEvent {

    @NotNull private final Task task;
    @Nullable private final TaskRunResult taskRunResult; // nullable only if operationType == ADD
    @NotNull private final EventOperationType operationType; // only ADD or DELETE

    public TaskEventImpl(LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull Task task, @Nullable TaskRunResult runResult,
            @NotNull EventOperationType operationType, String channel) {
        super(lightweightIdentifierGenerator);
        this.task = task;
        this.taskRunResult = runResult;
        this.operationType = operationType;
        setChannel(channel);
    }

    @Override
    @NotNull
    public Task getTask() {
        return task;
    }

    @Override
    @Nullable
    public TaskRunResult getTaskRunResult() {
        return taskRunResult;
    }

    @Override
    @NotNull
    public EventOperationType getOperationType() {
        return operationType;
    }

    @Override
    public boolean isTemporaryError() {
        return taskRunResult != null && taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;
    }

    @Override
    public boolean isPermanentError() {
        return taskRunResult != null && taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
    }

    @Override
    public boolean isHaltingError() {
        return taskRunResult != null && taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.HALTING_ERROR;
    }

    @Override
    public boolean isFinished() {
        return taskRunResult != null &&
                taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.FINISHED;
    }

    @Override
    public boolean isInterrupted() {
        return taskRunResult != null && taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.INTERRUPTED;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatus) {
        if (eventStatus == null) {
            return false;
        }
        OperationResultStatus status = getOperationResultStatus();
        if (status == null) {
            // TODO consider if we really want to return 'true' for both success and in_progress here
            return eventStatus == EventStatusType.SUCCESS || eventStatus == EventStatusType.ALSO_SUCCESS || eventStatus == EventStatusType.IN_PROGRESS;
        }

        return statusMatches(eventStatus, status);
    }

    /**
     * Returns if the real status of the event matches the value against which we are filtering.
     *
     * TODO reconsider this method
     *
     * @param filtering Status against which we are filtering (or, generally, what we are asking about)
     * @param real Status of the specific event.
     */
    private boolean statusMatches(@NotNull EventStatusType filtering, @NotNull OperationResultStatus real) {
        switch (filtering) {
            case SUCCESS:
            case ALSO_SUCCESS:
                return real == OperationResultStatus.SUCCESS ||
                        real == OperationResultStatus.HANDLED_ERROR ||
                        real == OperationResultStatus.WARNING;
            case IN_PROGRESS:
                return false; // OK?
            case FAILURE:
                return real == OperationResultStatus.FATAL_ERROR ||
                        real == OperationResultStatus.PARTIAL_ERROR;
            case ONLY_FAILURE:
                return real == OperationResultStatus.FATAL_ERROR;
            default:
                throw new IllegalStateException("Invalid eventStatusType: " + filtering);
        }
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperation) {
        return this.operationType == eventOperation;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return eventCategory == EventCategoryType.TASK_EVENT;
    }

    @Override
    public OperationResultStatus getOperationResultStatus() {
        if (taskRunResult != null && taskRunResult.getOperationResultStatus() != null) {
            return taskRunResult.getOperationResultStatus();
        } else if (task.getResult() != null) {
            return task.getResult().getStatus();
        } else {
            return null;
        }
    }

    @Override
    public String getMessage() {
        if (taskRunResult != null && taskRunResult.getMessage() != null) {
            return taskRunResult.getMessage();
        } else if (task.getResult() != null) {
            return task.getResult().getMessage();
        } else {
            return null;
        }
    }

    @Override
    public long getProgress() {
        return taskRunResult != null && taskRunResult.getProgress() != null ?
                taskRunResult.getProgress() : task.getLegacyProgress();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "task", task, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "taskRunResult", taskRunResult, indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "operationType", operationType, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return toStringPrefix() +
                ", task=" + getTask() +
                ", operationResultStatus=" + getOperationResultStatus() +
                '}';
    }
}
