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

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.*;

import java.util.List;

/**
 * @author lazyman
 */
public class PrismObjectPanel extends Panel {

    private boolean showHeader = true;

    public PrismObjectPanel(String id, IModel<ObjectWrapper> model, ResourceReference image) {
        super(id);
        setOutputMarkupId(true);

        initLayout(model, image);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(PrismObjectPanel.class, "PrismObjectPanel.css"));
    }

    private void initLayout(final IModel<ObjectWrapper> model, ResourceReference image) {
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

        Image headerImg = new Image("headerImg", image);
        headerPanel.add(headerImg);

        initButtons(headerPanel, model);

        WebMarkupContainer body = new WebMarkupContainer("body");
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ObjectWrapper wrapper = model.getObject();
                return !wrapper.isMinimalized();
            }
        });
        add(body);

        ListView<ContainerWrapper> containers = new ListView<ContainerWrapper>("containers",
                new PropertyModel<List<ContainerWrapper>>(model, "containers")) {

            @Override
            protected void populateItem(ListItem<ContainerWrapper> item) {
                item.add(new PrismContainerPanel("container", item.getModel()));
            }
        };
        body.add(containers);
    }

    private void initButtons(WebMarkupContainer headerPanel, final IModel<ObjectWrapper> model) {
        AjaxLink showEmpty = new AjaxLink("showEmptyButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectWrapper wrapper = model.getObject();
                wrapper.setShowEmpty(!wrapper.isShowEmpty());

                target.add(PrismObjectPanel.this);
            }
        };
        headerPanel.add(showEmpty);

        Image showEmptyImg = new Image("showEmptyImg", new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                ObjectWrapper wrapper = model.getObject();
                if (wrapper.isShowEmpty()) {
                    return new PackageResourceReference(PrismObjectPanel.class,
                            "ShowEmptyFalse.png");
                }

                return new PackageResourceReference(PrismObjectPanel.class,
                        "ShowEmptyTrue.png");
            }
        });
        showEmpty.add(showEmptyImg);

        AjaxLink minimize = new AjaxLink("minimizeButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectWrapper wrapper = model.getObject();
                wrapper.setMinimalized(!wrapper.isMinimalized());

                target.add(PrismObjectPanel.this);
            }
        };
        headerPanel.add(minimize);

        Image minimizeImg = new Image("minimizeImg", new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                ObjectWrapper wrapper = model.getObject();
                if (wrapper.isMinimalized()) {
                    return new PackageResourceReference(PrismObjectPanel.class,
                            "Maximize.png");
                }

                return new PackageResourceReference(PrismObjectPanel.class,
                        "Minimize.png");
            }
        });
        minimize.add(minimizeImg);
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }
}
