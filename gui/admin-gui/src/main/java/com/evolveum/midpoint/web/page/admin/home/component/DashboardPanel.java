/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

/**
 * @author lazyman
 */
public abstract class DashboardPanel<T extends Serializable> extends SimplePanel<T> {

    private static final String ID_DASHBOARD_PARENT = "dashboardParent";
    private static final String ID_DASHBOARD_TITLE = "dashboardTitle";
    private static final String ID_TITLE = "title";
    private static final String ID_DASHBOARD_CONTENT = "dashboardContent";
    private static final String ID_CONTENT = "content";
    private static final String ID_ICON = "icon";

    public DashboardPanel(String id, IModel<T> model, IModel<String> title, String icon, DashboardColor color) {
        super(id, model);

        WebMarkupContainer dashboardTitle = (WebMarkupContainer) get(
                createComponentPath(ID_DASHBOARD_PARENT, ID_DASHBOARD_TITLE));

        Label label = (Label) dashboardTitle.get(ID_TITLE);
        label.setDefaultModel(title);

        if (color == null) {
            color = DashboardColor.GRAY;
        }
        Component dashboardParent = get(ID_DASHBOARD_PARENT);
        dashboardParent.add(new AttributeAppender("class", " " + color.getCssClass()));

        WebMarkupContainer iconI = new WebMarkupContainer(ID_ICON);
        iconI.add(AttributeModifier.replace("class", icon));
        dashboardTitle.add(iconI);
    }

    public String getDashboardBodyCss() {
        return "padding: 0px;";
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer dashboardParent = new WebMarkupContainer(ID_DASHBOARD_PARENT);
        add(dashboardParent);

        WebMarkupContainer dashboardTitle = new WebMarkupContainer(ID_DASHBOARD_TITLE);
        dashboardParent.add(dashboardTitle);
        Label title = new Label(ID_TITLE);
        title.setRenderBodyOnly(true);
        dashboardTitle.add(title);

        WebMarkupContainer dashboardContent = new WebMarkupContainer(ID_DASHBOARD_CONTENT);
        dashboardContent.add(AttributeModifier.append("style", getDashboardBodyCss()));
        dashboardContent.add(getMainComponent(ID_CONTENT));
        dashboardParent.add(dashboardContent);
    }

    protected abstract Component getMainComponent(String markupId);
}
