/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandler;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class AccCertTimedActionTriggerHandler implements TriggerHandler {

	public static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TRIGGER_PREFIX + "/timed-action/handler-3";

	private static final transient Trace LOGGER = TraceManager.getTrace(AccCertTimedActionTriggerHandler.class);

	@Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
	@Autowired private AccCertQueryHelper queryHelper;
	@Autowired private AccCertUpdateHelper updateHelper;
	@Autowired private CertificationManagerImpl certManager;
	@Autowired private PrismContext prismContext;

	@PostConstruct
	private void initialize() {
		triggerHandlerRegistry.register(HANDLER_URI, this);
	}

	@Override
	public <O extends ObjectType> void handle(PrismObject<O> object, TriggerType trigger, Task triggerScannerTask, OperationResult parentResult) {
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
		} catch (RuntimeException|ObjectNotFoundException|ObjectAlreadyExistsException|SchemaException|SecurityViolationException|ExpressionEvaluationException e) {
			String message = "Exception while handling campaign trigger for " + campaign + ": " + e.getMessage();
			result.recordFatalError(message, e);
			throw new SystemException(message, e);
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private void executeNotifications(Duration timeBeforeAction, AbstractWorkItemActionType action, AccessCertificationCampaignType campaign,
			Task wfTask, OperationResult result) throws SchemaException {
		WorkItemOperationKindType operationKind = WfContextUtil.getOperationKind(action);
		WorkItemEventCauseInformationType cause = WfContextUtil.createCause(action);
		// TODO notifications before
		throw new UnsupportedOperationException("Custom notifications are not implemented yet.");
//		WorkItemAllocationChangeOperationInfo operationInfo =
//				new WorkItemAllocationChangeOperationInfo(operationKind, workItem.getAssigneeRef(), null);
//		WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(null, cause, action);
//		wfTaskController.notifyWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBeforeAction, wfTask, result);
	}

	private void executeActions(WorkItemActionsType actions, AccessCertificationCampaignType campaign, Task triggerScannerTask,
			OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
			ObjectAlreadyExistsException {
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
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
		List<AccessCertificationWorkItemType> workItems = queryHelper.searchWorkItems(
				CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaign.getOid(), prismContext),
				null, true, null, task, result);
		for (AccessCertificationWorkItemType workItem : workItems) {
			AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCase(workItem);
			if (aCase == null || aCase.getId() == null || workItem.getId() == null) {
				LOGGER.error("Couldn't auto-complete work item {} in case {}: some identifiers are missing", aCase, workItem);	// shouldn't occur
			} else {
				certManager.recordDecision(campaign.getOid(), aCase.getId(), workItem.getId(),
						OutcomeUtils.fromUri(completeAction.getOutcome()), null, task, result);
			}
		}
	}

	private void executeDelegateAction(AccessCertificationCampaignType campaign, DelegateWorkItemActionType delegateAction,
			Task task, OperationResult result)
			throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			ObjectAlreadyExistsException {
		List<AccessCertificationWorkItemType> workItems = queryHelper.searchWorkItems(
				CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaign.getOid(), prismContext),
				null, true, null, task, result);
		certManager.delegateWorkItems(campaign.getOid(), workItems, delegateAction, task, result);
	}

	private void executeEscalateAction(AccessCertificationCampaignType campaign, EscalateWorkItemActionType escalateAction,
			Task task, OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException, ObjectAlreadyExistsException {
		WorkItemEventCauseInformationType causeInformation = new WorkItemEventCauseInformationType()
				.type(WorkItemEventCauseTypeType.TIMED_ACTION)
				.name(escalateAction.getName())
				.displayName(escalateAction.getDisplayName());
		updateHelper.escalateCampaign(campaign.getOid(), escalateAction, causeInformation, task, result);
	}

	private void executeNotificationAction(AccessCertificationCampaignType campaign, @NotNull WorkItemNotificationActionType notificationAction, OperationResult result) throws SchemaException {
//		WorkItemEventCauseInformationType cause = createCauseInformation(notificationAction);
//		if (BooleanUtils.isNotFalse(notificationAction.isPerAssignee())) {
//			for (ObjectReferenceType assignee : campaign.getAssigneeRef()) {
//				wfTaskController.notifyWorkItemCustom(assignee, campaign, cause, wfTask, notificationAction, result);
//			}
//		} else {
//			wfTaskController.notifyWorkItemCustom(null, campaign, cause, wfTask, notificationAction, result);
//		}
		// TODO implement
		throw new UnsupportedOperationException("Not implemented yet.");
	}



}
