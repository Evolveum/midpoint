/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class DeleteShadowConfirmationPanel extends ConfirmationPanel {

    public DeleteShadowConfirmationPanel(String id) {
        super(id);
    }

    public DeleteShadowConfirmationPanel(String id, IModel<String> message) {
        super(id, message);
    }

    private static final String ID_DELETE_RESOURCE_DATA = "deleteResourceData";

    private IModel<Boolean> deleteResourceData = Model.of(false);
    @Override
    public StringResourceModel getTitle() {
        return ((PageBase)getPage()).createStringResource("AssignmentTablePanel.modal.title.confirmDeletion");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        iniLayout();
    }

    private void iniLayout() {
        ToggleCheckBoxPanel simulationPanel = new ToggleCheckBoxPanel(ID_DELETE_RESOURCE_DATA,
                deleteResourceData,
                createStringResource("DeleteShadowConfirmationPanel.deleteResourceData.label"),
                createStringResource("DeleteShadowConfirmationPanel.deleteResourceData.tooltip"));
        simulationPanel.setOutputMarkupId(true);
        add(simulationPanel);
    }

    protected final boolean isDeletedResourceData(){
        return deleteResourceData.getObject();
    }
}
