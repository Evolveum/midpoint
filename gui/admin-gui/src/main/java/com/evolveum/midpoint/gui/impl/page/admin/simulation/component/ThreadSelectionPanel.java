/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.component;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class ThreadSelectionPanel extends BasePanel<Integer> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_EXECUTION_THREADS = "executionThreads";
    private static final String ID_EXECUTION_THREADS_LABEL = "executionThreadsLabel";

    public ThreadSelectionPanel(String id, IModel<Integer> threadsModel) {
        super(id, threadsModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(
                ID_EXECUTION_THREADS_LABEL,
                createStringResource("ThreadSelectionPanel.worker.threads")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("ThreadSelectionPanel.threadConfiguration.description");
            }
        };
        add(labelWithHelpPanel);

        NumberTextField<Integer> executionThreads = new NumberTextField<>(
                ID_EXECUTION_THREADS,
                getModel(),
                Integer.class);

        executionThreads.setMinimum(1);

        executionThreads.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
            }
        });

        add(executionThreads);
    }
}
