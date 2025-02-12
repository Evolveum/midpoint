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

public class TaskProgress implements Serializable {

    private int progress;

    private String progressLabel;

    private OperationResultStatus processedObjectsStatus;

    private int processedObjectsErrorCount;

    private OperationResultStatus taskStatus;

    private LocalizableMessage taskStatusMessage;

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

    public LocalizableMessage getTaskStatusMessage() {
        return taskStatusMessage;
    }

    public void setTaskStatusMessage(LocalizableMessage taskStatusMessage) {
        this.taskStatusMessage = taskStatusMessage;
    }
}
