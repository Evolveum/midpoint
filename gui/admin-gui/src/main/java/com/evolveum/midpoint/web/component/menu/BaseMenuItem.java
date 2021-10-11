/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Viliam Repan (lazyman)
 */
public class BaseMenuItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String F_ICON_CLASS = "iconClass";

    //TODO why model? would be string key enought
    private IModel<String> nameModel;
    private Class<? extends WebPage> pageClass;
    private PageParameters params;
    private VisibleEnableBehaviour visibleEnable;
    private Class<? extends WebPage>[] aliases;
    private String iconClass;
    /**
     * Optional field that can be used for sorting. Used for some dynamic submenus, such as collections.
     * It does not affect the display of menu item in any way. It is just a convenient intermediary place to store
     * the order for sorting the items before adding them to menu.
     */
    private transient Integer displayOrder;

    public BaseMenuItem(IModel<String> name, Class<? extends WebPage> page) {
        this(name, "", page, null, null);
    }

    public BaseMenuItem(IModel<String> nameModel, String iconClass, Class<? extends WebPage> pageClass,
                        PageParameters params, VisibleEnableBehaviour visibleEnable,
                        Class<? extends WebPage>... aliases) {
        this.aliases = aliases;
        this.nameModel = nameModel;
        this.pageClass = pageClass;
        this.params = params;
        this.visibleEnable = visibleEnable;
        this.iconClass = iconClass;
    }

    /**
     * @return Returns array of {@link WebPage} classes where this menu should be marked as <b>active</b>.
     */
    public Class<? extends WebPage>[] getAliases() {
        return aliases;
    }

    public IModel<String> getNameModel() {
        return nameModel;
    }

    public Class<? extends WebPage> getPageClass() {
        return pageClass;
    }

    public PageParameters getParams() {
        return params;
    }

    public VisibleEnableBehaviour getVisibleEnable() {
        return visibleEnable;
    }

    public String getIconClass() {
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

        boolean isMenuActive = isMenuActive();

        if (pageClass.equals(this.pageClass)) {
            return isMenuActive;
        }

        if (aliases == null) {
            return false;
        }

        for (Class c : aliases) {
            if (pageClass.equals(c)) {
                return isMenuActive;
            }
        }

        return false;
    }

    protected boolean isMenuActive() {
        return true;
    }

    @Override
    public String toString() {
        return "BaseMenuItem(nameModel=" + nameModel + ", pageClass=" + pageClass + ", params=" + params
                + ", visibleEnable=" + visibleEnable + ", aliases=" + Arrays.toString(aliases) + ")";
    }


}
