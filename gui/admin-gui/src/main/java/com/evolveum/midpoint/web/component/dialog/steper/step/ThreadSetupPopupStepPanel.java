/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.steper.step;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.ThreadSelectionPanel;
import com.evolveum.midpoint.web.component.dialog.steper.BasicPopupStepPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;

/**
 * Popup step that allows the user to configure the number of worker threads
 * used by a background operation.
 *
 * <p>The selected value is stored in the provided model and can be retrieved
 * using {@link #getWorkerThreads()} after the step is completed.</p>
 *
 * <p>This step is typically used before starting a background task that
 * supports configurable execution parallelism.</p>
 */
public class ThreadSetupPopupStepPanel extends BasicPopupStepPanel<Integer> {
    @Serial private static final long serialVersionUID = 1L;

    public ThreadSetupPopupStepPanel(IModel<Integer> threadsModel) {
        super(threadsModel);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("ThreadSelectionPanel.threadConfiguration.title");
    }

    @Override
    public IModel<String> getSubTitle() {
        return createStringResource("ThreadSelectionPanel.threadConfiguration.description");
    }

    @Override
    protected WebMarkupContainer createContentPanel(String id) {
        return new ThreadSelectionPanel(id, getModel());
    }

    @Override
    public boolean onSubmitPerformed(AjaxRequestTarget target) {
        return getModelObject() != null && getModelObject() > 0;
    }

    @Override
    public IModel<String> getFinishLabel() {
        return createStringResource("ThreadSetupPopupStepPanel.finish");
    }

    @Override
    public IModel<String> getFinishIcon() {
        return Model.of("fa fa-magic");
    }

    @Override
    public String getFinishCssClass() {
        return "btn-primary";
    }

    public Integer getWorkerThreads() {
        return getModelObject();
    }
}
