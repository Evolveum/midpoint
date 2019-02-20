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

package com.evolveum.midpoint.wf.impl.processes.common;

import com.evolveum.midpoint.model.impl.trigger.MultipleTriggersHandler;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.impl.engine.dao.CompleteAction;
import com.evolveum.midpoint.wf.impl.engine.dao.WorkItemManager;
import com.evolveum.midpoint.wf.impl.engine.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.impl.tasks.WfNotificationHelper;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 */
@Component
public class WfTimedActionTriggerHandler implements MultipleTriggersHandler {

	public static final String HANDLER_URI = WorkflowConstants.NS_WORKFLOW_TRIGGER_PREFIX + "/timed-action/handler-3";

	private static final transient Trace LOGGER = TraceManager.getTrace(WfTimedActionTriggerHandler.class);

	@Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
	@Autowired private WorkItemProvider workItemProvider;
	@Autowired private WorkItemManager workItemManager;
	@Autowired private WfTaskController wfTaskController;
	@Autowired private WfNotificationHelper notificationHelper;
	@Autowired private TaskManager taskManager;
	@Autowired private WfExpressionEvaluationHelper evaluationHelper;
	@Autowired private WfStageComputeHelper stageComputeHelper;

	@PostConstruct
	private void initialize() {
		triggerHandlerRegistry.register(HANDLER_URI, this);
	}

	@Override
	public <O extends ObjectType> Collection<TriggerType> handle(PrismObject<O> object, Collection<TriggerType> triggers, RunningTask triggerScannerTask, OperationResult parentResult) {
		if (!(object.asObjectable() instanceof TaskType)) {
			throw new IllegalArgumentException("Unexpected object type: should be TaskType: " + object);
		}
		TaskType wfTaskType = (TaskType) object.asObjectable();
		WfContextType wfc = wfTaskType.getWorkflowContext();
		if (wfc == null) {
			LOGGER.warn("Task without workflow context; ignoring it: " + object);
			return triggers;
		}

		OperationResult triggersResult = parentResult.createSubresult(WfTimedActionTriggerHandler.class.getName() + ".handle");
		try {
			List<TriggerType> processedTriggers = new ArrayList<>();

			/*
			 * "Complete" action has to be executed for all the work items at once. Otherwise the first work item will be regularly
			 * completed and the other ones cancelled because the stage is being closed. (Before midPoint 4.0 there has to be
			 * a special code to treat this.)
			 */
			List<CompleteAction> completeActions = new ArrayList<>();
			Task wfTask;
			try {
				wfTask = taskManager.createTaskInstance(wfTaskType.asPrismObject(), triggersResult);
			} catch (SchemaException e) {
				String message = "Exception while handling work item triggers: " + e.getMessage();
				triggersResult.recordFatalError(message, e);
				throw new SystemException(message, e);
			}

			for (TriggerType trigger : triggers) {
				boolean ok = true;
				OperationResult triggerResult = triggersResult
						.createSubresult(WfTimedActionTriggerHandler.class.getName() + ".handleTrigger");
				String workItemId = ObjectTypeUtil
						.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
				if (workItemId == null) {
					LOGGER.warn("Trigger without workItemId; ignoring it: " + trigger);
					triggerResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No work item ID");
				} else {
					try {
						WorkItemType workItem = workItemProvider.getWorkItem(workItemId, triggerResult);
						Duration timeBeforeAction = ObjectTypeUtil
								.getExtensionItemRealValue(trigger.getExtension(),
										SchemaConstants.MODEL_EXTENSION_TIME_BEFORE_ACTION);
						if (timeBeforeAction != null) {
							AbstractWorkItemActionType action = ObjectTypeUtil
									.getExtensionItemRealValue(trigger.getExtension(),
											SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTION);
							if (action == null) {
								LOGGER.warn("Notification trigger without workItemAction; ignoring it: {}", trigger);
								continue;
							}
							executeNotifications(timeBeforeAction, action, workItem, wfTask, triggerResult);
						} else {
							WorkItemActionsType actions = ObjectTypeUtil
									.getExtensionItemRealValue(trigger.getExtension(),
											SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTIONS);
							if (actions == null) {
								LOGGER.warn("Trigger without workItemActions; ignoring it: " + trigger);
								continue;
							}
							executeActions(actions, workItem, wfTask, completeActions, triggerScannerTask, triggerResult);
						}
						triggerResult.computeStatusIfUnknown();
					} catch (RuntimeException | ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | CommunicationException | ConfigurationException | ObjectAlreadyExistsException e) {
						String message =
								"Exception while handling work item trigger for ID " + workItemId + ": " + e.getMessage();
						triggerResult.recordPartialError(message, e);
						ok = false;
					}
				}
				if (ok) {
					processedTriggers.add(trigger);
				}
			}

			if (!completeActions.isEmpty()) {
				OperationResult result = triggersResult
						.createSubresult(WfTimedActionTriggerHandler.class.getName() + ".handleCompletions");
				try {
					workItemManager.completeWorkItems(completeActions, triggerScannerTask, result);
					result.recordSuccessIfUnknown();
				} catch (Throwable t) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't handler work item completion", t);
					result.recordFatalError("Couldn't handle work item completion", t);
					// but let's ignore this fact and mark all triggers as processed
				}
			}
			triggersResult.computeStatus();
			return processedTriggers;
		} catch (Throwable t) {
			triggersResult.recordFatalError("Couldn't process triggers: " + t.getMessage(), t);
			throw t;
		}
	}

	private void executeNotifications(Duration timeBeforeAction, AbstractWorkItemActionType action, WorkItemType workItem,
			Task wfTask, OperationResult result) throws SchemaException {
		WorkItemOperationKindType operationKind = WfContextUtil.getOperationKind(action);
		WorkItemEventCauseInformationType cause = WfContextUtil.createCause(action);
		List<ObjectReferenceType> assigneesAndDeputies = wfTaskController.getAssigneesAndDeputies(workItem, wfTask, result);
		WorkItemAllocationChangeOperationInfo operationInfo =
				new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputies, null);
		WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(null, cause, action);
		notificationHelper.notifyWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBeforeAction, wfTask, result);
	}

	private void executeActions(WorkItemActionsType actions, WorkItemType workItem, Task wfTask,
			List<CompleteAction> completeActions, Task triggerScannerTask, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException {
		for (WorkItemNotificationActionType notificationAction : actions.getNotify()) {
			executeNotificationAction(workItem, notificationAction, wfTask, result);
		}
		if (actions.getDelegate() != null) {
			executeDelegateAction(workItem, actions.getDelegate(), false, wfTask, triggerScannerTask, result);
		}
		if (actions.getEscalate() != null) {
			executeDelegateAction(workItem, actions.getEscalate(), true, wfTask, triggerScannerTask, result);
		}
		CompleteWorkItemActionType complete = actions.getComplete();
		if (complete != null) {
			completeActions.add(new CompleteAction(workItem,
					defaultIfNull(complete.getOutcome(), SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT),
					null, null, WfContextUtil.createCause(complete)));
		}
	}

//	private void executeCompleteAction(WorkItemType workItem, CompleteWorkItemActionType completeAction,
//			OperationResult result)
//			throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
//			CommunicationException, ConfigurationException, ObjectAlreadyExistsException {
//		WorkItemOutcomeType outcome = completeAction.getOutcome() != null ? ApprovalUtils.fromUri(completeAction.getOutcome()) : WorkItemOutcomeType.REJECT;
//		workItemManager.completeWorkItem(workItem.getExternalId(), ApprovalUtils.toUri(outcome),
//				null, null, WfContextUtil.createCause(completeAction), result);
//	}

	private void executeDelegateAction(WorkItemType workItem, DelegateWorkItemActionType delegateAction, boolean escalate,
			Task wfTask, Task triggerScannerTask, OperationResult result)
			throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		WorkItemEscalationLevelType escLevel = escalate ? WfContextUtil.createEscalationLevelInformation(delegateAction) : null;
		List<ObjectReferenceType> delegates = computeDelegateTo(delegateAction, workItem, wfTask, triggerScannerTask, result);
		workItemManager.delegateWorkItem(workItem.getExternalId(), delegates,
				delegateAction.getDelegationMethod(), escLevel,
				delegateAction.getDuration(), WfContextUtil.createCause(delegateAction), result);
	}

	private List<ObjectReferenceType> computeDelegateTo(DelegateWorkItemActionType delegateAction, WorkItemType workItem,
			Task wfTask, Task triggerScannerTask, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		List<ObjectReferenceType> rv = new ArrayList<>();
		rv.addAll(CloneUtil.cloneCollectionMembers(delegateAction.getApproverRef()));
		if (!delegateAction.getApproverExpression().isEmpty()) {
			ExpressionVariables variables = stageComputeHelper.getDefaultVariables(null, wfTask, result);
			variables.addVariableDefinition(SchemaConstants.C_WORK_ITEM, workItem);
			rv.addAll(evaluationHelper.evaluateRefExpressions(delegateAction.getApproverExpression(),
					variables, "computing delegates", triggerScannerTask, result));
		}
//		if (!delegateAction.getApproverRelation().isEmpty()) {
//			throw new UnsupportedOperationException("Approver relation in delegate/escalate action is not supported yet.");
//		}
		return rv;
	}

	private void executeNotificationAction(WorkItemType workItem, @NotNull WorkItemNotificationActionType notificationAction, Task wfTask,
			OperationResult result) throws SchemaException {
		WorkItemEventCauseInformationType cause = WfContextUtil.createCause(notificationAction);
		if (BooleanUtils.isNotFalse(notificationAction.isPerAssignee())) {
			List<ObjectReferenceType> assigneesAndDeputies = wfTaskController.getAssigneesAndDeputies(workItem, wfTask, result);
			for (ObjectReferenceType assigneeOrDeputy : assigneesAndDeputies) {
				notificationHelper.notifyWorkItemCustom(assigneeOrDeputy, workItem, cause, wfTask, notificationAction, result);
			}
		} else {
			notificationHelper.notifyWorkItemCustom(null, workItem, cause, wfTask, notificationAction, result);
		}

	}

}
