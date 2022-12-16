/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

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
public abstract class PopoverSearchPanel<T> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TEXT_FIELD = "valueTextField";
    private static final String ID_EDIT_BUTTON = "editButton";
    private static final String ID_POPOVER_PANEL = "popoverPanel";
    private static final String ID_POPOVER = "popover";

    public PopoverSearchPanel(String id) {
        super(id);
    }

    public PopoverSearchPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        TextPanel<String> textField = new TextPanel<String>(ID_TEXT_FIELD, getTextValue());
        textField.setOutputMarkupId(true);
        textField.add(AttributeAppender.append("title", getTextValue().getObject()));
        textField.setEnabled(false);
        add(textField);

        Popover popover = new Popover(ID_POPOVER) {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getPopoverReferenceComponent() {
                return PopoverSearchPanel.this.get(ID_EDIT_BUTTON);
            }
        };
        add(popover);

        AjaxButton setDateButton = new AjaxButton(ID_EDIT_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                popover.toggle(target);
            }
        };
        setDateButton.setOutputMarkupId(true);
        add(setDateButton);

        WebMarkupContainer searchPopupPanel = createPopupPopoverPanel(ID_POPOVER_PANEL);
        popover.add(searchPopupPanel);
    }

    protected abstract IModel<String> getTextValue();

    protected abstract PopoverSearchPopupPanel createPopupPopoverPanel(String id);

    public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
        StringBuilder script = new StringBuilder();
        script.append("MidPointTheme.toggleSearchPopover('");
        script.append(button.getMarkupId()).append("','");
        script.append(popover.getMarkupId()).append("',");
        script.append(paddingRight).append(");");

        target.appendJavaScript(script.toString());
    }

    public Boolean isItemPanelEnabled() {
        return true;
    }
}
