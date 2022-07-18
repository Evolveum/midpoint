/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListGroupMenu<T extends Serializable> implements Serializable {

    private List<ListGroupMenuItem<T>> items;

    public List<ListGroupMenuItem<T>> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<ListGroupMenuItem<T>> items) {
        this.items = items;
    }

    public void activateItem(ListGroupMenuItem item) {
        if (!item.getItems().isEmpty()) {
            return;
        }

        getItems().forEach(i -> deactivateItem(i));

        item.setActive(true);
    }

    private void deactivateItem(ListGroupMenuItem<T> item) {
        item.setActive(false);

        item.getItems().forEach(i -> deactivateItem(i));
    }

    public ListGroupMenuItem<T> getActiveMenu() {
        for (ListGroupMenuItem item : getItems()) {
            ListGroupMenuItem active = getActiveMenu(item);
            if (active != null) {
                return active;
            }
        }

        return null;
    }

    private ListGroupMenuItem getActiveMenu(ListGroupMenuItem<T> parent) {
        if (parent.isActive()) {
            return parent;
        }

        return parent.getItems().stream().filter(i -> i.isActive()).findFirst().orElse(null);
    }

}
