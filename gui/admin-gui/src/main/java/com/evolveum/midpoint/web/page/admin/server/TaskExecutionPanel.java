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
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;

public class TaskExecutionPanel extends BasePanel<TaskExecutionProgress> {

    private static final String ID_EXECUTION_STATE = "executionState";
    private static final String ID_TASK_PROBLEM_ICON = "taskProblemIcon";
    private static final String ID_TASK_PROBLEM_LABEL = "taskProblemLabel";

    public TaskExecutionPanel(String id, IModel<TaskExecutionProgress> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        Label executionState = new Label(ID_EXECUTION_STATE, () -> getModelObject().getExecutionStateMessage());
        add(executionState);

        WebMarkupContainer taskProblemIcon = new WebMarkupContainer(ID_TASK_PROBLEM_ICON);
        taskProblemIcon.add(
                AttributeAppender.append("class", () -> "fa fa-exclamation-triangle text-warning"));  // currently fixed icon
        add(taskProblemIcon);

        IModel<String> taskProblemModel = new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                return getModelObject().createSingleTaskHealthMessage();
            }
        };

        Label taskProblemLabel = new Label(ID_TASK_PROBLEM_LABEL, taskProblemModel);
        taskProblemLabel.add(new VisibleBehaviour(() -> showTaskHealth() && StringUtils.isNotEmpty(taskProblemModel.getObject())));
        taskProblemLabel.add(
                AttributeAppender.append("title",
                        () -> {
                            List<String> translated = getModelObject().createAllTaskHealthMessages();

                            return String.join("\n", translated);
                        }));
        taskProblemLabel.add(new TooltipBehavior());
        add(taskProblemLabel);
    }

    private boolean showTaskHealth() {
        TaskExecutionProgress execution = getModelObject();

        OperationResultStatus health = execution.getTaskHealthStatus();
        return health != null && health != OperationResultStatus.SUCCESS;
    }
}
