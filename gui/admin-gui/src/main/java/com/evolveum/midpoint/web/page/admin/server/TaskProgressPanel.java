/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;

public class TaskProgressPanel extends BasePanel<TaskProgress> {

    private static final String ID_PROGRESS = "progress";
    private static final String ID_DONE_ICON = "doneIcon";
    private static final String ID_PROGRESS_LABEL = "progressLabel";
    private static final String ID_PROGRESS_PROBLEM_ICON = "progressProblemIcon";
    private static final String ID_PROGRESS_PROBLEM_LABEL = "progressProblemLabel";
    private static final String ID_TASK_PROBLEM_ICON = "taskProblemIcon";
    private static final String ID_TASK_PROBLEM_LABEL = "taskProblemLabel";

    public TaskProgressPanel(String id, IModel<TaskProgress> model) {
        super(id, model);

        initLayout();
    }

    private boolean showProgressBar() {
        if (getModelObject().getProgress() < 0) {
            // useless for tasks that can't report on progress
            return false;
        }

        if (getModelObject().getExecutionState() == TaskExecutionStateType.SUSPENDED
                || getModelObject().getExecutionState() == TaskExecutionStateType.CLOSED) {
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

        WebMarkupContainer doneIcon = new WebMarkupContainer(ID_DONE_ICON);
        // todo task status should be here? [this is probably ok, status few lines below should be related not to
        //  overall task status but to task subtasks status if they were fatal or something infra related]
        doneIcon.add(AttributeAppender.append("class", () -> OperationResultStatusPresentationProperties
                .parseOperationalResultStatus(getModelObject().getTaskStatus()).getIcon()));
        doneIcon.add(AttributeAppender.append("title", () -> getString(getModelObject().getTaskStatus())));
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

        WebMarkupContainer taskProblemIcon = new WebMarkupContainer(ID_TASK_PROBLEM_ICON);
        taskProblemIcon.add(
                AttributeAppender.append(
                        "class",
                        () -> "fa fa-exclamation-triangle " + getIconColor(getModelObject().getTaskStatus()))); // todo task status is also here
        add(taskProblemIcon);

        IModel<String> taskProblemModel = new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                LocalizableMessage msg = getModelObject().getTaskStatusMessage();
                if (msg == null) {
                    return null;
                }

                return LocalizationUtil.translateMessage(msg);
            }
        };

        Label taskProblemLabel = new Label(ID_TASK_PROBLEM_LABEL, taskProblemModel);
        taskProblemLabel.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(taskProblemModel.getObject())));
        add(taskProblemLabel);
    }

    private String getIconColor(OperationResultStatus status) {
        if (status == null) {
            return "text-secondary";
        }

        return switch (status) {
            case FATAL_ERROR, PARTIAL_ERROR -> "text-danger";
            case WARNING -> "text-warning";
            case IN_PROGRESS -> "text-info";
            case SUCCESS -> "text-success";
            default -> "text-secondary";
        };
    }
}
