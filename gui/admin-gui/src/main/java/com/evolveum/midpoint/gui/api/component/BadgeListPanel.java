/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
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
public class BadgeListPanel extends BasePanel<List<Badge>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_BADGES = "badges";
    private static final String ID_BADGE = "badge";

    public BadgeListPanel(String id, IModel<List<Badge>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex flex-wrap align-items-center gap-2"));

        ListView<Badge> badges = new ListView<>(ID_BADGES, getModel()) {

            @Override
            protected void populateItem(ListItem<Badge> item) {
                item.add(new BadgePanel(ID_BADGE, item.getModel()));
            }
        };
        add(badges);
    }
}
