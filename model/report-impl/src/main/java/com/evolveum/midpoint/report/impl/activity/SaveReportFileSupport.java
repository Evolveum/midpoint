/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.engine.CollectionEngineController;
import com.evolveum.midpoint.report.impl.controller.fileformat.AbstractReportDataWriter;
import com.evolveum.midpoint.report.impl.controller.fileformat.FileFormatController;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataWriter;
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
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

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

    /**
     * Used to derive the file path and to save report data.
     *
     * TODO remove dependency on this class
     */
    private FileFormatController fileFormatController;

    /**
     * File to which the aggregated data are stored.
     */
    private String aggregatedFilePath;

    SaveReportFileSupport(ReportType report, RunningTask task, ReportServiceImpl reportService) {
        this.report = report;
        runningTask = task;
        this.reportService = reportService;
    }

    void initializeExecution(OperationResult result) {
        CollectionEngineController engineController = new CollectionEngineController(reportService);

        fileFormatController = ReportUtils.createExportController(
                report,
                engineController.getDefaultFileFormat(),
                reportService);

        aggregatedFilePath =
                replaceColons(
                        engineController.getDestinationFileName(report, fileFormatController));

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

    public void saveReportFile(String aggregatedData, ReportDataWriter dataWriter, OperationResult result) throws CommonException {
        writeToReportFile(dataWriter.completizeReport(aggregatedData));
        saveReportDataObject(result);
        if (report.getPostReportScript() != null) {
            processPostReportScript(report, aggregatedFilePath, runningTask, result);
        }
    }

    public void saveReportFile(ReportDataWriter dataWriter, OperationResult result) throws CommonException {
        writeToReportFile(dataWriter.completizeReport());
        saveReportDataObject(result);
        if (report.getPostReportScript() != null) {
            processPostReportScript(report, aggregatedFilePath, runningTask, result);
        }
    }

    private void writeToReportFile(String contextOfFile) {
        try {
            FileUtils.writeByteArrayToFile(
                    new File(aggregatedFilePath),
                    contextOfFile.getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new SystemException("Couldn't write aggregated report to " + aggregatedFilePath, e);
        }
    }

    private void saveReportDataObject(OperationResult result) throws CommonException {
        saveReportDataType(
                aggregatedFilePath,
                report,
                fileFormatController,
                runningTask,
                result);

        LOGGER.info("Aggregated report was saved - the file is {}", aggregatedFilePath);
    }

    private void saveReportDataType(String filePath, ReportType reportType, FileFormatController fileFormatController, Task task,
            OperationResult parentResult) throws CommonException {

        String fileName = FilenameUtils.getBaseName(filePath);
        String reportDataName = fileName + " - " + fileFormatController.getType();

        ReportDataType reportDataType = new ReportDataType();
        reportService.getPrismContext().adopt(reportDataType);

        reportDataType.setFilePath(filePath);
        reportDataType.setReportRef(MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE));
        reportDataType.setName(new PolyStringType(reportDataName));
        if (reportType.getDescription() != null) {
            reportDataType.setDescription(reportType.getDescription() + " - " + fileFormatController.getType());
        }
        if (fileFormatController != null && fileFormatController.getFileFormatConfiguration() != null) {
            reportDataType.setFileFormat(fileFormatController.getFileFormatConfiguration().getType());
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
        //noinspection unchecked
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
