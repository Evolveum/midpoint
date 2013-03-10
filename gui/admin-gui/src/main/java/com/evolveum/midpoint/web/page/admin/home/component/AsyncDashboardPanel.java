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

import com.evolveum.midpoint.web.component.async.AsyncUpdatePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

/**
 * @author lazyman
 */
public abstract class AsyncDashboardPanel<V, T> extends AsyncUpdatePanel<V, T> {

    private static final String ID_DASHBOARD_TITLE = "dashboardTitle";
    private static final String ID_TITLE = "title";
    private static final String ID_PRELOADER_CONTAINER = "preloaderContainer";
    private static final String ID_PRELOADER = "preloader";
    private static final String ID_DASHBOARD_CONTENT = "dashboardContent";
    private static final String ID_CONTENT = "content";

    public AsyncDashboardPanel(String id, IModel<String> title) {
        this(id, title, new Model());
    }

    public AsyncDashboardPanel(String id, IModel<String> title, IModel<V> callableParameterModel) {
        this(id, title, callableParameterModel, Duration.seconds(DEFAULT_TIMER_DURATION));
    }

    public AsyncDashboardPanel(String id, IModel<String> title, IModel<V> callableParameterModel, Duration durationSecs) {
        super(id, callableParameterModel, durationSecs);

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

        dashboardContent.add(new Label(ID_CONTENT));

        WebMarkupContainer preloaderContainer = new WebMarkupContainer(ID_PRELOADER_CONTAINER);
        preloaderContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isLoadingVisible();
            }
        });
        dashboardContent.add(preloaderContainer);

        preloaderContainer.add(getLoadingComponent(ID_PRELOADER));
    }

    @Override
    protected void onPostSuccess(AjaxRequestTarget target) {
        WebMarkupContainer dashboardContent = (WebMarkupContainer) get(ID_DASHBOARD_CONTENT);
        dashboardContent.replace(getMainComponent(ID_CONTENT));

        PageBase page = (PageBase) getPage();
        target.add(this, page.getFeedbackPanel());
    }

    @Override
    protected void onUpdateError(AjaxRequestTarget target, Exception ex) {
        String message = "Error occurred while fetching data: " + ex.getMessage();
        Label errorLabel = new Label(ID_CONTENT, message);
        WebMarkupContainer dashboardContent = (WebMarkupContainer) get(ID_DASHBOARD_CONTENT);
        dashboardContent.replace(errorLabel);

        PageBase page = (PageBase) getPage();
        target.add(this, page.getFeedbackPanel());
    }
}
