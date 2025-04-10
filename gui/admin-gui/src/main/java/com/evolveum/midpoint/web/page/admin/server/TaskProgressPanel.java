/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import java.util.List;

import com.evolveum.midpoint.schema.util.task.TaskResultStatus;
import com.evolveum.midpoint.web.page.admin.server.dto.GuiTaskResultStatus;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;

public class TaskProgressPanel extends BasePanel<TaskExecutionProgress> {

    private static final String ID_PROGRESS = "progress";
    private static final String ID_RESULT_ICON = "resultIcon";
    private static final String ID_PROGRESS_LABEL = "progressLabel";
    private static final String ID_PROGRESS_PROBLEM_ICON = "progressProblemIcon";
    private static final String ID_PROGRESS_PROBLEM_LABEL = "progressProblemLabel";

    public TaskProgressPanel(String id, IModel<TaskExecutionProgress> model) {
        super(id, model);

        initLayout();
    }

    private boolean showProgressBar() {
        TaskExecutionProgress progress = getModelObject();
        if (progress.getProgress() < 0) {
            // useless for tasks that can't report on progress
            return false;
        }

        if (progress.getExecutionState() == TaskExecutionStateType.SUSPENDED
                || progress.getExecutionState() == TaskExecutionStateType.CLOSED) {
            return false;
        }

        return !getModelObject().isComplete();
    }

    private void initLayout() {
        IModel<List<ProgressBar>> progressModel = new LoadableDetachableModel<>() {

            @Override
            protected List<ProgressBar> load() {
                return List.of(new ProgressBar(getModelObject().getProgress(), ProgressBar.State.INFO));
            }
        };

        ProgressBarPanel progress = new ProgressBarPanel(ID_PROGRESS, progressModel);
        progress.add(new VisibleBehaviour(() -> showProgressBar()));
        add(progress);

        IModel<GuiTaskResultStatus> resultStatusModel = new LoadableDetachableModel<>() {

            @Override
            protected GuiTaskResultStatus load() {
                TaskResultStatus status = getModelObject().getTaskUserFriendlyStatus();
                return GuiTaskResultStatus.fromTaskResultStatus(status);
            }
        };

        WebMarkupContainer doneIcon = new WebMarkupContainer(ID_RESULT_ICON);
        doneIcon.add(
                AttributeAppender.append(
                        "class",
                        () -> resultStatusModel.getObject() != null ? resultStatusModel.getObject().icon : null));
        doneIcon.add(
                AttributeAppender.append(
                        "title",
                        () -> resultStatusModel.getObject() != null ? getString(resultStatusModel.getObject()) : null));
        doneIcon.add(new VisibleBehaviour(() -> !showProgressBar()));
        add(doneIcon);

        Label progressLabel = new Label(ID_PROGRESS_LABEL, () -> getModelObject().getProgressLabel());
        add(progressLabel);

        WebMarkupContainer progressProblemIcon = new WebMarkupContainer(ID_PROGRESS_PROBLEM_ICON);
        progressProblemIcon.add(
                AttributeAppender.append(
                        "class",
                        () -> "fa fa-exclamation-triangle " + getIconColor(getModelObject().getProcessedObjectsStatus())));
        add(progressProblemIcon);

        Label progressProblemLabel = new Label(ID_PROGRESS_PROBLEM_LABEL, () -> getModelObject().getProcessedObjectsErrorCount());
        progressProblemLabel.add(new VisibleBehaviour(() -> getModelObject().getProcessedObjectsErrorCount() > 0));
        add(progressProblemLabel);

    }

    private String createResultIcon() {
        TaskExecutionProgress data = getModelObject();
        OperationResultStatus status = data.getTaskStatus();

        return createResultIcon(data.getExecutionState(), data.isComplete(), status);
    }

    public static String createResultIcon(TaskExecutionStateType executionState, boolean complete, OperationResultStatus status) {

        if (status == null || status == OperationResultStatus.UNKNOWN) {
            return OperationResultStatusPresentationProperties.UNKNOWN.getIcon();
        }

        if (executionState == TaskExecutionStateType.SUSPENDED
                && !complete) {
            return "fa fa-circle-pause text-secondary";
        }

        return OperationResultStatusPresentationProperties.parseOperationalResultStatus(status).getIcon();
    }

    private String getIconColor(OperationResultStatus status) {
        if (status == null) {
            return "text-secondary";
        }

        return switch (status) {
            case FATAL_ERROR -> "text-danger";
            case WARNING, PARTIAL_ERROR -> "text-warning";
            case IN_PROGRESS -> "text-info";
            case SUCCESS -> "text-success";
            default -> "text-secondary";
        };
    }
}
