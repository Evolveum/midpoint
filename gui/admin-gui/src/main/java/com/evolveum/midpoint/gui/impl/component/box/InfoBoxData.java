/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.box;

import java.io.Serializable;

import org.apache.wicket.request.component.IRequestablePage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class InfoBoxData implements Serializable {

    private String text;

    private String number;

    private Integer progress;

    private String description;

    private String description2;

    private String icon;

    private String infoBoxCssClass;

    private String iconCssClass;

    private Class<? extends IRequestablePage> link;

    public InfoBoxData() {
    }

    public InfoBoxData(String infoBoxCssClass, String icon, String text) {
        this.infoBoxCssClass = infoBoxCssClass;
        this.icon = icon;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription2() {
        return description2;
    }

    public void setDescription2(String description2) {
        this.description2 = description2;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getInfoBoxCssClass() {
        return infoBoxCssClass;
    }

    public void setInfoBoxCssClass(String infoBoxCssClass) {
        this.infoBoxCssClass = infoBoxCssClass;
    }

    public String getIconCssClass() {
        return iconCssClass;
    }

    public void setIconCssClass(String iconCssClass) {
        this.iconCssClass = iconCssClass;
    }

    public Class<? extends IRequestablePage> getLink() {
        return link;
    }

    public void setLink(Class<? extends IRequestablePage> link) {
        this.link = link;
    }
}
