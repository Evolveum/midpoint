/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import java.io.Serial;
import java.time.Duration;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.StatusRowRecord;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Panel for monitoring and controlling a "smart generating" task.
 * <p>
 * Provides buttons for suggesting, refreshing, or displaying results.
 * Shows task progress with status rows and polls until the task is finished,
 * failed, or suspended using {@link AbstractAjaxTimerBehavior}.
 */
public abstract class SmartAlertGeneratingPanel extends BasePanel<SmartGeneratingAlertDto> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ALERT_CONTAINER = "alertContainer";
    private static final String ID_MESSAGE = "message";
    private static final String ID_PRIMARY_PANEL = "primaryPanel";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_STOP_SUGGESTION_BUTTON = "stopSuggestion";
    private static final String ID_SUGGESTION_INFO = "suggestionInfo";
    private static final String ID_PROGRESS_PANEL = "progressPanel";
    private static final String ID_PROGRESS_ICON = "icon";
    private static final String ID_PROGRESS_TEXT = "text";
    private static final String ID_PROGRESS_INFO = "info";

    private static final Trace LOGGER = TraceManager.getTrace(SmartAlertGeneratingPanel.class);

    private AbstractAjaxTimerBehavior timerBehavior;

    public SmartAlertGeneratingPanel(String id, IModel<SmartGeneratingAlertDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    /** Builds the component layout: container, message, buttons, progress, info, timer. */
    private void initLayout() {
        WebMarkupContainer alertContainer = new WebMarkupContainer(ID_ALERT_CONTAINER);
        alertContainer.setOutputMarkupId(true);
        alertContainer.setOutputMarkupPlaceholderTag(true);
        add(alertContainer);

        Label message = new Label(ID_MESSAGE,
                LoadableDetachableModel.of(() -> getModelObject()
                        .getDefaultMessageModel(getPageBase()).getObject()));
        message.setOutputMarkupId(true);
        alertContainer.add(message);

        WebMarkupContainer primaryPanel = new WebMarkupContainer(ID_PRIMARY_PANEL);
        primaryPanel.setOutputMarkupId(true);
        alertContainer.add(primaryPanel);

        initButtons(primaryPanel);
        initProgressInlinePanel(primaryPanel);

        Label suggestionInfo = new Label(ID_SUGGESTION_INFO,
                LoadableDetachableModel.of(() -> createStringResource(
                        "SmartGeneratingPanel.suggestion.last.update.info",
                        getModelObject().getLastUpdatedDate()).getString()));
        suggestionInfo.setOutputMarkupId(true);
        suggestionInfo.add(new VisibleBehaviour(() -> getModelObject().getLastUpdatedDate() != null));
        primaryPanel.add(suggestionInfo);

        initAjaxTimeBehaviour(alertContainer);
    }

    /** Initializes action buttons (suggest, show, refresh). */
    private void initButtons(@NotNull WebMarkupContainer primaryPanel) {
        RepeatingView buttonsView = new RepeatingView(ID_BUTTONS);

        AjaxIconButton suggestionButton = new AjaxIconButton(buttonsView.newChildId(),
                Model.of("ml-2 fa fa-wand-magic-sparkles " + getIconSpecialEffectCss()),
                createStringResource("SmartGeneratingPanel.button.ai.suggestions.suggest")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                performSuggestOperation(target);
                target.add(SmartAlertGeneratingPanel.this);
                refreshAssociatedComponents(target);
                restartTimeBehavior(target);
            }
        };
        suggestionButton.add(AttributeModifier.append(
                "class", "btn-light ml-auto d-flex flex-row-reverse"));
        suggestionButton.showTitleAsLabel(true);
        suggestionButton.add(new VisibleBehaviour(() -> getModelObject().isSuggestionButtonVisible()));
        buttonsView.add(suggestionButton);

        AjaxIconButton showSuggestionsButton = new AjaxIconButton(buttonsView.newChildId(),
                Model.of("ml-2 fa fa-mouse-pointer " + getIconSpecialEffectCss()),
                createStringResource("SmartGeneratingPanel.button.ai.suggestions.show")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                performShowSuggestOperation(target);
            }
        };
        showSuggestionsButton.add(AttributeModifier.append(
                "class", "ml-auto bg-purple d-flex flex-row-reverse"));
        showSuggestionsButton.showTitleAsLabel(true);
        showSuggestionsButton.add(new VisibleBehaviour(() -> getModelObject().isShowSuggestionButtonVisible()));
        buttonsView.add(showSuggestionsButton);

        AjaxIconButton refreshButton = new AjaxIconButton(buttonsView.newChildId(),
                Model.of("fa fa-arrows-rotate text-purple"),
                createStringResource("SmartGeneratingPanel.button.ai.suggestions.refresh")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                performRefreshOperation(target);
                target.add(SmartAlertGeneratingPanel.this);
                refreshAssociatedComponents(target);
                restartTimeBehavior(target);
            }
        };
        refreshButton.add(AttributeModifier.append("class", "btn-light"));
        refreshButton.showTitleAsLabel(true);
        refreshButton.add(new VisibleBehaviour(() -> getModelObject().isRefreshButtonVisible()));
        buttonsView.add(refreshButton);

        primaryPanel.add(buttonsView);
    }

    /** Restarts the polling timer if it exists. */
    private void restartTimeBehavior(AjaxRequestTarget target) {
        if (timerBehavior != null) {
            timerBehavior.restart(target);
        }
    }

    /** Builds the inline progress panel (status row text, icon, info button). */
    private void initProgressInlinePanel(@NotNull WebMarkupContainer primaryPanel) {
        WebMarkupContainer progressPanel = new WebMarkupContainer(ID_PROGRESS_PANEL);
        progressPanel.setOutputMarkupId(true);

        Label label = new Label(ID_PROGRESS_TEXT,
                LoadableDetachableModel.of(() -> {
                    StatusRowRecord safeRow = getSafeRow();
                    return safeRow != null ? safeRow.text().getObject() : null;
                }));
        label.setOutputMarkupId(true);
        label.setEscapeModelStrings(false);
        progressPanel.add(label);

        WebMarkupContainer icon = new WebMarkupContainer(ID_PROGRESS_ICON);
        icon.setOutputMarkupId(true);
        icon.add(AttributeModifier.replace("class",
                () -> getSafeRow() != null ? getSafeRow().getIconCss() : null));
        progressPanel.add(icon);

        initProgressActionButton(progressPanel, this::getSafeRow);

        progressPanel.add(new VisibleBehaviour(() -> getModelObject().isProgressPanelVisible()));
        primaryPanel.add(progressPanel);
    }

    //TODO we currently have functionalities for suspending and resuming tasks, but its not align with BE yet

    /** Initializes the action button in the progress panel (info or stop). */
    private void initProgressActionButton(
            @NotNull WebMarkupContainer progressPanel,
            @NotNull IModel<StatusRowRecord> statusRecord) {
        progressPanel.add(buildInfoButton(statusRecord));
        progressPanel.add(buildStopSuggestionButton(statusRecord));
    }

    /** Creates a stop button that cancels the suggestion task if it's running. */
    private @NotNull AjaxIconButton buildStopSuggestionButton(@NotNull IModel<StatusRowRecord> statusRecord) {
        AjaxIconButton stopSuggestionButton = new AjaxIconButton(
                ID_STOP_SUGGESTION_BUTTON,
                Model.of("fa fa-stop text-purple"),
                createStringResource("SmartGeneratingPanel.button.stop")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                SmartGeneratingAlertDto dto = SmartAlertGeneratingPanel.this.getModelObject();
                dto.removeExistingSuggestionTask(getPageBase());
                target.add(SmartAlertGeneratingPanel.this);
                refreshAssociatedComponents(target);
            }
        };
        stopSuggestionButton.setOutputMarkupId(true);
        stopSuggestionButton.showTitleAsLabel(true);
        stopSuggestionButton.add(new VisibleBehaviour(() -> {
            StatusRowRecord statusRow = statusRecord.getObject();
            return statusRow != null && statusRow.isExecuting();
        }));
        return stopSuggestionButton;
    }

    //TODO consider moving into abstract base class

    /** Creates an info button that shows details in a popup if the row failed. */
    private @NotNull AjaxIconButton buildInfoButton(@NotNull IModel<StatusRowRecord> row) {
        AjaxIconButton infoButton = new AjaxIconButton(
                ID_PROGRESS_INFO,
                Model.of("fa fa-info-circle"),
                createStringResource("SmartGeneratingPanel.button.info")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                StatusInfo<?> statusInfo = row.getObject().getStatusInfo();
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
                getPageBase().showMainPopup(helpInfoPanel, target);
            }
        };
        infoButton.setOutputMarkupId(true);
        infoButton.showTitleAsLabel(true);
        infoButton.add(new VisibleBehaviour(() -> {
            StatusRowRecord statusRow = row.getObject();
            return statusRow != null && statusRow.isFailed();
        }));
        return infoButton;
    }

    /** Initializes the timer polling behaviour. */
    private void initAjaxTimeBehaviour(WebMarkupContainer alertContainer) {
        this.timerBehavior = createSuggestionAjaxTimerBehavior(
                alertContainer, getRefreshInterval(), getModel(), this::onFinishActionPerform);
        alertContainer.add(timerBehavior);
    }

    /** Creates the polling timer behavior. */
    public @NotNull AbstractAjaxTimerBehavior createSuggestionAjaxTimerBehavior(
            @NotNull WebMarkupContainer bodyContainer,
            @NotNull Duration refreshDuration,
            @NotNull IModel<SmartGeneratingAlertDto> model,
            @NotNull SerializableConsumer<AjaxRequestTarget> onFinishAction) {

        return new AbstractAjaxTimerBehavior(refreshDuration) {
            @Override
            protected void onTimer(AjaxRequestTarget target) {
                try {
                    final SmartGeneratingAlertDto dto = model.getObject();

                    if (dto == null || !dto.suggestionExists()) {
                        stop(target);
                        return;
                    }

                    boolean finished = dto.isFinished();
                    boolean failed = dto.isFailed();
                    boolean suspended = dto.isSuspended();

                    if (dto.getStatusInfo() != null) {
                        dto.getStatusInfo().reset();
                    } else {
                        LOGGER.debug("StatusInfo is null for DTO {}", dto);
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

    /** Override to provide special effect CSS classes for the icon. */
    protected String getIconSpecialEffectCss() {
        return "spinner-fade-slow";
    }

    /** Returns the last safe row, or null if none exists. */
    protected StatusRowRecord getSafeRow() {
        SmartGeneratingAlertDto dto = getModelObject();
        if (dto != null) {
            List<StatusRowRecord> statusRows = dto.getStatusRows(getPageBase());
            if (!statusRows.isEmpty()) {
                return statusRows.get(statusRows.size() - 1);
            }
        }
        return null;
    }

    /** Called when task finishes successfully. Default no-op. */
    protected void onFinishActionPerform(AjaxRequestTarget target) {
        // default no-op
    }

    /** Default polling interval (1s). Override if needed. */
    protected static Duration getRefreshInterval() {
        return Duration.ofSeconds(1);
    }

    /** Shows suggestions in UI. */
    protected void performShowSuggestOperation(@NotNull AjaxRequestTarget target) {
        getModelObject().setSuggestionDisplayed(Boolean.TRUE);
        target.add(SmartAlertGeneratingPanel.this);
        refreshAssociatedComponents(target);
    }

    /** Refreshes suggestions (removes existing task and starts again). */
    protected void performRefreshOperation(AjaxRequestTarget target) {
        getModelObject().setSuggestionDisplayed(Boolean.FALSE);
        getModelObject().removeExistingSuggestionTask(getPageBase());
        performSuggestOperation(target);
    }

    /** Must be implemented to trigger suggestion generation. */
    protected abstract void performSuggestOperation(AjaxRequestTarget target);

    /** Must be implemented to refresh related UI components. */
    protected abstract void refreshAssociatedComponents(AjaxRequestTarget target);
}
