/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class MenuItemLinkPanel<T extends Serializable> extends BasePanel<ListGroupMenuItem<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";
    private static final String ID_BADGE = "badge";
    private static final String ID_CHEVRON = "chevron";
    private static final String ID_CHEVRON_LINK = "chevronLink";

    public MenuItemLinkPanel(String id, IModel<ListGroupMenuItem<T>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "item"));
        add(AttributeAppender.append("class", () -> getModelObject().isActive() ? "active" : null));
        add(AttributeAppender.append("class", () -> getModelObject().isDisabled() ? "disabled" : null));
        AjaxLink link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target, MenuItemLinkPanel.this.getModelObject());
            }
        };
        link.setOutputMarkupId(true);
        add(link);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class",
                () -> StringUtils.isNotEmpty(getModelObject().getIconCss()) ? getModelObject().getIconCss() : "far fa-fw fa-circle"));
        link.add(icon);

        Label label = new Label(ID_LABEL, createLabelModel());
        label.add(new TooltipBehavior());
        label.add(AttributeAppender.append("title", createLabelModel()));
        link.add(label);

        Label badge = new Label(ID_BADGE, () -> getModelObject().getBadge());
        badge.add(AttributeAppender.replace("class", () -> getModelObject().getBadgeCss()));
        badge.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getBadge())));
        link.add(badge);

        AjaxLink chevronLink = new AjaxLink<>(ID_CHEVRON_LINK) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onChevronClickPerformed(target, MenuItemLinkPanel.this.getModelObject());
            }
        };
        chevronLink.add(new VisibleBehaviour(() -> {
            ListGroupMenuItem item = getModelObject();
            return StringUtils.isEmpty(item.getBadge()) && !item.isEmpty();
        }));
        add(chevronLink);

        WebMarkupContainer chevron = new WebMarkupContainer(ID_CHEVRON);
        chevron.add(AttributeAppender.append("class",
                () -> getModelObject().isOpen() ? "fa fa-chevron-down" : "fa fa-chevron-left"));

        chevronLink.add(chevron);
    }

    private IModel<String> createLabelModel() {
        return () -> {
            String key = getModelObject().getLabel();
            return getString(key, null, key);
        };
    }

    protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {

    }

    protected void onChevronClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {

    }
}
