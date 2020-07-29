/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.io.IOException;
import java.util.*;
import javax.annotation.PostConstruct;

import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.report.impl.controller.engine.CollectionEngineController;
import com.evolveum.midpoint.report.impl.controller.engine.DashboardEngineController;
import com.evolveum.midpoint.report.impl.controller.engine.EngineController;
import com.evolveum.midpoint.report.impl.controller.fileformat.CsvController;
import com.evolveum.midpoint.report.impl.controller.fileformat.FileFormatController;
import com.evolveum.midpoint.report.impl.controller.fileformat.HtmlController;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class ReportTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ReportTaskHandler.class);

    static final String REPORT_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/handler-3";
    private static final String OP_CREATE_REPORT_DATA = ReportTaskHandler.class.getName() + "createReportData";

    @Autowired
    private ReportServiceImpl reportService;

    @PostConstruct
    protected void initialize() {
        LOGGER.trace("Registering with taskManager as a handler for {}", REPORT_TASK_URI);
        reportService.getTaskManager().registerHandler(REPORT_TASK_URI, this);
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partitionDefinition) {
        OperationResult parentResult = task.getResult();
        OperationResult result = parentResult
                .createSubresult(ReportTaskHandler.class.getSimpleName() + ".run");
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(result);

        try {
            ReportType report = reportService.getObjectResolver().resolve(task.getObjectRefOrClone(), ReportType.class, null,
                    "resolving report", task, result);

            ReportBehaviorType behaviour = report.getBehavior();
            DirectionTypeType direction = null;
            if (behaviour != null) {
                direction = behaviour.getDirection();
            }

            String reportDataFilePath;
            if (DirectionTypeType.IMPORT.equals(direction)) {
                ReportDataType reportData;

                PrismReference reportDataRef = task.getExtensionReferenceOrClone(ReportConstants.REPORT_DATA_PROPERTY_NAME);
                reportData = reportService.getObjectResolver().resolve((ObjectReferenceType) reportDataRef.getRealValue(), ReportDataType.class, null,
                        "resolving report data", task, result);
                reportDataFilePath = reportData.getFilePath();

                try {
                    ExecuteScriptType script = null;
                    if (behaviour != null) {
                        script = behaviour.getImportExecuteScript();
                    }
                    if (script == null) {
                        EngineController engineController = resolveEngine(report);
                        FileFormatController fileFormatController = resolveExport(report, engineController.getDefaultFileFormat());
                        List<VariablesMap> variables = fileFormatController.createVariablesFromFile(report, reportData, false, task, result);
                        engineController.importReport(report, variables, fileFormatController, task, result);
                    } else {
                        FileFormatController fileFormatController = resolveExport(report, FileFormatTypeType.CSV);
                        importScriptProcessing(report, reportData, script, fileFormatController, task, result);
                    }
                } catch (IOException e) {
                    String message = "Couldn't create virtual container from file path " + reportData.getFilePath();
                    LOGGER.error(message, e);
                    result.recordFatalError(message, e);
                    runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                    return runResult;
                }

            } else {
                if (!reportService.isAuthorizedToRunReport(report.asPrismObject(), task, parentResult)) {
                    LOGGER.error("Task {} is not authorized to run report {}", task, report);
                    throw new SecurityViolationException("Not authorized");
                }

                EngineController engineController = resolveEngine(report);
                FileFormatController fileFormatController = resolveExport(report, engineController.getDefaultFileFormat());
                reportDataFilePath = engineController.createReport(report, fileFormatController, task, result);

                saveReportDataType(reportDataFilePath, report, fileFormatController, task, result);
                LOGGER.trace("create report output type : {}", reportDataFilePath);
            }


            if (report.getPostReportScript() != null) {
                processPostReportScript(report, reportDataFilePath, task, result);
            }
            result.computeStatus();

        } catch (Exception ex) {
            LOGGER.error("ProcessingReport: {}", ex.getMessage(), ex);
            result.recordFatalError(ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        LOGGER.trace("ProcessingReportTaskHandler.run stopping");
        return runResult;
    }

    private void importScriptProcessing(ReportType report, ReportDataType reportData, ExecuteScriptType script,
            FileFormatController fileFormatController, RunningTask task, OperationResult result) throws Exception {
        List<VariablesMap> listOfVariables = fileFormatController.createVariablesFromFile(report, reportData, true, task, result);

        if (listOfVariables == null || listOfVariables.isEmpty()) {
            throw new IllegalArgumentException("Variables for import report is null or empty");
        }
        long i = 0;
        task.setExpectedTotal((long) listOfVariables.size());
        task.setProgressImmediate(i, result);
        for (VariablesMap variales : listOfVariables) {
            reportService.getScriptingService().evaluateExpression(script, variales, false, task, result);
            i++;
            task.setProgressImmediate(i, result);
        }

    }

    private EngineController resolveEngine(ReportType parentReport) {
        if (parentReport.getDashboard() != null) {
            return new DashboardEngineController(reportService);
        }
        if (parentReport.getObjectCollection() != null) {
            return new CollectionEngineController(reportService);
        }
        LOGGER.error("Report don't contains engine");
        throw new IllegalArgumentException("Report don't contains engine");
    }

    private FileFormatController resolveExport(ReportType parentReport, FileFormatTypeType defaultType) {
        FileFormatConfigurationType fileFormat;
        if (parentReport.getFileFormat() == null || parentReport.getFileFormat().getType() == null) {
            fileFormat = new FileFormatConfigurationType();
            fileFormat.setType(defaultType);
        } else {
            fileFormat = parentReport.getFileFormat();
        }
        switch (fileFormat.getType()) {
            case HTML:
                return new HtmlController(fileFormat, reportService);
            case CSV:
                return new CsvController(fileFormat, reportService);
            default:
                LOGGER.error("Unsupported ExportType " + fileFormat);
                throw new IllegalArgumentException("Unsupported ExportType " + fileFormat);
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.REPORT;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_REPORT_TASK.value();
    }

    private void saveReportDataType(String filePath, ReportType reportType, FileFormatController fileFormatController, Task task,
            OperationResult parentResult) throws Exception {

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

        ExpressionVariables variables = new ExpressionVariables();
        variables.put(ExpressionConstants.VAR_OBJECT, parentReport, parentReport.asPrismObject().getDefinition());
        PrismObject<TaskType> taskObject = task.getUpdatedOrClonedTaskObject();
        variables.put(ExpressionConstants.VAR_TASK, taskObject.asObjectable(), taskObject.getDefinition());
        variables.put(ExpressionConstants.VAR_FILE, reportService.getCommandLineScriptExecutor().getOsSpecificFilePath(reportOutputFilePath), String.class);

        try {
            reportService.getCommandLineScriptExecutor().executeScript(scriptType, variables, "post-report script in "+parentReport, task, parentResult);
        } catch (Exception e) {
            LOGGER.error("An exception has occurred during post report script execution {}", e.getLocalizedMessage(), e);
        }
    }
}
