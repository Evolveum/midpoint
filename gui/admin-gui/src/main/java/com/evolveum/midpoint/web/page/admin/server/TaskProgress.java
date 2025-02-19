/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import java.io.Serializable;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;

public class TaskProgress implements Serializable {

    private TaskExecutionStateType executionState;

    private boolean complete;

    private int progress;

    private String progressLabel;

    private OperationResultStatus processedObjectsStatus;

    private int processedObjectsErrorCount;

    /**
     * Status related to task (and subtasks) execution health (e.g. failed workers).
     */
    private OperationResultStatus taskHealthStatus;

    /**
     * Message describing task health status.
     */
    private LocalizableMessage taskHealthStatusMessage;

    /**
     * Overall task status.
     */
    private OperationResultStatus taskStatus;

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getProcessedObjectsErrorCount() {
        return processedObjectsErrorCount;
    }

    public void setProcessedObjectsErrorCount(int processedObjectsErrorCount) {
        this.processedObjectsErrorCount = processedObjectsErrorCount;
    }

    public OperationResultStatus getProcessedObjectsStatus() {
        return processedObjectsStatus;
    }

    public void setProcessedObjectsStatus(OperationResultStatus processedObjectsStatus) {
        this.processedObjectsStatus = processedObjectsStatus;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getProgressLabel() {
        return progressLabel;
    }

    public void setProgressLabel(String progressLabel) {
        this.progressLabel = progressLabel;
    }

    public OperationResultStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(OperationResultStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public LocalizableMessage getTaskHealthStatusMessage() {
        return taskHealthStatusMessage;
    }

    public void setTaskHealthStatusMessage(LocalizableMessage taskHealthStatusMessage) {
        this.taskHealthStatusMessage = taskHealthStatusMessage;
    }

    public TaskExecutionStateType getExecutionState() {
        return executionState;
    }

    public void setExecutionState(TaskExecutionStateType executionState) {
        this.executionState = executionState;
    }

    public OperationResultStatus getTaskHealthStatus() {
        return taskHealthStatus;
    }

    public void setTaskHealthStatus(OperationResultStatus taskHealthStatus) {
        this.taskHealthStatus = taskHealthStatus;
    }
}
