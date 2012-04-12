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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

/**
 * @author lazyman
 */
public class PrismObjectPanel extends Panel {

    private boolean showHeader = true;

    public PrismObjectPanel(String id, IModel<ObjectWrapper> model, IModel<String> image) {
        super(id);
        
        initLayout(model);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(PrismObjectPanel.class, "PrismObjectPanel.css"));
    }
    
    private void initLayout(IModel<ObjectWrapper> model) {
        WebMarkupContainer headerPanel = new WebMarkupContainer("headerPanel");
        add(headerPanel);
        headerPanel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isShowHeader();
            }
        });
        headerPanel.add(new Label("header", new PropertyModel<Object>(model, "displayName")));
        headerPanel.add(new Label("description", new PropertyModel<Object>(model, "description")));

        //todo image and buttons

        ListView<ContainerWrapper> containers = new ListView<ContainerWrapper>("containers",
                new PropertyModel<List<ContainerWrapper>>(model, "containers")) {

            @Override
            protected void populateItem(ListItem<ContainerWrapper> item) {
                item.add(new PrismContainerPanel("container", item.getModel()));
            }
        };
        add(containers);
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }
}
