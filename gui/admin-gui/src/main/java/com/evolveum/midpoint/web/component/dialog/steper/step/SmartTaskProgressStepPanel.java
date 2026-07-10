/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.steper.step;

import com.evolveum.midpoint.gui.impl.page.admin.task.component.SmartTaskProgressContentPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.steper.BasicPopupStepPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Popup step that displays progress information for a running background task.
 *
 * <p>The step renders a {@link SmartTaskProgressContentPanel} and periodically
 * refreshes task status, elapsed time, and progress information.</p>
 *
 * <p>Subclasses can react to task completion by overriding
 * {@link #showResultAfterCompletion()} and
 * {@link #onShowResults(AjaxRequestTarget)}.</p>
 *
 * <p>This step is commonly used after a configuration step that starts an
 * asynchronous operation.</p>
 */
public abstract class SmartTaskProgressStepPanel extends BasicPopupStepPanel<TaskType> {
    @Serial private static final long serialVersionUID = 1L;

    private final IModel<String> titleModel;
    private final IModel<String> subtitleModel;

    public SmartTaskProgressStepPanel(
            IModel<String> titleModel,
            IModel<String> subtitleModel,
            IModel<TaskType> taskModel) {
        super(taskModel);
        this.titleModel = titleModel;
        this.subtitleModel = subtitleModel;
    }

    @Override
    protected WebMarkupContainer createContentPanel(String id) {
        return new SmartTaskProgressContentPanel(id, titleModel, subtitleModel, getModel()) {

            @Override

            protected void onProgressUpdated(AjaxRequestTarget target) {
                refreshStepper(target);            }

            @Override
            protected boolean showResultAfterCompletion() {
                return SmartTaskProgressStepPanel.this.showResultAfterCompletion();
            }

            @Override
            protected void onShowResults(AjaxRequestTarget target) {
                SmartTaskProgressStepPanel.this.onShowResults(target);
            }
        };
    }

    @Override
    public boolean onSubmitPerformed(AjaxRequestTarget target) {
        onShowResults(target);
        return true;
    }

    @Override
    public IModel<String> getFinishLabel() {
        return createStringResource("SmartTaskProgressPanel.button.showResults");
    }

    public IModel<Boolean> isShowResultButtonEnabled() {
        return () -> {
            SmartTaskProgressContentPanel contentPanel = getSmartTaskProgressContentPanel();
            return contentPanel != null && contentPanel.getTaskExecutionProgress().isComplete();
        };
    }

    @Override
    public boolean isFinishButtonEnabled() {
        return isShowResultButtonEnabled().getObject();
    }

    protected SmartTaskProgressContentPanel getSmartTaskProgressContentPanel() {
        return (SmartTaskProgressContentPanel) getContentPanel();
    }

    @Override
    public void addCustomButtons(@NotNull RepeatingView buttons) {
        buttons.add(buildViewTaskButton(buttons.newChildId()));
    }

    private @NotNull AjaxIconButton buildViewTaskButton(String id) {
        AjaxIconButton button = new AjaxIconButton(
                id,
                Model.of(""),
                createStringResource("SmartTaskProgressPanel.button.navigateToTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                TaskType task = SmartTaskProgressStepPanel.this.getModelObject();
                DetailsPageUtil.dispatchToObjectDetailsPage(
                        TaskType.class, task.getOid(), this, false);
            }
        };

        button.showTitleAsLabel(true);
        button.setOutputMarkupId(true);
        button.add(AttributeModifier.append("class", "btn btn-outline-primary ms-auto"));
        return button;
    }

    @Override
    public boolean isBackButtonVisible() {
        return false;
    }

    protected boolean showResultAfterCompletion() {
        return false;
    }

    protected abstract void onShowResults(AjaxRequestTarget target);
}
