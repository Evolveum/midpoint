/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;

/**
 * Created by Honchar.
 *
 * class is created based on the ConfirmationDialog. ConfirmationPanel panel is
 * to be added to main popup (from PageBase class) as a content
 */
public class ConfirmationPanel extends BasePanel<String> implements Popupable {

    private static final long serialVersionUID = 1L;
    private static final String ID_PANEL = "panel";
    private static final String ID_CONFIRM_TEXT = "confirmText";
    private static final String ID_YES = "yes";
    private static final String ID_NO = "no";

    public ConfirmationPanel(String id) {
        this(id, null);
    }

    public ConfirmationPanel(String id, IModel<String> message) {
        super(id, message);

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer panel = new WebMarkupContainer(ID_PANEL);

        Label label = new Label(ID_CONFIRM_TEXT, getModel());
        label.setEscapeModelStrings(true);
        panel.add(label);

        AjaxButton yesButton = new AjaxButton(ID_YES, createYesLabel()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // todo this is wrong, this panel shouldn't know about it being used as content for dialog, also this hideMainPopup should be probably part of yesPerformed
                getPageBase().hideMainPopup(target);
                yesPerformed(target);
            }
        };
        panel.add(yesButton);

        AjaxButton noButton = new AjaxButton(ID_NO, createNoLabel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        panel.add(noButton);
        customInitLayout(panel);
        add(panel);
    }

    protected void customInitLayout(WebMarkupContainer panel) {

    }

    public void yesPerformed(AjaxRequestTarget target) {
    }

    public void noPerformed(AjaxRequestTarget target) {
        // todo this is wrong, this panel shouldn't know about it being used as content for dialog
        getPageBase().hideMainPopup(target);
    }

    @Override
    public int getWidth() {
        return 350;
    }

    @Override
    public int getHeight() {
        return 150;
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
    public IModel<String> getTitle() {
        return createStringResource("pageUsers.message.confirmActionPopupTitle");
    }

    @Override
    public Component getContent() {
        return this;
    }

    protected IModel<String> createYesLabel() {
        return createStringResource("confirmationDialog.yes");
    }

    protected IModel<String> createNoLabel() {
        return createStringResource("confirmationDialog.no");
    }

}
