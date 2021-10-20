/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
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
