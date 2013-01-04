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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public class DashboardPanel extends Panel {

    private static final String ID_TITLE = "title";
    private static final String ID_CONTENT = "content";

    public DashboardPanel(String id, IModel<String> titleModel) {
        super(id);

        initLayout(titleModel);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderCSSReference(new PackageResourceReference(DashboardPanel.class, "DashboardPanel.css"));
    }

    private void initLayout(IModel<String> titleModel) {
        add(new Label(ID_TITLE, titleModel));

        add(new AjaxLazyLoadPanel(ID_CONTENT) {

            @Override
            public Component getLazyLoadComponent(String componentId) {
                return DashboardPanel.this.getLazyLoadComponent(componentId);
            }
        });
    }

    protected Component getLazyLoadComponent(String componentId) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new Label(componentId, "I'm here now");
    }
}
