/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.cleanup;

import java.util.ArrayList;
import java.util.function.Predicate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.CompositeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class CleanupActivityHandler
        extends ModelActivityHandler<CleanupWorkDefinition, CleanupActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_CLEANUP_TASK.value();

    private static final Trace LOGGER = TraceManager.getTrace(CleanupActivityHandler.class);

    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private TaskManager taskManager;
    @Autowired private AuditService auditService;
    @Autowired(required = false) private CaseManager caseManager; // may switch to required? (see the dependencies)
    @Autowired private AccessCertificationService certificationService;
    @Autowired(required = false) private ReportManager reportManager;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                CleanupWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_CLEANUP,
                CleanupWorkDefinition.class, CleanupWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(ShadowRefreshWorkDefinitionType.COMPLEX_TYPE, CleanupWorkDefinition.class);
    }

    @Override
    public @NotNull CompositeActivityRun<CleanupWorkDefinition, CleanupActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<CleanupWorkDefinition, CleanupActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<CleanupWorkDefinition, CleanupActivityHandler> parentActivity) {
        ArrayList<Activity<?, ?>> children = new ArrayList<>();

        // TODO or should we create only activities that correspond to real work we are going to do?
        //  Actually, it's not that easy. In the work definition we have only explicit policies.
        //  If they are null, policies from system config should be used.

        ActivityStateDefinition<AbstractActivityWorkStateType> stateDef = ActivityStateDefinition.normal();
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) ->
                        new CleanupPartialActivityRun<>(
                                context, Part.AUDIT_RECORDS, CleanupPoliciesType::getAuditRecords,
                                this::cleanupAudit),
                null,
                (i) -> Part.AUDIT_RECORDS.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) ->
                        new CleanupPartialActivityRun<>(
                                context, Part.CLOSED_TASKS, CleanupPoliciesType::getClosedTasks,
                                this::cleanupTasks),
                null,
                (i) -> Part.CLOSED_TASKS.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) ->
                        new CleanupPartialActivityRun<>(
                                context, Part.CLOSED_CASES, CleanupPoliciesType::getClosedCases,
                                this::cleanupCases),
                null,
                (i) -> Part.CLOSED_CASES.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) ->
                        new CleanupPartialActivityRun<>(
                                context, Part.DEAD_NODES, CleanupPoliciesType::getDeadNodes,
                                this::cleanupNodes),
                null,
                (i) -> Part.DEAD_NODES.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) ->
                        new CleanupPartialActivityRun<>(
                                context, Part.OUTPUT_REPORTS, CleanupPoliciesType::getOutputReports,
                                this::cleanupReports),
                null,
                (i) -> Part.OUTPUT_REPORTS.identifier,
                stateDef,
                parentActivity));

        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) ->
                        new CleanupPartialActivityRun<>(
                                context, Part.CLOSED_CERTIFICATION_CAMPAIGNS, CleanupPoliciesType::getClosedCertificationCampaigns,
                                this::cleanupCampaigns),
                null,
                (i) -> Part.CLOSED_CERTIFICATION_CAMPAIGNS.identifier,
                stateDef,
                parentActivity));

        return children;
    }

    private void cleanupAudit(CleanupPolicyType p, RunningTask task, OperationResult result) throws CommonException {
        // Global authorization (we cannot filter by containerables yet)
        securityEnforcer.authorize(
                ModelAuthorizationAction.CLEANUP_AUDIT_RECORDS.getUrl(), task, result);
        auditService.cleanupAudit(p, result);
    }

    private void cleanupTasks(CleanupPolicyType p, RunningTask task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        // Authorization by selector
        taskManager.cleanupTasks(p, createAutzSelector(task, result), task, result);
    }

    private void cleanupCases(CleanupPolicyType p, RunningTask task, OperationResult result)
            throws CommonException {
        if (caseManager != null) {
            // No autz check needed. Workflow manager does it for us (by relying on model API).
            caseManager.cleanupCases(p, task, result);
        } else {
            throw new IllegalStateException("Workflow manager was not autowired, cases cleanup will be skipped.");
        }
    }

    private void cleanupNodes(DeadNodeCleanupPolicyType p, RunningTask task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        // Authorization by selector
        taskManager.cleanupNodes(p, createAutzSelector(task, result), task, result);
    }

    private void cleanupReports(CleanupPolicyType p, RunningTask task, OperationResult result) {
        if (reportManager != null) {
            // No autz check needed. Report manager does it for us (by relying on model API).
            reportManager.cleanupReports(p, task, result);
        } else {
            //TODO improve dependencies for report-impl (probably for tests) and set autowire to required
            throw new IllegalStateException("Report manager was not autowired, reports cleanup will be skipped.");
        }
    }

    private void cleanupCampaigns(CleanupPolicyType p, RunningTask task, OperationResult result1) {
        // No autz check needed. Certification manager does it for us (by relying on model API).
        certificationService.cleanupCampaigns(p, task, result1);
    }

    private <T extends ObjectType> Predicate<T> createAutzSelector(@NotNull Task task, @NotNull OperationResult result) {
        return object -> {
            try {
                boolean authorizedToDelete = securityEnforcer.isAuthorized(
                        ModelAuthorizationAction.DELETE.getUrl(),
                        null,
                        AuthorizationParameters.Builder.buildObject(object.asPrismObject()),
                        SecurityEnforcer.Options.create(),
                        task, result);
                if (authorizedToDelete) {
                    return true;
                } else {
                    LOGGER.debug("Cleanup of {} rejected by authorizations", object);
                    return false;
                }
            } catch (CommonException e) {
                throw new SystemException("Couldn't evaluate authorizations needed to cleanup (delete) " + object, e);
            }
        };
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
