/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serial;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.OperationLogCollapsedItem;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;

/**
 * Content of the log viewer drawer, see {@link OperationLogCollapsedItem}. A thin coordinator that swaps its
 * single child between {@link OperationLogListPanel} (the filterable entry list) and
 * {@link OperationLogEventDetailPanel}.
 */
public class OperationCompoundLogPanel extends BasePanel<OperationLogCollapsedItem> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT = "content";

    private final WizardModelWithParentSteps wizardModel;

    private OperationLogLevel selectedLevel;

    private ProvidedLogEntry selectedEntry;

    public OperationCompoundLogPanel(String id, IModel<OperationLogCollapsedItem> model, WizardModelWithParentSteps wizardModel) {
        super(id, model);
        this.wizardModel = wizardModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        add(new WebMarkupContainer(ID_CONTENT));
    }

    @Override
    protected void onBeforeRender() {
        addOrReplace(selectedEntry != null ? createEventPanel() : createListPanel());

        super.onBeforeRender();
    }

    private @NotNull OperationLogListPanel createListPanel() {
        return new OperationLogListPanel(ID_CONTENT, getModel(), wizardModel, selectedLevel, this::onListAction);
    }

    private @NotNull OperationLogEventDetailPanel createEventPanel() {
        return new OperationLogEventDetailPanel(ID_CONTENT, Model.of(selectedEntry), this::onBack);
    }

    private void onListAction(OperationLogListAction action, AjaxRequestTarget target) {
        if (action instanceof OperationLogListAction.LevelSelected levelSelected) {
            selectedLevel = levelSelected.level();
        } else if (action instanceof OperationLogListAction.EntrySelected entrySelected) {
            selectedEntry = entrySelected.entry();
        }
        target.add(this);
    }

    private void onBack(AjaxRequestTarget target) {
        selectedEntry = null;
        target.add(this);
    }
}
