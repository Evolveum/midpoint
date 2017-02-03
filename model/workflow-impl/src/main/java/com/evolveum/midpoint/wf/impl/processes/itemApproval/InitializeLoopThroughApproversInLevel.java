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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedDecisionReasonType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GroupExpansionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfStageCompletionEventType;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.APPROVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.REJECT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedDecisionReasonType.AUTO_APPROVAL_CONDITION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedDecisionReasonType.NO_APPROVERS_FOUND;

/**
 * @author mederly
 */
public class InitializeLoopThroughApproversInLevel implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(InitializeLoopThroughApproversInLevel.class);

    public void execute(DelegateExecution execution) {

		PrismContext prismContext = getPrismContext();
    	WfExpressionEvaluationHelper evaluator = SpringApplicationContextHolder.getExpressionEvaluationHelper();

        LOGGER.trace("Executing the delegate; execution = {}", execution);

        OperationResult opResult = new OperationResult(InitializeLoopThroughApproversInLevel.class.getName() + ".execute");
        Task wfTask = ActivitiUtil.getTask(execution, opResult);
		Task opTask = getTaskManager().createTaskInstance();

        ExpressionVariables expressionVariables = null;

        ApprovalLevelImpl level = ActivitiUtil.getRequiredVariable(execution, ProcessVariableNames.LEVEL, ApprovalLevelImpl.class,
				null);
		level.setPrismContext(prismContext);

		int levelIndex = ActivitiUtil.getRequiredVariable(execution, ProcessVariableNames.LEVEL_INDEX, Integer.class, null);
		int stageNumber = levelIndex+1;

        ApprovalLevelOutcomeType predeterminedOutcome = null;
        if (level.getAutomaticallyApproved() != null) {
            try {
                opTask.setChannel(wfTask.getChannel());
                expressionVariables = evaluator.getDefaultVariables(execution, wfTask, opResult);
                boolean preApproved = evaluator.evaluateBooleanExpression(level.getAutomaticallyApproved(), expressionVariables, execution, "automatic approval expression", opTask, opResult);
				LOGGER.trace("Pre-approved = {} for level {}", preApproved, level);
				if (preApproved) {
					predeterminedOutcome = APPROVE;
					recordAutoApprovalDecision(wfTask.getOid(), APPROVE, AUTO_APPROVAL_CONDITION, stageNumber, level, opResult);
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
						expressionVariables = evaluator.getDefaultVariables(execution, wfTask, opResult);
					}
                    approverRefs.addAll(evaluator.evaluateRefExpressions(level.getApproverExpressions(), expressionVariables,
							execution, "resolving approver expression", opTask, opResult));
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
                switch (predeterminedOutcome) {
					case APPROVE:
						recordAutoApprovalDecision(wfTask.getOid(), APPROVE, NO_APPROVERS_FOUND, stageNumber, level, opResult);
						break;
					case REJECT:
						recordAutoApprovalDecision(wfTask.getOid(), REJECT, NO_APPROVERS_FOUND, stageNumber, level, opResult);
						break;
					case SKIP:
						// do nothing, just silently skip the level
						break;
					default:
						throw new IllegalStateException("Unexpected outcome: " + level.getOutcomeIfNoApprovers() + " in " + level);
				}
            }
        }

        Boolean stop;
        if (predeterminedOutcome != null) {
            stop = Boolean.TRUE;
        } else {
            stop = Boolean.FALSE;
        }

        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_NUMBER, stageNumber);
        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_NAME, level.getName());
        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_DISPLAY_NAME, level.getDisplayName());
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

	private void recordAutoApprovalDecision(String taskOid, ApprovalLevelOutcomeType outcome, AutomatedDecisionReasonType reason,
			int stageNumber, ApprovalLevel level, OperationResult opResult) {
    	WfStageCompletionEventType event = new WfStageCompletionEventType();
		event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
		event.setStageNumber(stageNumber);
		event.setStageName(level.getName());
		event.setStageDisplayName(level.getDisplayName());
		event.setAutomatedDecisionReason(reason);
		event.setOutcome(outcome);
		MidpointUtil.recordEventInTask(event, null, taskOid, opResult);
	}
}