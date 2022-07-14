/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.List;

import org.apache.poi.ss.formula.functions.T;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListGroupMenuPanel<T extends Serializable> extends BasePanel<ListGroupMenu<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    public ListGroupMenuPanel(String id, IModel<ListGroupMenu<T>> model) {
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
        add(AttributeAppender.append("data-widget", "list-group-menu"));
        setOutputMarkupId(true);

        ListView<ListGroupMenuItem<T>> items = new ListView<>(ID_ITEMS, () -> getModelObject().getItems()) {

            @Override
            protected void populateItem(ListItem<ListGroupMenuItem<T>> item) {
                ListGroupMenuItemPanel menu = new ListGroupMenuItemPanel(ID_ITEM, item.getModel()) {

                    @Override
                    protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                        ListGroupMenuPanel.this.onMenuClickPerformed(target, item);
                    }
                };
                item.add(menu);
            }
        };
        add(items);
    }

    protected void onMenuClickPerformed(AjaxRequestTarget target, ListGroupMenuItem<T> item) {
        getModelObject().activateItem(item);

//        target.add(this);
    }
}
