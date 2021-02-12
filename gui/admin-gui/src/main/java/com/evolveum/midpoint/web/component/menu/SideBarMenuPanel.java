/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.web.session.SessionStorage;

/**
 * @author Viliam Repan (lazyman)
 */
public class SideBarMenuPanel extends BasePanel<List<SideBarMenuItem>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_SIDEBAR = "sidebar";
    private static final String ID_MENU_ITEMS = "menuItems";
    private static final String ID_HEADER = "header";
    private static final String ID_NAME = "name";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    public SideBarMenuPanel(String id, IModel<List<SideBarMenuItem>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    protected void initLayout() {
        WebMarkupContainer sidebar = new WebMarkupContainer(ID_SIDEBAR);
        sidebar.setOutputMarkupId(true);
        add(sidebar);

        ListView<SideBarMenuItem> menuItems = new ListView<SideBarMenuItem>(ID_MENU_ITEMS, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<SideBarMenuItem> item) {
                item.add(createHeader(item.getModel()));
                item.add(createMenuItems(item.getModel()));
            }
        };
        menuItems.setOutputMarkupId(true);
        menuItems.setReuseItems(true);
        sidebar.add(menuItems);
    }

    private Component createHeader(IModel<SideBarMenuItem> model) {
        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onMenuClick(model);
            }
        });
        header.add(AttributeAppender.append("class", new ReadOnlyModel<>(() -> isMenuExpanded(model.getObject()) ? "" : "closed")));
        Label name = new Label(ID_NAME, new StringResourceModel("${name}",  model));
        header.add(name);
        return header;
    }

    private Component createMenuItems(IModel<SideBarMenuItem> model) {
        ListView<MainMenuItem> items = new ListView<MainMenuItem>(ID_ITEMS, new PropertyModel<>(model, SideBarMenuItem.F_ITEMS)) {

            @Override
            protected void populateItem(final ListItem<MainMenuItem> listItem) {
                MainMenuPanel item = new MainMenuPanel(ID_ITEM, listItem.getModel()) {
                    @Override
                    protected boolean isMenuExpanded() {
                        return SideBarMenuPanel.this.isMenuExpanded(model.getObject());
                    }
                };

                item.setOutputMarkupId(true);
                listItem.add(item);

            }
        };

        items.setReuseItems(true);
        return items;
    }

    private void onMenuClick(IModel<SideBarMenuItem> model) {
        SideBarMenuItem mainMenu = model.getObject();

        SessionStorage storage = getPageBase().getSessionStorage();
        Map<String, Boolean> menuState = storage.getMainMenuState();

        String menuLabel = mainMenu.getName();
        // we'll use menu label as key
        Boolean expanded = menuState.get(menuLabel);

        if (expanded == null) {
            expanded = true;
        }
        menuState.put(menuLabel, !expanded);
    }

    private boolean isMenuExpanded(SideBarMenuItem mainMenu) {
        SessionStorage storage = getPageBase().getSessionStorage();
        Map<String, Boolean> menuState = storage.getMainMenuState();

        String menuLabel = mainMenu.getName();
        // we'll use menu label as key
        Boolean expanded = menuState.get(menuLabel);

        return expanded != null ? expanded : true;
    }

}
