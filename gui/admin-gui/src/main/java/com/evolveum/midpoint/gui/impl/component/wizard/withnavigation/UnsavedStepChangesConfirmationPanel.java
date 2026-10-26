/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.withnavigation;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.web.component.dialog.AdditionalOperationConfirmationPanel;

/**
 * Save / Discard / Cancel confirmation shown when the user tries to leave a connector development
 * wizard step (via the "Back" button or a left-navigation step click) while the step has unsaved changes.
 */
public abstract class UnsavedStepChangesConfirmationPanel extends AdditionalOperationConfirmationPanel {

    public UnsavedStepChangesConfirmationPanel(String id) {
        super(id, PageAdminLTE.createStringResourceStatic("UnsavedStepChangesConfirmationPanel.message"));
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("UnsavedStepChangesConfirmationPanel.title");
    }

    @Override
    protected IModel<String> createYesLabel() {
        return createStringResource("UnsavedStepChangesConfirmationPanel.button.save");
    }

    @Override
    protected IModel<String> createNoLabel() {
        return createStringResource("UnsavedStepChangesConfirmationPanel.button.cancel");
    }

    @Override
    protected IModel<String> getProcessButtonLabel() {
        return createStringResource("UnsavedStepChangesConfirmationPanel.button.discard");
    }

    @Override
    public final void yesPerformed(AjaxRequestTarget target) {
        onSavePerformed(target);
    }

    @Override
    protected final void performOnProcess(AjaxRequestTarget target) {
        onDiscardPerformed(target);
    }

    protected abstract void onSavePerformed(AjaxRequestTarget target);

    protected abstract void onDiscardPerformed(AjaxRequestTarget target);
}
