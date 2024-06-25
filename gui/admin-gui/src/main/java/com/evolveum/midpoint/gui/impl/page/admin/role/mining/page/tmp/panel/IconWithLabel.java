/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

public class IconWithLabel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TEXT = "label";
    private static final String ID_ICON = "icon";

    private static final String ID_SUB_COMPONENT = "subComponent";

    public IconWithLabel(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", getComponentCssClass()));

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.replace("class", getIconCssClass()));
        image.add(AttributeModifier.replace("style", getIconComponentCssStyle()));
        image.setOutputMarkupId(true);
        add(image);

        Component subComponent = getSubComponent(ID_SUB_COMPONENT);
        subComponent.setOutputMarkupId(true);
        add(subComponent);

        if (isLink()) {
            AjaxLinkPanel components = new AjaxLinkPanel(ID_TEXT, getModel()) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    onClickPerform(target);
                }
            };
            components.setOutputMarkupId(true);
            add(components);
        } else {
            Label label = new Label(ID_TEXT, getModel());
            label.setOutputMarkupId(true);
            add(label);
        }

    }

    protected String getComponentCssClass() {
        return "d-flex align-items-center";
    }

    protected void onClickPerform(AjaxRequestTarget target) {

    }

    protected boolean isLink() {
        return false;
    }

    protected String getIconCssClass() {
        return "";
    }

    protected String getIconComponentCssStyle() {
        return null;
    }

    protected Component getSubComponent(String id) {
        WebMarkupContainer container = new WebMarkupContainer(id);
        container.add(AttributeModifier.remove("class"));
        return container;
    }
}
