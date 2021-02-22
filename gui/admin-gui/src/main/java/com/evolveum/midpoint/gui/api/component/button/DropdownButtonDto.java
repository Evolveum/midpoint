/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.button;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

public class DropdownButtonDto implements Serializable {

    /**
     *
     */
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
