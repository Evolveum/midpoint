/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.TextPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * @author honchar
 */
public abstract class SpecialPopoverSearchPanel<T> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TEXT_FIELD = "valueTextField";
    private static final String ID_EDIT_BUTTON = "editButton";
    private static final String ID_POPOVER_PANEL = "popoverPanel";
    private static final String ID_POPOVER = "popover";

    public SpecialPopoverSearchPanel(String id) {
        super(id);
    }

    public SpecialPopoverSearchPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        TextPanel<String> textField = new TextPanel<String>(ID_TEXT_FIELD, getTextValue());
        textField.setOutputMarkupId(true);
        textField.add(AttributeAppender.append("title", getTextValue().getObject()));
        textField.setEnabled(false);
        add(textField);

        AjaxButton setDateButton = new AjaxButton(ID_EDIT_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                togglePopover(target, SpecialPopoverSearchPanel.this.get(ID_TEXT_FIELD),
                        SpecialPopoverSearchPanel.this.get(ID_POPOVER), 0);
            }
        };
        setDateButton.setOutputMarkupId(true);
        add(setDateButton);

        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        WebMarkupContainer searchPopupPanel = createPopupPopoverPanel(ID_POPOVER_PANEL);
        popover.add(searchPopupPanel);

    }

    protected abstract IModel<String> getTextValue();

    protected abstract SpecialPopoverSearchPopupPanel createPopupPopoverPanel(String id);

    public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
        StringBuilder script = new StringBuilder();
        script.append("toggleSearchPopover('");
        script.append(button.getMarkupId()).append("','");
        script.append(popover.getMarkupId()).append("',");
        script.append(paddingRight).append(");");

        target.appendJavaScript(script.toString());
    }
}
