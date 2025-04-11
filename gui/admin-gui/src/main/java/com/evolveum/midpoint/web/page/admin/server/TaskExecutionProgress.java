/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.schema.util.task.TaskResultStatus;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class TaskExecutionProgress implements Serializable {

    private String executionStateMessage;

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
     * More information about task health status. E.g. errors/warnings in workers.
     */
    private List<LocalizableMessage> taskHealthUserFriendlyMessages = new ArrayList<>();

    /**
     * Overall task status.
     */
    private OperationResultStatus taskStatus;

    private TaskResultStatus taskUserFriendlyStatus;

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getProcessedObjectsErrorCount() {
        return processedObjectsErrorCount;
    }

    public OperationResultStatus getProcessedObjectsStatus() {
        return processedObjectsStatus;
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

    public OperationResultStatus getTaskStatus() {
        return taskStatus;
    }

    public LocalizableMessage getTaskHealthStatusMessage() {
        return taskHealthStatusMessage;
    }

    public TaskExecutionStateType getExecutionState() {
        return executionState;
    }

    public String getExecutionStateMessage() {
        return executionStateMessage;
    }

    public OperationResultStatus getTaskHealthStatus() {
        return taskHealthStatus;
    }

    public List<LocalizableMessage> getTaskHealthUserFriendlyMessages() {
        return taskHealthUserFriendlyMessages;
    }

    public List<String> createAllTaskHealthMessages() {
        List<LocalizableMessage> msgs = getTaskHealthUserFriendlyMessages();
        return msgs.stream()
                .map(msg -> LocalizationUtil.translateMessage(msg))
                .distinct()
                .sorted()
                .toList();
    }

    public String createSingleTaskHealthMessage() {
        List<String> translated = createAllTaskHealthMessages();
        if (translated.size() > 1) {
            return LocalizationUtil.translate("TaskExecutionPanel.moreMessages", translated.get(0));
        } else if (translated.size() == 1) {
            return translated.get(0);
        }

        LocalizableMessage msg = getTaskHealthStatusMessage();
        if (msg != null) {
            return LocalizationUtil.translateMessage(msg);
        }

        return null;
    }

    public TaskResultStatus getTaskUserFriendlyStatus() {
        return taskUserFriendlyStatus;
    }

    public static TaskExecutionProgress fromTaskInformation(TaskInformation info, PageBase page) {
        TaskExecutionProgress progress = new TaskExecutionProgress();

        progress.executionStateMessage = createExecutionStateMessage(info, page);

        progress.executionState = info.getTask().getExecutionState();
        progress.complete = info.isComplete();

        progress.progress = (int) (info.getProgress() * 100);
        progress.progressLabel = info.getProgressDescriptionShort();

        progress.processedObjectsStatus = OperationResultStatus.WARNING;
        progress.processedObjectsErrorCount = info.getAllErrors() == null ? 0 : info.getAllErrors();

        progress.taskHealthStatus = OperationResultStatus.parseStatusType(info.getTaskHealthStatus());
        progress.taskHealthStatusMessage = info.getTaskHealthDescription();
        List<LocalizableMessage> taskHealthUserFriendlyMessages = info.getTaskHealthUserFriendlyMessages();
        if (taskHealthUserFriendlyMessages != null) {
            progress.taskHealthUserFriendlyMessages = List.copyOf(taskHealthUserFriendlyMessages);
        }

        progress.taskStatus = OperationResultStatus.parseStatusType(info.getResultStatus());

        progress.taskUserFriendlyStatus = info.getTaskUserFriendlyStatus();

        return progress;
    }

    private static String createExecutionStateMessage(TaskInformation info, PageBase page) {
        TaskType task = info.getTask();

        TaskDtoExecutionState state =
                TaskDtoExecutionState.fromTaskExecutionState(
                        task.getExecutionState(), task.getNodeAsObserved() != null);
        if (state == null) {
            return null;
        }

        switch (state) {
            case RUNNING:
                String executingAt = info.getNodesDescription();
                if (StringUtils.isNotEmpty(executingAt)) {
                    return LocalizationUtil.translate("PageTasks.task.execution.runningAt", executingAt);
                }

                return LocalizationUtil.translate("PageTasks.task.execution.runningAtZeroNodes");
            case RUNNABLE:
            case RUNNING_OR_RUNNABLE:
                List<Object> localizationObjects = new ArrayList<>();
                String key = TaskTypeUtil.createScheduledToRunAgain(task, localizationObjects);
                return LocalizationUtil.translate(key, localizationObjects.toArray());
            case WAITING:
            case SUSPENDED:
            case SUSPENDING:
                return LocalizationUtil.translateEnum(state);
            case CLOSED:
                XMLGregorianCalendar completionTimestamp = task.getCompletionTimestamp();
                if (completionTimestamp == null) {
                    return LocalizationUtil.translate("PageTasks.task.execution.closed");
                }

                String date = WebComponentUtil.getShortDateTimeFormattedValue(XmlTypeConverter.toDate(completionTimestamp), page);
                return LocalizationUtil.translate("PageTasks.task.execution.closedAt", date);
        }

        return LocalizationUtil.translateEnum(state);
    }
}
