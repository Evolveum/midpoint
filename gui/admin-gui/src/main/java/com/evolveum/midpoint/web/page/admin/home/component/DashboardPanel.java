/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public abstract class DashboardPanel<T extends Serializable> extends BasePanel<T> {
    private static final long serialVersionUID = 1L;

    private static final String ID_DASHBOARD_PARENT = "dashboardParent";
    private static final String ID_DASHBOARD_TITLE = "dashboardTitle";
    private static final String ID_TITLE = "title";
    private static final String ID_DASHBOARD_CONTENT = "dashboardContent";
    private static final String ID_CONTENT = "content";
    private static final String ID_ICON = "icon";

    public DashboardPanel(String id, IModel<T> model, IModel<String> titleModel, String icon, String boxCssClasses) {
        super(id, model);
        initLayout(titleModel, icon, boxCssClasses);
    }

    private void initLayout(IModel<String> titleModel, String icon, String boxCssClasses) {
        if (boxCssClasses == null) {
            boxCssClasses = GuiStyleConstants.CLASS_BOX_DEFAULT;
        }

        WebMarkupContainer dashboardParent = new WebMarkupContainer(ID_DASHBOARD_PARENT);
        dashboardParent.add(AttributeAppender.append("class", boxCssClasses));
        add(dashboardParent);

        WebMarkupContainer dashboardTitle = new WebMarkupContainer(ID_DASHBOARD_TITLE);
        dashboardParent.add(dashboardTitle);
        Label title = new Label(ID_TITLE);
        title.setRenderBodyOnly(true);
        title.setDefaultModel(titleModel);
        dashboardTitle.add(title);

        WebMarkupContainer dashboardContent = new WebMarkupContainer(ID_DASHBOARD_CONTENT);
        Component content = getMainComponent(ID_CONTENT);
        content.setRenderBodyOnly(true);
        dashboardContent.add(content);
        dashboardParent.add(dashboardContent);

        WebMarkupContainer iconI = new WebMarkupContainer(ID_ICON);
        iconI.add(AttributeModifier.append("class", icon));
        dashboardTitle.add(iconI);
    }

    protected abstract Component getMainComponent(String markupId);
}
