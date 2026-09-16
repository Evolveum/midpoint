/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * Popup panel that displays progress of a running task.
 *
 * <p>Shows a title/subtitle, current task status, elapsed time, and progress information.
 * The panel periodically refreshes itself and updates footer buttons (stop / show results).
 * When the task is finished, the auto-refresh is stopped and the "Show results" action becomes enabled.</p>
 *
 * <p>The "Stop task" action requests cancellation of the underlying task via {@link SmartIntegrationService}.
 * The "See results" action is abstract and should be implemented by concrete subclasses.
 */
public abstract class SmartTaskProgressPanel extends BasePanel<TaskType> implements Popupable {

    private static final String ID_CONTAINER = "container";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_STOP = "stop";
    private static final String ID_NAVIGATE_TO_TASK = "navigateToTask";
    private static final String ID_SHOW_RESULT = "showResult";

    private final IModel<String> titleModel;
    private final IModel<String> subtitleModel;

    private Fragment footer;
    private SmartTaskProgressContentPanel contentPanel;

    public SmartTaskProgressPanel(
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

        contentPanel = new SmartTaskProgressContentPanel(
                ID_CONTAINER,
                titleModel,
                subtitleModel,
                getModel()) {

            @Override
            protected boolean showResultAfterCompletion() {
                return SmartTaskProgressPanel.this.showResultAfterCompletion();
            }

            @Override
            protected void onShowResults(AjaxRequestTarget target) {
                SmartTaskProgressPanel.this.onShowResults(target);
            }

            @Override
            protected void onProgressUpdated(AjaxRequestTarget target) {
                target.add(getFooter());
            }
        };
        add(contentPanel);

        initFooter();
    }

    protected boolean showResultAfterCompletion() {
        return false;
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);

        AjaxIconButton stopButton = new AjaxIconButton(ID_STOP, Model.of(""), getStopButtonLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onStop(target);
            }
        };
        stopButton.showTitleAsLabel(true);
        footer.add(stopButton);

        footer.add(buildViewTaskButton());

        AjaxIconButton showButton = new AjaxIconButton(
                ID_SHOW_RESULT,
                Model.of(""),
                createStringResource("SmartTaskProgressPanel.button.showResults")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowResults(target);
            }
        };
        showButton.showTitleAsLabel(true);
        showButton.add(new EnableBehaviour(this::isShowResultsEnable));
        footer.add(showButton);

        add(footer);
    }

    private @NotNull AjaxIconButton buildViewTaskButton() {
        AjaxIconButton button = new AjaxIconButton(
                ID_NAVIGATE_TO_TASK,
                Model.of(""),
                createStringResource("SmartTaskProgressPanel.button.navigateToTask")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                TaskType task = SmartTaskProgressPanel.this.getModelObject();
                DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, task.getOid(), this, false);
            }
        };
        button.showTitleAsLabel(true);
        return button;
    }

    protected boolean isShowResultsEnable() {
        return contentPanel != null && contentPanel.getTaskExecutionProgress().isComplete();
    }

    protected IModel<String> getStopButtonLabel() {
        return createStringResource("SmartTaskProgressPanel.button.stop.task");
    }

    protected void onStop(AjaxRequestTarget target) {
        String token = getModelObject().getOid();
        Task task = getPageBase().createSimpleTask("SmartTaskProgressPanel.onStop");
        OperationResult result = task.getResult();

        try {
            getPageBase().getSmartIntegrationService()
                    .cancelRequest(token, 2000L, task, result);
        } catch (CommonException e) {
            result.recordFatalError("Couldn't suspend task: " + e.getMessage(), e);
            result.computeStatus();
            getPageBase().showResult(result);
        }

        getPageBase().hideMainPopup(target);
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    protected abstract void onShowResults(AjaxRequestTarget target);

    @Override public int getWidth() { return 40; }
    @Override public int getHeight() { return 50; }
    @Override public String getWidthUnit() { return "%"; }
    @Override public String getHeightUnit() { return "%"; }
    @Override public IModel<String> getTitle() { return Model.of(); }
    @Override public Component getContent() { return this; }
}
