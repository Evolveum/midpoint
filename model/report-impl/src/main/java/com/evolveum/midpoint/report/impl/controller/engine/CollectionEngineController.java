/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.engine;

import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.export.ExportController;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * @author skublik
 */

public class CollectionEngineController extends EngineController {

    private static final Trace LOGGER = TraceManager.getTrace(CollectionEngineController.class);

    public CollectionEngineController(ReportServiceImpl reportService) {
        super(reportService);
    }

    @Override
    public String createReport(ReportType parentReport, ExportController exportController, Task task, OperationResult result) throws Exception {
        if (parentReport.getObjectCollection() != null && parentReport.getObjectCollection().getCollection() != null) {
            ObjectCollectionReportEngineConfigurationType collectionConfig = parentReport.getObjectCollection();

            String reportFilePath = getDestinationFileName(parentReport, exportController);
            FileUtils.writeByteArrayToFile(new File(reportFilePath), exportController.processCollection(parentReport.getName().getOrig(), collectionConfig, task, result));
            return reportFilePath;
        } else {
            LOGGER.error("CollectionRefSpecification is null");
            throw new IllegalArgumentException("CollectionRefSpecification is null");
        }
    }

    @Override
    public ExportType getDefaultExport() {
        return ExportType.CSV;
    }
}
