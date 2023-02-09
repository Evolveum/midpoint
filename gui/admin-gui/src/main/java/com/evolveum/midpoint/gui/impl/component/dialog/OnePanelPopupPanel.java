/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.dialog;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.web.component.dialog.Popupable;

public abstract class OnePanelPopupPanel extends BasePanel implements Popupable {
    private static final String ID_PANEL = "panel";
    private static final String ID_BUTTON_DONE = "doneButton";
    public OnePanelPopupPanel(String id){
        super(id);
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

    @Override
    public int getWidth() {
        return 400;
    }

    @Override
    public int getHeight() {
        return 600;
    }

    @Override
    public String getWidthUnit(){
        return "px";
    }

    @Override
    public String getHeightUnit(){
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }
}
