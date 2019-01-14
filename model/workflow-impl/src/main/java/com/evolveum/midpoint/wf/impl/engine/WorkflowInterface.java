/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.processes.ItemApprovalProcessOrchestrator;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.activiti.engine.delegate.DelegateTask;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *  Transports messages between midPoint and Activiti. (Originally via Camel, currently using direct java calls.)
 */

@Component
public class WorkflowInterface {

    private static final Trace LOGGER = TraceManager.getTrace(WorkflowInterface.class);
    private static final String DOT_CLASS = WorkflowInterface.class.getName() + ".";

	@Autowired private WorkflowEngine workflowEngine;
	@Autowired private ItemApprovalProcessOrchestrator itemApprovalProcessOrchestrator;

	public static String createWorkItemId(String caseOid, Long workItemId) {
		return caseOid + ":" + workItemId;
	}

	public static String getCaseOidFromWorkItemId(String workItemId) {
		return parseWorkItemId(workItemId)[0];
	}

	public static long getIdFromWorkItemId(String workItemId) {
		return Long.parseLong(parseWorkItemId(workItemId)[1]);
	}

	private static String[] parseWorkItemId(@NotNull String workItemId) {
		String[] components = workItemId.split(":");
		if (components.length != 2) {
			throw new IllegalStateException("Illegal work item ID: " + workItemId);
		} else {
			return components;
		}
	}

	public void startWorkflowProcessInstance(WfTaskCreationInstruction<?,?> instruction, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

		WfContextType wfContext = instruction.getWfContext();

		ItemApprovalEngineInvocationContext ctx = new ItemApprovalEngineInvocationContext(wfContext, task, task);
		workflowEngine.startProcessInstance(ctx, itemApprovalProcessOrchestrator, result);
	}

//	@Override
//	public void onProcessEvent(ProcessEvent event, Task task, OperationResult result)
//			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
//		wfTaskController.onProcessEvent(event, false, task, result);
//	}

//	public void notifyMidpointAboutProcessFinishedEvent(DelegateExecution execution) {
//        notifyMidpointAboutProcessEvent(new ProcessFinishedEvent(execution, processInterfaceFinder));
//    }

//    public void notifyMidpointAboutProcessEvent(DelegateExecution execution) {
//        notifyMidpointAboutProcessEvent(new ProcessEvent(execution, processInterfaceFinder));
//    }

//    private void notifyMidpointAboutProcessEvent(ProcessEvent event) {
//		OperationResult result = new OperationResult(DOT_CLASS + "notifyMidpointAboutProcessEvent");
//
//		String taskOid = event.getTaskOid();
//		Task task;
//		try {
//			task = taskManager.getTaskWithResult(taskOid, result);
//		} catch (ObjectNotFoundException|SchemaException|RuntimeException e) {
//			throw new SystemException("Couldn't get task " + taskOid + " from repository: " + e.getMessage(), e);
//		}
//		if (task.getExecutionStatus() != TaskExecutionStatus.WAITING) {
//			LOGGER.trace("Asynchronous message received in a state different from WAITING (actual state: {}), ignoring it. Task = {}", task.getExecutionStatus(), task);
//			return;
//		}
//		try {
//			wfTaskController.onProcessEvent(event, false, task, result);
//		} catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|RuntimeException e) {
//			throw new SystemException("Couldn't process a process-related event: " + e.getMessage(), e);
//		}
//    }

    public void notifyMidpointAboutTaskEvent(DelegateTask delegateTask) {
//        OperationResult result = new OperationResult(DOT_CLASS + "notifyMidpointAboutTaskEvent");
//
//        WorkItemEvent workItemEvent;
//        if (TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
//            workItemEvent = new WorkItemCreatedEvent();     // TODO distinguish created vs. assigned event
//        } else if (TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
//            workItemEvent = new WorkItemCompletedEvent();
//        } else if (TaskListener.EVENTNAME_DELETE.equals(delegateTask.getEventName())) {
//            workItemEvent = new WorkItemDeletedEvent();
//        } else {
//            return;         // ignoring other events
//        }
//
//        workItemEvent.setVariables(delegateTask.getVariables());
//        workItemEvent.setAssigneeOid(delegateTask.getAssignee());
//        workItemEvent.setTaskId(delegateTask.getId());
//        workItemEvent.setTaskName(delegateTask.getName());
//        workItemEvent.setProcessInstanceName((String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME));
//        workItemEvent.setProcessInstanceId(delegateTask.getProcessInstanceId());
//        workItemEvent.setCreateTime(delegateTask.getCreateTime());
//        workItemEvent.setDueDate(delegateTask.getDueDate());
//        workItemEvent.setExecutionId(delegateTask.getExecutionId());
//        workItemEvent.setOwner(delegateTask.getOwner());
//        for (IdentityLink identityLink : delegateTask.getCandidates()) {
//            if (identityLink.getUserId() != null) {
//                workItemEvent.getCandidateUsers().add(identityLink.getUserId());
//            } else if (identityLink.getGroupId() != null) {
//                workItemEvent.getCandidateGroups().add(identityLink.getGroupId());
//            } else {
//                throw new IllegalStateException("Neither candidate user nor group id is provided in delegateTask: " + delegateTask);
//            }
//        }
//
//        try {
//			WorkItemType workItem = workItemProvider.taskEventToWorkItemNew(workItemEvent, null, true, true, true, result);
//            wfTaskController.onTaskEvent(workItem, workItemEvent, result);
//        } catch (Exception e) {     // todo fix the exception processing e.g. think about situation where an event cannot be audited - should we allow to proceed?
//            String message = "Couldn't process an event coming from the workflow management system";
//            LoggingUtils.logUnexpectedException(LOGGER, message, e);
//            result.recordFatalError(message, e);
//        }
    }
}
