/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.ExportedReportDataRow;
import com.evolveum.midpoint.report.impl.controller.ExportedReportHeaderRow;
import com.evolveum.midpoint.report.impl.controller.ReportDataWriter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Contains common functionality for save exported report file executions.
 * This is an experiment - using object composition instead of inheritance.
 */
class SaveReportFileSupport {

    private static final Trace LOGGER = TraceManager.getTrace(SaveReportFileSupport.class);

    private static final String OP_CREATE_REPORT_DATA = SaveReportFileSupport.class.getName() + "createReportData";

    @NotNull protected final RunningTask runningTask;
    @NotNull protected final ReportServiceImpl reportService;

    /**
     * Resolved report object.
     */
    private final ReportType report;

    SaveReportFileSupport(ReportType report, @NotNull RunningTask task, @NotNull ReportServiceImpl reportService) {
        this.report = report;
        runningTask = task;
        this.reportService = reportService;
    }

    /**
     * Very strange: colons are no problem for Windows, but Apache file utils complain for them (when running on Windows).
     * So they will be replaced, at least temporarily.
     *
     * TODO research this
     */
    private String replaceColons(String path) {
        if (onWindows()) {
            return path.replaceAll(":", "_");
        } else {
            return path;
        }
    }

    private boolean onWindows() {
        return File.separatorChar == '\\';
    }

    public void saveReportFile(String aggregatedData,
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            OperationResult result) throws CommonException {

        String aggregatedFilePath = getDestinationFileName(report, dataWriter);

        writeToReportFile(dataWriter.completizeReport(aggregatedData), aggregatedFilePath);
        saveReportDataObject(dataWriter, aggregatedFilePath, result);
        if (report.getPostReportScript() != null) {
            processPostReportScript(report, aggregatedFilePath, runningTask, result);
        }
    }

    public void saveReportFile(ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            OperationResult result) throws CommonException {

        String aggregatedFilePath = getDestinationFileName(report, dataWriter);

        writeToReportFile(dataWriter.completizeReport(), aggregatedFilePath);
        saveReportDataObject(dataWriter, aggregatedFilePath, result);
        if (report.getPostReportScript() != null) {
            processPostReportScript(report, aggregatedFilePath, runningTask, result);
        }
    }

    public String getDestinationFileName(ReportType reportType,
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter) {
        File exportDir = getExportDir();
        if (!exportDir.exists() || !exportDir.isDirectory()) {
            if (!exportDir.mkdir()) {
                LOGGER.error("Couldn't create export dir {}", exportDir);
            }
        }

        String fileNamePrefix = reportType.getName().getOrig() + "-EXPORT " + getDateTime();
        String fileName = fileNamePrefix + dataWriter.getTypeSuffix();
        return replaceColons(new File(exportDir, fileName).getPath());
    }

    private static String getDateTime() {
        Date createDate = new Date(System.currentTimeMillis());
        SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss.SSS");
        return formatDate.format(createDate);
    }

    private File getExportDir() {
        return new File(getMidPointHomeDirName(), "export");
    }

    private String getMidPointHomeDirName() {
        return System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
    }

    private void writeToReportFile(String contextOfFile, String aggregatedFilePath) {
        try {
            FileUtils.writeByteArrayToFile(
                    new File(aggregatedFilePath),
                    contextOfFile.getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new SystemException("Couldn't write aggregated report to " + aggregatedFilePath, e);
        }
    }

    private void saveReportDataObject(
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            String aggregatedFilePath,
            OperationResult result) throws CommonException {
        saveReportDataType(
                aggregatedFilePath,
                report,
                dataWriter,
                runningTask,
                result);

        LOGGER.info("Aggregated report was saved - the file is {}", aggregatedFilePath);
    }

    private void saveReportDataType(String filePath, ReportType reportType,
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            Task task,OperationResult parentResult) throws CommonException {

        String fileName = FilenameUtils.getBaseName(filePath);
        String reportDataName = fileName + " - " + dataWriter.getType();

        ReportDataType reportDataType = new ReportDataType();
        reportService.getPrismContext().adopt(reportDataType);

        reportDataType.setFilePath(filePath);
        reportDataType.setReportRef(MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE));
        reportDataType.setName(new PolyStringType(reportDataName));
        if (reportType.getDescription() != null) {
            reportDataType.setDescription(reportType.getDescription() + " - " + dataWriter.getType());
        }
        if (dataWriter.getFileFormatConfiguration() != null) {
            reportDataType.setFileFormat(dataWriter.getFileFormatConfiguration().getType());
        }

        SearchResultList<PrismObject<NodeType>> nodes = reportService.getModelService().searchObjects(NodeType.class, reportService.getPrismContext()
                .queryFor(NodeType.class).item(NodeType.F_NODE_IDENTIFIER).eq(task.getNode()).build(), null, task, parentResult);
        if (nodes == null || nodes.isEmpty()) {
            LOGGER.error("Could not found node for storing the report.");
            throw new ObjectNotFoundException("Could not find node where to save report");
        }

        if (nodes.size() > 1) {
            LOGGER.error("Found more than one node with ID {}.", task.getNode());
            throw new IllegalStateException("Found more than one node with ID " + task.getNode());
        }

        reportDataType.setNodeRef(ObjectTypeUtil.createObjectRef(nodes.iterator().next(), reportService.getPrismContext()));

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ReportDataType> objectDelta = DeltaFactory.Object.createAddDelta(reportDataType.asPrismObject());
        deltas.add(objectDelta);
        OperationResult subResult = parentResult.createSubresult(OP_CREATE_REPORT_DATA);

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = reportService.getModelService().executeChanges(deltas, null, task, subResult);
        String reportDataOid = ObjectDeltaOperation.findAddDeltaOid(executedDeltas, reportDataType.asPrismObject());

        LOGGER.debug("Created report output with OID {}", reportDataOid);
        PrismReference reportDataRef = reportService.getPrismContext().getSchemaRegistry()
                .findReferenceDefinitionByElementName(ReportConstants.REPORT_DATA_PROPERTY_NAME).instantiate();
        PrismReferenceValue refValue = reportService.getPrismContext().itemFactory().createReferenceValue(reportDataOid, ReportDataType.COMPLEX_TYPE);
        reportDataRef.getValues().add(refValue);
        task.setExtensionReference(reportDataRef);

        subResult.computeStatus();
    }

    private void processPostReportScript(ReportType parentReport, String reportOutputFilePath, Task task, OperationResult parentResult) {
        CommandLineScriptType scriptType = parentReport.getPostReportScript();
        if (scriptType == null) {
            LOGGER.debug("No post report script found in {}, skipping", parentReport);
            return;
        }

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_OBJECT, parentReport, parentReport.asPrismObject().getDefinition());
        PrismObject<TaskType> taskObject = task.getRawTaskObjectClonedIfNecessary();
        variables.put(ExpressionConstants.VAR_TASK, taskObject.asObjectable(), taskObject.getDefinition());
        variables.put(ExpressionConstants.VAR_FILE, reportService.getCommandLineScriptExecutor().getOsSpecificFilePath(reportOutputFilePath), String.class);

        try {
            reportService.getCommandLineScriptExecutor().executeScript(scriptType, variables, "post-report script in " + parentReport, task, parentResult);
        } catch (Exception e) {
            LOGGER.error("An exception has occurred during post report script execution {}", e.getLocalizedMessage(), e);
        }
    }
}
