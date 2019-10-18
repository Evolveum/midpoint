/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

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

/**
 * @author mederly
 */
public class TaskEvent extends BaseEvent {

    private static final Trace LOGGER = TraceManager.getTrace(TaskEvent.class);

    @NotNull private final Task task;
    @Nullable private final TaskRunResult taskRunResult;            // nullable only if operationType == ADD
    @NotNull private final EventOperationType operationType;        // only ADD or DELETE

    public TaskEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull Task task, @Nullable TaskRunResult runResult,
            @NotNull EventOperationType operationType, String channel) {
        super(lightweightIdentifierGenerator);
        this.task = task;
        this.taskRunResult = runResult;
        this.operationType = operationType;
        setChannel(channel);
    }

    @NotNull
    public Task getTask() {
        return task;
    }

    @Nullable
    public TaskRunResult getTaskRunResult() {
        return taskRunResult;
    }

    @NotNull
    public EventOperationType getOperationType() {
        return operationType;
    }

    public boolean isTemporaryError() {
        return taskRunResult != null && taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;
    }

    public boolean isPermanentError() {
        return taskRunResult != null && taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
    }

    public boolean isFinished() {
        return taskRunResult != null &&
                (taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.FINISHED ||
                        taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.FINISHED_HANDLER);
    }

    public boolean isInterrupted() {
        return taskRunResult != null && taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.INTERRUPTED;
    }

    public boolean isRestartRequested() {
        return taskRunResult != null && taskRunResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.RESTART_REQUESTED;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        if (eventStatusType == null) {
            return false;
        }
        if (taskRunResult == null || taskRunResult.getOperationResult() == null) {
            // TODO consider if we really want to return 'true' for both success and in_progress here
            return eventStatusType == EventStatusType.SUCCESS || eventStatusType == EventStatusType.ALSO_SUCCESS || eventStatusType == EventStatusType.IN_PROGRESS;
        }
        OperationResult result = taskRunResult.getOperationResult();
        switch (eventStatusType) {
            case SUCCESS:
            case ALSO_SUCCESS: return result.isSuccess() || result.isHandledError() || result.isWarning();
            case IN_PROGRESS: return false;
            case FAILURE: return result.isError();
            case ONLY_FAILURE: return result.isFatalError();
            default: throw new IllegalStateException("Invalid eventStatusType: " + eventStatusType);
        }
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        return this.operationType == eventOperationType;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.TASK_EVENT;
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        return false;
    }

    @Override
    public boolean isUserRelated() {
        return false;
    }

    public OperationResultStatus getOperationResultStatus() {
        return taskRunResult != null && taskRunResult.getOperationResult() != null ? taskRunResult.getOperationResult().getStatus() : null;
    }

    public String getMessage() {
        return taskRunResult != null && taskRunResult.getOperationResult() != null ? taskRunResult.getOperationResult().getMessage() : null;
    }

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
