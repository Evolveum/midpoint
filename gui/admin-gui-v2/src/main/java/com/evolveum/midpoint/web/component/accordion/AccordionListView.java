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

package com.evolveum.midpoint.web.component.accordion;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

/**
 * @author lazyman
 */
public abstract class AccordionListView<T> extends Panel {

    private boolean expanded = false;
    private boolean multipleSelect = true;
    private int openedPanel = -1;

    public AccordionListView(String id, IModel<? extends List<? extends T>> model) {
        super(id);

        add(new AttributeAppender("class", new Model<String>("accordions"), " "));

        WebMarkupContainer parent = new WebMarkupContainer("parent");
        parent.setOutputMarkupId(true);
        add(parent);

        initLayout(parent, model);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderJavaScriptReference(new PackageResourceReference(Accordion.class, "Accordion.js"));
        response.renderCSSReference(new PackageResourceReference(Accordion.class, "Accordion.css"));

        WebMarkupContainer parent = (WebMarkupContainer) get("parent");

        response.renderOnLoadJavaScript("createAccordion('" + parent.getMarkupId() + "'," + getExpanded()
                + "," + getMultipleSelect() + "," + getOpenedPanel() + ")");
    }

    private void initLayout(WebMarkupContainer parent, IModel<? extends List<? extends T>> model) {
        ListView<T> repeater = new ListView<T>("repeater", model) {

            @Override
            protected void populateItem(ListItem<T> item) {
                item.add(new Label("headerText", createHeaderLabel(item.getModel())));
                item.add(createPanelBody("body", item.getModel()));
            }
        };
        parent.add(repeater);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        this.multipleSelect = multipleSelect;
    }

    public void setOpenedPanel(int openedPanel) {
        this.openedPanel = openedPanel;
    }

    public boolean getExpanded() {
        return expanded;
    }

    private int getMultipleSelect() {
        if (multipleSelect) {
            return 0;
        }
        return -1;
    }

    public int getOpenedPanel() {
        return openedPanel;
    }

    protected IModel<String> createHeaderLabel(IModel<T> itemModel) {
        return new Model<String>("");
    }

    protected Component createPanelBody(String componentId, IModel<T> itemModel) {
        return new EmptyPanel(componentId);
    }
}
