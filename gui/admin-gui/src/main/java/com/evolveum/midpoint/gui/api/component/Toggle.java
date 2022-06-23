/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Toggle<T extends Serializable> implements Serializable {

    private String iconCss;

    private String label;

    private String badge;

    private String badgeCss;

    private boolean active;

    private T value;

    public Toggle(String iconCss, String label) {
        this.iconCss = iconCss;
        this.label = label;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getBadgeCss() {
        return badgeCss;
    }

    public void setBadgeCss(String badgeCss) {
        this.badgeCss = badgeCss;
    }
}
