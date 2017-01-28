/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.*;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.*;

/**
 * @author mederly
 */
public class InitializeLoopThroughApproversInLevel implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(InitializeLoopThroughApproversInLevel.class);

    public void execute(DelegateExecution execution) {

		PrismContext prismContext = getPrismContext();
    	WfExpressionEvaluationHelper evaluator = SpringApplicationContextHolder.getExpressionEvaluationHelper();

        LOGGER.trace("Executing the delegate; execution = {}", execution);

        OperationResult result = new OperationResult("dummy");
        Task wfTask = ActivitiUtil.getTask(execution, result);
		Task opTask = getTaskManager().createTaskInstance();

        ExpressionVariables expressionVariables = null;

        ApprovalLevelImpl level = ActivitiUtil.getRequiredVariable(execution, ProcessVariableNames.LEVEL, ApprovalLevelImpl.class,
				null);
		level.setPrismContext(prismContext);

		int levelIndex = ActivitiUtil.getRequiredVariable(execution, ProcessVariableNames.LEVEL_INDEX, Integer.class, null);
		int stageNumber = levelIndex+1;

		List<Decision> decisionList = new ArrayList<>();
        ApprovalLevelOutcomeType predeterminedOutcome = null;

        if (level.getAutomaticallyApproved() != null) {
            try {
                opTask.setChannel(wfTask.getChannel());
                expressionVariables = evaluator.getDefaultVariables(execution, wfTask, result);
                boolean preApproved = evaluator.evaluateBooleanExpression(level.getAutomaticallyApproved(), expressionVariables, execution, "automatic approval expression", opTask, result);
				LOGGER.trace("Pre-approved = {} for level {}", preApproved, level);
				if (preApproved) {
					predeterminedOutcome = ApprovalLevelOutcomeType.APPROVE;
					recordAutoApprovalDecision(wfTask, true, "Approved automatically by the auto-approval condition.", stageNumber, level, prismContext);
				}
            } catch (Exception e) {     // todo
                throw new SystemException("Couldn't evaluate auto-approval expression", e);
            }
        }

        Set<LightweightObjectRef> approverRefs = new HashSet<>();

        if (predeterminedOutcome == null) {
            approverRefs.addAll(level.getApproverRefs());

            if (!level.getApproverExpressions().isEmpty()) {
                try {
                	if (expressionVariables == null) {
						expressionVariables = evaluator.getDefaultVariables(execution, wfTask, result);
					}
                    approverRefs.addAll(evaluator.evaluateRefExpressions(level.getApproverExpressions(), expressionVariables,
							execution, "resolving approver expression", opTask, result));
                } catch (Exception e) {     // todo
                    throw new SystemException("Couldn't evaluate approvers expressions", e);
                }
            }

            LOGGER.trace("Approvers at the level {} (before potential group expansion) are: {}", level, approverRefs);
            if (level.getGroupExpansion() == GroupExpansionType.ON_WORK_ITEM_CREATION) {
            	approverRefs = MidpointUtil.expandGroups(approverRefs);
				LOGGER.trace("Approvers at the level {} (after group expansion) are: {}", level, approverRefs);
			}

            if (approverRefs.isEmpty()) {
                LOGGER.debug("No approvers at the level '{}' for process {} (id {}) - response is {}", level.getName(),
						execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
						execution.getProcessInstanceId(), level.getOutcomeIfNoApprovers());
                predeterminedOutcome = level.getOutcomeIfNoApprovers();
                if (predeterminedOutcome == ApprovalLevelOutcomeType.APPROVE) {
					recordAutoApprovalDecision(wfTask, true,
							"Approved automatically because there were no approvers found.", stageNumber, level,
							prismContext);
				} else {
					recordAutoApprovalDecision(wfTask, false,
							"Rejected automatically because there were no approvers found.", stageNumber, level,
							prismContext);
				}
            }
        }

        Boolean stop;
        if (predeterminedOutcome != null) {
			execution.setVariableLocal(ProcessVariableNames.PREDETERMINED_LEVEL_OUTCOME, predeterminedOutcome);
            stop = Boolean.TRUE;
        } else {
            stop = Boolean.FALSE;
        }

        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_NUMBER, stageNumber);
        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_NAME, level.getName());
        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_DISPLAY_NAME, level.getDisplayName());
        execution.setVariableLocal(ProcessVariableNames.DECISIONS_IN_LEVEL, decisionList);
        execution.setVariableLocal(ProcessVariableNames.APPROVERS_IN_LEVEL, new ArrayList<>(approverRefs));
        execution.setVariableLocal(ProcessVariableNames.LOOP_APPROVERS_IN_LEVEL_STOP, stop);

        if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Approval process instance {} (id {}), level {}: predetermined outcome: {}, approvers: {}",
					execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
					execution.getProcessInstanceId(),
					level.getDebugName(), predeterminedOutcome, LightweightObjectRef.toDebugNames(approverRefs));
		}
		getActivitiInterface().notifyMidpointAboutProcessEvent(execution);		// store stage information in midPoint task
    }

	private void recordAutoApprovalDecision(Task wfTask, boolean approved, String comment, int stageNumber, ApprovalLevel level,
			PrismContext prismContext) {
		Decision decision = new Decision();
		decision.setApproved(approved);
		decision.setComment(comment);
		decision.setDate(new Date());
		decision.setStageNumber(stageNumber);
		decision.setStageName(level.getName());
		decision.setStageDisplayName(level.getDisplayName());
		MidpointUtil.recordDecisionInTask(decision, wfTask.getOid(), prismContext);
	}



}