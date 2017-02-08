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

package com.evolveum.midpoint.wf.impl.activiti;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.impl.messages.*;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricDetailQuery;
import org.activiti.engine.history.HistoricFormProperty;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceBuilder;
import org.activiti.engine.task.IdentityLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 *  Transports messages between midPoint and Activiti. (Originally via Camel, currently using direct java calls.)
 */

@Component
public class ActivitiInterface {

    private static final Trace LOGGER = TraceManager.getTrace(ActivitiInterface.class);
    private static final String DOT_CLASS = ActivitiInterface.class.getName() + ".";

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private WfTaskController wfTaskController;

    @Autowired
    private ProcessInterfaceFinder processInterfaceFinder;

	@Autowired
	private WorkItemProvider workItemProvider;

	public void startActivitiProcessInstance(StartProcessCommand spic, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

		String owner = spic.getProcessOwner();
		if (owner != null) {
			activitiEngine.getIdentityService().setAuthenticatedUserId(owner);
		}

		RuntimeService rs = activitiEngine.getProcessEngine().getRuntimeService();
		ProcessInstanceBuilder builder = rs.createProcessInstanceBuilder()
				.processDefinitionKey(spic.getProcessName())
				.processInstanceName(spic.getProcessInstanceName());
		for (Map.Entry<String, Object> varEntry : spic.getVariables().entrySet()) {
			builder.addVariable(varEntry.getKey(), varEntry.getValue());
		}
		ProcessInstance pi = builder.start();

		if (spic.isSendStartConfirmation()) {		// let us send a reply back (useful for listener-free processes)
			ProcessStartedEvent event = new ProcessStartedEvent(
					pi.getProcessInstanceId(),
					((ExecutionEntity) pi).getVariables(),	// a bit of hack...
					processInterfaceFinder);
			event.setRunning(!pi.isEnded());
			LOGGER.trace("Event to be sent to IDM: {}", event);
			wfTaskController.onProcessEvent(event, task, result);
		}
	}

	public void queryActivitiProcessInstance(QueryProcessCommand qpc, Task task, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {

		String pid = qpc.getPid();
		LOGGER.trace("Querying process instance id {}", pid);

		HistoryService hs = activitiEngine.getHistoryService();

		HistoricDetailQuery hdq = hs.createHistoricDetailQuery()
				.variableUpdates()
				.processInstanceId(pid)
				.orderByTime().desc();

		Map<String,Object> variables = new HashMap<>();
		for (HistoricDetail hd : hdq.list()) {
			HistoricVariableUpdate hvu = (HistoricVariableUpdate) hd;
			String varname = hvu.getVariableName();
			Object value = hvu.getValue();
			LOGGER.trace(" - found historic variable update: {} <- {}", varname, value);
			if (!variables.containsKey(varname)) {
				variables.put(varname, value);
			}
		}

		HistoricDetailQuery hdq2 = hs.createHistoricDetailQuery()
				.formProperties()
				.processInstanceId(pid)
				.orderByVariableRevision().desc();
		for (HistoricDetail hd : hdq2.list()) {
			HistoricFormProperty hfp = (HistoricFormProperty) hd;
			String varname = hfp.getPropertyId();
			Object value = hfp.getPropertyValue();
			LOGGER.trace(" - found historic form property: {} <- {}", varname, value);
			variables.put(varname, value);
		}

		QueryProcessResponse qpr = new QueryProcessResponse(pid, variables, processInterfaceFinder);

		ProcessInstance pi = activitiEngine.getProcessEngine().getRuntimeService().createProcessInstanceQuery().processInstanceId(pid).singleResult();
		qpr.setRunning(pi != null && !pi.isEnded());
		LOGGER.trace("Running process instance = {}, isRunning: {}", pi, qpr.isRunning());
		LOGGER.trace("Response to be sent to midPoint: {}", qpr);

		wfTaskController.onProcessEvent(qpr, task, result);
	}

	public void notifyMidpointAboutProcessFinishedEvent(DelegateExecution execution) {
        notifyMidpointAboutProcessEvent(new ProcessFinishedEvent(execution, processInterfaceFinder));
    }

    public void notifyMidpointAboutProcessEvent(DelegateExecution execution) {
        notifyMidpointAboutProcessEvent(new ProcessEvent(execution, processInterfaceFinder));
    }

    private void notifyMidpointAboutProcessEvent(ProcessEvent event) {
		OperationResult result = new OperationResult(DOT_CLASS + "notifyMidpointAboutProcessEvent");

		String taskOid = ActivitiUtil.getTaskOid(event.getVariables());
		Task task;
		try {
			task = taskManager.getTask(taskOid, result);
		} catch (ObjectNotFoundException|SchemaException|RuntimeException e) {
			throw new SystemException("Couldn't get task " + taskOid + " from repository: " + e.getMessage(), e);
		}
		if (task.getExecutionStatus() != TaskExecutionStatus.WAITING) {
			LOGGER.trace("Asynchronous message received in a state different from WAITING (actual state: {}), ignoring it. Task = {}", task.getExecutionStatus(), task);
			return;
		}
		try {
			wfTaskController.onProcessEvent(event, task, result);
		} catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|RuntimeException e) {
			throw new SystemException("Couldn't process a process-related event: " + e.getMessage(), e);
		}
    }

    public void notifyMidpointAboutTaskEvent(DelegateTask delegateTask) {
        OperationResult result = new OperationResult(DOT_CLASS + "notifyMidpointAboutTaskEvent");

        TaskEvent taskEvent;
        if (TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            taskEvent = new TaskCreatedEvent();     // TODO distinguish created vs. assigned event
        } else if (TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            taskEvent = new TaskCompletedEvent();
        } else if (TaskListener.EVENTNAME_DELETE.equals(delegateTask.getEventName())) {
            taskEvent = new TaskDeletedEvent();
        } else {
            return;         // ignoring other events
        }

        taskEvent.setVariables(delegateTask.getVariables());
        taskEvent.setAssigneeOid(delegateTask.getAssignee());
        taskEvent.setTaskId(delegateTask.getId());
        taskEvent.setTaskName(delegateTask.getName());
        taskEvent.setProcessInstanceName((String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME));
        taskEvent.setProcessInstanceId(delegateTask.getProcessInstanceId());
        taskEvent.setCreateTime(delegateTask.getCreateTime());
        taskEvent.setDueDate(delegateTask.getDueDate());
        taskEvent.setExecutionId(delegateTask.getExecutionId());
        taskEvent.setOwner(delegateTask.getOwner());
        for (IdentityLink identityLink : delegateTask.getCandidates()) {
            if (identityLink.getUserId() != null) {
                taskEvent.getCandidateUsers().add(identityLink.getUserId());
            } else if (identityLink.getGroupId() != null) {
                taskEvent.getCandidateGroups().add(identityLink.getGroupId());
            } else {
                throw new IllegalStateException("Neither candidate user nor group id is provided in delegateTask: " + delegateTask);
            }
        }

        try {
			WorkItemType workItem = workItemProvider.taskEventToWorkItemNew(taskEvent, null, true, true, true, result);
            wfTaskController.onTaskEvent(workItem, taskEvent, result);
        } catch (Exception e) {     // todo fix the exception processing e.g. think about situation where an event cannot be audited - should we allow to proceed?
            String message = "Couldn't process an event coming from the workflow management system";
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordFatalError(message, e);
        }
    }
}
