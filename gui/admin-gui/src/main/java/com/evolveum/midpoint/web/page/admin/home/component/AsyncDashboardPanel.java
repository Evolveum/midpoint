/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.time.Duration;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AsyncUpdatePanel;
import com.evolveum.midpoint.web.component.util.CallableResult;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author lazyman
 */
public abstract class AsyncDashboardPanel<V, T> extends AsyncUpdatePanel<V, CallableResult<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_DASHBOARD_PARENT = "dashboardParent";
    private static final String ID_DASHBOARD_TITLE = "dashboardTitle";
    private static final String ID_TITLE = "title";
    private static final String ID_PRELOADER_CONTAINER = "preloaderContainer";
    private static final String ID_PRELOADER = "preloader";
    private static final String ID_DASHBOARD_CONTENT = "dashboardContent";
    private static final String ID_CONTENT = "content";
    private static final String ID_ICON = "icon";

    public AsyncDashboardPanel(String id, IModel<String> title, String icon, String boxCssClasses) {
        this(id, title, icon, new Model(), boxCssClasses);
    }

    public AsyncDashboardPanel(String id, IModel<String> title, String icon, String boxCssClasses, boolean noPadding) {
        this(id, title, icon, new Model(), Duration.ofSeconds(DEFAULT_TIMER_DURATION), boxCssClasses, noPadding);
    }

    public AsyncDashboardPanel(String id, IModel<String> title, String icon, IModel<V> callableParameterModel,
            String boxCssClasses) {
        this(id, title, icon, callableParameterModel, Duration.ofSeconds(DEFAULT_TIMER_DURATION), boxCssClasses);
    }

    public AsyncDashboardPanel(String id, IModel<String> title, String icon, IModel<V> callableParameterModel,
            Duration durationSecs, String boxCssClasses) {
        this(id, title, icon, callableParameterModel, Duration.ofSeconds(DEFAULT_TIMER_DURATION), boxCssClasses, false);
    }

    public AsyncDashboardPanel(String id, IModel<String> title, String icon, IModel<V> callableParameterModel,
            Duration durationSecs, String boxCssClasses, boolean noPadding) {
        super(id, callableParameterModel, durationSecs);

        initLayout(noPadding);

        WebMarkupContainer dashboardTitle = (WebMarkupContainer) get(
                createComponentPath(ID_DASHBOARD_PARENT, ID_DASHBOARD_TITLE));

        Label label = (Label) dashboardTitle.get(ID_TITLE);
        label.setDefaultModel(title);

        if (boxCssClasses == null) {
            boxCssClasses = GuiStyleConstants.CLASS_BOX_DEFAULT;
        }
        Component dashboardParent = get(ID_DASHBOARD_PARENT);
        dashboardParent.add(new AttributeAppender("class", " " + boxCssClasses));

        WebMarkupContainer iconI = new WebMarkupContainer(ID_ICON);
        iconI.add(AttributeModifier.append("class", icon));
        dashboardTitle.add(iconI);
    }

    private void initLayout(boolean noPadding) {
        WebMarkupContainer dashboardParent = new WebMarkupContainer(ID_DASHBOARD_PARENT);
        add(dashboardParent);

        WebMarkupContainer dashboardTitle = new WebMarkupContainer(ID_DASHBOARD_TITLE);
        dashboardParent.add(dashboardTitle);
        Label title = new Label(ID_TITLE);
        title.setRenderBodyOnly(true);
        dashboardTitle.add(title);

        WebMarkupContainer dashboardContent = new WebMarkupContainer(ID_DASHBOARD_CONTENT);
        dashboardParent.add(dashboardContent);

        if (noPadding) {
            dashboardContent.add(AttributeAppender.append("class", "p-0"));
        }

        dashboardContent.add(new Label(ID_CONTENT));

        WebMarkupContainer preloaderContainer = new WebMarkupContainer(ID_PRELOADER_CONTAINER);
        preloaderContainer.add(new VisibleBehaviour(() -> isLoadingVisible()));
        dashboardContent.add(preloaderContainer);

        preloaderContainer.add(getLoadingComponent(ID_PRELOADER));
    }

    @Override
    protected void onPostSuccess(AjaxRequestTarget target) {
        WebMarkupContainer dashboardContent = getDashboardContent();
        dashboardContent.replace(getMainComponent(ID_CONTENT));

        PageBase page = (PageBase) getPage();
        showResultIfError(page);

        target.add(this, page.getFeedbackPanel());
    }

    private WebMarkupContainer getDashboardContent() {
        return (WebMarkupContainer) get(createComponentPath(ID_DASHBOARD_PARENT, ID_DASHBOARD_CONTENT));
    }

    @Override
    protected void onUpdateError(AjaxRequestTarget target, Exception ex) {
        String message = "Error occurred while fetching data: " + ex.getMessage();
        Label errorLabel = new Label(ID_CONTENT, message);
        WebMarkupContainer dashboardContent = getDashboardContent();
        dashboardContent.replace(errorLabel);

        PageBase page = (PageBase) getPage();
        showResultIfError(page);

        target.add(this, page.getFeedbackPanel());
    }

    private void showResultIfError(PageBase page) {
        CallableResult<T> result = getModel().getObject();
        if (result != null && result.getResult() != null) {
            OperationResult opResult = result.getResult();
            if (!WebComponentUtil.isSuccessOrHandledError(opResult)) {
                page.showResult(opResult);
            }
        }
    }
}
