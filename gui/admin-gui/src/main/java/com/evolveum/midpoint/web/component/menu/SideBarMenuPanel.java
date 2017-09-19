/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;
import java.util.Map;

/**
 * @author Viliam Repan (lazyman)
 */
public class SideBarMenuPanel extends SimplePanel<List<SideBarMenuItem>> {

    private static final String ID_SIDEBAR = "sidebar";
    private static final String ID_MENU_ITEMS = "menuItems";
    private static final String ID_NAME = "name";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_MINIMIZED_ICON = "minimizedIcon";

    public SideBarMenuPanel(String id, IModel<List<SideBarMenuItem>> model) {
        super(id, model);

        setOutputMarkupId(true);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer sidebar = new WebMarkupContainer(ID_SIDEBAR);
        sidebar.setOutputMarkupId(true);
        add(sidebar);

        ListView<SideBarMenuItem> menuItems = new ListView<SideBarMenuItem>(ID_MENU_ITEMS, getModel()) {

            @Override
            protected void populateItem(final ListItem<SideBarMenuItem> item) {
                Label name = new Label(ID_NAME, item.getModelObject().getName());
                name.add(new AjaxEventBehavior("click") {

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        SideBarMenuItem mainMenu = item.getModelObject();

                        SessionStorage storage = getPageBase().getSessionStorage();
                        Map<String, Boolean> menuState = storage.getMainMenuState();

                        String menuLabel = mainMenu.getName().getObject();
                        // we'll use menu label as key
                        Boolean expanded = menuState.get(menuLabel);

                        if (expanded == null) {
                            expanded = true;
                        }

                        menuState.put(menuLabel, !expanded);

                        target.add(sidebar);
                    }
                });
                item.add(name);

                WebMarkupContainer icon = new WebMarkupContainer(ID_MINIMIZED_ICON);
                icon.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        SideBarMenuItem mainMenu = item.getModelObject();

                        return !isMenuExpanded(mainMenu);
                    }
                });
                item.add(icon);

                ListView<MainMenuItem> items = new ListView<MainMenuItem>(ID_ITEMS,
                        new PropertyModel<List<MainMenuItem>>(item.getModel(), SideBarMenuItem.F_ITEMS)) {

                    @Override
                    protected void populateItem(final ListItem<MainMenuItem> listItem) {
                        MainMenuPanel item = new MainMenuPanel(ID_ITEM, listItem.getModel());
                        listItem.add(item);

                        listItem.add(new VisibleEnableBehaviour() {

                            @Override
                            public boolean isVisible() {
                                MainMenuItem mmi = listItem.getModelObject();
                                if (!SecurityUtils.isMenuAuthorized(mmi)) {
                                    return false;
                                }

                                if (mmi.getItems().isEmpty()) {
                                    return true;
                                }

                                for (MenuItem i : mmi.getItems()) {
                                    if (SecurityUtils.isMenuAuthorized(i)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
                    }
                };
                item.add(items);

                item.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        SideBarMenuItem mainMenu = item.getModelObject();

                        for (MainMenuItem i : mainMenu.getItems()) {
                            boolean visible = true;
                            if (i.getVisibleEnable() != null) {
                                visible = i.getVisibleEnable().isVisible();
                            }

                            if (visible && SecurityUtils.isMenuAuthorized(i)) {
                                return true;
                            }
                        }

                        return false;
                    }
                });

                items.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        SideBarMenuItem mainMenu = item.getModelObject();
                        return isMenuExpanded(mainMenu);
                    }
                });
            }
        };
        sidebar.add(menuItems);
    }

    private boolean isMenuExpanded(SideBarMenuItem mainMenu) {
        SessionStorage storage = getPageBase().getSessionStorage();
        Map<String, Boolean> menuState = storage.getMainMenuState();

        String menuLabel = mainMenu.getName().getObject();
        // we'll use menu label as key
        Boolean expanded = menuState.get(menuLabel);

        return expanded != null ? expanded : true;
    }
}
