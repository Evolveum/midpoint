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
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public abstract class DashboardPanel<T extends Serializable> extends SimplePanel<T> {

    private static final String ID_DASHBOARD_TITLE = "dashboardTitle";
    private static final String ID_TITLE = "title";
    private static final String ID_DASHBOARD_CONTENT = "dashboardContent";
    private static final String ID_CONTENT = "content";

    public DashboardPanel(String id, IModel<T> model, IModel<String> title) {
        super(id, model);

        Label label = (Label) get(createComponentPath(ID_DASHBOARD_TITLE, ID_TITLE));
        label.setDefaultModel(title);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer dashboardTitle = new WebMarkupContainer(ID_DASHBOARD_TITLE);
        add(dashboardTitle);
        Label title = new Label(ID_TITLE);
        title.setRenderBodyOnly(true);
        dashboardTitle.add(title);

        WebMarkupContainer dashboardContent = new WebMarkupContainer(ID_DASHBOARD_CONTENT);
        add(dashboardContent);
        dashboardContent.add(getMainComponent(ID_CONTENT));
    }

    protected abstract Component getMainComponent(String markupId);
}
