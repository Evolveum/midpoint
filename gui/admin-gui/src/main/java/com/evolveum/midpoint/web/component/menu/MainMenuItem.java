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

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class MainMenuItem extends BaseMenuItem {

    public static final String F_ITEMS = "items";
    public static final String F_ICON_CLASS = "iconClass";

    private String iconClass;
    private List<MenuItem> items;

    public MainMenuItem(String iconClass, IModel<String> name) {
        this(iconClass, name, null, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page) {
        this(iconClass, name, page, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page,
                        List<MenuItem> items) {
        this(iconClass, name, page, items, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page,
                        List<MenuItem> items, VisibleEnableBehaviour visibleEnable) {
        super(name, page, null, visibleEnable);
        this.iconClass = iconClass;
        this.items = items;
    }

    public String getIconClass() {
        return iconClass;
    }

    public List<MenuItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }
}
