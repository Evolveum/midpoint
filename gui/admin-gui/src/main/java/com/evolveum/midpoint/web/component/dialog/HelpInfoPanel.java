/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Created by Kate on 07.04.2016.
 */
public class HelpInfoPanel extends Panel implements Popupable{
    private static final String ID_HELP = "helpLabel";
    private static final String ID_BUTTON_OK = "okButton";
    private static final String ID_CONTENT = "content";

    public HelpInfoPanel(String id){
        this(id, null);
    }

    public HelpInfoPanel(String id, IModel<String> messageModel){
        super (id);
        initLayout(messageModel);
    }

    public void initLayout(final IModel<String> messageModel){
        WebMarkupContainer content = new WebMarkupContainer(ID_CONTENT);
        add(content);

        Label helpLabel = new Label(ID_HELP, messageModel);
        helpLabel.setEscapeModelStrings(false);
        content.add(helpLabel);

        AjaxLink<Void> ok = new AjaxLink<Void>(ID_BUTTON_OK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closePerformed(target);
            }
        };
        content.add(ok);
    }

    protected void closePerformed(AjaxRequestTarget target){
    }

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
    public StringResourceModel getTitle() {
        return new StringResourceModel("ChangePasswordPanel.helpPopupTitle");
    }

    @Override
    public Component getComponent() {
        return this;
    }

}
