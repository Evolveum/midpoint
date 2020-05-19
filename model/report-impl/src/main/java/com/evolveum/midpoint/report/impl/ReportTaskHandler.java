/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.commandline.CommandLineScriptExecutor;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.report.impl.controller.engine.CollectionEngineController;
import com.evolveum.midpoint.report.impl.controller.engine.DashboardEngineController;
import com.evolveum.midpoint.report.impl.controller.engine.EngineController;
import com.evolveum.midpoint.report.impl.controller.export.CsvExporterController;
import com.evolveum.midpoint.report.impl.controller.export.ExportController;
import com.evolveum.midpoint.report.impl.controller.export.HtmlExportController;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.model.api.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.AuditConstants;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author skublik
 */

@Component
public class ReportTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ReportTaskHandler.class);

    static final String REPORT_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/handler-3";
    private static final String OP_CREATE_REPORT_OUTPUT = ReportTaskHandler.class.getName() + "createReportOutput";

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
            ReportType parentReport = reportService.getObjectResolver().resolve(task.getObjectRefOrClone(), ReportType.class, null,
                    "resolving report", task, result);

            if (!reportService.isAuthorizedToRunReport(parentReport.asPrismObject(), task, parentResult)) {
                LOGGER.error("Task {} is not authorized to run report {}", task, parentReport);
                throw new SecurityViolationException("Not authorized");
            }

            if (parentReport.getReportEngine() == null) {
                throw new IllegalArgumentException("Report Object doesn't have ReportEngine attribute");
            }

            EngineController engineController = resolveEngine(parentReport);
            ExportController exportController = resolveExport(parentReport, engineController);

            String reportFilePath = engineController.createReport(parentReport, exportController, task, result);

            saveReportOutputType(reportFilePath, parentReport, exportController, task, result);
            LOGGER.trace("create report output type : {}", reportFilePath);

            if (parentReport.getPostReportScript() != null) {
                processPostReportScript(parentReport, reportFilePath, task, result);
            }
            result.computeStatus();

        } catch (Exception ex) {
            LOGGER.error("CreateReport: {}", ex.getMessage(), ex);
            result.recordFatalError(ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        LOGGER.trace("CreateReportTaskHandler.run stopping");
        return runResult;
    }

    private EngineController resolveEngine(ReportType parentReport) {
        if (parentReport.getReportEngine().equals(ReportEngineSelectionType.DASHBOARD)) {
            return new DashboardEngineController(reportService);
        }
        if (parentReport.getReportEngine().equals(ReportEngineSelectionType.COLLECTION)) {
            return new CollectionEngineController(reportService);
        }
        LOGGER.error("Dashboard or DashboardRef is null");
        throw new IllegalArgumentException("Dashboard or DashboardRef is null");
    }

    private ExportController resolveExport(ReportType parentReport, EngineController engine) {
        ExportConfigurationType export;
        if (parentReport.getExport() == null || isEmpty(parentReport.getExport())) {
            export = engine.getDefaultExport();
        } else {
            export = parentReport.getExport();
        }
        switch (export.getType()) {
            case HTML:
                return new HtmlExportController(export, reportService);
            case CSV:
                return new CsvExporterController(export, reportService);
            default:
                LOGGER.error("Unsupported ExportType " + export);
                throw new IllegalArgumentException("Unsupported ExportType " + export);
        }
    }

    private boolean isEmpty(ExportConfigurationType export) {
        if (export.getCsv() != null) {
            return false;
        }
        if (export.getHtml() != null) {
            return false;
        }
        return true;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.REPORT;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_REPORT_TASK.value();
    }

    private void saveReportOutputType(String filePath, ReportType reportType, ExportController exportController, Task task,
            OperationResult parentResult) throws Exception {

        String fileName = FilenameUtils.getBaseName(filePath);
        String reportOutputName = fileName + " - " + exportController.getType();

        ReportOutputType reportOutputType = new ReportOutputType();
        reportService.getPrismContext().adopt(reportOutputType);

        reportOutputType.setFilePath(filePath);
        reportOutputType.setReportRef(MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE));
        reportOutputType.setName(new PolyStringType(reportOutputName));
        if (reportType.getDescription() != null) {
            reportOutputType.setDescription(reportType.getDescription() + " - " + exportController.getType());
        }
        reportOutputType.setExportType(exportController.getExportConfiguration());


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

        reportOutputType.setNodeRef(ObjectTypeUtil.createObjectRef(nodes.iterator().next(), reportService.getPrismContext()));

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ReportOutputType> objectDelta = DeltaFactory.Object.createAddDelta(reportOutputType.asPrismObject());
        deltas.add(objectDelta);
        OperationResult subResult = parentResult.createSubresult(OP_CREATE_REPORT_OUTPUT);

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = reportService.getModelService().executeChanges(deltas, null, task, subResult);
        String reportOutputOid = ObjectDeltaOperation.findAddDeltaOid(executedDeltas, reportOutputType.asPrismObject());

        LOGGER.debug("Created report output with OID {}", reportOutputOid);
        //noinspection unchecked
        PrismProperty<String> outputOidProperty = reportService.getPrismContext().getSchemaRegistry()
                .findPropertyDefinitionByElementName(ReportConstants.REPORT_OUTPUT_OID_PROPERTY_NAME).instantiate();
        outputOidProperty.setRealValue(reportOutputOid);
        task.setExtensionPropertyImmediate(outputOidProperty, subResult);

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
