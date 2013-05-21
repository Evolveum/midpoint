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

package com.evolveum.midpoint.web.component.option;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public class OptionPanel extends Border {

    private OptionPanelHidde optionHide;
    private boolean hidden;

    public OptionPanel(String id, IModel<String> title, final Boolean hidden) {
        super(id);
        this.hidden = hidden;
        WebMarkupContainer parent = new WebMarkupContainer("parent");
        parent.setOutputMarkupId(true);
        addToBorder(parent);
        parent.add(new Label("title", title));
        WebMarkupContainer bar = new WebMarkupContainer("bar");
        bar.setOutputMarkupId(true);
        bar.add(new AjaxEventBehavior("onClick") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                Page page = OptionPanel.this.getPage();
                setHidden(page, !getHiddenFromSession());
            }
        });
        addToBorder(bar);
    }

    public void setHidden(Page page, Boolean hidden) {
        getSession().setAttribute("optionHide_" + page.getClass().getName(), new OptionPanelHidde(page, hidden));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(JavaScriptHeaderItem.forReference(
                new PackageResourceReference(OptionPanel.class, "OptionPanel.js")));
        response.render(OnDomReadyHeaderItem.forScript("initOptionPanel(" + getHiddenFromSession() + ")"));

    }

    private Boolean getHiddenFromSession() {
        Page page = getPage();
        OptionPanelHidde optionHide = (OptionPanelHidde) getSession().getAttribute("optionHide_" + page.getClass().getName());
        if (optionHide != null) {
            return optionHide.isHidden();
        }
        return hidden;
    }
}
