/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.dialog;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.dialog.SimplePopupable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.web.component.dialog.Popupable;

import org.apache.wicket.model.IModel;

public abstract class OnePanelPopupPanel extends SimplePopupable {
    private static final String ID_PANEL = "panel";
    private static final String ID_BUTTON_DONE = "doneButton";

    public OnePanelPopupPanel(String id){
        this(id, null);
    }
    public OnePanelPopupPanel(String id, IModel<String> title){
        super(id, 400, 600, title);
        initLayout();
    }

    private void initLayout(){
        add(createPanel(ID_PANEL));
        AjaxLink<Void> done = new AjaxLink<>(ID_BUTTON_DONE) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                processHide(target);
            }
        };
        add(done);
    }

    protected void processHide(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    protected Component getPanel () {
        return get(ID_PANEL);
    }

    protected abstract WebMarkupContainer createPanel(String id);
}
