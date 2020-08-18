/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.engine;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.fileformat.FileFormatController;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

/**
 * @author skublik
 */

public class DashboardEngineController extends EngineController {

    private static final Trace LOGGER = TraceManager.getTrace(DashboardEngineController.class);

    public DashboardEngineController (ReportServiceImpl reportService) {
        super(reportService);
    }

    @Override
    public String createReport(ReportType parentReport, FileFormatController fileFormatController, Task task, OperationResult result) throws Exception {
        if (parentReport.getDashboard() != null && parentReport.getDashboard().getDashboardRef() != null) {
            DashboardReportEngineConfigurationType dashboardConfig = parentReport.getDashboard();

            String reportFilePath = getDestinationFileName(parentReport, fileFormatController);
            FileUtils.writeByteArrayToFile(new File(reportFilePath), fileFormatController.processDashboard(dashboardConfig, task, result));
            return reportFilePath;
        } else {
            LOGGER.error("Dashboard or DashboardRef is null");
            throw new IllegalArgumentException("Dashboard or DashboardRef is null");
        }
    }

    @Override
    public FileFormatTypeType getDefaultFileFormat() {
        return FileFormatTypeType.HTML;
    }

    @Override
    public void importReport(ReportType report, List<VariablesMap> listOfVariables, FileFormatController fileFormatController, RunningTask task, OperationResult result) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation import for dashboard engine");
    }
}
