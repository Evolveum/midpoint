/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.box;

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

/**
 * @author skublik
 */
public class SmallInfoBoxPanel extends InfoBoxPanel{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SmallInfoBoxPanel.class);

    private static final String ID_MORE_INFO_BOX = "moreInfoBox";
    private static final String ID_MORE_INFO_BOX_ICON = "moreInfoBoxIcon";
    private static final String ID_MORE_INFO_BOX_LABEL = "moreInfoBoxLabel";

    public SmallInfoBoxPanel(String id, IModel<DashboardWidgetType> model, PageBase pageBase) {
        super(id, model, pageBase);
    }

    @Override
    protected void customInitLayout(WebMarkupContainer parentInfoBox) {
        IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
        WebMarkupContainer moreInfoBox = new WebMarkupContainer(ID_MORE_INFO_BOX);
        parentInfoBox.add(moreInfoBox);
        WebMarkupContainer moreInfoBoxIcon = new WebMarkupContainer(ID_MORE_INFO_BOX_ICON);
        moreInfoBox.add(moreInfoBoxIcon);
        Label moreInfoBoxLabel = new Label(ID_MORE_INFO_BOX_LABEL, getPageBase().createStringResource("PageDashboard.infobox.moreInfo"));
        moreInfoBox.add(moreInfoBoxLabel);

        if (existLinkRef()) {
            moreInfoBox.add(new AjaxEventBehavior("click") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    WebPage page = getLinkRef();
                    getPageBase().navigateToNext(page);
                }
            });
            moreInfoBox.add(AttributeModifier.append("class", "cursor-pointer"));
        } else {
            LOGGER.warn("Link is not found for widget " + model.getObject().getIdentifier());
            setInvisible(moreInfoBoxIcon);
            setInvisible(moreInfoBoxLabel);
            moreInfoBox.add(AttributeModifier.append("style", "height: 26px; background:rgba(0, 0, 0, 0.1) !important;"));
        }

    }

//    @Override
//    protected void customInitLayout(WebMarkupContainer parentInfoBox, IModel<InfoBoxType> model,
//            Class<? extends IRequestablePage> linkPage) {
//
//        WebMarkupContainer moreInfoBox = new WebMarkupContainer(ID_MORE_INFO_BOX);
//        parentInfoBox.add(moreInfoBox);
//        WebMarkupContainer moreInfoBoxIcon = new WebMarkupContainer(ID_MORE_INFO_BOX_ICON);
//        moreInfoBox.add(moreInfoBoxIcon);
//        Label moreInfoBoxLabel = new Label(ID_MORE_INFO_BOX_LABEL, this.pageBase.createStringResource("PageDashboard.infobox.moreInfo"));
//        moreInfoBox.add(moreInfoBoxLabel);
//
//        if (linkPage != null) {
//            moreInfoBox.add(new AjaxEventBehavior("click") {
//                private static final long serialVersionUID = 1L;
//
//                @Override
//                protected void onEvent(AjaxRequestTarget target) {
//                    setResponsePage(linkPage);
//                }
//            });
//            moreInfoBox.add(AttributeModifier.append("class", "cursor-pointer"));
//        } else {
//            setInvisible(moreInfoBoxIcon);
//            setInvisible(moreInfoBoxLabel);
//            moreInfoBox.add(AttributeModifier.append("style", "height: 26px;"));
//        }
//    }

    private void setInvisible(Component component) {
        component.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return false;
            }
        });
    }
}
