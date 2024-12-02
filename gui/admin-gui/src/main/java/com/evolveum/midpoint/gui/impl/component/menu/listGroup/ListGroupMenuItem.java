/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.menu.listGroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListGroupMenuItem<T extends Serializable> implements Serializable {

    private String iconCss;

    private String label;

    private String badgeCss;

    private String badge;

    private boolean active;

    private boolean disabled;

    private boolean open;

    private T value;

    private LoadableModel<List<ListGroupMenuItem<T>>> items = new LoadableModel<>() {

        @Override
        protected List<ListGroupMenuItem<T>> load() {
            return new ArrayList<>();
        }
    };

    public ListGroupMenuItem() {
    }

    public ListGroupMenuItem(String label) {
        this(null, label);
    }

    public ListGroupMenuItem(String iconCss, String label) {
        this.iconCss = iconCss;
        this.label = label;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getIconCss() {
        return iconCss;
    }

    public void setIconCss(String iconCss) {
        this.iconCss = iconCss;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBadgeCss() {
        return badgeCss;
    }

    public void setBadgeCss(String badgeCss) {
        this.badgeCss = badgeCss;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<ListGroupMenuItem<T>> getItems() {
        return items.getObject();
    }

    public void setItems(List<ListGroupMenuItem<T>> items) {
        this.items.setObject(items);
    }

    public void setItemsModel(@NotNull LoadableModel<List<ListGroupMenuItem<T>>> items) {
        this.items = items;
    }

    public boolean isOpen() {
        if (isEmpty()) {
            return false;
        }

        return open;
    }

    public void setOpen(boolean open) {
        if (isEmpty()) {
            return;
        }

        this.open = open;
    }

    public boolean isLoaded() {
        return items.isLoaded();
    }

    public boolean isEmpty() {
        if (!isLoaded()) {
            return true;
        }

        return getItems().isEmpty();
    }
}
