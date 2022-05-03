/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.box;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;

/**
 * @author skublik
 */
public abstract class SmallInfoBoxPanel extends InfoBoxPanel2 {

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
        AjaxLink moreInfoBox = new AjaxLink<String>(ID_MORE_INFO_BOX) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToPage();
            }
        };
        parentInfoBox.add(moreInfoBox);
        WebMarkupContainer moreInfoBoxIcon = new WebMarkupContainer(ID_MORE_INFO_BOX_ICON);
        moreInfoBox.add(moreInfoBoxIcon);

        Label moreInfoBoxLabel = new Label(ID_MORE_INFO_BOX_LABEL, getPageBase().createStringResource("PageDashboard.infobox.moreInfo"));
        moreInfoBoxLabel.setRenderBodyOnly(true);
        moreInfoBox.add(moreInfoBoxLabel);

        moreInfoBox.add(new VisibleBehaviour((SerializableSupplier<Boolean>) () -> existLinkRef()));
    }

    @Override
    public abstract String getDashboardOid();
}
