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
        if (parentReport.getObjectCollection() != null && parentReport.getObjectCollection().getCollection() != null
                && parentReport.getObjectCollection().getCollection().getCollectionRef() != null) {
            ObjectCollectionReportEngineConfigurationType collectionConfig = parentReport.getObjectCollection();

            String reportFilePath = getDestinationFileName(parentReport, exportController);
            FileUtils.writeByteArrayToFile(new File(reportFilePath), exportController.processCollection(collectionConfig, task, result));
            return reportFilePath;
        } else {
            LOGGER.error("Collection or CollectionRef is null");
            throw new IllegalArgumentException("Collection or CollectionRef is null");
        }
    }

    @Override
    public ExportConfigurationType getDefaultExport() {
        ExportConfigurationType export = new ExportConfigurationType();
        export.setType(ExportType.CSV);
        CsvExportType csv = new CsvExportType();
        csv.setFieldDelimiter(";");
        csv.setEncoding("utf-8");
        csv.setMultivalueDelimiter(",");
        csv.setCreateHeader(true);
        csv.setEscape("\\");
        csv.setQuote("\"");
        csv.setQuoteMode(QuoteMode.MINIMAL.name());
        csv.setRecordSeparator("\r\n");
        csv.setTrailingDelimiter(false);
        csv.setTrim(false);
        export.setCsv(csv);
        return export;
    }
}
