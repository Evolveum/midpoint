/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.box;

import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetSourceTypeType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author skublik
 */
public abstract class SmallInfoBoxPanel extends InfoBoxPanel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SmallInfoBoxPanel.class);

    private static final String ID_MORE_INFO_BOX = "moreInfoBox";
    private static final String ID_MORE_INFO_BOX_ICON = "moreInfoBoxIcon";
    private static final String ID_MORE_INFO_BOX_LABEL = "moreInfoBoxLabel";

    public SmallInfoBoxPanel(String id, IModel<DashboardWidgetType> model, PageBase pageBase) {
        super(id, model);
    }

    @Override
    protected void customInitLayout(WebMarkupContainer parentInfoBox) {
        WebMarkupContainer moreInfoBox = new WebMarkupContainer(ID_MORE_INFO_BOX);
        parentInfoBox.add(moreInfoBox);
        WebMarkupContainer moreInfoBoxIcon = new WebMarkupContainer(ID_MORE_INFO_BOX_ICON);
        moreInfoBox.add(moreInfoBoxIcon);
        Label moreInfoBoxLabel = new Label(ID_MORE_INFO_BOX_LABEL, getPageBase().createStringResource("PageDashboard.infobox.moreInfo"));
        moreInfoBox.add(moreInfoBoxLabel);

        moreInfoBox.add(new AjaxEventBehavior("click") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                navigateToPage();
            }
        });
        moreInfoBox.add(AttributeModifier.append("class", "cursor-pointer"));

        setInvisible(moreInfoBox);
        moreInfoBox.add(AttributeModifier.append("style", "height: 26px; background:rgba(0, 0, 0, 0.1) !important;"));
    }

    @Override
    public abstract String getDashboardOid();

    private void setInvisible(Component component) {
        component.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return existLinkRef();
            }
        });
    }

}
