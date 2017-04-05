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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.MidPointTaskListener;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getActivitiInterface;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getItemApprovalProcessInterface;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;
import static com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames.LOOP_APPROVERS_IN_LEVEL_STOP;

/**
 * @author mederly
 */
public class TaskCompleteListener implements TaskListener {

    private static final Trace LOGGER = TraceManager.getTrace(TaskCompleteListener.class);
	private static final long serialVersionUID = 1L;

	@Override
	public void notify(DelegateTask delegateTask) {

		DelegateExecution execution = delegateTask.getExecution();
		PrismContext prismContext = getPrismContext();
		OperationResult opResult = new OperationResult(TaskCompleteListener.class.getName() + ".notify");
		Task wfTask = ActivitiUtil.getTask(execution, opResult);
		ApprovalLevelType level = ActivitiUtil.getAndVerifyCurrentStage(execution, wfTask, true, prismContext);

		delegateTask.setVariableLocal(CommonProcessVariableNames.VARIABLE_WORK_ITEM_WAS_COMPLETED, Boolean.TRUE);

		//		System.out.println("%%% Task " + delegateTask + " has been completed.");
//		LOGGER.info("%%% Task {} has been completed", delegateTask);

		MidPointPrincipal user;
		try {
			user = SecurityUtil.getPrincipal();
		} catch (SecurityViolationException e) {
			throw new SystemException("Couldn't record a decision: " + e.getMessage(), e);
		}

		if (user != null && user.getOid() != null) {
			delegateTask.setVariableLocal(CommonProcessVariableNames.VARIABLE_WORK_ITEM_COMPLETED_BY, user.getOid());
		}

		LOGGER.trace("======================================== Recording individual decision of {}", user);

		@NotNull WorkItemResultType result1 = getItemApprovalProcessInterface().extractWorkItemResult(delegateTask.getVariables());
		boolean isApproved = ApprovalUtils.isApproved(result1);

        LevelEvaluationStrategyType levelEvaluationStrategyType = level.getEvaluationStrategy();
        Boolean setLoopApprovesInLevelStop = null;
        if (levelEvaluationStrategyType == LevelEvaluationStrategyType.FIRST_DECIDES) {
			LOGGER.trace("Setting " + LOOP_APPROVERS_IN_LEVEL_STOP + " to true, because the level evaluation strategy is 'firstDecides'.");
            setLoopApprovesInLevelStop = true;
        } else if ((levelEvaluationStrategyType == null || levelEvaluationStrategyType == LevelEvaluationStrategyType.ALL_MUST_AGREE) && !isApproved) {
			LOGGER.trace("Setting " + LOOP_APPROVERS_IN_LEVEL_STOP + " to true, because the level eval strategy is 'allMustApprove' and the decision was 'reject'.");
            setLoopApprovesInLevelStop = true;
        }

        if (setLoopApprovesInLevelStop != null) {
			//noinspection ConstantConditions
			execution.setVariable(LOOP_APPROVERS_IN_LEVEL_STOP, setLoopApprovesInLevelStop);
        }
        // consider removing this
        execution.setVariable(
                CommonProcessVariableNames.VARIABLE_WF_STATE, "User " + (user!=null?user.getName():null) + " decided to " + (isApproved ? "approve" : "reject") + " the request.");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Approval process instance {} (id {}), level {}: recording decision {}; level stops now: {}",
					execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
                    execution.getProcessInstanceId(),
					WfContextUtil.getLevelDiagName(level), result1.getOutcome(), setLoopApprovesInLevelStop);
        }

		getActivitiInterface().notifyMidpointAboutTaskEvent(delegateTask);
		getActivitiInterface().notifyMidpointAboutProcessEvent(execution);
    }

}
