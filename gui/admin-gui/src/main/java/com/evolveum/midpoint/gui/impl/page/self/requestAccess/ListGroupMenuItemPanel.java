/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.message.Attachment;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListGroupMenuItemPanel extends BasePanel<ListGroupMenuItem> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";
    private static final String ID_BADGE = "badge";
    private static final String ID_CHEVRON = "chevron";
    private static final String ID_CONTENT = "content";

    public ListGroupMenuItemPanel(String id, IModel<ListGroupMenuItem> model) {
        super(id, model);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "li");
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "list-group-menu"));

        AjaxLink link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target);
            }
        };
        link.add(AttributeAppender.append("class", () -> getModelObject().isActive() ? "active" : null));
        link.add(AttributeAppender.append("class", () -> getModelObject().isDisabled() ? "disabled" : null));
        add(link);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        link.add(icon);

        Label label = new Label(ID_LABEL, () -> getModelObject().getLabel());
        link.add(label);

        Label badge = new Label(ID_BADGE, () -> getModelObject().getBadge());
        badge.add(AttributeAppender.replace("class", () -> getModelObject().getBadgeCss()));
        badge.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getBadge())));
        link.add(badge);

        WebMarkupContainer chevron = new WebMarkupContainer(ID_CHEVRON);
        chevron.add(new VisibleBehaviour(() -> {
            ListGroupMenuItem item = getModelObject();
            return StringUtils.isNotEmpty(item.getBadge()) && !item.getItems().isEmpty();
        }));
        link.add(chevron);

        WebMarkupContainer content = new WebMarkupContainer(ID_CONTENT);
        add(content);
    }

    protected void onClickPerformed(AjaxRequestTarget target) {

    }
}
