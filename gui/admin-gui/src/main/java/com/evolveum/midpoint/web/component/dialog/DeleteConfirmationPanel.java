/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.page.PageBase;

public class DeleteConfirmationPanel extends ConfirmationPanel {

    public DeleteConfirmationPanel(String id) {
        super(id);
    }

    public DeleteConfirmationPanel(String id, IModel<String> message) {
        super(id, message);
    }

    @Override
    public StringResourceModel getTitle() {
        return ((PageBase)getPage()).createStringResource("AssignmentTablePanel.modal.title.confirmDeletion");
    }

}
