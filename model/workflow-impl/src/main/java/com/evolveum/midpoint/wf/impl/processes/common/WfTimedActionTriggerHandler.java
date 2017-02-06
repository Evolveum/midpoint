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

import com.evolveum.midpoint.model.impl.trigger.TriggerHandler;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemManager;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class WfTimedActionTriggerHandler implements TriggerHandler {

	public static final String HANDLER_URI = WorkflowConstants.NS_WORKFLOW_TRIGGER_PREFIX + "/timed-action/handler-3";

	private static final transient Trace LOGGER = TraceManager.getTrace(WfTimedActionTriggerHandler.class);

	@Autowired
	private TriggerHandlerRegistry triggerHandlerRegistry;

	@Autowired
	private WorkItemProvider workItemProvider;

	@Autowired
	private WorkItemManager workItemManager;

	@Autowired
	private WfTaskController wfTaskController;

	@Autowired
	private TaskManager taskManager;

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
		String workItemId = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstantsGenerated.C_WORK_ITEM_ID);
		WorkItemActionsType actions = ObjectTypeUtil.getExtensionItemRealValue(trigger.getExtension(), SchemaConstantsGenerated.C_WORK_ITEM_ACTIONS);
		if (workItemId == null || actions == null) {
			LOGGER.warn("Trigger without workItemId and workItemActions; ignoring it: " + trigger);
			return;
		}
		OperationResult result = parentResult.createSubresult(WfTimedActionTriggerHandler.class.getName() + ".handle");
		try {
			WorkItemType workItem = workItemProvider.getWorkItem(workItemId, result);
			if (workItem == null) {
				throw new ObjectNotFoundException("No work item with ID " + workItemId);
			}
			Task wfTask = taskManager.createTaskInstance(wfTaskType.asPrismObject(), result);
			for (WorkItemNotificationActionType notificationAction : actions.getNotify()) {
				executeNotificationAction(workItem, notificationAction, wfTask, result);
			}
			if (actions.getDelegate() != null) {
				executeDelegateAction(workItem, actions.getDelegate(), false, wfTask, result);
			}
			if (actions.getEscalate() != null) {
				executeDelegateAction(workItem, actions.getEscalate(), true, wfTask, result);
			}
			if (actions.getComplete() != null) {
				executeCompleteAction(workItem, actions.getComplete(), wfTask, result);
			}
		} catch (RuntimeException|ObjectNotFoundException|SchemaException|SecurityViolationException e) {
			String message = "Exception while handling work item trigger for ID " + workItemId + ": " + e.getMessage();
			result.recordFatalError(message, e);
			throw new SystemException(message, e);
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private void executeCompleteAction(WorkItemType workItem, CompleteWorkItemActionType completeAction, Task wfTask,
			OperationResult result) throws SchemaException, SecurityViolationException {
		WorkItemOutcomeType outcome = completeAction.getOutcome() != null ? completeAction.getOutcome() : WorkItemOutcomeType.REJECT;
		workItemManager.completeWorkItem(workItem.getWorkItemId(), ApprovalUtils.approvalStringValue(outcome),
				null, null, createCauseInformation(completeAction), result);
	}

	private void executeDelegateAction(WorkItemType workItem, DelegateWorkItemActionType delegateAction, boolean escalate, Task wfTask,
			OperationResult result) throws SecurityViolationException, ObjectNotFoundException {
		String escalationLevelName;
		String escalationLevelDisplayName;
		if (escalate && delegateAction instanceof EscalateWorkItemActionType) {
			escalationLevelName = ((EscalateWorkItemActionType) delegateAction).getEscalationLevelName();
			escalationLevelDisplayName = ((EscalateWorkItemActionType) delegateAction).getEscalationLevelDisplayName();
		} else {
			escalationLevelName = escalationLevelDisplayName = null;
		}
		workItemManager.delegateWorkItem(workItem.getWorkItemId(), delegateAction.getApproverRef(),
				delegateAction.getDelegationMethod(), escalate, escalationLevelName, escalationLevelDisplayName,
				createCauseInformation(delegateAction), result);
	}

	private void executeNotificationAction(WorkItemType workItem, WorkItemNotificationActionType notificationAction, Task wfTask,
			OperationResult result) throws SchemaException {
		wfTaskController.executeWorkItemNotificationAction(workItem, notificationAction, wfTaskController.recreateWfTask(wfTask), result);
	}

	private WorkItemEventCauseInformationType createCauseInformation(AbstractWorkItemActionType action) {
		WorkItemEventCauseInformationType cause = new WorkItemEventCauseInformationType();
		cause.setCause(WorkItemEventCauseType.TIMED_ACTION);
		cause.setCauseName(action.getName());
		cause.setCauseDisplayName(action.getDisplayName());
		return cause;
	}



}
