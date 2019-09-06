/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class MainMenuItem extends BaseMenuItem {
	private static final long serialVersionUID = 1L;

	public static final String F_ITEMS = "items";
    public static final String F_BUBBLE_LABEL = "bubbleLabel";

    private boolean insertDefaultBackBreadcrumb = true;
    private List<MenuItem> items;

    public MainMenuItem(String iconClass, IModel<String> name) {
        this(iconClass, name, null, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page) {
        this(iconClass, name, page, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page,
                        List<MenuItem> items) {
        this(iconClass, name, page, items, null);
    }

    public MainMenuItem(String iconClass, IModel<String> name, Class<? extends PageBase> page,
                        List<MenuItem> items, VisibleEnableBehaviour visibleEnable) {
        super(name, iconClass, page, null, visibleEnable);
        this.items = items;
    }

    public List<MenuItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public String getBubbleLabel() {
    	return null;
    }

    public boolean isInsertDefaultBackBreadcrumb() {
        return insertDefaultBackBreadcrumb;
    }

    public void setInsertDefaultBackBreadcrumb(boolean insertDefaultBackBreadcrumb) {
        this.insertDefaultBackBreadcrumb = insertDefaultBackBreadcrumb;
    }
}
