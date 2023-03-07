/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.activity;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ReportOutputCreatedListener;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.reports.ReportSupportUtil;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.DashboardReportDataWriter;
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
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.StoreExportedWidgetDataType.*;

/**
 * Saves report data to the filesystem and creates {@link ReportDataType} objects in repository.
 */
class SaveReportFileSupport {

    private static final Trace LOGGER = TraceManager.getTrace(SaveReportFileSupport.class);

    private static final String OP_CREATE_REPORT_DATA = SaveReportFileSupport.class.getName() + "createReportData";

    @NotNull private final AbstractActivityRun<?, ?, ?> activityRun;
    @NotNull private final RunningTask runningTask;
    @NotNull private final ReportServiceImpl reportService;

    /** Resolved report object. */
    @NotNull private final ReportType report;

    /** Type of storing exported data. */
    @NotNull private final StoreExportedWidgetDataType storeType;

    SaveReportFileSupport(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull ReportType report,
            @NotNull ReportServiceImpl reportService) {
        this.activityRun = activityRun;
        this.report = report;
        this.runningTask = activityRun.getRunningTask();
        this.reportService = reportService;

        StoreExportedWidgetDataType storeType = report.getDashboard() == null ?
                null :
                report.getDashboard().getStoreExportedWidgetData();
        this.storeType = Objects.requireNonNullElse(storeType, ONLY_FILE);
    }

    /** @see ExportActivitySupport#saveSimpleReportData(ReportDataWriter, OperationResult) */
    void saveSimpleReportData(
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            OperationResult result) throws CommonException {
        saveReportData(dataWriter.completeReport(), dataWriter, null, result);
    }

    /** @see #saveAggregatedReportData(String, ReportDataWriter, ObjectReferenceType, OperationResult) */
    void saveAggregatedReportData(
            @NotNull String aggregatedData,
            @NotNull ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> completingDataWriter,
            @NotNull ObjectReferenceType preExistingDataRef,
            @NotNull OperationResult result) throws CommonException {
        saveReportData(
                completingDataWriter.completeReport(aggregatedData),
                completingDataWriter,
                preExistingDataRef,
                result);
    }

    private void saveReportData(
            String completedReport,
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            @Nullable ObjectReferenceType emptyExportedDataObjectRef,
            OperationResult result) throws CommonException {

        if (!activityRun.getRunningTask().canRun()) {
            LOGGER.warn("Not storing the resulting report, as the activity is being suspended: {}", report);
            return;
        }

        String aggregatedFilePath = getDestinationFileName(report, dataWriter);

        if (storeType == ONLY_FILE || storeType == WIDGET_AND_FILE)  {
            writeToReportFile(completedReport, aggregatedFilePath, dataWriter.getEncoding());
            saveReportDataObject(dataWriter, aggregatedFilePath, emptyExportedDataObjectRef, result);
            if (report.getPostReportScript() != null) {
                processPostReportScript(report, aggregatedFilePath, runningTask, result);
            }
        }
        if ((storeType == ONLY_WIDGET || storeType == WIDGET_AND_FILE)
                && dataWriter instanceof DashboardReportDataWriter) {
            DashboardType dashboard = reportService.getObjectResolver().resolve(
                    report.getDashboard().getDashboardRef(),
                    DashboardType.class,
                    null,
                    "resolve dashboard",
                    runningTask,
                    result);
            List<DashboardWidgetType> widgets = dashboard.getWidget();
            Map<String, String> widgetsData = ((DashboardReportDataWriter) dataWriter).getWidgetsData();
            List<ItemDelta<?, ?>> dashboardModifications = new ArrayList<>();
            widgets.forEach(widget -> {
                String widgetData = widgetsData.get(widget.getIdentifier());
                if (StringUtils.isEmpty(widgetData)) {
                    return;
                }
                DashboardWidgetDataType data = widget.getData();
                if (data == null) {
                    data = new DashboardWidgetDataType().storedData(widgetData);
                    PrismContainerDefinition<Containerable> def = dashboard.asPrismObject().getDefinition().findContainerDefinition(
                            ItemPath.create(DashboardType.F_WIDGET, DashboardWidgetType.F_DATA));
                    ContainerDelta<Containerable> delta = def.createEmptyDelta(
                            ItemPath.create(widget.asPrismContainerValue().getPath(), DashboardWidgetType.F_DATA));
                    delta.addValuesToAdd(data.asPrismContainerValue());
                    dashboardModifications.add(delta);
                    return;
                }

                PrismPropertyDefinition<Object> def = dashboard.asPrismObject().getDefinition().findPropertyDefinition(
                        ItemPath.create(DashboardType.F_WIDGET,
                                DashboardWidgetType.F_DATA,
                                DashboardWidgetDataType.F_STORED_DATA));
                PropertyDelta<Object> delta = def.createEmptyDelta(
                        ItemPath.create(widget.asPrismContainerValue().getPath(),
                                DashboardWidgetType.F_DATA,
                                DashboardWidgetDataType.F_STORED_DATA));
                if (data.getStoredData() == null) {
                    PrismPropertyValue<Object> newValue = PrismContext.get().itemFactory().createPropertyValue(widgetData);
                    //noinspection unchecked
                    delta.addValuesToAdd(newValue);
                } else {
                    delta.setRealValuesToReplace(widgetData);
                }
                dashboardModifications.add(delta);
            });
            reportService.getRepositoryService().modifyObject(
                    DashboardType.class, dashboard.getOid(), dashboardModifications, null, result);
        }
    }

    private String getDestinationFileName(ReportType reportType,
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter) {
        File exportDir = ReportSupportUtil.getOrCreateExportDir();

        String reportName = StringUtils.replace(reportType.getName().getOrig(), File.separator, "_");
        String fileNamePrefix = reportName + "-EXPORT " + getDateTime();
        String fileName = fileNamePrefix + dataWriter.getTypeSuffix();
        return new File(exportDir, MiscUtil.fixFileName(fileName)).getPath();
    }

    static String getNameOfExportedReportData(ReportType reportType, String type) {
        String fileName = reportType.getName().getOrig() + "-EXPORT " + getDateTime();
        return fileName + " - " + type;
    }

    private static String getDateTime() {
        Date createDate = new Date(System.currentTimeMillis());
        SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss.SSS");
        return formatDate.format(createDate);
    }

    private void writeToReportFile(String contextOfFile, String aggregatedFilePath, @NotNull Charset encoding) {
        try {
            FileUtils.writeByteArrayToFile(
                    new File(aggregatedFilePath),
                    contextOfFile.getBytes(encoding));
        } catch (IOException e) {
            throw new SystemException("Couldn't write aggregated report to " + aggregatedFilePath, e);
        }
    }

    private void saveReportDataObject(
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            String filePath,
            @Nullable ObjectReferenceType emptyExportedDataObjectRef,
            OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createSubresult(OP_CREATE_REPORT_DATA);
        try {
            ReportDataType reportDataObject = createReportDataObject(dataWriter, filePath, emptyExportedDataObjectRef, result);
            String reportDataOid = putReportDataObjectToRepository(reportDataObject, result);
            recordDataOidIntoTask(reportDataOid, result);
            sendReportCreatedEvent(reportDataObject, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
        LOGGER.info("Report was saved - the file is {}", filePath); // TODO change to .debug?
    }

    private void recordDataOidIntoTask(String reportDataOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        // Write to the task extension ("legacy way" - however, it is still used from there e.g. by GUI)
        runningTask.setItemRealValues(
                TaskType.F_EXTENSION.append(ReportConstants.REPORT_DATA_PROPERTY_NAME),
                createRef(reportDataOid));

        // Write to activity state ("new way")
        // 1. For distributed reports, this is into the state of aggregation sub-activity. The ref is already present in parent.
        // 2. For classic activities, this is into the state of the (only) activity.
        activityRun.getActivityState().setWorkStateItemRealValues(
                ReportExportWorkStateType.F_REPORT_DATA_REF, createRef(reportDataOid));

        // Save both deltas
        runningTask.flushPendingModifications(result);
    }

    // Using repeatedly to avoid "attempt to reset value of parent" exceptions when sharing the reference
    private static ObjectReferenceType createRef(String reportDataOid) {
        return ObjectTypeUtil.createObjectRef(reportDataOid, ObjectTypes.REPORT_DATA);
    }

    private @NotNull ReportDataType createReportDataObject(
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            String filePath,
            @Nullable ObjectReferenceType emptyExportedDataObjectRef,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        String reportDataName = getNameOfExportedReportData(report, dataWriter.getType());

        ReportDataType reportDataObject = new ReportDataType();

        reportDataObject.setFilePath(filePath);
        reportDataObject.setReportRef(MiscSchemaUtil.createObjectReference(report.getOid(), ReportType.COMPLEX_TYPE));
        reportDataObject.setName(new PolyStringType(reportDataName));
        if (report.getDescription() != null) {
            reportDataObject.setDescription(report.getDescription() + " - " + dataWriter.getType());
        }
        if (dataWriter.getFileFormatConfiguration() != null) {
            reportDataObject.setFileFormat(dataWriter.getFileFormatConfiguration().getType());
        }

        reportDataObject.setNodeRef(
                getCurrentNodeRef(result));

        if (emptyExportedDataObjectRef != null) {
            reportDataObject.setOid(emptyExportedDataObjectRef.getOid());
        }

        return reportDataObject;
    }

    private String putReportDataObjectToRepository(ReportDataType reportDataObject, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ModelService model = reportService.getModelService();
        String oid = reportDataObject.getOid();
        if (oid != null) {
            LOGGER.trace("Modifying pre-existing report data object: {}", oid);
            // Using repository to avoid performance and authorization problems. The READ operation here is purely technical,
            // to be able to push our new state into the repository.
            PrismObject<ReportDataType> existing =
                    ModelCommonBeans.get().cacheRepositoryService.getObject(ReportDataType.class, oid, null, result);
            model.executeChanges(
                    List.of(existing.diff(reportDataObject.asPrismObject())),
                    null, runningTask, result);
            return oid;
        } else {
            LOGGER.trace("Adding new report data object: {}", reportDataObject);
            var executedDeltas = model.executeChanges(
                    List.of(reportDataObject.asPrismObject().createAddDelta()), null, runningTask, result);
            var newOid = MiscUtil.stateNonNull(
                    ObjectDeltaOperation.findAddDeltaOid(executedDeltas, reportDataObject.asPrismObject()),
                    () -> "No OID for newly created report data object: " + reportDataObject);
            LOGGER.debug("Created report output with OID {}", newOid);
            return newOid;
        }
    }

    /**
     * Returns a reference to the current node.
     *
     * TODO this functionality could (and should) be provided directly by {@link TaskManager}.
     *
     * But even here, it could be replaced by something like:
     *
     *      createObjectRef(taskManager.getLocalNode())
     */
    private ObjectReferenceType getCurrentNodeRef(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        String nodeId = runningTask.getNode();
        SearchResultList<PrismObject<NodeType>> nodes = reportService.getModelService().searchObjects(
                NodeType.class,
                reportService.getPrismContext()
                        .queryFor(NodeType.class).item(NodeType.F_NODE_IDENTIFIER).eq(nodeId).build(),
                null,
                runningTask,
                parentResult);
        if (nodes == null || nodes.isEmpty()) {
            LOGGER.error("Could not found node for storing the report.");
            throw new ObjectNotFoundException("Could not find node where to save report", NodeType.class, nodeId);
        }

        if (nodes.size() > 1) {
            LOGGER.error("Found more than one node with ID {}.", nodeId);
            throw new IllegalStateException("Found more than one node with ID " + nodeId);
        }

        return ObjectTypeUtil.createObjectRef(nodes.iterator().next());
    }

    /**
     * Creates and sends "report output created" event. This is a temporary solution that uses
     * {@link ReportOutputCreatedListener}. It will be replaced by directly creating and sending those
     * events as soon as it will be possible. (I.e. as soon as the events will be directly creatable from
     * modules other than `notification-impl`.)
     */
    private void sendReportCreatedEvent(ReportDataType reportDataObject, OperationResult subResult) {
        for (ReportOutputCreatedListener listener : reportService.getReportCreatedListeners()) {
            try {
                listener.onReportOutputCreated(
                        activityRun,
                        report,
                        reportDataObject,
                        activityRun.getRunningTask(),
                        subResult);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER,
                        "'Report created' listener {} failed when processing 'report created' event for {}", e,
                        listener, reportDataObject);
            }
        }
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
