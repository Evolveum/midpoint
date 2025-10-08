/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import java.io.Serial;
import java.time.Duration;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.smart.api.info.StatusInfo;

import com.evolveum.midpoint.web.component.dialog.SmartPermissionRecordDto;
import com.evolveum.midpoint.web.component.dialog.SmartSuggestConfirmationPanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import static com.evolveum.midpoint.web.component.dialog.SmartPermissionRecordDto.initDummyMappingPermissionData;

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

    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_SUGGESTION_INFO = "suggestionInfo";

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

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.setOutputMarkupId(true);
        icon.add(AttributeModifier.replace("class",
                () -> getModelObject().getDefaultIconModel().getObject()));
        alertContainer.add(icon);

        Label textPanel = new Label(ID_TEXT,
                LoadableDetachableModel.of(() -> getModelObject()
                        .getDefaultTextModel(getPageBase()).getObject()));
        textPanel.setOutputMarkupId(true);
        alertContainer.add(textPanel);

        Label subtextPanel = new Label(ID_SUBTEXT,
                LoadableDetachableModel.of(() -> getModelObject()
                        .getDefaultSubTextModel(getPageBase()).getObject()));
        subtextPanel.setOutputMarkupId(true);
        alertContainer.add(subtextPanel);

        initButtons(alertContainer);

        Label suggestionInfo = new Label(ID_SUGGESTION_INFO,
                LoadableDetachableModel.of(() -> createStringResource(
                        "SmartGeneratingPanel.suggestion.last.update.info",
                        getModelObject().getLastUpdatedDate()).getString()));
        suggestionInfo.setOutputMarkupId(true);
        suggestionInfo.add(new VisibleBehaviour(() -> getModelObject().getLastUpdatedDate() != null));
        alertContainer.add(suggestionInfo);

        initAjaxTimeBehaviour(alertContainer);
    }

    /** Initializes action buttons (suggest, show, refresh). */
    private void initButtons(@NotNull WebMarkupContainer primaryPanel) {
        RepeatingView buttonsView = new RepeatingView(ID_BUTTONS);

        AjaxIconButton suggestButton = new AjaxIconButton(buttonsView.newChildId(),
                Model.of("mr-2 fa fa-wand-magic-sparkles "),
                createStringResource("SmartGeneratingPanel.button.ai.suggestions.suggest")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showSuggestConfirmDialog(getPageBase(), target);
            }
        };
        suggestButton.add(AttributeModifier.append(
                "class", "bg-purple ml-auto"));
        suggestButton.showTitleAsLabel(true);
        suggestButton.add(new VisibleBehaviour(() -> getModelObject().isSuggestionButtonVisible()));
        buttonsView.add(suggestButton);

        AjaxIconButton showSuggestionsButton = new AjaxIconButton(buttonsView.newChildId(),
                Model.of("ml-2 fa fa-mouse-pointer "),
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

        AjaxIconButton refreshSuggestionButton = new AjaxIconButton(buttonsView.newChildId(),
                Model.of("fa fa-arrows-rotate"),
                createStringResource("SmartGeneratingPanel.button.ai.suggestions.refresh")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                performRefreshOperation(target);
                target.add(SmartAlertGeneratingPanel.this);
                refreshAssociatedComponents(target);
                restartTimeBehavior(target);
            }
        };
        refreshSuggestionButton.add(AttributeModifier.append("class", "bg-purple"));
        refreshSuggestionButton.showTitleAsLabel(true);
        refreshSuggestionButton.add(new VisibleBehaviour(() -> getModelObject().isRefreshButtonVisible()));
        buttonsView.add(refreshSuggestionButton);

        AjaxIconButton stopSuggestionButton = new AjaxIconButton(buttonsView.newChildId(),
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
        stopSuggestionButton.showTitleAsLabel(true);
        stopSuggestionButton.setOutputMarkupId(true);
        stopSuggestionButton.add(new VisibleBehaviour(() -> {
            LoadableModel<StatusInfo<?>> statusInfo = getModelObject().getStatusInfo();
            if (statusInfo == null || statusInfo.getObject() == null) {
                return false;
            }
            return statusInfo.getObject().isExecuting();
        }));
        buttonsView.add(stopSuggestionButton);

        primaryPanel.add(buttonsView);
    }

    /** Restarts the polling timer if it exists. */
    private void restartTimeBehavior(AjaxRequestTarget target) {
        if (timerBehavior != null) {
            try {
                timerBehavior.restart(target);
            } catch (Exception e) {
                LOGGER.debug("Unable to restart timer for {}: {}", getId(), e.getMessage());
            }
        }
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

    private void showSuggestConfirmDialog(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target) {
        SmartSuggestConfirmationPanel dialog = new SmartSuggestConfirmationPanel(
                pageBase.getMainPopupBodyId(),
                getPermissionRecordDtoIModel()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                performSuggestOperation(target);
                target.add(SmartAlertGeneratingPanel.this);
                refreshAssociatedComponents(target);
                restartTimeBehavior(target);
            }
        };
        pageBase.showMainPopup(dialog, target);
    }

    protected IModel<SmartPermissionRecordDto> getPermissionRecordDtoIModel() {
        return Model.of(new SmartPermissionRecordDto(null, initDummyMappingPermissionData()));
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
