/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;

/**
 * @author lazyman, garbika
 */
@Service(value = "reportManager")
public class ReportManagerImpl implements ReportManager {

    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/report-hook-1";

    private static final Trace LOGGER = TraceManager.getTrace(ReportManagerImpl.class);

    private static final String CLASS_NAME_WITH_DOT = ReportManagerImpl.class.getSimpleName() + ".";
    private static final String CLEANUP_REPORT_OUTPUTS = CLASS_NAME_WITH_DOT + "cleanupReportOutputs";
    private static final String DELETE_REPORT_OUTPUT = CLASS_NAME_WITH_DOT + "deleteReportOutput";
    private static final String REPORT_OUTPUT_DATA = CLASS_NAME_WITH_DOT + "getReportOutputData";

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private ReportServiceImpl reportService;
    @Autowired private ModelService modelService;
    @Autowired private ClusterExecutionHelper clusterExecutionHelper;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private SecurityEnforcer securityEnforcer;

    private boolean isRaw(Collection<SelectorOptions<GetOperationOptions>> options) {
        return GetOperationOptions.isRaw(SelectorOptions.findRootOptions(options));
    }

    /**
     * Creates and starts task with proper handler, also adds necessary information to task
     * (like ReportType reference and so on).
     *
     * @param report
     * @param task
     * @param parentResult describes report which has to be created
     */

    @Override
    public void runReport(PrismObject<ReportType> report, PrismContainer<ReportParameterType> paramContainer, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {

        task.addArchetypeInformation(SystemObjectsType.ARCHETYPE_REPORT_EXPORT_CLASSIC_TASK.value());

        if (!reportService.isAuthorizedToRunReport(report, task, parentResult)) {
            LOGGER.error("User is not authorized to run report {}", report);
            throw new SecurityViolationException("Not authorized");
        }

        ClassicReportExportWorkDefinitionType reportConfig = new ClassicReportExportWorkDefinitionType()
                .reportRef(new ObjectReferenceType().oid(report.getOid()).type(ReportType.COMPLEX_TYPE));
        if (paramContainer != null && !paramContainer.isEmpty()) {
            reportConfig.reportParam(paramContainer.getRealValue());
        }

        task.getUpdatedTaskObject().getRealValue()
                .activity(new ActivityDefinitionType()
                        .work(new WorkDefinitionsType()
                                .reportExport(reportConfig)
                        )
                );

        task.setThreadStopAction(ThreadStopActionType.CLOSE);
        task.makeSingle();

        taskManager.switchToBackground(task, parentResult);
        parentResult.setBackgroundTaskOid(task.getOid());
    }

    /**
     * Creates and starts task with proper handler, also adds necessary information to task
     * (like ReportType reference and so on).
     *
     * @param report
     * @param task
     * @param parentResult describes report which has to be created
     */

    @Override
    public void importReport(PrismObject<ReportType> report, PrismObject<ReportDataType> reportData, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {

        task.addArchetypeInformation(SystemObjectsType.ARCHETYPE_REPORT_IMPORT_CLASSIC_TASK.value());

        if (!reportService.isAuthorizedToImportReport(report, task, parentResult)) {
            LOGGER.error("User is not authorized to import report {}", report);
            throw new SecurityViolationException("Not authorized");
        }

        ClassicReportImportWorkDefinitionType reportConfig = new ClassicReportImportWorkDefinitionType()
                .reportRef(new ObjectReferenceType().oid(report.getOid()).type(ReportType.COMPLEX_TYPE))
                .reportDataRef(new ObjectReferenceType().oid(reportData.getOid()).type(ReportDataType.COMPLEX_TYPE));

        task.getUpdatedTaskObject().getRealValue()
                .activity(new ActivityDefinitionType()
                        .work(new WorkDefinitionsType()
                                .reportImport(reportConfig)
                        )
                );

        task.setThreadStopAction(ThreadStopActionType.CLOSE);
        task.makeSingle();

        taskManager.switchToBackground(task, parentResult);
        parentResult.setBackgroundTaskOid(task.getOid());
    }

    @Override
    public void cleanupReports(CleanupPolicyType cleanupPolicy, RunningTask task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS);

        // This operation does not need any extra authorization check. All model operations carried out by this
        // method are executed through modelService. Therefore usual object authorizations are checked.

        if (cleanupPolicy.getMaxAge() == null) {
            return;
        }

        Duration duration = cleanupPolicy.getMaxAge();
        if (duration.getSign() > 0) {
            duration = duration.negate();
        }
        Date deleteReportOutputsTo = new Date();
        duration.addTo(deleteReportOutputsTo);

        LOGGER.info("Starting cleanup for report outputs deleting up to {} (duration '{}').",
                deleteReportOutputsTo, duration);

        XMLGregorianCalendar timeXml = XmlTypeConverter.createXMLGregorianCalendar(deleteReportOutputsTo.getTime());

        List<PrismObject<ReportDataType>> obsoleteReportDataObjects;
        try {
            ObjectQuery obsoleteReportOutputsQuery = prismContext.queryFor(ReportDataType.class)
                    .item(ReportDataType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP).le(timeXml)
                    .build();
            obsoleteReportDataObjects =
                    modelService.searchObjects(ReportDataType.class, obsoleteReportOutputsQuery, null, task, result);
        } catch (Exception e) {
            throw new SystemException("Couldn't get the list of obsolete report outputs: " + e.getMessage(), e);
        }

        LOGGER.debug("Found {} report output(s) to be cleaned up", obsoleteReportDataObjects.size());

        boolean interrupted = false;
        int deleted = 0;
        int problems = 0;

        for (PrismObject<ReportDataType> reportDataPrism : obsoleteReportDataObjects) {
            if (!task.canRun()) {
                interrupted = true;
                break;
            }

            if (ObjectTypeUtil.isIndestructible(reportDataPrism)) {
                LOGGER.trace("NOT removing report output {} as it is marked as indestructible", reportDataPrism);
                continue;
            }

            ReportDataType reportData = reportDataPrism.asObjectable();

            LOGGER.trace("Removing report output {} along with {} file.", reportData.getName().getOrig(),
                    reportData.getFilePath());
            boolean problem = false;
            try {
                deleteReportData(reportData, task, result);
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't delete obsolete report output {} due to a exception", e, reportData);
                problem = true;
            }

            if (problem) {
                problems++;
            } else {
                deleted++;
            }
        }
        result.computeStatusIfUnknown();

        LOGGER.info("Report cleanup procedure " +
                (interrupted ? "was interrupted" : "finished") +
                ". Successfully deleted {} report outputs; there were problems with deleting {} report ouptuts.", deleted, problems);
        String suffix = interrupted ? " Interrupted." : "";
        if (problems == 0) {
            parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS + ".statistics").recordStatus(OperationResultStatus.SUCCESS,
                    "Successfully deleted " + deleted + " report output(s)." + suffix);
        } else {
            parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS + ".statistics").recordPartialError("Successfully deleted " +
                    deleted + " report output(s), "
                    + "there was problems with deleting " + problems + " report outputs.");
        }
    }

    @Override
    public void deleteReportData(ReportDataType reportData, Task task, OperationResult parentResult) throws Exception {
        String oid = reportData.getOid();

        // This operation does not need any extra authorization check. All model operations carried out by this
        // method are executed through modelService. Therefore usual object authorizations are checked.

        OperationResult result = parentResult.createSubresult(DELETE_REPORT_OUTPUT);

        String filePath = reportData.getFilePath();
        result.addParam("oid", oid);
        try {
            File file = new File(filePath);

            if (file.exists()) {
                if (!file.delete()) {
                    LOGGER.error("Couldn't delete report file {}", file);
                }
            } else {
                String fileName = remoteFileName(reportData, null, file, task, result);
                if (fileName == null) {
                    return;
                }
                String originalNodeId = reportData.getNodeRef() != null ? reportData.getNodeRef().getOid() : null;
                clusterExecutionHelper.executeWithFallback(originalNodeId,
                        (client, node, result1) -> {
                            client.path(ModelPublicConstants.CLUSTER_REPORT_FILE_PATH);
                            client.query(ModelPublicConstants.CLUSTER_REPORT_FILE_FILENAME_PARAMETER, fileName);
                            Response response = client.delete();
                            Response.StatusType statusInfo = response.getStatusInfo();
                            LOGGER.debug("Deleting report output file ({}) from {} finished with status {}: {}",
                                    fileName, node, statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                            if (statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL) {
                                LOGGER.warn("Deleting report output file ({}) from {} finished with status {}: {}",
                                        fileName, node, statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                                result1.recordFatalError("Could not delete report output file: Got " + statusInfo.getStatusCode() + ": " + statusInfo.getReasonPhrase());
                            }
                            response.close();
                        }, new ClusterExecutionOptions().tryNodesInTransition(), "delete report output", result);
                result.computeStatusIfUnknown();
            }

            ObjectDelta<ReportDataType> delta = prismContext.deltaFactory().object()
                    .createDeleteDelta(ReportDataType.class, oid);
            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
            modelService.executeChanges(deltas, null, task, result);

            result.computeStatusIfUnknown();
        } catch (Exception e) {
            result.recordFatalError("Cannot delete the report output because of a exception.", e);
            throw e;
        }
    }

    @Override
    public InputStream getReportDataStream(String reportDataOid, OperationResult parentResult)
            throws CommonException, IOException {
        Task task = taskManager.createTaskInstance(REPORT_OUTPUT_DATA);

        OperationResult result = parentResult.createSubresult(REPORT_OUTPUT_DATA);
        result.addParam("oid", reportDataOid);

        // This operation does not need any extra authorization check. All model operations carried out by this
        // method are executed through modelService. Therefore usual object authorizations are checked.
        // Here we assume that anyone that can read the ReportOutputType object can also read report data. Which is a fair assumption.
        //
        // An exception is updating report location in case of node unavailability. But this is completely autonomous
        // and should not depend on user privileges.

        try {
            // MID-7219: We need report type in case of cluster to look for file in correct remote folders
            String reportType = null;

            ReportDataType reportData = modelService.getObject(ReportDataType.class, reportDataOid, null, task,
                    result).asObjectable();

            // Extra safety check: traces can be retrieved only when special authorization is present
            if (ObjectTypeUtil.hasArchetypeRef(reportData, SystemObjectsType.ARCHETYPE_TRACE.value())) {
                securityEnforcer.authorize(ModelAuthorizationAction.READ_TRACE.getUrl(), null,
                        AuthorizationParameters.EMPTY, null, task, result);
                reportType = "trace";
            }

            String filePath = reportData.getFilePath();
            if (StringUtils.isEmpty(filePath)) {
                result.recordFatalError("Report output file path is not defined.");
                return null;
            }
            File file = new File(filePath);
            if (file.exists()) {
                return FileUtils.openInputStream(file);
            } else {
                String fileName = remoteFileName(reportData, reportType, file, task, result);
                if (fileName == null) {
                    return null;
                }
                Holder<InputStream> inputStreamHolder = new Holder<>();
                @Nullable String originalNodeOid = reportData.getNodeRef() != null ? reportData.getNodeRef().getOid() : null;
                PrismObject<NodeType> executorNode = clusterExecutionHelper.executeWithFallback(originalNodeOid,
                        (client, node, result1) -> {
                            client.path(ModelPublicConstants.CLUSTER_REPORT_FILE_PATH);
                            client.query(ModelPublicConstants.CLUSTER_REPORT_FILE_FILENAME_PARAMETER, fileName);
                            client.accept(MediaType.APPLICATION_OCTET_STREAM);
                            Response response = client.get();
                            Response.StatusType statusInfo = response.getStatusInfo();
                            LOGGER.debug("Retrieving report output file ({}) from {} finished with status {}: {}",
                                    fileName, originalNodeOid, statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                            if (statusInfo.getFamily() == Response.Status.Family.SUCCESSFUL) {
                                Object entity = response.getEntity();
                                if (entity == null || entity instanceof InputStream) {
                                    inputStreamHolder.setValue((InputStream) entity);
                                    // do NOT close the response; input stream will be closed later by the caller(s)
                                } else {
                                    LOGGER.error("Content of the report output file retrieved from the remote node is not an InputStream; "
                                            + "it is {} instead -- this is not currently supported", entity.getClass());
                                    response.close();
                                }
                            } else {
                                LOGGER.warn("Retrieving report output file ({}) from {} finished with status {}: {}",
                                        fileName, originalNodeOid, statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                                result1.recordFatalError("Could not retrieve report output file: Got " + statusInfo.getStatusCode() + ": " + statusInfo.getReasonPhrase());
                                response.close();
                            }
                        }, new ClusterExecutionOptions().tryNodesInTransition().skipDefaultAccept(), "get report output", result);

                if (executorNode != null && !executorNode.getOid().equals(originalNodeOid)) {
                    LOGGER.info("Recording new location of {}: {}", reportData, executorNode);
                    List<ItemDelta<?, ?>> deltas = prismContext.deltaFor(ReportDataType.class)
                            .item(ReportDataType.F_NODE_REF).replace(createObjectRef(executorNode, prismContext))
                            .asItemDeltas();
                    try {
                        repositoryService.modifyObject(ReportDataType.class, reportDataOid, deltas, result);
                    } catch (ObjectAlreadyExistsException e) {
                        throw new SystemException("Unexpected exception: " + e.getMessage(), e);
                    }
                }

                result.computeStatusIfUnknown();
                return inputStreamHolder.getValue();
            }
        } catch (IOException ex) {
            LoggingUtils.logException(LOGGER, "Error while fetching file. File might not exist on the corresponding file system", ex);
            result.recordPartialError("Error while fetching file. File might not exist on the corresponding file system. Reason: " + ex.getMessage(), ex);
            throw ex;
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException
                | ConfigurationException | ExpressionEvaluationException e) {
            result.recordFatalError("Problem with reading report output. Reason: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private String remoteFileName(ReportDataType reportOutput, String reportType, File file, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String localFileName = checkFileName(file, result);
        if (localFileName == null) {
            return null;
        }

        if (reportType == null && reportOutput.getReportRef() != null && reportOutput.getReportRef().getOid() != null) {
            try {
            ReportType report = modelService.getObject(ReportType.class, reportOutput.getReportRef().getOid(), null, task, result).asObjectable();
            // If direction is import
            if (report.getBehavior() != null && DirectionTypeType.IMPORT.equals(report.getBehavior().getDirection())) {
                reportType = "import";
            }
            } catch (ObjectNotFoundException e) {
                // NOOP
            }

        }
        return reportType != null ? (reportType + "/" + localFileName) : localFileName;
    }

    @Nullable
    private String checkFileName(File file, OperationResult result) {
        String fileName = file.getName();
        if (StringUtils.isEmpty(fileName)) {
            result.recordFatalError("Report output file name is empty.");
            return null;
        } else {
            return fileName;
        }
    }

    @Override
    public CompiledObjectCollectionView createCompiledView(ObjectCollectionReportEngineConfigurationType collectionConfig, boolean useDefaultView, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return reportService.createCompiledView(collectionConfig, useDefaultView, task, result);
    }

    @Override
    public Object evaluateScript(PrismObject<ReportType> report, @NotNull ExpressionType expression, VariablesMap variables, String shortDesc, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        return reportService.evaluateScript(report, expression, variables, shortDesc, task, result);
    }

    @Override
    public VariablesMap evaluateSubreportParameters(PrismObject<ReportType> report, VariablesMap variables, Task task, OperationResult result) {
        return reportService.evaluateSubreports(report, variables, task, result);
    }
}
