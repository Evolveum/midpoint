/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;

import org.apache.wicket.markup.html.WebPage;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author Viliam Repan (lazyman)
 */
public class MainMenuItem extends BaseMenuItem {
    private static final long serialVersionUID = 1L;

    public static final String F_ITEMS = "items";
    public static final String F_BUBBLE_LABEL = "bubbleLabel";

    private boolean insertDefaultBackBreadcrumb = true;
    private List<MenuItem> items;

    public MainMenuItem(String name, String iconClass) {
        this(name, iconClass, null);
    }

    public MainMenuItem(String name, String iconClass, Class<? extends PageBase> page) {
        this(name, iconClass, page, null);
    }

    public MainMenuItem(String name, String iconClass, Class<? extends PageBase> page, PageParameters params) {
        super(name, iconClass, page, params);
    }

    private List<MenuItem> getItems() {
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

    public void addMenuItem(MenuItem menuItem) {
        if (SecurityUtils.isMenuAuthorized(menuItem)) {
            getItems().add(menuItem);
        }
    }

    public void addMenuItemAtIndex(MenuItem menuItem, int position) {
        if (SecurityUtils.isMenuAuthorized(menuItem)) {
            if (getItems().size() >= position) {
                getItems().add(position, menuItem);
            }else {
                getItems().add(menuItem);
            }
        }
    }

    public void addCollectionMenuItem(MenuItem menuItem) {
        if (SecurityUtils.isCollectionMenuAuthorized(menuItem)) {
            getItems().add(menuItem);
        }
    }

    protected boolean isNotEmpty() {
        // If pageClass is not null, we can check page authorization
        // otherwise, empty items means that no sub-items were authorized
        if (getPageClass() != null) {
            return true;
        }
        return items != null;
    }

    public boolean containsSubMenu() {
        return items != null;
    }

    public boolean shouldBeMenuAdded(boolean experimentalFeaturesEnabled) {
        if (!checkExperimental(experimentalFeaturesEnabled)) {
            return false;
        }
        return SecurityUtils.isMenuAuthorized(this) && isNotEmpty();
    }

    private boolean checkExperimental(boolean experimentalFeaturesEnabled) {
        if (experimentalFeaturesEnabled) {
            return true;
        }
        Class<? extends WebPage> clazz = getPageClass();
        if (clazz == null) {
            return true;
        }
        PageDescriptor desc = clazz.getAnnotation(PageDescriptor.class);
        if (desc == null) {
            return true;
        }
        return !desc.experimental();
    }

    public boolean hasActiveSubmenu(WebPage page) {
        return getActiveMenu(page) != null;
    }

    public MenuItem getActiveMenu(WebPage page) {
        if (items == null) {
            return null;
        }
        for (MenuItem item : items) {
            if (item.isMenuActive(page)) {
                return item;
            }
        }
        return null;
    }

    public MenuItem getFirstMenuItem() {
        if (isNotEmpty()) {
            return items.get(0);
        }
        return null;
    }
}
