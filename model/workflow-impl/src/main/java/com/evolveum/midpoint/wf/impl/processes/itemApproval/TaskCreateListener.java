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
import com.evolveum.midpoint.wf.impl.processes.common.MidPointTaskListener;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelType;
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

		// duration/deadline
		ApprovalLevelType level = WfContextUtil.getCurrentApprovalLevel(wfTask.getWorkflowContext());
		if (level == null) {
			throw new IllegalStateException("No approval level information in " + delegateTask);
		}
		if (level.getDuration() != null) {
			MidpointUtil.setTaskDeadline(delegateTask, level.getDuration());
		}

		// triggers
		MidpointUtil.createTriggersForTimedActions(delegateTask, wfTask, level.getTimedActions(), result);

		// originalAssignee
		String assignee = delegateTask.getAssignee();
		if (assignee != null) {
			delegateTask.setVariableLocal(CommonProcessVariableNames.VARIABLE_ORIGINAL_ASSIGNEE, assignee);
		}

		getActivitiInterface().notifyMidpointAboutTaskEvent(delegateTask);
	}
}
