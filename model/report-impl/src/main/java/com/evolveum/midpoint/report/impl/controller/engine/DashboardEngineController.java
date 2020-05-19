/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.engine;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.export.ExportController;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;

/**
 * @author skublik
 */

public class DashboardEngineController extends EngineController {

    private static final Trace LOGGER = TraceManager.getTrace(DashboardEngineController.class);

    public DashboardEngineController (ReportServiceImpl reportService) {
        super(reportService);
    }

    @Override
    public String createReport(ReportType parentReport, ExportController exportController, Task task, OperationResult result) throws Exception {
        if (parentReport.getDashboard() != null && parentReport.getDashboard().getDashboardRef() != null) {
            DashboardReportEngineConfigurationType dashboardConfig = parentReport.getDashboard();

            String reportFilePath = getDestinationFileName(parentReport, exportController);
            FileUtils.writeByteArrayToFile(new File(reportFilePath), exportController.processDashboard(dashboardConfig, task, result));
            return reportFilePath;
        } else {
            LOGGER.error("Dashboard or DashboardRef is null");
            throw new IllegalArgumentException("Dashboard or DashboardRef is null");
        }
    }

    @Override
    public ExportConfigurationType getDefaultExport() {
        ExportConfigurationType export = new ExportConfigurationType();
        export.setType(ExportType.HTML);
        export.setHtml(new HtmlExportType());
        return export;
    }
}
