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

    public void onItemChevronClickPerformed(ListGroupMenuItem item) {
        if (item.getItems().isEmpty()) {    // force load of items, calling item.isEmpty() would return false if items are not loaded
            item.setOpen(false);
            return;
        }

        item.setOpen(!item.isOpen());
    }

    public void onItemClickPerformed(ListGroupMenuItem item) {
        onItemChevronClickPerformed(item);

        getItems().forEach(this::deactivateItem);
        item.setActive(true);
    }

    public ListGroupMenuItem<T> activateFirstAvailableItem() {
        List<ListGroupMenuItem<T>> items = getItems();
        if (items.isEmpty()) {
            return null;
        }

        for (ListGroupMenuItem<T> i : items) {
            if (!i.isDisabled()) {
                i.setActive(true);
                return i;
            }
        }

        return null;
    }

    private void deactivateItem(ListGroupMenuItem<T> item) {
        item.setActive(false);

        if (item.isLoaded()) {
            item.getItems().forEach(this::deactivateItem);
        }
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

        if (!item.isLoaded()) {
            return null;
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
