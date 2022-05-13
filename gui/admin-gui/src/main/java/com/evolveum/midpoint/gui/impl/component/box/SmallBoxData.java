/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.box;

import org.apache.wicket.request.component.IRequestablePage;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SmallBoxData implements Serializable {

    private String title;

    private String description;

    private String icon;

    private String smallBoxCssClass;

    private Class<? extends IRequestablePage> link;

    private String linkText = "SmallBox.moreInfo";

    private String linkIcon = "fas fa-arrow-circle-right";

    public SmallBoxData() {
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getSmallBoxCssClass() {
        return smallBoxCssClass;
    }

    public void setSmallBoxCssClass(String smallBoxCssClass) {
        this.smallBoxCssClass = smallBoxCssClass;
    }

    public Class<? extends IRequestablePage> getLink() {
        return link;
    }

    public void setLink(Class<? extends IRequestablePage> link) {
        this.link = link;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public String getLinkIcon() {
        return linkIcon;
    }

    public void setLinkIcon(String linkIcon) {
        this.linkIcon = linkIcon;
    }
}
