/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BadgePanel extends BasePanel<List<Badge>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_BADGES = "badges";
    private static final String ID_BADGE = "badge";
    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "text";

    public BadgePanel(String id, IModel<List<Badge>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex flex-wrap align-items-center gap-2"));

        ListView<Badge> badges = new ListView<>(ID_BADGES, getModel()) {

            @Override
            protected void populateItem(ListItem<Badge> item) {
                WebMarkupContainer badge = new WebMarkupContainer(ID_BADGE);
                badge.add(AttributeAppender.append("class", () -> item.getModelObject().getCssClass()));
                badge.add(new VisibleBehaviour(() ->
                        StringUtils.isNotEmpty(item.getModelObject().getText()) || StringUtils.isNotEmpty(item.getModelObject().getIconCssClass())));

                WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
                icon.add(AttributeAppender.append("class", () -> item.getModelObject().getIconCssClass()));
                icon.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(item.getModelObject().getIconCssClass())));
                badge.add(icon);

                Label text = new Label(ID_TEXT, () -> item.getModelObject().getText());
                badge.add(text);

                item.add(badge);
            }
        };
        add(badges);
    }
}
