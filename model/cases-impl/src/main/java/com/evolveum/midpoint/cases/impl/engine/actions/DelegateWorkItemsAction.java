/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.api.events.FutureNotificationEvent.AllocationChangeCurrent;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.cases.api.events.FutureNotificationEvent;
import com.evolveum.midpoint.cases.impl.engine.helpers.WorkItemHelper;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.CaseRelatedUtils;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.cases.api.events.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.cases.api.events.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.cases.api.request.DelegateWorkItemsRequest;
import com.evolveum.midpoint.cases.impl.helpers.AuthorizationHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType.ESCALATE;

/**
 * Delegates (or escalates) given work items.
 */
public class DelegateWorkItemsAction extends RequestedAction<DelegateWorkItemsRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(DelegateWorkItemsAction.class);

    public DelegateWorkItemsAction(CaseEngineOperationImpl ctx, DelegateWorkItemsRequest request) {
        super(ctx, request, LOGGER);
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        XMLGregorianCalendar now = // TODO use clock here
                request.getNow() != null ? request.getNow() : XmlTypeConverter.createXMLGregorianCalendar();
        for (DelegateWorkItemsRequest.SingleDelegation delegation : request.getDelegations()) {
            executeDelegation(delegation, now, result);
        }
        return null;
    }

    // TODO check work item state;
    //  check if there are any approvers etc

    private void executeDelegation(
            DelegateWorkItemsRequest.SingleDelegation delegation,
            XMLGregorianCalendar now,
            OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        CaseWorkItemType workItem = operation.getWorkItemById(delegation.getWorkItemId());

        if (!beans.authorizationHelper.isAuthorized(
                workItem, AuthorizationHelper.RequestedOperation.DELEGATE, operation.getTask(), result)) {
            throw new SecurityViolationException("You are not authorized to delegate this work item.");
        }

        if (workItem.getCloseTimestamp() != null) {
            LOGGER.debug("Work item {} in {} was already closed, ignoring the delegation request", workItem, operation.getCurrentCase());
            result.recordWarning("Work item was already closed");
            return;
        }

        List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
        List<ObjectReferenceType> assigneesAndDeputiesBefore =
                beans.miscHelper.getAssigneesAndDeputies(workItem, operation.getTask(), result);

        WorkItemOperationKindType operationKind = delegation.getTargetEscalationInfo() != null ? ESCALATE : DELEGATE;

        WorkItemAllocationChangeOperationInfo operationInfoBefore =
                new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputiesBefore, null);

        WorkItemEventCauseInformationType causeInformation = request.getCauseInformation();
        ObjectReferenceType initiator =
                causeInformation == null || causeInformation.getType() == WorkItemEventCauseTypeType.USER_ACTION ?
                        ObjectTypeUtil.createObjectRef(operation.getPrincipal().getFocus()) : null;

        WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(initiator, causeInformation, null);
        notificationEvents.add(
                new AllocationChangeCurrent(
                        operation.getCurrentCase(), workItem, operationInfoBefore, sourceInfo, null));

        List<ObjectReferenceType> newAssignees = new ArrayList<>();
        List<ObjectReferenceType> delegatedTo = new ArrayList<>();
        CaseRelatedUtils
                .computeAssignees(
                        newAssignees, delegatedTo, delegation.getDelegates(), delegation.getMethod(), workItem.getAssigneeRef());

        workItem.getAssigneeRef().clear();
        workItem.getAssigneeRef().addAll(CloneUtil.cloneCollectionMembers(newAssignees));
        if (delegation.getNewDuration() != null) {
            XMLGregorianCalendar newDeadline;
            newDeadline = CloneUtil.clone(now);
            newDeadline.add(delegation.getNewDuration());
            workItem.setDeadline(newDeadline);
        }

        int escalationLevel = WorkItemTypeUtil.getEscalationLevelNumber(workItem);
        WorkItemEscalationLevelType newEscalationInfo;
        if (delegation.getTargetEscalationInfo() != null) {
            newEscalationInfo = delegation.getTargetEscalationInfo().clone();
            newEscalationInfo.setNumber(++escalationLevel);
        } else {
            newEscalationInfo = null;
        }

        WorkItemDelegationEventType event = ApprovalContextUtil
                .createDelegationEvent(
                        newEscalationInfo,
                        assigneesBefore,
                        delegatedTo,
                        delegation.getMethod(),
                        causeInformation);
        event.setComment(delegation.getComment());
        if (newEscalationInfo != null) {
            workItem.setEscalationLevel(newEscalationInfo);
        }

        WorkItemId workItemId = operation.createWorkItemId(workItem);

        WorkItemHelper.fillInWorkItemEvent(event, operation.getPrincipal(), workItemId, workItem);
        operation.addCaseHistoryEvent(event);

        ApprovalStageDefinitionType level = operation.getCurrentStageDefinition();
        if (level != null) {
            beans.triggerHelper.createTriggersForTimedActions(
                    operation.getCurrentCase(),
                    workItem.getId(),
                    escalationLevel,
                    XmlTypeConverter.toDate(workItem.getCreateTimestamp()),
                    XmlTypeConverter.toDate(workItem.getDeadline()),
                    level.getTimedActions()
            );
        }

        List<ObjectReferenceType> assigneesAndDeputiesAfter =
                beans.miscHelper.getAssigneesAndDeputies(workItem, operation.getTask(), result);
        WorkItemAllocationChangeOperationInfo operationInfoAfter =
                new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputiesBefore, assigneesAndDeputiesAfter);
        notificationEvents.add(
                new FutureNotificationEvent.AllocationChangeNew(operation.getCurrentCase(), workItem, operationInfoAfter, sourceInfo));
    }
}
