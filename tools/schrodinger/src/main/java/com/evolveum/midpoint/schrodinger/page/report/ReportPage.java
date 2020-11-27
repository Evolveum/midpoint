/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.report;

import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.report.ReportEngineTab;
import com.evolveum.midpoint.schrodinger.component.report.ReportExportTab;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
/**
 * Created by honchar
 */
public class ReportPage extends AssignmentHolderDetailsPage<ReportPage> {

    public ReportPage() {
    }

    @Override
    public AssignmentsTab<ReportPage> selectTabAssignments() {
        return null;
    }

    @Override
    public AssignmentHolderBasicTab<ReportPage> selectTabBasic() {
        return new AssignmentHolderBasicTab<>(this, getTabSelenideElement("pageReport.basic.title"));
    }

    public ReportExportTab selectCollectionReportTabExport() {
        return new ReportExportTab(this, getTabSelenideElement("pageReport.export.title"));
    }

    public ReportEngineTab selectCollectionReportTabEngine() {
        return new ReportEngineTab(this, getTabSelenideElement("pageReport.engine.title"));
    }
}
