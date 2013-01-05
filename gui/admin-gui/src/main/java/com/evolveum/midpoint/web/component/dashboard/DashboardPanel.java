/*
 * Copyright (c) 2013 Evolveum
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

package com.evolveum.midpoint.web.component.dashboard;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.time.Duration;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class DashboardPanel<T extends Serializable> extends SimplePanel<Dashboard<T>> {

    private static final String ID_DASHBOARD_TITLE = "dashboardTitle";
    private static final String ID_TITLE = "title";
    private static final String ID_MINIMIZE = "minimize";
    private static final String ID_MINIMIZE_ICON = "minimizeIcon";
    private static final String ID_PRELOADER_CONTAINER = "preloaderContainer";
    private static final String ID_PRELOADER = "preloader";
    private static final String ID_DASHBOARD_CONTENT = "dashboardContent";
    private static final String ID_CONTENT = "content";

    public DashboardPanel(String id, IModel<String> titleModel) {
        this(id, titleModel, new Model(new Dashboard(false)));
    }

    public DashboardPanel(String id, IModel<String> titleModel, IModel<Dashboard<T>> dashboardModel) {
        super(id, dashboardModel);
        setOutputMarkupId(true);

        initLayout(titleModel);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderCSSReference(new PackageResourceReference(DashboardPanel.class, "DashboardPanel.css"));
    }

    private void initLayout(IModel<String> titleModel) {
        WebMarkupContainer dashboardTitle = new WebMarkupContainer(ID_DASHBOARD_TITLE);
        dashboardTitle.add(new AttributeModifier("style", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                Dashboard dashboard = DashboardPanel.this.getModel().getObject();
                if (dashboard == null || !dashboard.isMinimized()) {
                    return "";
                }

                return "border-radius: 4px; border-style: solid;";
            }
        }));
        add(dashboardTitle);
        dashboardTitle.add(new Label(ID_TITLE, titleModel));
        dashboardTitle.add(createMinimizeLink(getModel()));

        WebMarkupContainer dashboardContent = new WebMarkupContainer(ID_DASHBOARD_CONTENT);
        dashboardContent.setOutputMarkupId(true);
        dashboardContent.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                Dashboard dashboard = DashboardPanel.this.getModel().getObject();
                return dashboard == null || !dashboard.isMinimized();
            }
        });
        add(dashboardContent);
        initDashboardContent(dashboardContent);
    }

    private void initDashboardContent(final WebMarkupContainer dashboardContent) {
        WebMarkupContainer preloaderContainer = new WebMarkupContainer(ID_PRELOADER_CONTAINER);
        preloaderContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                Dashboard dashboard = DashboardPanel.this.getModel().getObject();
                if (dashboard == null) {
                    return true;
                }
                return dashboard.isLazyLoading() && !dashboard.isLoaded();
            }
        });
        dashboardContent.add(preloaderContainer);

        Image preloader = new Image(ID_PRELOADER, new PackageResourceReference(ImgResources.class, "preloader-panel.gif"));
        preloaderContainer.add(preloader);

        //todo improve this...we're calling model.getObject() during initLayout...
        IModel<Dashboard<T>> dashboardModel = getModel();
        if (!dashboardModel.getObject().isLazyLoading()) {
            dashboardContent.add(getLazyLoadComponent(ID_CONTENT));
            return;
        }

        AbstractAjaxTimerBehavior selfUpdating = new AbstractAjaxTimerBehavior(Duration.seconds(2)) {

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                Dashboard dashboard = DashboardPanel.this.getModel().getObject();
                if (dashboard == null || !dashboard.isLoaded()) {
                    return;
                }

                dashboardContent.replace(getLazyLoadComponent(ID_CONTENT));
                stop();
                target.add(DashboardPanel.this);
            }
        };
        dashboardContent.add(selfUpdating);
        dashboardContent.add(new Label(ID_CONTENT));
    }

    private AjaxLink createMinimizeLink(final IModel<? extends Dashboard> dashboardModel) {
        AjaxLink minimize = new AjaxLink(ID_MINIMIZE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                minimizePerformed(target);
            }
        };
        minimize.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                Dashboard dashboard = dashboardModel.getObject();
                return dashboard != null && dashboard.isShowMinimize();
            }
        });
        minimize.add(new AttributeModifier("title", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                Dashboard dashboard = dashboardModel.getObject();
                String key = dashboard.isMinimized() ? "DashboardPanel.maximize" : "DashboardPanel.minimize";
                return DashboardPanel.this.getString(key);
            }
        }));

        Image minimizeIcon = new Image(ID_MINIMIZE_ICON, new AbstractReadOnlyModel<PackageResourceReference>() {

            @Override
            public PackageResourceReference getObject() {
                Dashboard dashboard = dashboardModel.getObject();

                String icon = dashboard.isMinimized() ? "bullet_toggle_plus.png" : "bullet_toggle_minus.png";
                return new PackageResourceReference(ImgResources.class, icon);
            }
        });
        minimize.add(minimizeIcon);

        return minimize;
    }

    protected void minimizePerformed(AjaxRequestTarget target) {
        Dashboard dashboard = DashboardPanel.this.getModel().getObject();
        dashboard.setMinimized(!dashboard.isMinimized());

        target.add(this);
        //todo implement
    }

    /**
     * //todo documentation
     *
     * @param componentId
     * @return
     */
    protected Component getLazyLoadComponent(String componentId) {
        return new Label(ID_CONTENT, "I'm here now.");
    }
}
