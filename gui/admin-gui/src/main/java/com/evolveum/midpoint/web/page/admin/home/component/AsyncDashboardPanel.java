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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.async.AsyncUpdatePanel;
import com.evolveum.midpoint.web.component.async.CallableResult;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

/**
 * @author lazyman
 */
public abstract class AsyncDashboardPanel<V, T> extends AsyncUpdatePanel<V, CallableResult<T>> {

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
        Label title = new Label(ID_TITLE);
        title.setRenderBodyOnly(true);
        dashboardTitle.add(title);

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
        showResultIfError(page);

        target.add(this, page.getFeedbackPanel());
    }

    @Override
    protected void onUpdateError(AjaxRequestTarget target, Exception ex) {
        String message = "Error occurred while fetching data: " + ex.getMessage();
        Label errorLabel = new Label(ID_CONTENT, message);
        WebMarkupContainer dashboardContent = (WebMarkupContainer) get(ID_DASHBOARD_CONTENT);
        dashboardContent.replace(errorLabel);

        PageBase page = (PageBase) getPage();
        showResultIfError(page);

        target.add(this, page.getFeedbackPanel());
    }

    private void showResultIfError(PageBase page) {
        CallableResult<T> result = (CallableResult<T>) getModel().getObject();
        if (result != null && result.getResult() != null) {
            OperationResult opResult = result.getResult();
            if (!WebMiscUtil.isSuccessOrHandledError(opResult)) {
                page.showResult(opResult);
            }
        }
    }
}
