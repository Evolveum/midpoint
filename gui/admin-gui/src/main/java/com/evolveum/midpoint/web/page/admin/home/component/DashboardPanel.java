/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
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
        dashboardTitle.add(new Label(ID_TITLE));

        WebMarkupContainer dashboardContent = new WebMarkupContainer(ID_DASHBOARD_CONTENT);
        add(dashboardContent);
        dashboardContent.add(getMainComponent(ID_CONTENT));
    }

    protected abstract Component getMainComponent(String markupId);
}
