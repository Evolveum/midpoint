/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl;

import com.evolveum.midpoint.cases.impl.engine.CaseEngineImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LevelOverrideTurboFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.logging.TracingAppender;
import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.api.request.ClaimWorkItemsRequest;
import com.evolveum.midpoint.cases.api.request.CompleteWorkItemsRequest;
import com.evolveum.midpoint.cases.api.request.DelegateWorkItemsRequest;
import com.evolveum.midpoint.cases.api.request.ReleaseWorkItemsRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * Provides basic work item actions for upper layers.
 *
 * Takes care of
 * - handling operation result
 *
 * Does NOT take care of
 *  - authorizations -- these are handled internally by CaseEngineImpl, because only at that time
 *      we read the appropriate objects (cases, work items).
 */

@Component
public class WorkItemManager {

    private static final Trace LOGGER = TraceManager.getTrace(WorkItemManager.class);

    @Autowired private CaseEngineImpl caseEngine;
    @Autowired private Tracer tracer;

    private static final String DOT_INTERFACE = CaseManager.class.getName() + ".";

    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_INTERFACE + "completeWorkItem";
    private static final String OPERATION_COMPLETE_WORK_ITEMS = DOT_INTERFACE + "completeWorkItems";
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_INTERFACE + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_INTERFACE + "releaseWorkItem";
    private static final String OPERATION_DELEGATE_WORK_ITEM = DOT_INTERFACE + "delegateWorkItem";

    public void completeWorkItem(WorkItemId workItemId, @NotNull AbstractWorkItemOutputType output,
            WorkItemEventCauseInformationType causeInformation, Task task, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SchemaException, ObjectAlreadyExistsException {

        OperationResultBuilder builder = parentResult.subresult(OPERATION_COMPLETE_WORK_ITEM)
                .addArbitraryObjectAsParam("workItemId", workItemId)
                .addParam("decision", output.getOutcome())
                .addParam("comment", output.getComment());
        boolean tracingRequested = startTracingIfRequested(builder, task, parentResult);

        OperationResult result = builder.build();

        try {
            LOGGER.trace("Completing work item {} with decision of {} ['{}']; cause: {}",
                    workItemId, output.getOutcome(), output.getComment(), causeInformation);
            CompleteWorkItemsRequest request = new CompleteWorkItemsRequest(workItemId.caseOid, causeInformation);
            request.getCompletions().add(new CompleteWorkItemsRequest.SingleCompletion(workItemId.id, output));
            caseEngine.executeRequest(request, task, result);
        } catch (SecurityViolationException | RuntimeException | SchemaException | ObjectAlreadyExistsException e) {
            result.recordFatalError("Couldn't complete the work item " + workItemId + ": " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
            storeTraceIfRequested(tracingRequested, task, result, parentResult);
        }
    }

    private void storeTraceIfRequested(boolean tracingRequested, Task task, OperationResult result,
            OperationResult parentResult) {
        if (tracingRequested) {
            tracer.storeTrace(task, result, parentResult);
            TracingAppender.removeSink(); // todo reconsider
            LevelOverrideTurboFilter.cancelLoggingOverride(); // todo reconsider
        }
    }

    private boolean startTracingIfRequested(OperationResultBuilder builder, Task task, OperationResult parentResult)
            throws SchemaException {
        if (task.isTracingRequestedFor(TracingRootType.WORKFLOW_OPERATION)) {
            builder.tracingProfile(
                    tracer.compileProfile(
                            task.getTracingProfile(), parentResult));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Bulk version of completeWorkItem method. It's necessary when we need to complete more items at the same time,
     * to provide accurate completion information (avoiding completion of the first one and automated closure of other ones).
     */
    void completeWorkItems(CompleteWorkItemsRequest request, Task task, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SchemaException, ObjectAlreadyExistsException {
        OperationResultBuilder builder = parentResult.subresult(OPERATION_COMPLETE_WORK_ITEMS);
        boolean tracingRequested = startTracingIfRequested(builder, task, parentResult);
        OperationResult result = builder.build();
        try {
            caseEngine.executeRequest(request, task, result);
        } catch (SecurityViolationException | RuntimeException | CommunicationException | ConfigurationException | SchemaException | ObjectAlreadyExistsException e) {
            result.recordFatalError("Couldn't complete work items: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
            storeTraceIfRequested(tracingRequested, task, result, parentResult);
        }
    }

    // We can eventually provide bulk version of this method as well.
    void claimWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResultBuilder builder = parentResult.subresult(OPERATION_CLAIM_WORK_ITEM)
                .addArbitraryObjectAsParam("workItemId", workItemId);
        boolean tracingRequested = startTracingIfRequested(builder, task, parentResult);
        OperationResult result = builder.build();
        try {
            LOGGER.trace("Claiming work item {}", workItemId);
            ClaimWorkItemsRequest request = new ClaimWorkItemsRequest(workItemId.caseOid);
            request.getClaims().add(new ClaimWorkItemsRequest.SingleClaim(workItemId.id));
            caseEngine.executeRequest(request, task, result);
        } catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException | ObjectAlreadyExistsException | ExpressionEvaluationException | ConfigurationException | CommunicationException e) {
            result.recordFatalError("Couldn't claim the work item " + workItemId + ": " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
            storeTraceIfRequested(tracingRequested, task, result, parentResult);
        }
    }

    // We can eventually provide bulk version of this method as well.
    void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResultBuilder builder = parentResult.subresult(OPERATION_RELEASE_WORK_ITEM)
                .addArbitraryObjectAsParam("workItemId", workItemId);
        boolean tracingRequested = startTracingIfRequested(builder, task, parentResult);
        OperationResult result = builder.build();
        try {
            LOGGER.trace("Releasing work item {}", workItemId);
            ReleaseWorkItemsRequest request = new ReleaseWorkItemsRequest(workItemId.caseOid);
            request.getReleases().add(new ReleaseWorkItemsRequest.SingleRelease(workItemId.id));
            caseEngine.executeRequest(request, task, result);
        } catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException | ObjectAlreadyExistsException | ExpressionEvaluationException | ConfigurationException | CommunicationException e) {
            result.recordFatalError("Couldn't release work item " + workItemId + ": " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
            storeTraceIfRequested(tracingRequested, task, result, parentResult);
        }
    }

    // TODO when calling from model API, what should we put into escalationLevelName+DisplayName ?
    // Probably the API should look different. E.g. there could be an "Escalate" button, that would look up the
    // appropriate escalation timed action, and invoke it. We'll solve this when necessary. Until that time, be
    // aware that escalationLevelName/DisplayName are for internal use only.

    // We can eventually provide bulk version of this method as well.
    void delegateWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull WorkItemDelegationRequestType delegationRequest,
            WorkItemEscalationLevelType escalation,
            Duration newDuration,
            WorkItemEventCauseInformationType causeInformation,
            XMLGregorianCalendar now,
            Task task,
            OperationResult parentResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<ObjectReferenceType> delegates = delegationRequest.getDelegate();
        WorkItemDelegationMethodType method = delegationRequest.getMethod();
        String comment = delegationRequest.getComment();

        OperationResultBuilder builder = parentResult.subresult(OPERATION_DELEGATE_WORK_ITEM)
                .addArbitraryObjectAsParam("workItemId", workItemId)
                .addArbitraryObjectAsParam("escalation", escalation)
                .addArbitraryObjectCollectionAsParam("delegates", delegates)
                .addArbitraryObjectAsParam("method", method)
                .addParam("comment", delegationRequest.getComment());
        boolean tracingRequested = startTracingIfRequested(builder, task, parentResult);
        OperationResult result = builder.build();

        try {
            LOGGER.trace("Delegating work item {} to {} ({}): escalation={}; cause={}; comment={}", workItemId, delegates, method,
                    escalation != null ? escalation.getName() + "/" + escalation.getDisplayName() : "none", causeInformation, comment);
            DelegateWorkItemsRequest request = new DelegateWorkItemsRequest(workItemId.caseOid, causeInformation, now);
            request.getDelegations().add(
                    new DelegateWorkItemsRequest.SingleDelegation(workItemId.id, delegationRequest, escalation, newDuration));
            caseEngine.executeRequest(request, task, result);
        } catch (SecurityViolationException | RuntimeException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't delegate/escalate work item " + workItemId + ": " + e.getMessage(), e);
            throw e;
        } catch (ObjectAlreadyExistsException e) {
            throw new IllegalStateException(e);
        } finally {
            result.computeStatusIfUnknown();
            storeTraceIfRequested(tracingRequested, task, result, parentResult);
        }
    }
}
