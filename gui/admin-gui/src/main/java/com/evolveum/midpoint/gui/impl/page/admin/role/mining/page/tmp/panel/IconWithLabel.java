/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

public class IconWithLabel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON_CONTAINER = "iconContainer";
    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "label";
    private static final String ID_SUB_COMPONENT = "subComponent";

    public IconWithLabel(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        add(AttributeModifier.append(CLASS_CSS, getComponentCssClass()));
        add(AttributeModifier.append(STYLE_CSS, getComponentCssStyle()));

        WebMarkupContainer iconContainer = new WebMarkupContainer(ID_ICON_CONTAINER);
        iconContainer.setOutputMarkupId(true);
        iconContainer.add(AttributeModifier.replace(CLASS_CSS, getIconContainerCssClass()));
        iconContainer.add(AttributeModifier.replace(STYLE_CSS, getIconContainerCssStyle()));
        iconContainer.add(new VisibleBehaviour(() -> !getIconCssClass().isEmpty()));
        add(iconContainer);

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.replace(CLASS_CSS, getIconCssClass() + " fa-sm"));
        image.add(AttributeModifier.replace(STYLE_CSS, getIconComponentCssStyle()));
        image.setOutputMarkupId(true);
        iconContainer.add(image);

        Component subComponent = getSubComponent(ID_SUB_COMPONENT);
        subComponent.setOutputMarkupId(true);
        add(subComponent);

        Component textComponent = createComponent(getModel());
        add(textComponent);
    }

    private @NotNull Component createComponent(IModel<String> model) {
        Component component = isLink() ? new AjaxLinkPanel(IconWithLabel.ID_TEXT, model) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerform(target);
            }
        }
                : new Label(IconWithLabel.ID_TEXT, model);
        component.setOutputMarkupId(true);
        component.add(AttributeModifier.replace(CLASS_CSS, getLabelComponentCssClass()));
        return component;
    }

    protected String getComponentCssClass() {
        return "d-flex align-items-center ";
    }

    protected void onClickPerform(AjaxRequestTarget target) {
    //override in subclass
    }

    protected boolean isLink() {
        return false;
    }

    protected String getIconCssClass() {
        return "";
    }

    protected String getLabelComponentCssClass() {
        return null;
    }

    protected String getIconComponentCssStyle() {
        return null;
    }

    protected String getIconContainerCssClass() {
        return null;
    }

    protected String getIconContainerCssStyle() {
        return null;
    }

    protected Component getSubComponent(String id) {
        WebMarkupContainer container = new WebMarkupContainer(id);
        container.add(AttributeModifier.remove("class"));
        return container;
    }

    protected String getComponentCssStyle() {
        return null;
    }
}
