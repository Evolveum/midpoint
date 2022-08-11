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

    public MenuItemLinkPanel(String id, IModel<ListGroupMenuItem<T>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        setRenderBodyOnly(true);

        AjaxLink link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target, MenuItemLinkPanel.this.getModelObject());
            }
        };
        link.add(AttributeAppender.append("class", () -> getModelObject().isActive() ? "active" : null));
        link.add(AttributeAppender.append("class", () -> getModelObject().isDisabled() ? "disabled" : null));
        link.setOutputMarkupId(true);
        add(link);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class",
                () -> StringUtils.isNotEmpty(getModelObject().getIconCss()) ? getModelObject().getIconCss() : "far fa-fw fa-circle"));
        link.add(icon);

        Label label = new Label(ID_LABEL, () -> {
            String key = getModelObject().getLabel();
            return getString(key, null, key);
        });
        link.add(label);

        Label badge = new Label(ID_BADGE, () -> getModelObject().getBadge());
        badge.add(AttributeAppender.replace("class", () -> getModelObject().getBadgeCss()));
        badge.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getBadge())));
        link.add(badge);

        WebMarkupContainer chevron = new WebMarkupContainer(ID_CHEVRON);
        chevron.add(AttributeAppender.append("class",
                () -> getModelObject().isActive() ? "fa fa-chevron-down" : "fa fa-chevron-left"));
        chevron.add(new VisibleBehaviour(() -> {
            ListGroupMenuItem item = getModelObject();
            return StringUtils.isEmpty(item.getBadge()) && !item.isEmpty();
        }));
        link.add(chevron);
    }

    protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {

    }
}
