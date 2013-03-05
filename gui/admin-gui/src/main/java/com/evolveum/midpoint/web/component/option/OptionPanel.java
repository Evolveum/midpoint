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
