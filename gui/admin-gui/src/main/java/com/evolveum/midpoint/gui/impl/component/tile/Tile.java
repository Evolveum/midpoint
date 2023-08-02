/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Tile<T extends Serializable> implements Comparable<Tile>, Serializable {

    private String icon;

    private String title;

    private String description;

    private boolean selected;

    private T value;

    public Tile() {
        this(null, null);
    }

    public Tile(String icon, String title) {
        this.icon = icon;
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void toggle() {
        setSelected(!isSelected());
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public int compareTo(@NotNull Tile o) {
        return this.title.compareTo(o.getTitle());
    }

}
