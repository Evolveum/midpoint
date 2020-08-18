/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author skublik
 *
 */
public class ReportConfigurationPanel extends BasePanel<ReportDto> {

    private static final String ID_BASIC_PANEL = "basicPanel";

    public ReportConfigurationPanel(String id, IModel<ReportDto> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        @NotNull ReportType report = getModel().getObject().getObject().asObjectable();

        if(report.getJasper() != null) {
            add(new JasperReportBasicConfigurationPanel(ID_BASIC_PANEL, getModel()));
        } else {
            add(new DashboardReportBasicConfigurationPanel(ID_BASIC_PANEL, getModel()));
        }
    }
}
