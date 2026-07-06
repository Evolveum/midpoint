/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.TimerProgressPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.schema.util.task.TaskResultStatus;
import com.evolveum.midpoint.web.page.admin.server.TaskExecutionProgress;
import com.evolveum.midpoint.web.page.admin.server.dto.GuiTaskResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public abstract class SmartTaskProgressContentPanel extends BasePanel<TaskType> {

    private static final String ID_TITLE = "title";
    private static final String ID_SUBTITLE = "subtitle";
    private static final String ID_STATUS_BOX = "statusBox";
    private static final String ID_TIME_LABEL = "timeLabel";
    private static final String ID_PROGRESS_LABEL = "progressLabel";
    private static final String ID_PROGRESS_BAR = "progressBar";

    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(2);

    private final IModel<String> titleModel;
    private final IModel<String> subtitleModel;

    private boolean taskOperationCompleted;

    public SmartTaskProgressContentPanel(
            String id,
            IModel<String> titleModel,
            IModel<String> subtitleModel,
            IModel<TaskType> taskModel) {
        super(id, taskModel);
        this.titleModel = titleModel;
        this.subtitleModel = subtitleModel;
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new AjaxSelfUpdatingTimerBehavior(REFRESH_INTERVAL) {
            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                super.onPostProcessTarget(target);

                getModel().detach();

                if (taskOperationCompleted) {
                    stop(target);
                    if (showResultAfterCompletion()) {
                        onShowResults(target);
                    }
                    return;
                }

                TaskExecutionProgress progress = getTaskExecutionProgress();
                OperationResultStatus status = progress.getTaskStatus();

                boolean finished = status != OperationResultStatus.FATAL_ERROR
                        && (progress.isComplete() || status != OperationResultStatus.IN_PROGRESS);

                if (finished) {
                    taskOperationCompleted = true;
                }

                if (progress.getExecutionState().equals(TaskExecutionStateType.SUSPENDED)
                        || progress.getExecutionState().equals(TaskExecutionStateType.CLOSED)) {
                    stop(target);
                }

                target.add(SmartTaskProgressContentPanel.this);
                onProgressUpdated(target);
            }
        });

        initHeader();
        initStatus();
        initProgress();
    }

    private void initHeader() {
        IconWithLabel title = new IconWithLabel(ID_TITLE, titleModel) {

            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "";
            }

            @Override
            protected @NotNull String getIconCssClass() {
                OperationResultStatus status = getTaskExecutionProgress().getTaskStatus();
                return switch (status) {
                    case SUCCESS -> "fa-solid fa-check-circle text-success me-2 mr-1";
                    case FATAL_ERROR -> "fa-solid fa-xmark-circle text-danger me-2 mr-1";
                    case WARNING, PARTIAL_ERROR -> "fa-solid fa-triangle-exclamation text-warning me-2 mr-1";
                    default -> "";
                };
            }
        };
        title.setOutputMarkupId(true);
        add(title);

        add(new Label(ID_SUBTITLE, subtitleModel));
    }

    private void initStatus() {
        BadgePanel badgePanel = new BadgePanel(ID_STATUS_BOX, () -> {
            TaskResultStatus status = getTaskExecutionProgress().getTaskUserFriendlyStatus();
            GuiTaskResultStatus guiStatus = GuiTaskResultStatus.fromTaskResultStatus(status);
            return new Badge(guiStatus.getBadgeState().getCss(), createStringResource(guiStatus.getLabelKey()).getString());
        });
        badgePanel.setOutputMarkupId(true);
        add(badgePanel);

        TaskType modelObject = getModelObject();
        TimerProgressPanel timer = new TimerProgressPanel(
                ID_TIME_LABEL,
                () -> modelObject != null ? modelObject.getLastRunStartTimestamp() : null,
                () -> modelObject != null ? modelObject.getLastRunFinishTimestamp() : null);
        timer.setOutputMarkupId(true);
        add(timer);
    }

    private void initProgress() {
        Label progressLabel = new Label(ID_PROGRESS_LABEL, this::getProgressText);
        progressLabel.setOutputMarkupId(true);
        add(progressLabel);

        WebMarkupContainer progressBar = new WebMarkupContainer(ID_PROGRESS_BAR);
        progressBar.add(AttributeModifier.replace("class", () -> {
            OperationResultStatus status = getTaskExecutionProgress().getTaskStatus();
            return switch (status) {
                case SUCCESS -> "progress-bar bg-success";
                case FATAL_ERROR -> "progress-bar bg-danger";
                case WARNING, PARTIAL_ERROR -> "progress-bar bg-warning progress-bar-animated";
                default -> "progress-bar progress-bar-striped progress-bar-animated";
            };
        }));
        progressBar.setOutputMarkupId(true);
        add(progressBar);
    }

    private @NotNull String getProgressText() {
        String value = getTaskExecutionProgress().getProgressLabel();
        if (value == null || value.isBlank()) {
            value = "0";
        }
        return createStringResource("SmartTaskProgressPanel.progressLabel", value).getString();
    }

    @NotNull
    public TaskExecutionProgress getTaskExecutionProgress() {
        TaskInformation info = TaskInformation.createForTask(getModelObject(), null);
        return TaskExecutionProgress.fromTaskInformation(info, getPageBase());
    }

    protected void onProgressUpdated(AjaxRequestTarget target) {
    }

    protected boolean showResultAfterCompletion() {
        return false;
    }

    protected abstract void onShowResults(AjaxRequestTarget target);
}
