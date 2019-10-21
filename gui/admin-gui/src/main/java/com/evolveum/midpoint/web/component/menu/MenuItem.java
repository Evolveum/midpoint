/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author Viliam Repan (lazyman)
 */
public class MenuItem extends BaseMenuItem {

    public MenuItem(IModel<String> nameModel, Class<? extends WebPage> pageClass) {
        this(nameModel, "", pageClass);
    }

    public MenuItem(IModel<String> nameModel, String iconClass, Class<? extends WebPage> pageClass) {
        this(nameModel, iconClass, pageClass, null, null);
    }

    public MenuItem(IModel<String> nameModel, Class<? extends WebPage> pageClass,
                    PageParameters params, VisibleEnableBehaviour visibleEnable, Class<? extends WebPage>... aliases) {
        this(nameModel, "", pageClass, params, visibleEnable, aliases);
    }

    public MenuItem(IModel<String> nameModel, String iconClass, Class<? extends WebPage> pageClass,
                    PageParameters params, VisibleEnableBehaviour visibleEnable, Class<? extends WebPage>... aliases) {
        super(nameModel, iconClass, pageClass, params, visibleEnable, aliases);
    }
}
