/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeWholeTaskObject;

import java.io.Serial;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.StatusRowRecord;
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
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
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

                    if (dto.getStatusInfo() != null) {
                        dto.getStatusInfo().reset();
                    } else {
                        LOGGER.debug("StatusInfo is null for DTO {}", dto);
                    }

                    final boolean finished = dto.isFinished();
                    final boolean failed = dto.isFailed();
                    final boolean suspended = dto.isSuspended();

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

        TimerProgressPanel elapsedTime = new TimerProgressPanel(ID_ELAPSED_TIME,
                () -> getModelObject().getSuggestedObjectsStartTime(),
                () -> getModelObject().getSuggestedObjectsEndTime());
        bodyContainer.add(elapsedTime);

        WebMarkupContainer listViewContainer = new WebMarkupContainer(ID_LIST_VIEW_CONTAINER);
        listViewContainer.setOutputMarkupId(true);
        listViewContainer.add(new VisibleBehaviour(this::isListViewVisible));
        bodyContainer.add(listViewContainer);

        ListView<StatusRowRecord> listView = createStatusListView();
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
        titleIcon.add(AttributeModifier.append("class", () -> {
            SmartGeneratingDto dto = getModelObject();
            return !dto.isFailed() && !dto.isSuspended() ? getIconSpecialEffectCss() : null;
        }));
        panelContainer.add(titleIcon);

        initTitleTextPanel(panelContainer);

        Label subTitle = new Label(ID_SUBTEXT, getSubTitleModel());
        subTitle.setOutputMarkupId(true);
        panelContainer.add(subTitle);
    }

    private void initTitleTextPanel(@NotNull WebMarkupContainer panelContainer) {
        Component titlePanel = !isLinkTitle()
                ? new Label(ID_TEXT, getTitleModel())
                : new AjaxLinkPanel(ID_TEXT, getTitleModel()) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                navigateToTaskDetails();
            }
        };

        titlePanel.setOutputMarkupId(true);
        titlePanel.add(AttributeAppender.append("class", getTitleCssClass()));
        panelContainer.add(titlePanel);
    }

    private void navigateToTaskDetails() {
        SmartGeneratingDto modelObject = SmartGeneratingPanel.this.getModelObject();
        String taskToken = modelObject.getToken();
        DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, taskToken, this, false);
    }

    /**
     * Override to provide special effect CSS classes for the icon.
     * Effects are applied only when the task is running (not failed or suspended).
     *
     * @return CSS class string, e.g. "fa-spin", "fa-pulse", "spinner-grow-slow" or "spinner-blur-slow" (FontAwesome classes)
     */
    protected String getIconSpecialEffectCss() {
        return "spinner-fade-fast";
    }

    protected @Nullable String getTitleCssClass() {
        return isWizardPanel ? "h3" : "h5";
    }

    private @NotNull ListView<StatusRowRecord> createStatusListView() {

        IModel<List<StatusRowRecord>> rowsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<StatusRowRecord> load() {
                return getSafeRows();
            }
        };

        ListView<StatusRowRecord> listView = new ListView<>(ID_LIST_VIEW, rowsModel) {
            @Override
            protected void populateItem(@NotNull ListItem<StatusRowRecord> item) {
                StatusRowRecord row = item.getModelObject();

                item.add(new Label(ID_LIST_ITEM_TEXT, row.text()));

                WebMarkupContainer icon = new WebMarkupContainer(ID_LIST_ITEM_ICON);
                icon.setOutputMarkupId(true);
                icon.add(AttributeModifier.replace("class", row.getIconCss()));
                item.add(icon);
                initInfoButton(item, row);
            }

            private void initInfoButton(
                    @NotNull ListItem<StatusRowRecord> item,
                    @NotNull StatusRowRecord row) {
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
                                label.add(AttributeModifier.append("style", "white-space: pre-line").setSeparator("; "));
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

        listView.setReuseItems(false);
        listView.setOutputMarkupId(true);
        listView.add(new VisibleBehaviour(this::isListViewVisible));
        return listView;
    }

    protected boolean isListViewVisible() {
        return !getSafeRows().isEmpty();
    }

    /** Null-safe accessor for rows. */
    protected List<StatusRowRecord> getSafeRows() {
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
        if (allowActionButton()) {
            initStopButton(buttonsView);
        }
        if (allowRerun()) {
            initReRunButton(buttonsView);
        } else {
            initDiscardButton(buttonsView);
        }
    }

    protected boolean allowActionButton() {
        return true;
    }

    private void initReRunButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton reRunButton = buildReRunButton(buttonsView);
        reRunButton.add(AttributeModifier.append("class", "btn btn-primary"));
        reRunButton.add(new VisibleBehaviour(() -> {
            SmartGeneratingDto dto = SmartGeneratingPanel.this.getModelObject();
            return dto != null && (dto.isFailed() || dto.isSuspended());
        }));
        buttonsView.add(reRunButton);
    }

    private @NotNull AjaxIconButton buildReRunButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton reRunButton = new AjaxIconButton(
                buttonsView.newChildId(),
                Model.of("fa fa-refresh"),
                createStringResource("SmartGeneratingPanel.button.reRun")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onReRunPerform(target);
            }
        };

        reRunButton.setOutputMarkupId(true);
        reRunButton.showTitleAsLabel(true);
        return reRunButton;
    }

    public void onReRunPerform(AjaxRequestTarget target) {
        SmartGeneratingDto dto = SmartGeneratingPanel.this.getModelObject();
        if (dto == null) {
            return;
        }
        TaskType taskObject = dto.getTaskObject();
        if (taskObject == null) {
            return;
        }

        TaskOperationUtils.resumeTasks(Collections.singletonList(taskObject), getPageBase());

        timerBehavior.restart(target);
        if (target != null) {
            target.add(SmartGeneratingPanel.this);
        }

    }

    protected boolean allowRerun() {
        return false;
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
                timerBehavior.stop(target);
                onRunInBackgroundPerform(target);
            }
        };

        runInBackgroundButton.setOutputMarkupId(true);
        runInBackgroundButton.showTitleAsLabel(true);
        runInBackgroundButton.add(AttributeModifier.append("class", "btn btn-light border"));
        buttonsView.add(runInBackgroundButton);
    }

    public void initDiscardButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton discardButton = buildDiscardButton(buttonsView);
        discardButton.showTitleAsLabel(true);
        discardButton.add(AttributeModifier.append("class", "btn btn-link text-danger"));
        discardButton.add(new VisibleBehaviour(() -> {
            SmartGeneratingDto dto = SmartGeneratingPanel.this.getModelObject();
            return dto != null && (dto.isFailed() || dto.isSuspended());
        }));
        buttonsView.add(discardButton);
    }

    private @NotNull AjaxIconButton buildDiscardButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton discardButton = new AjaxIconButton(
                buttonsView.newChildId(),
                Model.of("fa fa-times"),
                createStringResource("SmartGeneratingPanel.button.discard")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                discardSuggestion();
                onDiscardPerform(target);
            }
        };

        discardButton.setOutputMarkupId(true);
        return discardButton;
    }

    private void discardSuggestion() {
        Task task = getPageBase().createSimpleTask("Discard smart generating task");
        OperationResult result = task.getResult();
        SmartGeneratingDto dto = SmartGeneratingPanel.this.getModelObject();
        String token = dto.getToken();
        if (token == null) {
            return;
        }
        removeWholeTaskObject(getPageBase(), task, result, token);
    }

    public void initStopButton(@NotNull RepeatingView buttonsView) {
        AjaxIconButton actionButton = new AjaxIconButton(
                buttonsView.newChildId(),
                Model.of("fa fa-stop"),
                createStringResource("SmartGeneratingPanel.button.stop")) {

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
                    case RUNNING, RUNNABLE, WAITING -> {
                        dto.removeExistingSuggestionTask(getPageBase());
                        onDiscardPerform(target);
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
        actionButton.add(AttributeModifier.append("class", "btn btn-link col-12"));
        buttonsView.add(actionButton);
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

    protected Label getSubTextLabelPanel() {
        return (Label) get(createComponentPath(ID_PANEL_CONTAINER, ID_SUBTEXT));
    }

    protected boolean isLinkTitle() {
        return false;
    }
}
