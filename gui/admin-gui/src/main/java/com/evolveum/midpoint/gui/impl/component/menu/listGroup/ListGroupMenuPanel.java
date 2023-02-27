/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.menu.listGroup;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListGroupMenuPanel<T extends Serializable> extends BasePanel<ListGroupMenu<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
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
        setOutputMarkupId(true);

        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.add(new VisibleBehaviour(() -> {
            ListGroupMenu menu = getModelObject();
            return menu.getTitle() != null || menu.getIconCss() != null;
        }));
        add(header);

        WebComponent icon = new WebComponent(ID_ICON);
        icon.add(new VisibleBehaviour(() -> getModelObject().getIconCss() != null));
        icon.add(AttributeAppender.replace("class", () -> getModelObject().getIconCss()));
        header.add(icon);

        Label title = new Label(ID_TITLE, () -> {
            String text = getModel().getObject().getTitle();
            return getString(text);
        });
        header.add(title);

        ListView<ListGroupMenuItem<T>> items = new ListView<>(ID_ITEMS, () -> getModelObject().getItems()) {

            @Override
            protected void populateItem(ListItem<ListGroupMenuItem<T>> item) {
                ListGroupMenuItem dto = item.getModelObject();

                if (dto instanceof CustomListGroupMenuItem) {
                    CustomListGroupMenuItem<T> custom = (CustomListGroupMenuItem) dto;
                    item.add(custom.createMenuItemPanel(
                            ID_ITEM, item.getModel(), (target, i) -> ListGroupMenuPanel.this.onMenuClickPerformed(target, i)));
                    return;
                }

                ListGroupMenuItemPanel menu = new ListGroupMenuItemPanel(ID_ITEM, item.getModel(), 0) {

                    @Override
                    protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                        ListGroupMenuPanel.this.onMenuClickPerformed(target, item);
                    }

                    @Override
                    protected void onChevronClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                        ListGroupMenuPanel.this.onChevronClickPerformed(target, item);
                    }
                };
                item.add(menu);
            }
        };
        add(items);
    }

    protected void onMenuClickPerformed(AjaxRequestTarget target, ListGroupMenuItem<T> item) {
        getModelObject().onItemClickPerformed(item);

        target.add(this);
    }

    protected void onChevronClickPerformed(AjaxRequestTarget target, ListGroupMenuItem<T> item) {
        getModelObject().onItemChevronClickPerformed(item);

        target.add(this);
    }
}
