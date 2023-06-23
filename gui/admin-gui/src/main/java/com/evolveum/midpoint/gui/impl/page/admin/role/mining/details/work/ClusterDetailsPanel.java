/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work;

import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleMiningOperationPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.ClusteringObjectMapped;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.getScaleScript;

public class ClusterDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";
    private static final String ID_DATATABLE = "datatable_extra";

    public ClusterDetailsPanel(String id, IModel<String> messageModel,
            List<ClusteringObjectMapped> mappedUsers, List<String> occupiedRoles) {
        super(id, messageModel);
        initLayout(mappedUsers, occupiedRoles);
    }

    private void initLayout(List<ClusteringObjectMapped> mappedUsers, List<String> occupiedRoles) {

        Component table = new RoleMiningOperationPanel(ID_DATATABLE, mappedUsers, occupiedRoles, false)
                .add(new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                target.appendJavaScript(getScaleScript());

            }

            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));

            }
        });

        table.add(new AjaxEventBehavior("change") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                target.appendJavaScript(getScaleScript());
            }
        });
        add(table);

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
        return 1700;
    }

    @Override
    public int getHeight() {
        return 1100;
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
        return new StringResourceModel("Details.panel");
    }


}
