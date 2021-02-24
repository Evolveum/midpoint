/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TaskEventImpl extends BaseEventImpl implements TaskEvent {

    private static final Trace LOGGER = TraceManager.getTrace(TaskEventImpl.class);

    @NotNull private final Task task;
    @Nullable private final TaskRunResult taskRunResult;            // nullable only if operationType == ADD
    @NotNull private final EventOperationType operationType;        // only ADD or DELETE

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
        if (taskRunResult == null || taskRunResult.getOperationResult() == null) {
            // TODO consider if we really want to return 'true' for both success and in_progress here
            return eventStatus == EventStatusType.SUCCESS || eventStatus == EventStatusType.ALSO_SUCCESS || eventStatus == EventStatusType.IN_PROGRESS;
        }
        OperationResult result = taskRunResult.getOperationResult();
        switch (eventStatus) {
            case SUCCESS:
            case ALSO_SUCCESS: return result.isSuccess() || result.isHandledError() || result.isWarning();
            case IN_PROGRESS: return false;
            case FAILURE: return result.isError();
            case ONLY_FAILURE: return result.isFatalError();
            default: throw new IllegalStateException("Invalid eventStatusType: " + eventStatus);
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
    public boolean isRelatedToItem(ItemPath itemPath) {
        return false;
    }

    @Override
    public OperationResultStatus getOperationResultStatus() {
        return taskRunResult != null && taskRunResult.getOperationResult() != null ? taskRunResult.getOperationResult().getStatus() : null;
    }

    @Override
    public String getMessage() {
        return taskRunResult != null && taskRunResult.getOperationResult() != null ? taskRunResult.getOperationResult().getMessage() : null;
    }

    @Override
    public long getProgress() {
        return taskRunResult != null && taskRunResult.getProgress() != null ? taskRunResult.getProgress() : task.getProgress();
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
}
