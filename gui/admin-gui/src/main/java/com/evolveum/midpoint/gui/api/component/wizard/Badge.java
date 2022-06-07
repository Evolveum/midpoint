/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Badge implements Serializable {

    private String cssClass;

    private String iconCssClass;

    private String text;

    public Badge() {
        this(null, null);
    }

    public Badge(String cssClass, String text) {
        this(cssClass, null, text);
    }

    public Badge(String cssClass, String iconCssClass, String text) {
        this.cssClass = cssClass;
        this.iconCssClass = iconCssClass;
        this.text = text;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getIconCssClass() {
        return iconCssClass;
    }

    public void setIconCssClass(String iconCssClass) {
        this.iconCssClass = iconCssClass;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
