/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeWholeTaskObject;

/**
 * Panel for monitoring and controlling a "smart generating" task.
 * <p>
 * Shows progress (elapsed time, status rows) and provides actions
 * to run in background, suspend/resume, or discard the task. Uses
 * an {@link AbstractAjaxTimerBehavior} to poll until the task
 * finishes, fails, or is suspended.
 * <p>
 * Subclasses may override hooks like
 * {@link #onFinishActionPerform(AjaxRequestTarget)},
 * {@link #onDiscardPerform(AjaxRequestTarget)},
 * {@link #onRunInBackgroundPerform(AjaxRequestTarget)} or
 * {@link #createButtons(org.apache.wicket.markup.repeater.RepeatingView)}
 * to customize behavior and appearance.
 */
public class SmartGeneratingPanel extends BasePanel<SmartGeneratingDto> {

    private static final String ID_PANEL_CONTAINER = "panelContainer";
    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_BODY_CONTAINER = "bodyContainer";
    private static final String ID_ELAPSED_TIME = "elapsedTime";

    private static final String ID_LIST_VIEW_CONTAINER = "listViewContainer";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_LIST_ITEM_ICON = "icon";
    private static final String ID_LIST_ITEM_TEXT = "text";
    private static final String ID_LIST_INFO = "info";

    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";
    private static final String ID_BUTTONS = "buttons";

    private static final Trace LOGGER = TraceManager.getTrace(SmartGeneratingPanel.class);

    boolean isWizardPanel;

    private AbstractAjaxTimerBehavior timerBehavior;

    public SmartGeneratingPanel(String id, IModel<SmartGeneratingDto> model, boolean isWizardPanel) {
        super(id, model);
        this.isWizardPanel = isWizardPanel;
        setOutputMarkupId(true);
        add(AttributeModifier.append("class", "p-0"));
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer panelContainer = new WebMarkupContainer(ID_PANEL_CONTAINER);
        panelContainer.setOutputMarkupId(true);
        panelContainer.add(AttributeModifier.append("class", isWizardPanel ? "gap-3 mt-5" : "gap-1"));
        add(panelContainer);

        initIntroPart(panelContainer);

        WebMarkupContainer bodyContainer = new WebMarkupContainer(ID_BODY_CONTAINER);
        bodyContainer.setOutputMarkupId(true);
        panelContainer.add(bodyContainer);

        initCorePart(bodyContainer);

        initAjaxTimeBehaviour(bodyContainer);
    }

    private void initAjaxTimeBehaviour(WebMarkupContainer bodyContainer) {
        timerBehavior = createSuggestionAjaxTimerBehavior(bodyContainer, getRefreshInterval(), getModel(), this::onFinishActionPerform);
        bodyContainer.add(timerBehavior);
    }

    @Contract("_, _, _, _ -> new")
    public @NotNull AbstractAjaxTimerBehavior createSuggestionAjaxTimerBehavior(
            @NotNull WebMarkupContainer bodyContainer,
            @NotNull Duration refreshDuration,
            @NotNull IModel<SmartGeneratingDto> model,
            @NotNull SerializableConsumer<AjaxRequestTarget> onFinishAction) {

        return new AbstractAjaxTimerBehavior(refreshDuration) {
            @Override
            protected void onTimer(AjaxRequestTarget target) {
                try {
                    final SmartGeneratingDto dto = model.getObject();

                    if (dto == null) {
                        stop(target);
                        return;
                    }

                    final boolean finished = dto.isFinished();
                    final boolean failed = dto.isFailed();
                    final boolean suspended = dto.isSuspended();

                    if (!finished && !failed && !suspended) {
                        if (dto.getStatusInfo() != null) {
                            dto.getStatusInfo().reset();
                        } else {
                            LOGGER.debug("StatusInfo is null for DTO {}", dto);
                        }
                    }

                    if (finished && !failed && !suspended) {
                        try {
                            onFinishAction.accept(target);
                        } catch (Exception e) {
                            LOGGER.error("Error during finishing action after generating suggestions", e);
                        } finally {
                            stop(target);
                        }
                    } else if (failed || suspended) {
                        stop(target);
                    }

                } finally {
                    target.add(bodyContainer);
                }
            }
        };
    }

    private void initCorePart(@NotNull WebMarkupContainer bodyContainer) {
        Label elapsedTime = new Label(ID_ELAPSED_TIME, () -> {
            SmartGeneratingDto dto = getModelObject();
            return dto != null ? dto.getTimeElapsed() : "";
        });
        elapsedTime.setOutputMarkupId(true);
        bodyContainer.add(elapsedTime);

        WebMarkupContainer listViewContainer = new WebMarkupContainer(ID_LIST_VIEW_CONTAINER);
        listViewContainer.setOutputMarkupId(true);
        listViewContainer.add(new VisibleBehaviour(this::isListViewVisible));
        bodyContainer.add(listViewContainer);

        ListView<SmartGeneratingDto.StatusRow> listView = createStatusListView();
        listView.setOutputMarkupId(true);
        listView.setReuseItems(false);
        listViewContainer.add(listView);

        WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
        buttonsContainer.setOutputMarkupId(true);
        bodyContainer.add(buttonsContainer);

        RepeatingView buttonsView = new RepeatingView(ID_BUTTONS);
        buttonsView.setOutputMarkupId(true);
        createButtons(buttonsView);
        buttonsContainer.add(buttonsView);
    }

    private void initIntroPart(@NotNull WebMarkupContainer panelContainer) {
        WebMarkupContainer titleIcon = new WebMarkupContainer(ID_TITLE_ICON);
        titleIcon.setOutputMarkupId(true);
        titleIcon.add(AttributeModifier.append("class", getIconCssModel()));
        panelContainer.add(titleIcon);

        AjaxLinkPanel title = new AjaxLinkPanel(ID_TEXT, getTitleModel()) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                SmartGeneratingDto modelObject = SmartGeneratingPanel.this.getModelObject();
                String taskToken = modelObject.getToken();
                DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, taskToken, this,false);
            }
        };
        title.setOutputMarkupId(true);
        title.add(AttributeAppender.append("class", getTitleCssClass()));
        panelContainer.add(title);

        Label subTitle = new Label(ID_SUBTEXT, getSubTitleModel());
        subTitle.setOutputMarkupId(true);
        panelContainer.add(subTitle);
    }

    protected @Nullable String getTitleCssClass() {
        return isWizardPanel ? "h3" : "h5";
    }

    private @NotNull ListView<SmartGeneratingDto.StatusRow> createStatusListView() {
        ListView<SmartGeneratingDto.StatusRow> listView = new ListView<>(ID_LIST_VIEW, this::getSafeRows) {
            @Override
            protected void populateItem(@NotNull ListItem<SmartGeneratingDto.StatusRow> item) {
                SmartGeneratingDto.StatusRow row = item.getModelObject();

                item.add(new Label(ID_LIST_ITEM_TEXT, row.text()));

                WebMarkupContainer icon = new WebMarkupContainer(ID_LIST_ITEM_ICON);
                icon.setOutputMarkupId(true);
                icon.add(AttributeModifier.replace("class", row.getIconCss()));
                item.add(icon);
                initInfoButton(item, row);
            }

            private void initInfoButton(
                    @NotNull ListItem<SmartGeneratingDto.StatusRow> item,
                    SmartGeneratingDto.@NotNull StatusRow row) {
                AjaxIconButton info = new AjaxIconButton(ID_LIST_INFO, Model.of("fa fa-info-circle"),
                        createStringResource("SmartGeneratingPanel.button.info")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(@NotNull AjaxRequestTarget target) {
                        StatusInfo<?> statusInfo = row.getStatusInfo();

                        HelpInfoPanel helpInfoPanel = new HelpInfoPanel(
                                getPageBase().getMainPopupBodyId(),
                                statusInfo::getLocalizedMessage) {
                            @Override
                            public StringResourceModel getTitle() {
                                return createStringResource("SmartGeneratingPanel.suggestion.details.title");
                            }

                            @Override
                            protected @NotNull Label initLabel(IModel<String> messageModel) {
                                Label label = super.initLabel(messageModel);
                                label.add(AttributeModifier.append("class", "alert alert-danger"));
                                return label;
                            }

                            @Override
                            public @NotNull Component getFooter() {
                                Component footer = super.getFooter();
                                footer.add(new VisibleBehaviour(() -> false));
                                return footer;
                            }
                        };

                        target.add(getPageBase().getMainPopup());

                        getPageBase().showMainPopup(
                                helpInfoPanel, target);
                    }
                };
                info.setOutputMarkupId(true);
                info.showTitleAsLabel(true);
                info.add(new VisibleBehaviour(row::isFailed));
                item.add(info);
            }
        };

        listView.setOutputMarkupId(true);
        listView.add(new VisibleBehaviour(this::isListViewVisible));
        return listView;
    }

    protected boolean isListViewVisible() {
        return !getSafeRows().isEmpty();
    }

    /** Null-safe accessor for rows. */
    protected List<SmartGeneratingDto.StatusRow> getSafeRows() {
        SmartGeneratingDto dto = getModelObject();
        return dto != null ? dto.getStatusRows(getPageBase()) : Collections.emptyList();
    }

    /** Polling interval; override if you want a different cadence. */
    protected static Duration getRefreshInterval() {
        return Duration.ofSeconds(1);
    }

    /**
     * Override this method to create custom buttons.
     * The default implementation creates a "Run in background" and a stateful action button.
     */
    // TODO: we don't want to access task in GUI (needs moving to service layer)
    protected void createButtons(@NotNull RepeatingView buttonsView) {
        if (allowShowInBackground()) {
            initRunInBackgroundButton(buttonsView);
        }
        initActionButton(buttonsView);
        initDiscardButton(buttonsView);
    }

    protected boolean allowShowInBackground() {
        return true;
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

    public void initDiscardButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton discardButton = new AjaxIconButton(
                buttonsView.newChildId(),
                Model.of("fa fa-times"),
                createStringResource("SmartGeneratingPanel.button.discard")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                discardSuggestion(target);
                onDiscardPerform(target);
            }
        };

        discardButton.setOutputMarkupId(true);
        discardButton.showTitleAsLabel(true);
        discardButton.add(AttributeModifier.append("class", "btn btn-link text-danger"));
        discardButton.add(new VisibleBehaviour(() -> {
            SmartGeneratingDto dto = SmartGeneratingPanel.this.getModelObject();
            return dto != null && (dto.isFailed() || dto.isSuspended());
        }));
        buttonsView.add(discardButton);
    }

    private void discardSuggestion(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask("Discard smart generating task");
        OperationResult result = task.getResult();
        SmartGeneratingDto dto = SmartGeneratingPanel.this.getModelObject();
        String token = dto.getToken();
        if (token == null) {
            return;
        }
        removeWholeTaskObject(getPageBase(), task, result, token);
    }

    public void initActionButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton actionButton = new AjaxIconButton(
                buttonsView.newChildId(),
                () -> iconCssFor(stateOf(getModelObject())),
                () -> labelFor(stateOf(getModelObject()))) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                SmartGeneratingDto dto = SmartGeneratingPanel.this.getModelObject();
                if (dto == null) {
                    return;
                }
                TaskType taskObject = dto.getTaskObject();
                if (taskObject == null) {
                    return;
                }

                TaskExecutionStateType executionState = taskObject.getExecutionState();
                if (executionState == null) {
                    return;
                }

                switch (executionState) {
                    case RUNNING, RUNNABLE, WAITING ->
                            TaskOperationUtils.suspendTasks(Collections.singletonList(taskObject), getPageBase());
                    case SUSPENDED -> {
                        TaskOperationUtils.resumeTasks(Collections.singletonList(taskObject), getPageBase());
                        if (timerBehavior != null) {
                            timerBehavior.restart(target);
                        }
                    }
                    default -> {
                        return;
                    }
                }

                target.add(SmartGeneratingPanel.this);
            }
        };

        actionButton.setOutputMarkupId(true);
        actionButton.showTitleAsLabel(true);
        actionButton.add(new VisibleBehaviour(() -> getModelObject() != null && !getModelObject().isFailed()));
        actionButton.add(AttributeModifier.append("class", "btn btn-link"));
        buttonsView.add(actionButton);
    }

    private static TaskExecutionStateType stateOf(SmartGeneratingDto dto) {
        return dto != null ? dto.getTaskExecutionState() : null;
    }

    @Contract(pure = true)
    private @NotNull String iconCssFor(TaskExecutionStateType state) {
        if (state == null) {
            return "fa fa-question text-muted";
        }
        return switch (state) {
            case RUNNING, RUNNABLE, WAITING -> "fa fa-pause";
            case SUSPENDED -> "fa fa-play";
            case CLOSED -> "fa fa-check";
        };
    }

    private String labelFor(TaskExecutionStateType state) {
        if (state == null) {
            return createStringResource("SmartGeneratingPanel.button.unknown").getString();
        }
        return switch (state) {
            case RUNNING, RUNNABLE, WAITING -> createStringResource("SmartGeneratingPanel.button.suspend").getString();
            case SUSPENDED -> createStringResource("SmartGeneratingPanel.button.resume").getString();
            case CLOSED -> createStringResource("SmartGeneratingPanel.button.closed").getString();
        };
    }

    /**
     * Action handlers - override as needed
     * Post-discard action.
     */
    protected void onDiscardPerform(AjaxRequestTarget target) {
        onFinishActionPerform(target);
    }

    protected void onFinishActionPerform(AjaxRequestTarget target) {
    }

    protected void onRunInBackgroundPerform(AjaxRequestTarget target) {
    }

    protected IModel<String> getRunInBackgroundButtonLabel() {
        return createStringResource("SmartGeneratingPanel.button.runInBackground");
    }

    protected IModel<String> getIconCssModel() {
        return Model.of("fa fa-cogs");
    }

    protected IModel<String> getTitleModel() {
        return createStringResource("SmartGeneratingSuggestionStep.wizard.step.generating.suggestion.action.text");
    }

    protected IModel<String> getSubTitleModel() {
        return createStringResource("SmartGeneratingSuggestionStep.wizard.step.generating.suggestion.action.subText");
    }
}
