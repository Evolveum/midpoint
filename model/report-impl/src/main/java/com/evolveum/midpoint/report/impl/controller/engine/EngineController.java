/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.engine;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.export.ExportController;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author skublik
 */

public abstract class EngineController {

    private static final Trace LOGGER = TraceManager.getTrace(EngineController.class);

    private ReportServiceImpl reportService;

    public EngineController(ReportServiceImpl reportService) {
        this.reportService = reportService;
    }

    public abstract String createReport(ReportType parentReport, ExportController exportController, Task task, OperationResult result) throws Exception;

    public abstract ExportType getDefaultExport();

    String getDestinationFileName(ReportType reportType, ExportController exportController) {
        File exportDir = getExportDir();
        if (!exportDir.exists() || !exportDir.isDirectory()) {
            if (!exportDir.mkdir()) {
                LOGGER.error("Couldn't create export dir {}", exportDir);
            }
        }

        String fileNamePrefix = reportType.getName().getOrig() + " " + getDateTime();
        String fileName = fileNamePrefix + exportController.getTypeSuffix();
        return new File(getExportDir(), fileName).getPath();
    }

    private File getExportDir() {
        return new File(getMidPointHomeDirName(), "export");
    }

    private String getMidPointHomeDirName() {
        return System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
    }

    private static String getDateTime() {
        Date createDate = new Date(System.currentTimeMillis());
        SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss.SSS");
        return formatDate.format(createDate);
    }

    protected ReportServiceImpl getReportService() {
        return reportService;
    }
}
