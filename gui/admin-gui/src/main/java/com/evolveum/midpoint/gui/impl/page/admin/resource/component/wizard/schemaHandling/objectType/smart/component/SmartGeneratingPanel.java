/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class SmartGeneratingPanel extends BasePanel<SmartGeneratingDto> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_ELAPSED_TIME = "elapsedTime";

    private static final String ID_LIST_VIEW_CONTAINER = "listViewContainer";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_LIST_ITEM_ICON = "icon";
    private static final String ID_LIST_ITEM_TEXT = "text";

    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";
    private static final String ID_BUTTONS = "buttons";

    public SmartGeneratingPanel(String id, IModel<SmartGeneratingDto> model) {
        super(id, model);
        add(AttributeModifier.append("class", "p-0"));
        initLayout();
    }

    private void initLayout() {
        final WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        Label elapsedTime = new Label(ID_ELAPSED_TIME, () -> getModelObject().getTimeElapsed());
        elapsedTime.setOutputMarkupId(true);
        container.add(elapsedTime);

        WebMarkupContainer listViewContainer = new WebMarkupContainer(ID_LIST_VIEW_CONTAINER);
        listViewContainer.setOutputMarkupId(true);
        listViewContainer.add(new VisibleBehaviour(() -> {
            List<SmartGeneratingDto.StatusRow> rows = getSafeRows();
            return rows != null && !rows.isEmpty();
        }));
        container.add(listViewContainer);

        ListView<SmartGeneratingDto.StatusRow> listView = createStatusListView();
        listView.setReuseItems(false);
        listView.setOutputMarkupId(true);
        listViewContainer.add(listView);

        WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
        buttonsContainer.setOutputMarkupId(true);
        container.add(buttonsContainer);

        RepeatingView buttonsView = new RepeatingView(ID_BUTTONS);
        buttonsView.setOutputMarkupId(true);

        createButtons(buttonsView);
        buttonsContainer.add(buttonsView);

        container.add(new AbstractAjaxTimerBehavior(getRefreshInterval()) {
            @Override
            protected void onTimer(AjaxRequestTarget target) {
                final SmartGeneratingDto dto = getModelObject();
                if (dto.isFinished() || dto.isFailed()) {
                    stop(target);
                }

                if (dto.isFinished() && !dto.isFailed()) {
                    onFinishActionPerform(target);
                }
                target.add(container);
            }
        });
    }

    private @NotNull ListView<SmartGeneratingDto.StatusRow> createStatusListView() {
        return new ListView<>(SmartGeneratingPanel.ID_LIST_VIEW, () -> getModelObject().getStatusRows()) {
            @Override
            protected void populateItem(@NotNull ListItem<SmartGeneratingDto.StatusRow> item) {
                SmartGeneratingDto.StatusRow row = item.getModelObject();

                item.add(new Label(ID_LIST_ITEM_TEXT, row.text()));

                WebMarkupContainer icon = new WebMarkupContainer(ID_LIST_ITEM_ICON);
                icon.setOutputMarkupId(true);
                String iconCss = row.getIconCss();
                icon.add(AttributeModifier.replace("class", iconCss));
                item.add(icon);

                item.add(AttributeModifier.append("class", "status-row"));
            }
        };
    }

    /** Null-safe accessor for rows. */
    private List<SmartGeneratingDto.StatusRow> getSafeRows() {
        SmartGeneratingDto dto = getModelObject();
        return dto != null ? dto.getStatusRows() : Collections.emptyList();
    }

    /** Polling interval; override if you want a different cadence. */
    protected Duration getRefreshInterval() {
        return Duration.ofSeconds(1);
    }

    /**
     * Override this method to create custom buttons.
     * The default implementation creates a close button that does nothing.
     */
    //TODO: we dont wanna access task in gui (need to be moved to service layer)
    protected void createButtons(@NotNull RepeatingView buttonsView) {

        initRunInBackgroundButton(buttonsView);
        initActionButton(buttonsView);
    }

    private void initRunInBackgroundButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton runInBackgroundButton = new AjaxIconButton(
                buttonsView.newChildId(),
                Model.of("fa fa-gears"),
                getRunInBackgroundButtonLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRunInBackgroundPerform(target);
            }
        };

        runInBackgroundButton.setOutputMarkupId(true);
        runInBackgroundButton.showTitleAsLabel(true);
        runInBackgroundButton.add(AttributeModifier.append("class", "btn btn-default"));
        buttonsView.add(runInBackgroundButton);
    }

    private void initActionButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton actionButton = new AjaxIconButton(
                buttonsView.newChildId(),
                () -> {
                    SmartGeneratingDto dto = getModelObject();
                    TaskExecutionStateType executionState = dto.getTaskExecutionState();

                    switch (executionState) {
                        case RUNNING, RUNNABLE, WAITING -> {
                            return "fa fa-pause";
                        }
                        case SUSPENDED -> {
                            return "fa fa-play";
                        }
                        case CLOSED -> {
                            return "fa fa-check";
                        }
                        default -> {
                            return "fa fa-question text-muted";
                        }
                    }
                },
                () -> {
                    SmartGeneratingDto dto = getModelObject();
                    TaskExecutionStateType executionState = dto.getTaskExecutionState();
                    switch (executionState) {
                        case RUNNING, RUNNABLE, WAITING -> {
                            return createStringResource("SmartGeneratingPanel.button.suspend").getString();
                        }
                        case SUSPENDED -> {
                            return createStringResource("SmartGeneratingPanel.button.resume").getString();
                        }
                        case CLOSED -> {
                            return createStringResource("SmartGeneratingPanel.button.closed").getString();
                        }
                        default -> {
                            return createStringResource("SmartGeneratingPanel.button.unknown").getString();
                        }
                    }
                }) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                TaskType taskObject = SmartGeneratingPanel.this.getModelObject().getTaskObject();
                if (taskObject == null) {
                    return;
                }

                TaskExecutionStateType executionState = taskObject.getExecutionState();
                switch (executionState) {
                    case RUNNING, RUNNABLE, WAITING ->
                            TaskOperationUtils.suspendTasks(Collections.singletonList(taskObject), getPageBase());
                    case SUSPENDED -> TaskOperationUtils.resumeTasks(Collections.singletonList(taskObject), getPageBase());
                    default -> {
                        return;
                    }
                }

                target.add(SmartGeneratingPanel.this);
                //TODO: implement the action for the button
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();

            }
        };

        actionButton.setOutputMarkupId(true);
        actionButton.showTitleAsLabel(true);
        actionButton.add(AttributeModifier.append("class", "btn btn-link"));
        buttonsView.add(actionButton);
    }

    protected void onFinishActionPerform(AjaxRequestTarget target) {
        // Override this method to perform an action when the generation succeeds.
    }

    protected void onRunInBackgroundPerform(AjaxRequestTarget target) {
        // Override this method to perform an action when the back button is clicked.
        // The default implementation does nothing.
    }

    protected IModel<String> getRunInBackgroundButtonLabel() {
        return createStringResource("SmartGeneratingPanel.button.runInBackground");
    }
}
