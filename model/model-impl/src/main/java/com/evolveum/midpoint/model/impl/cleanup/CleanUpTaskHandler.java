/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.cleanup;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;

@Component
public class CleanUpTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = ModelPublicConstants.CLEANUP_TASK_HANDLER_URI;

    @Autowired private TaskManager taskManager;
    @Autowired private RepositoryService repositoryService;
    @Autowired private AuditService auditService;
    @Autowired private WorkflowManager workflowManager;
    @Autowired private AccessCertificationService certificationService;

    @Autowired(required = false)
    private ReportManager reportManager;

    private static final Trace LOGGER = TraceManager.getTrace(CleanUpTaskHandler.class);

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromZero()
                .maintainIterationStatistics();
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        LOGGER.trace("CleanUpTaskHandler.run starting");

        OperationResult opResult = new OperationResult(OperationConstants.CLEANUP);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        CleanupPoliciesType cleanupPolicies;
        CleanupPoliciesType taskCleanupPolicies = task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_CLEANUP_POLICIES);
        if (taskCleanupPolicies != null) {
            LOGGER.info("Using task-specific cleanupPolicies: {}", taskCleanupPolicies);
            cleanupPolicies = taskCleanupPolicies;
        } else {
            PrismObject<SystemConfigurationType> systemConfig;
            try {
                systemConfig = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, opResult);
            } catch (ObjectNotFoundException ex) {
                LOGGER.error("Cleanup: Object does not exist: {}", ex.getMessage(), ex);
                opResult.recordFatalError("Object does not exist: " + ex.getMessage(), ex);
                runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                return runResult;
            } catch (SchemaException ex) {
                LOGGER.error("Cleanup: Error dealing with schema: {}", ex.getMessage(), ex);
                opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
                runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                return runResult;
            }
            SystemConfigurationType systemConfigType = systemConfig.asObjectable();
            cleanupPolicies = systemConfigType.getCleanupPolicy();
        }

        if (cleanupPolicies == null) {
            LOGGER.trace("Cleanup: No clean up polices specified. Finishing clean up task.");
            opResult.computeStatus();
            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
            return runResult;
        }

        if (task.canRun()) {
            CleanupPolicyType auditCleanupPolicy = cleanupPolicies.getAuditRecords();
            if (auditCleanupPolicy != null) {
                try {
                    // TODO report progress
                    auditService.cleanupAudit(auditCleanupPolicy, opResult);
                } catch (Exception ex) {
                    LOGGER.error("Audit cleanup: {}", ex.getMessage(), ex);
                    opResult.recordFatalError(ex.getMessage(), ex);
                    runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                }
            } else {
                LOGGER.trace("Cleanup: No clean up policy for audit specified. Finishing clean up task.");
            }
        }

        if (task.canRun()) {
            CleanupPolicyType closedTasksPolicy = cleanupPolicies.getClosedTasks();
            if (closedTasksPolicy != null) {
                try {
                    taskManager.cleanupTasks(closedTasksPolicy, task, opResult);
                } catch (Exception ex) {
                    LOGGER.error("Tasks cleanup: {}", ex.getMessage(), ex);
                    opResult.recordFatalError(ex.getMessage(), ex);
                    runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                }
            } else {
                LOGGER.trace("Cleanup: No clean up policy for closed tasks specified. Finishing clean up task.");
            }
        }

        if (task.canRun()) {
            CleanupPolicyType closedCasesPolicy = cleanupPolicies.getClosedCases();
            if (closedCasesPolicy != null) {
                try {
                    workflowManager.cleanupCases(closedCasesPolicy, task, opResult);
                } catch (Exception ex) {
                    LOGGER.error("Cases cleanup: {}", ex.getMessage(), ex);
                    opResult.recordFatalError(ex.getMessage(), ex);
                    runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                }
            } else {
                LOGGER.trace("Cleanup: No clean up policy for closed cases specified. Finishing clean up task.");
            }
        }

        if (task.canRun()) {
            DeadNodeCleanupPolicyType deadNodesPolicy = cleanupPolicies.getDeadNodes();
            if (deadNodesPolicy != null) {
                try {
                    taskManager.cleanupNodes(deadNodesPolicy, task, opResult);
                } catch (Exception ex) {
                    LOGGER.error("Nodes cleanup: {}", ex.getMessage(), ex);
                    opResult.recordFatalError(ex.getMessage(), ex);
                    runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                }
            } else {
                LOGGER.trace("Cleanup: No clean up policy for closed tasks specified. Finishing clean up task.");
            }
        }

        if (task.canRun()) {
            CleanupPolicyType reportCleanupPolicy = cleanupPolicies.getOutputReports();
            if (reportCleanupPolicy != null) {
                try {
                    if (reportManager == null) {
                        //TODO improve dependencies for report-impl (probably for tests) and set autowire to required
                        LOGGER.error("Report manager was not autowired, reports cleanup will be skipped.");
                    } else {
                        // TODO report progress
                        reportManager.cleanupReports(reportCleanupPolicy, opResult);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Reports cleanup: {}", ex.getMessage(), ex);
                    opResult.recordFatalError(ex.getMessage(), ex);
                    runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                }
            } else {
                LOGGER.trace("Cleanup: No clean up policy for report specified. Finishing clean up task.");
            }
        }

        if (task.canRun()) {
            CleanupPolicyType closedCampaignsPolicy = cleanupPolicies.getClosedCertificationCampaigns();
            if (closedCampaignsPolicy != null) {
                try {
                    certificationService.cleanupCampaigns(closedCampaignsPolicy, task, opResult);
                } catch (Throwable ex) {
                    LOGGER.error("Campaigns cleanup: {}", ex.getMessage(), ex);
                    opResult.recordFatalError(ex.getMessage(), ex);
                    runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                }
            } else {
                LOGGER.trace("Cleanup: No clean up policy for closed tasks specified. Finishing clean up task.");
            }
        }

        opResult.computeStatusIfUnknown();
        if (runResult.getRunResultStatus() == null) {
            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        }
        LOGGER.trace("CleanUpTaskHandler.run stopping");
        return runResult;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.CLEANUP;
    }

    @Override
    public List<String> getCategoryNames() {
        return Collections.singletonList(TaskCategory.CLEANUP);
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_CLEANUP_TASK.value();
    }
}
