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

package com.evolveum.midpoint.web.component.accordion;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * Don't use this component, it will be gradually removed from gui.
 * Maybe replaced with something else later. [lazyman]
 */
@Deprecated
public class Accordion extends Border {

    private static final long serialVersionUID = 7554515215048790384L;
    private boolean expanded = false;
    private boolean multipleSelect = true;
    private int openedPanel = -1;

    public Accordion(String id) {
        super(id);

        add(new AttributeAppender("class", new Model<>("accordions"), " "));

        WebMarkupContainer parent = new WebMarkupContainer("parent");
        parent.setOutputMarkupId(true);
        addToBorder(parent);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(JavaScriptHeaderItem.forReference(
                new PackageResourceReference(Accordion.class, "Accordion.js")));
        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(Accordion.class, "Accordion.css")));

        WebMarkupContainer parent = (WebMarkupContainer) get("parent");
        response.render(OnDomReadyHeaderItem.forScript("createAccordion('" + parent.getMarkupId()
                + "'," + getExpanded() + "," + getMultipleSelect() + "," + getOpenedPanel() + ")"));
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
}
