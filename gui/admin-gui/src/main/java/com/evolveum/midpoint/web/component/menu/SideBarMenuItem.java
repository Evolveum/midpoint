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

/**
 * @author Viliam Repan (lazyman)
 */
public class SideBarMenuItem implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_ITEMS = "items";

    private final String name;
    private List<MainMenuItem> items;
    private boolean experimentalFeaturesEnabled;

    public SideBarMenuItem(String name, boolean experimentalFeaturesEnabled) {
        this.name = name;
        this.experimentalFeaturesEnabled = experimentalFeaturesEnabled;
    }

    public List<MainMenuItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SideBarMenuItem{" +
                "name=" + name +
                ", items=" + items +
                '}';
    }

    public void addMainMenuItem(MainMenuItem mainMenuItem) {
        if (mainMenuItem.shouldBeMenuAdded(experimentalFeaturesEnabled)) {
            getItems().add(mainMenuItem);
        }
    }

}
