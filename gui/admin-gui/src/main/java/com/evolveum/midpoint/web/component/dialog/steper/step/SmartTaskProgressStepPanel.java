/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.steper.step;

import com.evolveum.midpoint.gui.impl.page.admin.task.component.SmartTaskProgressContentPanel;
import com.evolveum.midpoint.web.component.dialog.steper.BasicPopupStepPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

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

    protected boolean showResultAfterCompletion() {
        return false;
    }

    protected abstract void onShowResults(AjaxRequestTarget target);
}
