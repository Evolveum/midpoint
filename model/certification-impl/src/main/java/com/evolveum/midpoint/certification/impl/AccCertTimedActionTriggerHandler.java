/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.model.impl.trigger.SingleTriggerHandler;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import java.util.List;

@Component
public class AccCertTimedActionTriggerHandler implements SingleTriggerHandler {

    static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TRIGGER_PREFIX + "/timed-action/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(AccCertTimedActionTriggerHandler.class);

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private AccCertQueryHelper queryHelper;
    @Autowired private AccCertCaseOperationsHelper operationsHelper;
    @Autowired private CertificationManagerImpl certManager;
    @Autowired private PrismContext prismContext;

    @PostConstruct
    private void initialize() {
        triggerHandlerRegistry.register(HANDLER_URI, this);
    }

    @Override
    public <O extends ObjectType> void handle(@NotNull PrismObject<O> object, @NotNull TriggerType trigger,
            @NotNull RunningTask triggerScannerTask, @NotNull OperationResult parentResult) {
        if (!(object.asObjectable() instanceof AccessCertificationCampaignType)) {
            throw new IllegalArgumentException("Unexpected object type: should be AccessCertificationCampaignType: " + object);
        }
        AccessCertificationCampaignType campaign = (AccessCertificationCampaignType) object.asObjectable();
        OperationResult result = parentResult.createSubresult(AccCertTimedActionTriggerHandler.class.getName() + ".handle");
        try {
            Duration timeBeforeAction = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_TIME_BEFORE_ACTION);
            if (timeBeforeAction != null) {
                AbstractWorkItemActionType action = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTION);
                if (action == null) {
                    LOGGER.warn("Notification trigger without workItemAction; ignoring it: {}", trigger);
                    return;
                }
                executeNotifications(timeBeforeAction, action, campaign, triggerScannerTask, result);
            } else {
                WorkItemActionsType actions = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTIONS);
                if (actions == null) {
                    LOGGER.warn("Trigger without workItemActions; ignoring it: {}", trigger);
                    return;
                }
                executeActions(actions, campaign, triggerScannerTask, result);
            }
        } catch (CommonException | RuntimeException e) {
            String message = "Exception while handling campaign trigger for " + campaign + ": " + e.getMessage();
            result.recordFatalError(message, e);
            throw new SystemException(message, e);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @SuppressWarnings("unused")
    private void executeNotifications(Duration timeBeforeAction, AbstractWorkItemActionType action, AccessCertificationCampaignType campaign,
            Task wfTask, OperationResult result) {
        WorkItemOperationKindType operationKind = ApprovalContextUtil.getOperationKind(action);
        WorkItemEventCauseInformationType cause = ApprovalContextUtil.createCause(action);
        // TODO notifications before
        throw new UnsupportedOperationException("Custom notifications are not implemented yet.");
//        WorkItemAllocationChangeOperationInfo operationInfo =
//                new WorkItemAllocationChangeOperationInfo(operationKind, workItem.getAssigneeRef(), null);
//        WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(null, cause, action);
//        caseController.notifyWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBeforeAction, wfTask, result);
    }

    private void executeActions(WorkItemActionsType actions, AccessCertificationCampaignType campaign, Task triggerScannerTask,
            OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            ObjectAlreadyExistsException, ConfigurationException, CommunicationException {
        for (WorkItemNotificationActionType notificationAction : actions.getNotify()) {
            executeNotificationAction(campaign, notificationAction, result);
        }
        if (actions.getDelegate() != null) {
            executeDelegateAction(campaign, actions.getDelegate(), triggerScannerTask, result);
        }
        if (actions.getEscalate() != null) {
            executeEscalateAction(campaign, actions.getEscalate(), triggerScannerTask, result);
        }
        if (actions.getComplete() != null) {
            executeCompleteAction(campaign, actions.getComplete(), triggerScannerTask, result);
        }
    }

    private void executeCompleteAction(AccessCertificationCampaignType campaign, CompleteWorkItemActionType completeAction,
            Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<AccessCertificationWorkItemType> workItems = queryHelper.searchOpenWorkItems(
                CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaign.getOid(), prismContext),
                true,
                result);
        for (AccessCertificationWorkItemType workItem : workItems) {
            AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCase(workItem);
            if (aCase == null || aCase.getId() == null || workItem.getId() == null) {
                LOGGER.error("Couldn't auto-complete work item {} in case {}: some identifiers are missing", aCase, workItem);    // shouldn't occur
            } else {
                certManager.recordDecision(campaign.getOid(), aCase.getId(), workItem.getId(),
                        OutcomeUtils.fromUri(completeAction.getOutcome()), null, task, result);
            }
        }
    }

    private void executeDelegateAction(AccessCertificationCampaignType campaign, DelegateWorkItemActionType delegateAction,
            Task task, OperationResult result)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            ObjectAlreadyExistsException, ConfigurationException, CommunicationException {
        List<AccessCertificationWorkItemType> workItems = queryHelper.searchOpenWorkItems(
                CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaign.getOid(), prismContext),
                true,
                result);
        certManager.delegateWorkItems(campaign.getOid(), workItems, delegateAction, task, result);
    }

    private void executeEscalateAction(AccessCertificationCampaignType campaign, EscalateWorkItemActionType escalateAction,
            Task task, OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ObjectAlreadyExistsException {
        WorkItemEventCauseInformationType causeInformation = new WorkItemEventCauseInformationType()
                .type(WorkItemEventCauseTypeType.TIMED_ACTION)
                .name(escalateAction.getName())
                .displayName(escalateAction.getDisplayName());
        operationsHelper.escalateCampaign(campaign.getOid(), escalateAction, causeInformation, task, result);
    }

    @SuppressWarnings("unused")
    private void executeNotificationAction(AccessCertificationCampaignType campaign, @NotNull WorkItemNotificationActionType notificationAction, OperationResult result) {
//        WorkItemEventCauseInformationType cause = createCauseInformation(notificationAction);
//        if (BooleanUtils.isNotFalse(notificationAction.isPerAssignee())) {
//            for (ObjectReferenceType assignee : campaign.getAssigneeRef()) {
//                caseController.notifyWorkItemCustom(assignee, campaign, cause, wfTask, notificationAction, result);
//            }
//        } else {
//            caseController.notifyWorkItemCustom(null, campaign, cause, wfTask, notificationAction, result);
//        }
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
