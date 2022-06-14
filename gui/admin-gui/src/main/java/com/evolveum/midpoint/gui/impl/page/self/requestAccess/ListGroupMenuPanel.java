/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListGroupMenuPanel extends BasePanel<List<ListGroupMenuItem>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    public ListGroupMenuPanel(String id, IModel<List<ListGroupMenuItem>> model) {
        super(id, model);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "ul");
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "list-group-menu"));

        ListView<ListGroupMenuItem> items = new ListView<>(ID_ITEMS, getModel()) {

            @Override
            protected void populateItem(ListItem<ListGroupMenuItem> item) {
                ListGroupMenuItemPanel menu = new ListGroupMenuItemPanel(ID_ITEM, item.getModel());
                item.add(menu);
            }
        };
        add(items);
    }

    // todo implement
}
