/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.TextPanel;

import org.apache.wicket.model.Model;

/**
 * @author honchar
 */
public abstract class PopoverSearchPanel<T> extends BasePanel<T> {

    @Serial private static final long serialVersionUID = 1L;

    public static final String ID_TEXT_FIELD = "valueTextField";
    public static final String ID_POPOVER_PANEL = "popoverPanel";
    private static final String ID_POPOVER = "popover";

    private static final String ID_CONFIGURE = "configure";
    private static final String ID_POPOVER_SEARCH_STATUS = "popoverSearchStatus";

    private boolean isPopoverOpen = false;

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

    protected TextPanel createTextPanel(String id, IModel<String> model) {
        TextPanel<String> textField = new TextPanel<>(ID_TEXT_FIELD, getTextValue());
        textField.setOutputMarkupId(true);
        textField.add(AttributeAppender.append("title", getTextValue().getObject()));
        textField.getBaseFormComponent().add(AttributeAppender.append("readonly", "readonly"));

        return textField;
    }

    private void initLayout() {
        setOutputMarkupId(true);
        add(AttributeAppender.append("class", "d-flex align-items-center gap-1"));

        Label popoverSearchStatus = new Label(ID_POPOVER_SEARCH_STATUS, Model.of(""));
        popoverSearchStatus.setOutputMarkupId(true);
        add(popoverSearchStatus);

        TextPanel<String> textField = createTextPanel(ID_POPOVER_PANEL, getTextValue());
        add(textField);

        Popover popover = new Popover(ID_POPOVER) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Component getPopoverReferenceComponent() {
                return PopoverSearchPanel.this.get(ID_CONFIGURE);
            }

            @Override
            public void toggle(AjaxRequestTarget target) {
                super.toggle(target);
                String message;
                if (isPopoverOpen) {
                    message = getString("PopoverSearchPanel.closed");
                } else {
                    message = getString("PopoverSearchPanel.opened");
                }
                target.appendJavaScript(String.format("MidPointTheme.updateStatusMessage('%s', '%s', %d);",
                        popoverSearchStatus.getMarkupId(), message, 100));
                isPopoverOpen = !isPopoverOpen;
            }
        };
        add(popover);

        AjaxButton edit = new AjaxButton(ID_CONFIGURE) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                popover.toggle(target);
            }
        };
        edit.setOutputMarkupId(true);
        add(edit);

        WebMarkupContainer searchPopupPanel = createPopupPopoverPanel(popover);
        popover.add(searchPopupPanel);
    }

    protected abstract LoadableModel<String> getTextValue();

    protected abstract PopoverSearchPopupPanel createPopupPopoverPanel(Popover popover);

    public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
        String script = "MidPointTheme.toggleSearchPopover('"
                + button.getMarkupId() + "','"
                + popover.getMarkupId() + "',"
                + paddingRight + ");";

        target.appendJavaScript(script);
    }

    public Boolean isItemPanelEnabled() {
        return true;
    }

}
