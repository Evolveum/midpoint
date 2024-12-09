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
public class Badge implements Serializable {

    public enum State {
        PRIMARY("badge badge-primary"),
        SECONDARY("badge badge-secondary"),
        SUCCESS("badge badge-success"),
        DANGER("badge badge-danger"),
        WARNING("badge badge-warning"),
        INFO("badge badge-info"),
        LIGHT("badge badge-light"),
        DARK("badge badge-dark");

        String css;

        State(String css) {
            this.css = css;
        }

        public String getCss() {
            return css;
        }
    }

    private String cssClass;

    private String iconCssClass;
    private String textCssClass;

    private String text;
    private String title;

    public Badge() {
        this(null, null);
    }

    public Badge(String cssClass, String text) {
        this(cssClass, null, text);
    }

    public Badge(String cssClass, String iconCssClass, String text) {
        this(cssClass, iconCssClass, text, null);
    }

    public Badge(String cssClass, String iconCssClass, String text, String title) {
        this.cssClass = cssClass;
        this.iconCssClass = iconCssClass;
        this.text = text;
        this.title = title;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public void setCssClass(State state) {
        if (state == null) {
            setCssClass((String) null);
        } else {
            setCssClass(state.getCss());
        }
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTextCssClass(){ return textCssClass;}
    public void setTextCssClass(String textCssClass){ this.textCssClass = textCssClass;}

}
