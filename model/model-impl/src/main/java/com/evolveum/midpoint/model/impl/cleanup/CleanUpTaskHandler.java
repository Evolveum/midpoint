/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.cleanup;

import static com.evolveum.midpoint.repo.common.task.TaskExceptionHandlingUtil.processException;
import static com.evolveum.midpoint.repo.common.task.TaskExceptionHandlingUtil.processFinish;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.*;

import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.task.ErrorState;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class CleanUpTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = ModelPublicConstants.CLEANUP_TASK_HANDLER_URI;
    public static final String CLEANUP_TASK = "Cleanup task";

    @Autowired private TaskManager taskManager;
    @Autowired private RepositoryService repositoryService;
    @Autowired private AuditService auditService;

    @Autowired(required = false)
    private WorkflowManager workflowManager;
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
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partitionDefinition) {
        TaskRunResult runResult = createRunResult();
        ErrorState errorState = new ErrorState(); // currently not used
        try {
            runInternal(task, runResult);
            return processFinish(LOGGER, partitionDefinition, CLEANUP_TASK, runResult, errorState);
        } catch (Throwable t) {
            return processException(t, LOGGER, partitionDefinition, CLEANUP_TASK, runResult);
        }
    }

    private TaskRunResult createRunResult() {
        OperationResult opResult = new OperationResult(OperationConstants.CLEANUP);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);
        return runResult;
    }

    public void runInternal(RunningTask task, TaskRunResult runResult) throws TaskException {
        LOGGER.trace("CleanUpTaskHandler.run starting");
        OperationResult result = runResult.getOperationResult();

        @NotNull CleanupPoliciesType cleanupPolicies = getCleanupPolicies(task, result);

        cleanupItems(Part.AUDIT_RECORDS, cleanupPolicies.getAuditRecords(), p -> auditService.cleanupAudit(p, result),
                task, runResult); // TODO progress reporting

        cleanupItems(Part.CLOSED_TASKS, cleanupPolicies.getClosedTasks(), p -> taskManager.cleanupTasks(p, task, result),
                task, runResult);

        cleanupItems(Part.CLOSED_CASES, cleanupPolicies.getClosedCases(), p -> workflowManager.cleanupCases(p, task, result),
                task, runResult);

        cleanupItems(Part.DEAD_NODES, cleanupPolicies.getDeadNodes(), p -> taskManager.cleanupNodes(p, task, result),
                task, runResult);

        cleanupItems(Part.OUTPUT_REPORTS, cleanupPolicies.getOutputReports(), p -> cleanupReports(p, result),
                task, runResult); // TODO progress reporting

        cleanupItems(Part.CLOSED_CERTIFICATION_CAMPAIGNS, cleanupPolicies.getClosedCertificationCampaigns(),
                p -> certificationService.cleanupCampaigns(p, task, result), task, runResult); // TODO progress reporting

        result.computeStatusIfUnknown();
        if (runResult.getRunResultStatus() != null) {
            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        }
        LOGGER.trace("CleanUpTaskHandler.run stopping");
    }

    private <P> void cleanupItems(Part part, P policy, Cleaner<P> cleaner, RunningTask task, TaskRunResult runResult)
            throws TaskException {
        if (!task.canRun()) {
            throw new TaskException("Running interrupted", SUCCESS, INTERRUPTED);
        }
        task.setStructuredProgressPartInformation(part.partUri, part.ordinal() + 1, Part.values().length);
        if (policy != null) {
            try {
                cleaner.cleanup(policy);
            } catch (Exception e) {
                LOGGER.error("{}: {}", part.label, e.getMessage(), e);
                runResult.getOperationResult().recordFatalError(part.label + ": " + e.getMessage(), e);
                runResult.setRunResultStatus(PERMANENT_ERROR);
            }
        } else {
            LOGGER.trace(part.label + ": No clean up policy for this kind of items present.");
        }
    }

    private void cleanupReports(CleanupPolicyType p, OperationResult result) {
        if (reportManager != null) {
            reportManager.cleanupReports(p, result);
        } else {
            //TODO improve dependencies for report-impl (probably for tests) and set autowire to required
            throw new IllegalStateException("Report manager was not autowired, reports cleanup will be skipped.");
        }
    }

    @NotNull
    private CleanupPoliciesType getCleanupPolicies(RunningTask task, OperationResult opResult) throws TaskException {
        CleanupPoliciesType cleanupPolicies;
        CleanupPoliciesType taskCleanupPolicies = task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_CLEANUP_POLICIES);
        if (taskCleanupPolicies != null) {
            LOGGER.info("Using task-specific cleanupPolicies: {}", taskCleanupPolicies);
            cleanupPolicies = taskCleanupPolicies;
        } else {
            cleanupPolicies = getSystemCleanupPolicies(opResult);
        }
        if (cleanupPolicies == null) {
            throw new TaskException("No clean up policies specified. Finishing clean up task.", SUCCESS, FINISHED);
        }
        return cleanupPolicies;
    }

    private CleanupPoliciesType getSystemCleanupPolicies(OperationResult opResult) throws TaskException {
        try {
            return repositoryService.getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, opResult)
                    .asObjectable().getCleanupPolicy();
        } catch (ObjectNotFoundException e) {
            throw new TaskException("No system configuration found", FATAL_ERROR, PERMANENT_ERROR, e);
        } catch (Exception e) {
            throw new TaskException("Couldn't get system configuration", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.CLEANUP;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_CLEANUP_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return Channel.CLEANUP.getUri();
    }

    private enum Part {
        AUDIT_RECORDS("Audit records cleanup", SchemaConstants.AUDIT_RECORDS_CLEANUP_TASK_PART_URI),
        CLOSED_TASKS("Closed tasks", SchemaConstants.CLOSED_TASKS_CLEANUP_TASK_PART_URI),
        CLOSED_CASES("Closed cases", SchemaConstants.CLOSED_CASES_CLEANUP_TASK_PART_URI),
        DEAD_NODES("Dead nodes", SchemaConstants.DEAD_NODES_CLEANUP_TASK_PART_URI),
        OUTPUT_REPORTS("Output reports", SchemaConstants.OUTPUT_REPORTS_CLEANUP_TASK_PART_URI),
        CLOSED_CERTIFICATION_CAMPAIGNS("Closed certification campaigns",
                SchemaConstants.CLOSED_CERTIFICATION_CAMPAIGNS_CLEANUP_TASK_PART_URI);

        @NotNull private final String label;
        @NotNull private final String partUri;

        Part(@NotNull String label, @NotNull String partUri) {
            this.label = label;
            this.partUri = partUri;
        }
    }

    @FunctionalInterface
    private interface Cleaner<P> {
        void cleanup(P policy) throws Exception;
    }
}
