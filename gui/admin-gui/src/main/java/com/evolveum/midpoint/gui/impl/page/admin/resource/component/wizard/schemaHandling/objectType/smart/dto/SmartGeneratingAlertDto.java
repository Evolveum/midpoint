/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.buildStatusRows;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.computeSuggestedObjectsCount;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.formatElapsedTime;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeWholeTaskObject;

/**
 * DTO backing the SmartAlertGeneratingPanel.
 * Holds start time, current suggestion statuses, and generates display rows.
 */
public class SmartGeneratingAlertDto implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final LoadableModel<StatusInfo<?>> statusInfo;
    private IModel<PrismObject<TaskType>> taskModel;
    IModel<Boolean> switchToggleModel;

    public SmartGeneratingAlertDto(
            LoadableModel<StatusInfo<?>> statusInfo,
            IModel<Boolean> switchToggleModel,
            PageBase pageBase) {
        this.statusInfo = statusInfo;
        this.switchToggleModel = switchToggleModel;
        this.taskModel = initTaskModel(statusInfo, pageBase);
    }

    /**
     * Initializes a {@link LoadableModel} that loads the {@link TaskType}
     * associated with the provided {@link StatusInfo}.
     * <p>
     * If no {@link StatusInfo} is available or the referenced task does not exist,
     * this method returns {@code null} when the model is loaded.
     */
    private IModel<PrismObject<TaskType>> initTaskModel(
            @Nullable LoadableModel<StatusInfo<?>> statusInfo,
            @NotNull PageBase pageBase) {
        this.taskModel = new LoadableModel<>() {
            @Override
            protected PrismObject<TaskType> load() {
                Task task = pageBase.createSimpleTask("Load task for status info");
                OperationResult result = task.getResult();
                if (statusInfo == null || statusInfo.getObject() == null) {
                    return null;
                }
                String token = statusInfo.getObject().getToken();
                return WebModelServiceUtils.loadObject(
                        TaskType.class, token, pageBase, task, result);
            }
        };
        return taskModel;
    }

    /**
     * Elapsed time as a human-readable string, e.g., "12s elapsed".
     */
    public String getTimeElapsed() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return "-";
        }
        return formatElapsedTime(statusInfo.getObject());
    }

    /**
     * Last updated date as a human-readable string, e.g., "Jan 3, 2024, 2:15:30 PM".
     * <p>
     * Returns {@code null} if the information is not available.
     */
    public String getLastUpdatedDate() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return null;
        }
        XMLGregorianCalendar realizationEndTimestamp = statusInfo.getObject().getRealizationEndTimestamp();
        if (realizationEndTimestamp == null) {
            return null;
        }

        ZonedDateTime zonedDateTime = realizationEndTimestamp.toGregorianCalendar()
                .toZonedDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a")
                .withZone(ZoneId.systemDefault());
        return formatter.format(zonedDateTime);
    }

    public LoadableModel<StatusInfo<?>> getStatusInfo() {
        return statusInfo;
    }

    /**
     * Token of the task associated with the current status info (task oid).
     * <p>
     * Returns {@code null} if no task is associated.
     */
    public String getToken() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return null;
        }
        return statusInfo.getObject().getToken();
    }

    /**
     * Builds a list of statusInfo rows for display in the UI.
     * Each row has a label and a done/in-progress flag.
     */
    public @NotNull List<StatusRowRecord> getStatusRows(PageBase pageBase) {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return List.of();
        }
        return buildStatusRows(pageBase, statusInfo.getObject(), addDefaultRow());
    }

    protected boolean addDefaultRow() {
        return true;
    }

    public boolean isFinished() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return false;
        }

        if (isSuspended()) {
            return true;
        }

        return statusInfo.getObject().isComplete();
    }

    public boolean isSuspended() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return false;
        }

        TaskExecutionStateType state = getTaskExecutionState();
        if (state == TaskExecutionStateType.SUSPENDED) {
            return true;
        }

        return statusInfo.getObject().isHalted();
    }

    public boolean isFailed() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return false;
        }

        return statusInfo.getObject().getStatus() == OperationResultStatusType.FATAL_ERROR;
    }

    public TaskType getTaskObject() {
        if (taskModel == null || taskModel.getObject() == null) {
            return null;
        }
        return taskModel.getObject().asObjectable();
    }

    public TaskExecutionStateType getTaskExecutionState() {
        TaskType task = getTaskObject();
        return task != null ? task.getExecutionState() : null;
    }

    public IModel<String> getDefaultMessageModel(PageBase pageBase) {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return pageBase.createStringResource("SmartGeneratingPanel.defaultMessage.notStarted");
        }

        if (isFailed()) {
            return pageBase.createStringResource("SmartGeneratingPanel.defaultMessage.failed");
        } else if (isSuspended()) {
            return pageBase.createStringResource("SmartGeneratingPanel.defaultMessage.suspended");
        } else if (isFinished()) {
            return pageBase.createStringResource("SmartGeneratingPanel.defaultMessage.finished",
                    getSuggestedObjectsCount());
        } else {
            return pageBase.createStringResource("SmartGeneratingPanel.defaultMessage.inProgress");
        }
    }

    private int getSuggestedObjectsCount() {
        return computeSuggestedObjectsCount(statusInfo != null ? statusInfo.getObject() : null);
    }

    public void setSuggestionDisplayed(Boolean suggestionDisplayed) {
        this.switchToggleModel.setObject(suggestionDisplayed);
    }

    /**
     * Removes the existing suggestion task from the backend.
     * Called when performed new suggestion.
     * <p>
     * Does nothing if no suggestion task exists.
     */
    public void removeExistingSuggestionTask(@NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("Remove suggestion task");
        OperationResult result = task.getResult();
        TaskType taskObject = getTaskObject();
        if (taskObject != null) {
            removeWholeTaskObject(pageBase, task, result, taskObject.getOid());
        }
    }

    /**
     * @return true if there is a persisted suggestion task (i.e., backend still holds state).
     */
    public boolean suggestionExists() {
        return getTaskObject() != null;
    }

    public boolean isSuggestionButtonVisible() {
        return !suggestionExists();
    }

    public boolean isRefreshButtonVisible() {
        return suggestionExists() && isFinished() &&
                (isSuggestionDisplayed() || isFailed() || getSuggestedObjectsCount() == 0);
    }

    public boolean isShowSuggestionButtonVisible() {
        return suggestionExists() && isFinished() &&
                !isSuggestionDisplayed() && !isFailed() && getSuggestedObjectsCount() > 0;
    }

    public boolean isProgressPanelVisible() {
        return suggestionExists() && !isFinished() || isFailed();
    }

    public boolean isSuggestionDisplayed() {
        return switchToggleModel.getObject();
    }
}
