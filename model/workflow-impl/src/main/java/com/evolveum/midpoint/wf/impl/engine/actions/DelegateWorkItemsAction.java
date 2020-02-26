/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.wf.api.request.DelegateWorkItemsRequest;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.DelayedNotification;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.engine.helpers.WorkItemHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType.ESCALATE;

/**
 *
 */
public class DelegateWorkItemsAction extends RequestedAction<DelegateWorkItemsRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(DelegateWorkItemsAction.class);

    private static final String OP_EXECUTE = DelegateWorkItemsAction.class.getName() + ".execute";

    public DelegateWorkItemsAction(EngineInvocationContext ctx, DelegateWorkItemsRequest request) {
        super(ctx, request);
    }

    @Override
    public Action execute(OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(OP_EXECUTE)
                .setMinor()
                .build();
        try {
            traceEnter(LOGGER);
            XMLGregorianCalendar now =
                    request.getNow() != null ? request.getNow() : XmlTypeConverter.createXMLGregorianCalendar();
            for (DelegateWorkItemsRequest.SingleDelegation delegation : request.getDelegations()) {
                executeDelegation(delegation, now, result);
            }
            traceExit(LOGGER, null);
            return null;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // TODO check work item state;
    //  check if there are any approvers etc

    private void executeDelegation(DelegateWorkItemsRequest.SingleDelegation delegation,
            XMLGregorianCalendar now, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        CaseWorkItemType workItem = ctx.findWorkItemById(delegation.getWorkItemId());

        if (!engine.authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.DELEGATE, ctx.getTask(), result)) {
            throw new SecurityViolationException("You are not authorized to delegate this work item.");
        }

        if (workItem.getCloseTimestamp() != null) {
            LOGGER.debug("Work item {} in {} was already closed, ignoring the delegation request", workItem, ctx.getCurrentCase());
            result.recordWarning("Work item was already closed");
            return;
        }

        List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
        List<ObjectReferenceType> assigneesAndDeputiesBefore = engine.miscHelper.getAssigneesAndDeputies(workItem,
                ctx.getTask(), result);

        WorkItemOperationKindType operationKind = delegation.getTargetEscalationInfo() != null ? ESCALATE : DELEGATE;

        WorkItemAllocationChangeOperationInfo operationInfoBefore =
                new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputiesBefore, null);

        WorkItemEventCauseInformationType causeInformation = request.getCauseInformation();
        ObjectReferenceType initiator =
                causeInformation == null || causeInformation.getType() == WorkItemEventCauseTypeType.USER_ACTION ?
                        ObjectTypeUtil.createObjectRef(ctx.getPrincipal().getFocus(), engine.prismContext) : null;

        WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(initiator, causeInformation, null);
        ctx.prepareNotification(new DelayedNotification.AllocationChangeCurrent(ctx.getCurrentCase(), workItem, operationInfoBefore, sourceInfo, null));

        List<ObjectReferenceType> newAssignees = new ArrayList<>();
        List<ObjectReferenceType> delegatedTo = new ArrayList<>();
        ApprovalContextUtil
                .computeAssignees(newAssignees, delegatedTo, delegation.getDelegates(), delegation.getMethod(), workItem.getAssigneeRef());

        workItem.getAssigneeRef().clear();
        workItem.getAssigneeRef().addAll(CloneUtil.cloneCollectionMembers(newAssignees));
        if (delegation.getNewDuration() != null) {
            XMLGregorianCalendar newDeadline;
            newDeadline = CloneUtil.clone(now);
            newDeadline.add(delegation.getNewDuration());
            workItem.setDeadline(newDeadline);
        }

        int escalationLevel = ApprovalContextUtil.getEscalationLevelNumber(workItem);
        WorkItemEscalationLevelType newEscalationInfo;
        if (delegation.getTargetEscalationInfo() != null) {
            newEscalationInfo = delegation.getTargetEscalationInfo().clone();
            newEscalationInfo.setNumber(++escalationLevel);
        } else {
            newEscalationInfo = null;
        }

        WorkItemDelegationEventType event = ApprovalContextUtil
                .createDelegationEvent(newEscalationInfo, assigneesBefore, delegatedTo,
                delegation.getMethod(), causeInformation, engine.prismContext);
        event.setComment(delegation.getComment());
        if (newEscalationInfo != null) {
            workItem.setEscalationLevel(newEscalationInfo);
        }

        WorkItemId workItemId = ctx.createWorkItemId(workItem);

        WorkItemHelper.fillInWorkItemEvent(event, ctx.getPrincipal(), workItemId, workItem, engine.prismContext);
        ctx.addEvent(event);

        ApprovalStageDefinitionType level = ctx.getCurrentStageDefinition();
        engine.triggerHelper.createTriggersForTimedActions(ctx.getCurrentCase(), workItem.getId(), escalationLevel,
                XmlTypeConverter.toDate(workItem.getCreateTimestamp()),
                XmlTypeConverter.toDate(workItem.getDeadline()), level.getTimedActions(), result);

        List<ObjectReferenceType> assigneesAndDeputiesAfter = engine.miscHelper.getAssigneesAndDeputies(workItem, ctx.getTask(), result);
        WorkItemAllocationChangeOperationInfo operationInfoAfter =
                new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputiesBefore, assigneesAndDeputiesAfter);
        ctx.prepareNotification(new DelayedNotification.AllocationChangeNew(ctx.getCurrentCase(), workItem, operationInfoAfter, sourceInfo));
    }
}
