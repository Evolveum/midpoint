/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.cleanup;

import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.CompositeActivityExecution;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;

@Component
public class CleanupActivityHandler
        extends ModelActivityHandler<CleanupWorkDefinition, CleanupActivityHandler> {

    public static final String LEGACY_HANDLER_URI = ModelPublicConstants.CLEANUP_TASK_HANDLER_URI;
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CLEANUP_TASK.value();

    @Autowired private TaskManager taskManager;
    @Autowired private AuditService auditService;
    @Autowired(required = false) private WorkflowManager workflowManager;
    @Autowired private AccessCertificationService certificationService;
    @Autowired(required = false) private ReportManager reportManager;

    @PostConstruct
    public void register() {
        handlerRegistry.register(CleanupWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                CleanupWorkDefinition.class, CleanupWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(ShadowRefreshWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI, CleanupWorkDefinition.class);
    }

    @Override
    public @NotNull CompositeActivityExecution<CleanupWorkDefinition, CleanupActivityHandler, ?> createExecution(
            @NotNull ExecutionInstantiationContext<CleanupWorkDefinition, CleanupActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityExecution<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<CleanupWorkDefinition, CleanupActivityHandler> parentActivity) {
        ArrayList<Activity<?, ?>> children = new ArrayList<>();

        // TODO or should we create only activities that correspond to real work we are going to do?

        ActivityStateDefinition<AbstractActivityWorkStateType> stateDef = ActivityStateDefinition.normal();
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().clone(),
                (context, result) ->
                        new CleanupPartialActivityExecution<>(
                                context, Part.AUDIT_RECORDS, CleanupPoliciesType::getAuditRecords,
                                (p, task, result1) -> auditService.cleanupAudit(p, result1)),
                null,
                (i) -> Part.AUDIT_RECORDS.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(parentActivity.getDefinition().clone(),
                (context, result) ->
                        new CleanupPartialActivityExecution<>(
                                context, Part.CLOSED_TASKS, CleanupPoliciesType::getClosedTasks,
                                (p, task, result1) -> taskManager.cleanupTasks(p, task, result1)),
                null,
                (i) -> Part.CLOSED_TASKS.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(parentActivity.getDefinition().clone(),
                (context, result) ->
                        new CleanupPartialActivityExecution<>(
                                context, Part.CLOSED_CASES, CleanupPoliciesType::getClosedCases,
                                this::cleanupCases),
                null,
                (i) -> Part.CLOSED_CASES.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(parentActivity.getDefinition().clone(),
                (context, result) ->
                        new CleanupPartialActivityExecution<>(
                                context, Part.DEAD_NODES, CleanupPoliciesType::getDeadNodes,
                                (p, task, result1) -> taskManager.cleanupNodes(p, task, result1)),
                null,
                (i) -> Part.DEAD_NODES.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(parentActivity.getDefinition().clone(),
                (context, result) ->
                        new CleanupPartialActivityExecution<>(
                                context, Part.OUTPUT_REPORTS, CleanupPoliciesType::getOutputReports,
                                (p, task, result1) -> cleanupReports(p, result1)),
                null,
                (i) -> Part.OUTPUT_REPORTS.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(parentActivity.getDefinition().clone(),
                (context, result) ->
                        new CleanupPartialActivityExecution<>(
                                context, Part.CLOSED_CERTIFICATION_CAMPAIGNS, CleanupPoliciesType::getClosedCertificationCampaigns,
                                (p, task, result1) -> certificationService.cleanupCampaigns(p, task, result1)),
                null,
                (i) -> Part.CLOSED_CERTIFICATION_CAMPAIGNS.identifier,
                stateDef,
                parentActivity));

        return children;
    }

    private void cleanupCases(CleanupPolicyType p, RunningTask task, OperationResult result1)
            throws SchemaException, ObjectNotFoundException {
        if (workflowManager != null) {
            workflowManager.cleanupCases(p, task, result1);
        } else {
            throw new IllegalStateException("Workflow manager was not autowired, cases cleanup will be skipped.");
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

    @Override
    public String getIdentifierPrefix() {
        return "cleanup";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    enum Part {
        // TODO progress
        AUDIT_RECORDS("Audit records cleanup", SchemaConstants.ID_AUDIT_RECORDS_CLEANUP, false),

        CLOSED_TASKS("Closed tasks", SchemaConstants.ID_CLOSED_TASKS_CLEANUP, true),

        CLOSED_CASES("Closed cases", SchemaConstants.ID_CLOSED_CASES_CLEANUP, true),

        DEAD_NODES("Dead nodes", SchemaConstants.ID_DEAD_NODES_CLEANUP, true),

        // TODO progress
        OUTPUT_REPORTS("Output reports", SchemaConstants.ID_OUTPUT_REPORTS_CLEANUP, false),

        // TODO progress
        CLOSED_CERTIFICATION_CAMPAIGNS("Closed certification campaigns",
                SchemaConstants.ID_CLOSED_CERTIFICATION_CAMPAIGNS_CLEANUP, false);

        @NotNull final String label;
        @NotNull private final String identifier;
        final boolean supportsProgress;

        Part(@NotNull String label, @NotNull String identifier, boolean supportsProgress) {
            this.label = label;
            this.identifier = identifier;
            this.supportsProgress = supportsProgress;
        }
    }

    @FunctionalInterface
    interface Cleaner<P> {
        void cleanup(P policy, RunningTask workerTask, OperationResult result) throws Exception;
    }
}
