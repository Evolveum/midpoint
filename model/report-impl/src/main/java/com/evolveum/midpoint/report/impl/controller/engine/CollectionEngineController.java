/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.engine;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
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

public class CollectionEngineController extends EngineController {

    private static final Trace LOGGER = TraceManager.getTrace(CollectionEngineController.class);

    public CollectionEngineController(ReportServiceImpl reportService) {
        super(reportService);
    }

    @Override
    public String createReport(ReportType parentReport, FileFormatController fileFormatController, Task task, OperationResult result) throws Exception {
        if (parentReport.getObjectCollection() != null && parentReport.getObjectCollection().getCollection() != null) {
            ObjectCollectionReportEngineConfigurationType collectionConfig = parentReport.getObjectCollection();

            String reportFilePath = getDestinationFileName(parentReport, fileFormatController);
            FileUtils.writeByteArrayToFile(new File(reportFilePath), fileFormatController.processCollection(parentReport.getName().getOrig(), collectionConfig, task, result));
            return reportFilePath;
        } else {
            LOGGER.error("CollectionRefSpecification is null");
            throw new IllegalArgumentException("CollectionRefSpecification is null");
        }
    }

    @Override
    public FileFormatTypeType getDefaultFileFormat() {
        return FileFormatTypeType.CSV;
    }

    @Override
    public void importReport(ReportType report, List<VariablesMap> listOfVariables, FileFormatController fileFormatController, RunningTask task, OperationResult result) throws Exception {
        if (listOfVariables == null || listOfVariables.isEmpty()) {
            throw new IllegalArgumentException("Variables for import report is null or empty");
        }
        int i = 0;
        task.setExpectedTotal((long) listOfVariables.size());
        recordProgress(task, i, result, LOGGER);
        for (VariablesMap variales : listOfVariables) {
            fileFormatController.importCollectionReport(report, variales, task, result);
            i++;
            recordProgress(task, i, result, LOGGER);
        }
    }
}
