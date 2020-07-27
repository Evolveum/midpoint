/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.engine;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.fileformat.FileFormatController;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
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

    public abstract String createReport(ReportType parentReport, FileFormatController fileFormatController, Task task, OperationResult result) throws Exception;

    public abstract FileFormatTypeType getDefaultFileFormat();

    String getDestinationFileName(ReportType reportType, FileFormatController fileFormatController) {
        File exportDir = getExportDir();
        if (!exportDir.exists() || !exportDir.isDirectory()) {
            if (!exportDir.mkdir()) {
                LOGGER.error("Couldn't create export dir {}", exportDir);
            }
        }

        String fileNamePrefix = reportType.getName().getOrig() + "-EXPORT " + getDateTime();
        String fileName = fileNamePrefix + fileFormatController.getTypeSuffix();
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

    public abstract void importReport(ReportType report, PrismContainer container, FileFormatController fileFormatController, RunningTask task, OperationResult result) throws Exception;

    protected void recordProgress(Task task, long progress, OperationResult opResult, Trace logger) {
        try {
            task.setProgressImmediate(progress, opResult);
        } catch (ObjectNotFoundException e) {             // these exceptions are of so little probability and harmless, so we just log them and do not report higher
            LoggingUtils.logException(logger, "Couldn't record progress to task {}, probably because the task does not exist anymore", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logException(logger, "Couldn't record progress to task {}, due to unexpected schema exception", e, task);
        }
    }
}
