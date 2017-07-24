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

import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandler;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemManager;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class WfTimedActionTriggerHandler implements TriggerHandler {

	public static final String HANDLER_URI = WorkflowConstants.NS_WORKFLOW_TRIGGER_PREFIX + "/timed-action/handler-3";

	private static final transient Trace LOGGER = TraceManager.getTrace(WfTimedActionTriggerHandler.class);

	@Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
	@Autowired private WorkItemProvider workItemProvider;
	@Autowired private WorkItemManager workItemManager;
	@Autowired private WfTaskController wfTaskController;
	@Autowired private TaskManager taskManager;
	@Autowired private WfExpressionEvaluationHelper evaluationHelper;

	@PostConstruct
	private void initialize() {
		triggerHandlerRegistry.register(HANDLER_URI, this);
	}

	@Override
	public <O extends ObjectType> void handle(PrismObject<O> object, TriggerType trigger, Task triggerScannerTask, OperationResult parentResult) {
		if (!(object.asObjectable() instanceof TaskType)) {
			throw new IllegalArgumentException("Unexpected object type: should be TaskType: " + object);
		}
		TaskType wfTaskType = (TaskType) object.asObjectable();
		WfContextType wfc = wfTaskType.getWorkflowContext();
		if (wfc == null) {
			LOGGER.warn("Task without workflow context; ignoring it: " + object);
			return;
		}
		String workItemId = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
		if (workItemId == null) {
			LOGGER.warn("Trigger without workItemId; ignoring it: " + trigger);
			return;
		}
		OperationResult result = parentResult.createSubresult(WfTimedActionTriggerHandler.class.getName() + ".handle");
		try {
			WorkItemType workItem = workItemProvider.getWorkItem(workItemId, result);
			if (workItem == null) {
				throw new ObjectNotFoundException("No work item with ID " + workItemId);
			}
			Task wfTask = taskManager.createTaskInstance(wfTaskType.asPrismObject(), result);
			Duration timeBeforeAction = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_TIME_BEFORE_ACTION);
			if (timeBeforeAction != null) {
				AbstractWorkItemActionType action = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTION);
				if (action == null) {
					LOGGER.warn("Notification trigger without workItemAction; ignoring it: " + trigger);
					return;
				}
				executeNotifications(timeBeforeAction, action, workItem, wfTask, result);
			} else {
				WorkItemActionsType actions = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTIONS);
				if (actions == null) {
					LOGGER.warn("Trigger without workItemActions; ignoring it: " + trigger);
					return;
				}
				executeActions(actions, workItem, wfTask, triggerScannerTask, result);
			}
		} catch (RuntimeException|ObjectNotFoundException|SchemaException|SecurityViolationException|ExpressionEvaluationException e) {
			String message = "Exception while handling work item trigger for ID " + workItemId + ": " + e.getMessage();
			result.recordFatalError(message, e);
			throw new SystemException(message, e);
		} finally {
			result.computeStatusIfUnknown();
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
		wfTaskController.notifyWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBeforeAction, wfTask, result);
	}

	private void executeActions(WorkItemActionsType actions, WorkItemType workItem, Task wfTask, Task triggerScannerTask,
			OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException {
		for (WorkItemNotificationActionType notificationAction : actions.getNotify()) {
			executeNotificationAction(workItem, notificationAction, wfTask, result);
		}
		if (actions.getDelegate() != null) {
			executeDelegateAction(workItem, actions.getDelegate(), false, wfTask, triggerScannerTask, result);
		}
		if (actions.getEscalate() != null) {
			executeDelegateAction(workItem, actions.getEscalate(), true, wfTask, triggerScannerTask, result);
		}
		if (actions.getComplete() != null) {
			executeCompleteAction(workItem, actions.getComplete(), result);
		}
	}

	private void executeCompleteAction(WorkItemType workItem, CompleteWorkItemActionType completeAction,
			OperationResult result) throws SchemaException, SecurityViolationException {
		WorkItemOutcomeType outcome = completeAction.getOutcome() != null ? ApprovalUtils.fromUri(completeAction.getOutcome()) : WorkItemOutcomeType.REJECT;
		workItemManager.completeWorkItem(workItem.getExternalId(), ApprovalUtils.toUri(outcome),
				null, null, WfContextUtil.createCause(completeAction), result);
	}

	private void executeDelegateAction(WorkItemType workItem, DelegateWorkItemActionType delegateAction, boolean escalate,
			Task wfTask, Task triggerScannerTask, OperationResult result)
			throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		WorkItemEscalationLevelType escLevel = escalate ? WfContextUtil.createEscalationLevelInformation(delegateAction) : null;
		List<ObjectReferenceType> delegates = computeDelegateTo(delegateAction, workItem, wfTask, triggerScannerTask, result);
		workItemManager.delegateWorkItem(workItem.getExternalId(), delegates,
				delegateAction.getDelegationMethod(), escLevel,
				delegateAction.getDuration(), WfContextUtil.createCause(delegateAction), result);
	}

	private List<ObjectReferenceType> computeDelegateTo(DelegateWorkItemActionType delegateAction, WorkItemType workItem,
			Task wfTask, Task triggerScannerTask, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		List<ObjectReferenceType> rv = new ArrayList<>();
		rv.addAll(CloneUtil.cloneCollectionMembers(delegateAction.getApproverRef()));
		if (!delegateAction.getApproverExpression().isEmpty()) {
			ExpressionVariables variables = evaluationHelper.getDefaultVariables(null, wfTask, result);
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
				wfTaskController.notifyWorkItemCustom(assigneeOrDeputy, workItem, cause, wfTask, notificationAction, result);
			}
		} else {
			wfTaskController.notifyWorkItemCustom(null, workItem, cause, wfTask, notificationAction, result);
		}

	}

}
