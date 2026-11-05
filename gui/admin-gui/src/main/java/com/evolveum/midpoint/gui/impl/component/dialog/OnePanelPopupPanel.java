/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.dialog;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class OnePanelPopupPanel extends SimplePopupable {

    private static final String ID_PANEL = "panel";
    private static final String ID_BUTTON_DONE = "doneButton";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
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

        Component cancel = createCancelButton(ID_BUTTON_CANCEL);
        cancel.setOutputMarkupId(true);
        footer.add(cancel);

        Component done = createDoneButton(ID_BUTTON_DONE);
        done.setOutputMarkupId(true);
        footer.add(done);
        this.footer = footer;
    }

    /**
     * Creates the button that closes the popup.
     *
     * @param id component id of the button
     * @return button closing the popup
     */
    protected Component createDoneButton(String id) {
        return new AjaxButton(id) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                processHide(target);
            }
        };
    }

    /**
     * Creates the button that closes the popup without applying anything. Visible only
     * when {@link #isCancelButtonVisible()} is overridden to return true.
     *
     * @param id component id of the button
     * @return button cancelling the popup
     */
    protected Component createCancelButton(String id) {
        AjaxButton cancel = new AjaxButton(id) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                processCancel(target);
            }
        };
        cancel.add(new VisibleBehaviour(this::isCancelButtonVisible));
        return cancel;
    }

    protected boolean isCancelButtonVisible() {
        return false;
    }

    protected void processCancel(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
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
