/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;

/**
 * @author Viliam Repan (lazyman)
 */
public class SideBarMenuItem implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_ITEMS = "items";

    private final IModel<String> name;
    private List<MainMenuItem> items;

    public SideBarMenuItem(IModel<String> name) {
        this.name = name;
    }

    public List<MainMenuItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public IModel<String> getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SideBarMenuItem{" +
                "name=" + name +
                ", items=" + items +
                '}';
    }
}
