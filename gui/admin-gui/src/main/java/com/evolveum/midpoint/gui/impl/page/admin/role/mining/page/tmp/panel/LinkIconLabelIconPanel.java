/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.api.model.LoadableModel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

public class LinkIconLabelIconPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK_CONTAINER = "linkContainer";
    private static final String ID_ICON_CONTAINER = "iconContainer";
    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "label";
    private static final String ID_SUB_COMPONENT = "subComponent";

    boolean isClicked = false;
    LoadableModel<String> iconModel = new LoadableModel<>() {
        @Contract(pure = true)
        @Override
        protected @NotNull String load() {
            //send icon class using model?
            if (isClicked) {
                return "fa fa-caret-down fa-sm";
            }
            return "fa fa-caret-right fa-sm";
        }
    };

    public LinkIconLabelIconPanel(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        add(new AjaxEventBehavior("click") {
                @Override
                protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                    isClicked = !isClicked;
                    onClickPerform(ajaxRequestTarget);
                    ajaxRequestTarget.add(getSubComponent());
                }
            }
        );

        add(AttributeModifier.append(CLASS_CSS, getComponentCssClass()));
        add(AttributeModifier.append(STYLE_CSS, getComponentCssStyle()));

        WebMarkupContainer linkContainer = new WebMarkupContainer(ID_LINK_CONTAINER);
        linkContainer.setOutputMarkupId(true);
        linkContainer.add(AttributeModifier.replace(CLASS_CSS, getLinkContainerCssClass()));
        add(linkContainer);

        WebMarkupContainer iconContainer = new WebMarkupContainer(ID_ICON_CONTAINER);
        iconContainer.setOutputMarkupId(true);
        iconContainer.add(AttributeModifier.replace(CLASS_CSS, getIconContainerCssClass()));
        iconContainer.add(AttributeModifier.replace(STYLE_CSS, getIconContainerCssStyle()));
        linkContainer.add(iconContainer);

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.replace(CLASS_CSS, getIconCssClass() + " fa-sm"));
        image.add(AttributeModifier.replace(STYLE_CSS, getIconComponentCssStyle()));
        image.setOutputMarkupId(true);
        iconContainer.add(image);

        Component subComponent = buildSubComponent();
        subComponent.setOutputMarkupId(true);
        subComponent.add(AttributeModifier.replace(STYLE_CSS, getSubComponentCssStyle()));
        linkContainer.add(subComponent);

        Component textComponent = createComponent(getModel());
        linkContainer.add(textComponent);
    }

    private @NotNull Component createComponent(IModel<String> model) {
        Component component = new Label(ID_TEXT, model);
        component.add(AttributeModifier.replace(CLASS_CSS, getLabelComponentCssClass()));
        component.setOutputMarkupId(true);
        return component;
    }

    private @NotNull Component buildSubComponent() {
        IconComponent container = new IconComponent(LinkIconLabelIconPanel.ID_SUB_COMPONENT, iconModel);
        container.setOutputMarkupId(true);
        return container;
    }

    private Component getSubComponent() {
        return get(createComponentPath(ID_LINK_CONTAINER, ID_SUB_COMPONENT));
    }

    protected void onClickPerform(AjaxRequestTarget target) {
        //perform action (override in subclass)
    }

    protected String getComponentCssClass() {
        return "d-flex align-items-center";
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

    protected String getComponentCssStyle() {
        return null;
    }

    protected String getSubComponentCssStyle() {
        return null;
    }

    protected String getLinkContainerCssClass() {
        return "d-flex gap-2 cursor-pointer align-items-center";
    }
}
