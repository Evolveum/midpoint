/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

public class DropdownButtonDto implements Serializable {

    public static final String F_INFO = "info";
    public static final String F_ICON = "icon";
    public static final String F_LABEL = "label";
    public static final String F_ITEMS = "items";

    private static final long serialVersionUID = 1L;
    private String info;
    private String icon;
    private String label;

    private List<InlineMenuItem> items;

    public DropdownButtonDto(String info, String icon, String label, List<InlineMenuItem> items) {
        this.info = info;
        this.icon = icon;
        this.label = label;
        this.items = items;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<InlineMenuItem> getMenuItems() {
        return items;
    }

}
