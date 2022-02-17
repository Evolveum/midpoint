/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.cases.api.events.PendingNotificationEventSupplier.AllocationChangeCurrent;
import com.evolveum.midpoint.cases.api.events.PendingNotificationEventSupplier.ItemDeletion;
import com.evolveum.midpoint.cases.api.events.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.cases.api.events.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.cases.api.extensions.WorkItemCompletionResult;
import com.evolveum.midpoint.cases.api.request.CompleteWorkItemsRequest.SingleCompletion;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.cases.impl.helpers.AuthorizationHelper.RequestedOperation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.cases.api.request.CompleteWorkItemsRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

import static com.evolveum.midpoint.cases.impl.engine.helpers.WorkItemHelper.fillInWorkItemEvent;

/**
 * Completes a work item (or work items).
 *
 * Responsibilities:
 *
 * - completes specified work items
 * - cancels other work items, if the engine extension indicates that
 * - checks authorization for this
 *
 * This mean - in detail:
 *
 * - updates work items in case
 * - updates case history (for completed items)
 * - prepares audit records and notifications
 * - removes triggers related to these work items
 */
public class CompleteWorkItemsAction extends RequestedAction<CompleteWorkItemsRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(CompleteWorkItemsAction.class);

    @NotNull private final XMLGregorianCalendar now;

    public CompleteWorkItemsAction(CaseEngineOperationImpl ctx, @NotNull CompleteWorkItemsRequest request) {
        super(ctx, request, LOGGER);
        this.now = beans.clock.currentTimeXMLGregorianCalendar();
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        LOGGER.trace("Completions: {}", request.getCompletions());

        boolean cancelRemainingWorkItems = false; // Should we auto-close other open work items?
        for (SingleCompletion completion : request.getCompletions()) {
            if (completeSingleWorkItem(completion, result)) {
                cancelRemainingWorkItems = true;
            }
        }

        if (cancelRemainingWorkItems) {
            cancelRemainingWorkItems(result);
        }

        if (isAnyCurrentStageWorkItemOpen()) {
            return null;
        } else {
            return new CloseStageAction(operation, null);
        }
    }

    private boolean isAnyCurrentStageWorkItemOpen() {
        int currentStage = operation.getCurrentStageNumber();
        return getCurrentCase().getWorkItem().stream()
                .anyMatch(wi -> wi.getStageNumber() != null
                        && wi.getStageNumber() == currentStage
                        && wi.getCloseTimestamp() == null);
    }

    /** Returns true if all work items should be cancelled because of this single operation. */
    private boolean completeSingleWorkItem(SingleCompletion completion, OperationResult result)
            throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException, SchemaException {

        CaseWorkItemType workItem = operation.getWorkItemById(completion.getWorkItemId());
        checkAuthorization(workItem, result);

        if (workItem.getCloseTimestamp() != null) {
            LOGGER.trace("Work item {} was already completed on {}", workItem.getId(), workItem.getCloseTimestamp());
            result.recordWarning("Work item " + workItem.getId() + " was already completed on " + workItem.getCloseTimestamp());
            return false;
        }

        completeOrCancelWorkItem(workItem, completion.getOutput(), result);

        WorkItemCompletionResult completionResult = getEngineExtension().processWorkItemCompletion(workItem, operation, result);
        return completionResult.shouldCloseOtherWorkItems();
    }

    private void cancelRemainingWorkItems(OperationResult result) {
        LOGGER.trace("+++ closeOtherWorkItems ENTER: ctx={}, cause type={}", operation, getCauseType());
        for (CaseWorkItemType workItem : getCurrentCase().getWorkItem()) {
            if (workItem.getCloseTimestamp() == null) {
                completeOrCancelWorkItem(workItem, null, result);
            }
        }
        LOGGER.trace("--- closeOtherWorkItems EXIT: operation={}", operation);
    }

    private void checkAuthorization(CaseWorkItemType workItem, OperationResult result)
            throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        if (!beans.authorizationHelper.isAuthorized(workItem, RequestedOperation.COMPLETE, getTask(), result)) {
            throw new SecurityViolationException("You are not authorized to complete the work item.");
        }
    }

    /**
     * Completes (if output is not null) or cancels (if output is null) the work item.
     *
     * Excludes the processing in engine extension!
     */
    private void completeOrCancelWorkItem(
            @NotNull CaseWorkItemType workItem,
            @Nullable AbstractWorkItemOutputType output,
            @NotNull OperationResult result) {

        WorkItemOperationKindType operationKind = output != null ?
                WorkItemOperationKindType.COMPLETE : WorkItemOperationKindType.CANCEL;

        LOGGER.trace("+++ completeOrCancelWorkItem ENTER: op={}, operationKind={}, workItem:\n{}",
                operation, operationKind, workItem.debugDumpLazily());

        updateWorkItemAsClosed(workItem, output);

        if (output != null) {
            updateCaseHistory(workItem, output);
        } else {
            // cancelled items are not maintained in history
        }

        prepareAuditRecord(workItem, result);
        prepareNotifications(workItem, operationKind, result);

        beans.triggerHelper.removeTriggersForWorkItem(getCurrentCase(), workItem.getId());

        LOGGER.trace("--- completeOrCancelWorkItem EXIT: workItem={}, op={}, operationKind={}", workItem, operation, operationKind);
    }

    private void prepareAuditRecord(@NotNull CaseWorkItemType workItem, @NotNull OperationResult result) {
        // We don't pass userRef (initiator) to the audit method. It does need the whole object (not only the reference),
        // so it fetches it directly from the security enforcer (logged-in user). This could change in the future.
        AuditEventRecord record = beans.auditHelper.prepareWorkItemDeletedAuditRecord(
                workItem, getCauseInformation(), getCurrentCase(), result);
        getEngineExtension().enrichWorkItemDeletedAuditRecord(record, workItem, operation, result);
        operation.addAuditRecord(record);
    }

    private void updateCaseHistory(@NotNull CaseWorkItemType workItem, @NotNull AbstractWorkItemOutputType output) {
        WorkItemId workItemId = WorkItemId.create(operation.getCaseOidRequired(), workItem.getId());
        WorkItemCompletionEventType event = new WorkItemCompletionEventType(PrismContext.get());
        fillInWorkItemEvent(event, operation.getPrincipal(), workItemId, workItem);
        event.setCause(request.getCauseInformation());
        event.setOutput(output);
        operation.addCaseHistoryEvent(event);
    }

    private void updateWorkItemAsClosed(@NotNull CaseWorkItemType workItem, @Nullable AbstractWorkItemOutputType output) {
        if (output != null) {
            workItem.setOutput(output.clone());
            workItem.setPerformerRef(operation.getPrincipal().toObjectReference());
        }
        workItem.setCloseTimestamp(now);
    }

    private void prepareNotifications(
            CaseWorkItemType workItem, WorkItemOperationKindType operationKind, OperationResult result) {
        ObjectReferenceType userRef = operation.getPrincipal().toObjectReference();
        CaseType currentCase = getCurrentCase();
        try {
            List<ObjectReferenceType> assigneesAndDeputies =
                    beans.miscHelper.getAssigneesAndDeputies(workItem, getTask(), result);
            WorkItemAllocationChangeOperationInfo operationInfo =
                    new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputies, null);
            WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(userRef, getCauseInformation(), null);
            if (workItem.getAssigneeRef().isEmpty()) {
                operation.addNotification(
                        new ItemDeletion(currentCase, workItem, operationInfo, sourceInfo, null));
            } else {
                for (ObjectReferenceType assigneeOrDeputy : assigneesAndDeputies) {
                    operation.addNotification(
                            new ItemDeletion(currentCase, workItem, operationInfo, sourceInfo, assigneeOrDeputy));
                }
            }
            operation.addNotification(
                    new AllocationChangeCurrent(currentCase, workItem, operationInfo, sourceInfo, null));
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare notifications for work item closure event", e);
        }
    }
}
