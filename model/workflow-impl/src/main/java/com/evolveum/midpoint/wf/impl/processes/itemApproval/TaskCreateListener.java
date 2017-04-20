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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalStageDefinitionType;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getActivitiInterface;

/**
 * @author mederly
 */
public class TaskCreateListener implements TaskListener {

	@Override
	public void notify(DelegateTask delegateTask) {
		OperationResult result = new OperationResult(TaskCreateListener.class.getName() + ".notify");
		Task wfTask = ActivitiUtil.getTask(delegateTask.getExecution(), result);
		String taskId = delegateTask.getId();

		// duration/deadline
		ApprovalStageDefinitionType stageDef = WfContextUtil.getCurrentStageDefinition(wfTask.getWorkflowContext());
		if (stageDef == null) {
			throw new IllegalStateException("No approval stage information in " + delegateTask);
		}
		if (stageDef.getDuration() != null) {
			MidpointUtil.setTaskDeadline(delegateTask, stageDef.getDuration());
		}

		// triggers
		int escalationLevel = ActivitiUtil.getEscalationLevelNumber(delegateTask.getVariables());
		MidpointUtil.createTriggersForTimedActions(taskId, escalationLevel, delegateTask.getCreateTime(),
				delegateTask.getDueDate(), wfTask, stageDef.getTimedActions(), result);

		// originalAssignee
		String assignee = delegateTask.getAssignee();
		if (assignee != null) {
			TaskService taskService = delegateTask.getExecution().getEngineServices().getTaskService();
			taskService.setVariableLocal(taskId, CommonProcessVariableNames.VARIABLE_ORIGINAL_ASSIGNEE, assignee);
			taskService.addUserIdentityLink(taskId, assignee, CommonProcessVariableNames.MIDPOINT_ASSIGNEE);
		}

		getActivitiInterface().notifyMidpointAboutTaskEvent(delegateTask);
	}
}
