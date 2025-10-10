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
import org.apache.wicket.model.Model;
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
    private final IModel<Boolean> switchToggleModel;

    public enum SmartGenerationState {
        NOT_STARTED("fa fa-wand-magic-sparkles text-purple",
                "SmartGeneratingPanel.defaultText.notStarted",
                "SmartGeneratingPanel.defaultSubtext.notStarted"),
        IN_PROGRESS("fa fa-spinner fa-spin text-purple",
                "SmartGeneratingPanel.defaultText.inProgress",
                "SmartGeneratingPanel.defaultSubtext.inProgress"),
        FAILED("fa fa-circle-xmark text-purple",
                "SmartGeneratingPanel.defaultText.failed",
                "SmartGeneratingPanel.defaultSubtext.failed"),
        SUSPENDED("fa fa-pause text-purple",
                "SmartGeneratingPanel.defaultText.suspended",
                "SmartGeneratingPanel.defaultSubtext.suspended"),
        FINISHED("fa fa-wand-magic-sparkles text-purple",
                "SmartGeneratingPanel.defaultText.finished",
                "SmartGeneratingPanel.defaultSubtext.finished");

        private final String iconCss;
        private final String textKey;
        private final String subTextKey;

        SmartGenerationState(String iconCss, String textKey, String subTextKey) {
            this.iconCss = iconCss;
            this.textKey = textKey;
            this.subTextKey = subTextKey;
        }

        public @NotNull IModel<String> createIconModel() {
            return Model.of(iconCss);
        }

        public IModel<String> createTextModel(@NotNull PageBase pageBase, Object... args) {
            return pageBase.createStringResource(textKey, args);
        }

        public IModel<String> createSubTextModel(@NotNull PageBase pageBase, Object... args) {
            return pageBase.createStringResource(subTextKey, args);
        }
    }

    public SmartGeneratingAlertDto(
            LoadableModel<StatusInfo<?>> statusInfo,
            IModel<Boolean> switchToggleModel,
            PageBase pageBase) {
        this.statusInfo = statusInfo;
        this.switchToggleModel = switchToggleModel;
        this.taskModel = initTaskModel(statusInfo, pageBase);
    }

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

    public String getTimeElapsed() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return "-";
        }
        return formatElapsedTime(statusInfo.getObject());
    }

    public String getLastUpdatedDate() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return null;
        }
        XMLGregorianCalendar realizationEndTimestamp = statusInfo.getObject().getRealizationEndTimestamp();
        if (realizationEndTimestamp == null) {
            return null;
        }
        ZonedDateTime zonedDateTime = realizationEndTimestamp.toGregorianCalendar().toZonedDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a")
                .withZone(ZoneId.systemDefault());
        return formatter.format(zonedDateTime);
    }

    public LoadableModel<StatusInfo<?>> getStatusInfo() {
        return statusInfo;
    }

    public String getToken() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return null;
        }
        return statusInfo.getObject().getToken();
    }

    public @NotNull List<StatusRowRecord> getStatusRows(PageBase pageBase) {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return List.of();
        }
        return buildStatusRows(pageBase, statusInfo.getObject(), rejectEmptyProgress());
    }

    protected boolean rejectEmptyProgress() {
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
        return state == TaskExecutionStateType.SUSPENDED || statusInfo.getObject().isHalted();
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

    private SmartGenerationState resolveState() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return SmartGenerationState.NOT_STARTED;
        } else if (isFailed()) {
            return SmartGenerationState.FAILED;
        } else if (isSuspended()) {
            return SmartGenerationState.SUSPENDED;
        } else if (isFinished()) {
            return SmartGenerationState.FINISHED;
        } else {
            return SmartGenerationState.IN_PROGRESS;
        }
    }

    public IModel<String> getDefaultIconModel() {
        return resolveState().createIconModel();
    }

    public IModel<String> getDefaultTextModel(PageBase pageBase) {
        SmartGenerationState state = resolveState();
        Object[] args = state == SmartGenerationState.FINISHED
                ? new Object[] { getSuggestedObjectsCount() }
                : new Object[0];
        return state.createTextModel(pageBase, args);
    }

    public IModel<String> getDefaultSubTextModel(PageBase pageBase) {
        SmartGenerationState state = resolveState();
        Object[] args = null;
        if (state == SmartGenerationState.FINISHED) {
            args = new Object[] { getSuggestedObjectsCount() };
        }else if (state == SmartGenerationState.IN_PROGRESS && getSafeRow(pageBase) != null) {
            StatusRowRecord safeRow = getSafeRow(pageBase);
            args = new Object[] {safeRow.text().getObject()};
        }else if (state == SmartGenerationState.FAILED && getStatusInfo() != null) {
            StatusInfo<?> object = getStatusInfo().getObject();
            String localizedMessage = object.getLocalizedMessage();
            args = new Object[] {localizedMessage != null ? localizedMessage : ""};
        }

        return state.createSubTextModel(pageBase, args);
    }

    protected StatusRowRecord getSafeRow(PageBase pageBase) {
        List<StatusRowRecord> statusRows = getStatusRows(pageBase);
        if (!statusRows.isEmpty()) {
            return statusRows.get(statusRows.size() - 1);
        }
        return null;
    }

    private int getSuggestedObjectsCount() {
        return computeSuggestedObjectsCount(statusInfo != null ? statusInfo.getObject() : null);
    }

    public void setSuggestionDisplayed(Boolean suggestionDisplayed) {
        this.switchToggleModel.setObject(suggestionDisplayed);
    }

    public void removeExistingSuggestionTask(@NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("Remove suggestion task");
        OperationResult result = task.getResult();
        TaskType taskObject = getTaskObject();
        if (taskObject != null) {
            removeWholeTaskObject(pageBase, task, result, taskObject.getOid());
        }
    }

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

    public boolean isSuggestionDisplayed() {
        return switchToggleModel.getObject();
    }
}
