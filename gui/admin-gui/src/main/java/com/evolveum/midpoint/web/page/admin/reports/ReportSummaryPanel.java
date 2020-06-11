/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.wicket.model.IModel;

/**
 * @author skublik
 */

public class ReportSummaryPanel extends ObjectSummaryPanel<ReportType> {

    public ReportSummaryPanel(String id, IModel<ReportType> model, ModelServiceLocator serviceLocator) {
        super(id, ReportType.class, model, serviceLocator);
    }

    @Override
    protected String getIconCssClass() {
        return GuiStyleConstants.CLASS_REPORT_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-shadow";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-shadow";
    }
}
