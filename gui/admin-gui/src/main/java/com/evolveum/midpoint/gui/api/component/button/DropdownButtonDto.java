/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

public class DropdownButtonDto implements Serializable {

    public static final String F_INFO = "info";
    public static final String F_ICON = "icon";
    public static final String F_LABEL = "label";
    public static final String F_ITEMS = "items";

    private static final long serialVersionUID = 1L;
    private String info;
    private String icon;

    private IModel<String> labelModel;

    private List<InlineMenuItem> items;

    public DropdownButtonDto(String info, String icon, String label, List<InlineMenuItem> items) {
        this(info, icon, Model.of(label), items);
    }

    private DropdownButtonDto(String info, String icon, IModel<String> labelModel, List<InlineMenuItem> items) {
        this.info = info;
        this.icon = icon;
        this.items = items;

        this.labelModel = labelModel;
    }

    public static DropdownButtonDto create(String info, String icon, IModel<String> labelModel, List<InlineMenuItem> items) {
        return new DropdownButtonDto(info, icon, labelModel, items);
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
        return labelModel.getObject();
    }

    public void setLabel(String label) {
        this.labelModel.setObject(label);
    }

    public IModel<String> getLabelModel() {
        return labelModel;
    }

    public List<InlineMenuItem> getMenuItems() {
        return items;
    }

}
