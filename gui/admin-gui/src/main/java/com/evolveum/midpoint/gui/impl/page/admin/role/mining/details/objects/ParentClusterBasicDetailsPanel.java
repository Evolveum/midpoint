/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;

public class ParentClusterBasicDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";


    public ParentClusterBasicDetailsPanel(String id, IModel<String> messageModel, IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClose(target);
            }
        };
        add(cancelButton);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }


    @Override
    public int getWidth() {
        return 1400;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("RoleMining.todo.title");
    }
}
