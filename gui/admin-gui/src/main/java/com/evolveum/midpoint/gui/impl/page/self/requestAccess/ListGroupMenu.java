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

    public void onItemClickPerformed(ListGroupMenuItem item) {
        if (item.isEmpty()) {
            getItems().forEach(i -> deactivateItem(i));
            item.setActive(true);

            return;
        }

        item.setOpen(!item.isOpen());
    }

    public void activateFirstAvailableItem() {
        for (ListGroupMenuItem i : getItems()) {
            if (activateFirstAvailableItem(i)) {
                i.setOpen(true);
                break;
            }
        }
    }

    public boolean activateFirstAvailableItem(ListGroupMenuItem item) {
        List<ListGroupMenuItem> items = item.getItems();
        if (items.isEmpty() && !item.isDisabled()) {
            item.setActive(true);

            return true;
        }

        for (ListGroupMenuItem i : items) {
            if (activateFirstAvailableItem(i)) {
                i.setOpen(true);
                return true;
            }
        }

        return false;
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

    private ListGroupMenuItem getActiveMenu(ListGroupMenuItem<T> item) {
        if (item.isActive()) {
            return item;
        }

        for (ListGroupMenuItem i : item.getItems()) {
            ListGroupMenuItem active = getActiveMenu(i);
            if (active != null) {
                return active;
            }
        }

        return null;
    }

    public boolean isEmpty() {
        return getItems().isEmpty();
    }
}
