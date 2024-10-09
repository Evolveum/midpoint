/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.dialog;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.web.component.dialog.Popupable;

import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class OnePanelPopupPanel extends SimplePopupable {

    private static final String ID_PANEL = "panel";
    private static final String ID_BUTTON_DONE = "doneButton";
    private static final String ID_BUTTONS = "buttons";

    private Fragment footer;

    public OnePanelPopupPanel(String id){
        this(id, null);
    }
    public OnePanelPopupPanel(String id, IModel<String> title){
        this(id, 500, 600, title);
    }

    public OnePanelPopupPanel(String id, int width, int height, IModel<String> title){
        super(id, width, height, title);
        initLayout();
        initFooter();
    }

    private void initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);

        AjaxButton done = new AjaxButton(ID_BUTTON_DONE) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                processHide(target);
            }
        };
        done.setOutputMarkupId(true);
        footer.add(done);
        this.footer = footer;
    }

    private void initLayout(){
        add(createPanel(ID_PANEL));
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    protected void processHide(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    protected Component getPanel () {
        return get(ID_PANEL);
    }

    protected abstract WebMarkupContainer createPanel(String id);
}
