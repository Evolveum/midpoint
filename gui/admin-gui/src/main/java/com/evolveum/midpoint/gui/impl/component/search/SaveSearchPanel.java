/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

public class SaveSearchPanel<C extends Containerable> extends BasePanel<Search<C>> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_NAME = "searchName";
    private static final String ID_BUTTONS_PANEL = "buttonsPanel";
    private static final String ID_SAVE_BUTTON = "saveButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";

    public SaveSearchPanel(String id, IModel<Search<C>> searchModel) {
        super(id, searchModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        TextField<String> nameField = new TextField(ID_SEARCH_NAME);
        nameField.setOutputMarkupId(true);
        add(nameField);

        WebMarkupContainer buttonsPanel = new WebMarkupContainer(ID_BUTTONS_PANEL);
        buttonsPanel.setOutputMarkupId(true);
        add(buttonsPanel);

        AjaxButton saveButton = new AjaxButton(ID_SAVE_BUTTON, createStringResource("PageBase.button.save")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        buttonsPanel.add(saveButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        buttonsPanel.add(cancelButton);
    }

    @Override
    public int getWidth() {
        return 500;
    }

    @Override
    public int getHeight() {
        return 400;
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
    public Component getComponent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return getPageBase().createStringResource("SaveSearchPanel.saveSearch");
    }

}
