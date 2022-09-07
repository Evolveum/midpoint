/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Viliam Repan (lazyman)
 */
public class BaseMenuItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String F_ICON_CLASS = "iconClass";

    public static final String DEFAULT_ICON = "fa fa-circle-o";

    //TODO why model? would be string key enought
    private String nameModel;
    private Class<? extends WebPage> pageClass;
    private PageParameters params;
    private Class<? extends WebPage>[] aliases;
    private String iconClass;

    private Boolean active;
    /**
     * Optional field that can be used for sorting. Used for some dynamic submenus, such as collections.
     * It does not affect the display of menu item in any way. It is just a convenient intermediary place to store
     * the order for sorting the items before adding them to menu.
     */
    private transient Integer displayOrder;

    /**
     * if the menu is generated dynamically, typically it is edit user, edit role, ...
     */
    private boolean dynamic;

    public BaseMenuItem(String nameModel, String iconClass, Class<? extends WebPage> pageClass,
                        PageParameters params, Class<? extends WebPage>... aliases) {
        this.aliases = aliases;
        this.nameModel = nameModel;
        this.pageClass = pageClass;
        this.params = params;
        this.iconClass = iconClass;
    }

    public BaseMenuItem(String nameModel, String iconClass, Class<? extends WebPage> pageClass,
            PageParameters params, boolean active) {

        this.nameModel = nameModel;
        this.pageClass = pageClass;
        this.params = params;

        this.iconClass = iconClass;
        this.active = active;
    }

    /**
     * @return Returns array of {@link WebPage} classes where this menu should be marked as <b>active</b>.
     */
    public Class<? extends WebPage>[] getAliases() {
        return aliases;
    }

    public String getNameModel() {
        return nameModel;
    }

    public Class<? extends WebPage> getPageClass() {
        return pageClass;
    }

    public PageParameters getParams() {
        return params;
    }

    public String getIconClass() {
        if (iconClass == null) {
            return DEFAULT_ICON;
        }
        return iconClass;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isMenuActive(WebPage page) {
        if (page == null) {
            return false;
        }

        Class pageClass = page.getClass();

        if (this.pageClass == null) {
            return false;
        }

        if (pageClass.equals(this.pageClass)) {
            return BooleanUtils.isNotFalse(active);
        }

        if (aliases == null) {
            return false;
        }

        for (Class c : aliases) {
            if (pageClass.equals(c)) {
                return active;
            }
        }

        return false;
    }

//    protected boolean isMenuActive() {
//        return active;
//    }

    @Override
    public String toString() {
        return "BaseMenuItem(nameModel=" + nameModel + ", pageClass=" + pageClass + ", params=" + params
                + ", active=" + active + ", aliases=" + Arrays.toString(aliases) + ")";
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean isDynamic() {
        return dynamic;
    }

}
