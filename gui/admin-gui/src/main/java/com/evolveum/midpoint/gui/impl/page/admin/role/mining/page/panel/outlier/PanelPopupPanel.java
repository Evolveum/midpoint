/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class PanelPopupPanel extends BasePanel<String> implements Popupable {

    private static final String ID_PANEL = "panel";

    public PanelPopupPanel(String id, IModel<String> messageModel) {
        super(id, messageModel);

        initLayout();
    }

    public void initLayout() {
        Component componentPanel = getComponentPanel(ID_PANEL);
        componentPanel.setOutputMarkupId(true);
        componentPanel.setOutputMarkupId(true);
        add(componentPanel);
    }

    protected Component getComponentPanel(String idPanel) {
        return new WebMarkupContainer(ID_PANEL);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        //TODO
        return null;
    }
}
