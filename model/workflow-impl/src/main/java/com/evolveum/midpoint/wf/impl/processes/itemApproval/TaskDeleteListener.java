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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;

/**
 * @author mederly
 */
public class TaskDeleteListener implements TaskListener {

    private static final Trace LOGGER = TraceManager.getTrace(TaskDeleteListener.class);

	private static final long serialVersionUID = 1L;

	@Override
	public void notify(DelegateTask delegateTask) {

		DelegateExecution execution = delegateTask.getExecution();
		PrismContext prismContext = getPrismContext();
		OperationResult opResult = new OperationResult(TaskCompleteListener.class.getName() + ".notify");
		Task wfTask = ActivitiUtil.getTask(execution, opResult);
		//ApprovalLevelType level = ActivitiUtil.getAndVerifyCurrentStage(execution, wfTask, true, prismContext);

		MidpointUtil.removeTriggersForWorkItem(wfTask, delegateTask.getId(), opResult);

		// We could send a "task deleted" notification, if needed.
		// In order to do this, task completion listener could create "wasCompleted" task variable (so we could know which
		// tasks were completed and which simply deleted - the former ones should not get 'delete' notification twice!).
		// And we would call new MidPointTaskListener().notify(delegateTask), and amend it to send a notification for
		// deleted non-completed tasks.


//		DelegateExecution execution = delegateTask.getExecution();
//		PrismContext prismContext = getPrismContext();
//		OperationResult opResult = new OperationResult(TaskDeleteListener.class.getName() + ".notify");
//		Task wfTask = ActivitiUtil.getTask(execution, opResult);
//		//ApprovalLevelType level = ActivitiUtil.getAndVerifyCurrentStage(execution, wfTask, true, prismContext);
//
//		System.out.println("%%% Task " + delegateTask + " is being deleted.");
//		LOGGER.info("%%% Task {} is being deleted", delegateTask);
//
//		System.out.println("%%% Variables: " + delegateTask.getVariables());

    }

}
