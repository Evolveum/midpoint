/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

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
