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

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.time.Duration;

/**
 * @author lazyman
 */
public class DashboardPanel extends Panel {

    private static final String ID_TITLE = "title";
    private static final String ID_PRELOADER_CONTAINER = "preloaderContainer";
    private static final String ID_PRELOADER = "preloader";
    private static final String ID_CONTENT = "content";

    public DashboardPanel(String id, IModel<String> titleModel) {
        this(id, titleModel, null);
    }

    public DashboardPanel(String id, IModel<String> titleModel, IModel<? extends Dashboard> dashboardModel) {
        super(id);
        setOutputMarkupId(true);

        initLayout(titleModel, dashboardModel);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderCSSReference(new PackageResourceReference(DashboardPanel.class, "DashboardPanel.css"));
    }

    private void initLayout(IModel<String> titleModel, final IModel<? extends Dashboard> dashboardModel) {
        add(new Label(ID_TITLE, titleModel));

        WebMarkupContainer preloaderContainer = new WebMarkupContainer(ID_PRELOADER_CONTAINER);
        add(preloaderContainer);
        preloaderContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (dashboardModel == null) {
                    return false;
                }

                Dashboard dashboard = dashboardModel.getObject();
                return dashboard != null ? !dashboard.isLoaded() : true;
            }
        });

        Image preloader = new Image(ID_PRELOADER, new PackageResourceReference(ImgResources.class, "preloader-panel.gif"));
        preloaderContainer.add(preloader);

        if (dashboardModel == null) {
            add(getLazyLoadComponent(ID_CONTENT));
            return;
        }

        AbstractAjaxTimerBehavior selfUpdating = new AbstractAjaxTimerBehavior(Duration.seconds(2)) {

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                Dashboard dashboard = dashboardModel.getObject();
                if (dashboard == null || !dashboard.isLoaded()) {
                    return;
                }

                replace(getLazyLoadComponent(ID_CONTENT));
                stop();
                target.add(DashboardPanel.this);
            }
        };
        add(selfUpdating);
        add(new Label(ID_CONTENT));
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
