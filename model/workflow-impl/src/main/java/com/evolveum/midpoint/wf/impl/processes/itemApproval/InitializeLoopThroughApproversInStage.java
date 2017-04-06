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
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.*;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.*;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.APPROVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.REJECT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.SKIP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedDecisionReasonType.AUTO_COMPLETION_CONDITION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedDecisionReasonType.NO_APPROVERS_FOUND;

/**
 * @author mederly
 */
public class InitializeLoopThroughApproversInStage implements JavaDelegate {

	private static final Trace LOGGER = TraceManager.getTrace(InitializeLoopThroughApproversInStage.class);

    public void execute(DelegateExecution execution) {

		PrismContext prismContext = getPrismContext();
    	WfExpressionEvaluationHelper evaluationHelper = SpringApplicationContextHolder.getExpressionEvaluationHelper();

        LOGGER.trace("Executing the delegate; execution = {}", execution);

		OperationResult opResult = new OperationResult(InitializeLoopThroughApproversInStage.class.getName() + ".execute");
		Task opTask = getTaskManager().createTaskInstance();
        Task wfTask = ActivitiUtil.getTask(execution, opResult);
		ApprovalStageDefinitionType stageDef = ActivitiUtil.getAndVerifyCurrentStage(execution, wfTask, false, prismContext);
		int stageNumber = stageDef.getOrder();
		opTask.setChannel(wfTask.getChannel());

		ExpressionVariables expressionVariables = null;

        ApprovalLevelOutcomeType predeterminedOutcome = null;
        if (stageDef.getAutomaticallyApproved() != null) {
            try {
                expressionVariables = evaluationHelper.getDefaultVariables(execution, wfTask, opResult);
                boolean preApproved = evaluationHelper.evaluateBooleanExpression(stageDef.getAutomaticallyApproved(), expressionVariables,
						"automatic approval expression", opTask, opResult);
				LOGGER.trace("Pre-approved = {} for stage {}", preApproved, stageDef);
				if (preApproved) {
					predeterminedOutcome = APPROVE;
					recordAutoCompletionDecision(wfTask.getOid(), APPROVE, AUTO_COMPLETION_CONDITION, stageNumber, opResult);
				}
            } catch (Exception e) {     // todo
                throw new SystemException("Couldn't evaluate auto-approval expression", e);
            }
        }

        if (predeterminedOutcome == null && stageDef.getAutomaticallyCompleted() != null) {
            try {
				if (expressionVariables == null) {
					expressionVariables = evaluationHelper.getDefaultVariables(execution, wfTask, opResult);
				}
                List<ApprovalLevelOutcomeType> outcomes = evaluationHelper.evaluateExpression(stageDef.getAutomaticallyCompleted(), expressionVariables,
						"automatic completion expression", ApprovalLevelOutcomeType.class,
						SchemaConstants.APPROVAL_LEVEL_OUTCOME_TYPE_COMPLEX_TYPE, opTask, opResult);
				LOGGER.trace("Pre-completed = {} for stage {}", outcomes, stageDef);
				Set<ApprovalLevelOutcomeType> distinctOutcomes = new HashSet<>(outcomes);
				if (distinctOutcomes.size() > 1) {
					throw new IllegalStateException("Ambiguous result from 'automatically completed' expression: " + distinctOutcomes);
				} else if (!distinctOutcomes.isEmpty()) {
					predeterminedOutcome = distinctOutcomes.iterator().next();
					if (predeterminedOutcome != null && predeterminedOutcome != SKIP) {
						recordAutoCompletionDecision(wfTask.getOid(), predeterminedOutcome, AUTO_COMPLETION_CONDITION,
								stageNumber, opResult);
					}
				}
            } catch (Exception e) {     // todo
                throw new SystemException("Couldn't evaluate auto-approval expression", e);
            }
        }

        Set<ObjectReferenceType> approverRefs = new HashSet<>();

        if (predeterminedOutcome == null) {
            approverRefs.addAll(CloneUtil.cloneCollectionMembers(stageDef.getApproverRef()));

            if (!stageDef.getApproverExpression().isEmpty()) {
                try {
                	if (expressionVariables == null) {
						expressionVariables = evaluationHelper.getDefaultVariables(execution, wfTask, opResult);
					}
                    approverRefs.addAll(evaluationHelper.evaluateRefExpressions(stageDef.getApproverExpression(), expressionVariables,
							"resolving approver expression", opTask, opResult));
                } catch (ExpressionEvaluationException|ObjectNotFoundException|SchemaException|RuntimeException e) {
                    throw new SystemException("Couldn't evaluate approvers expressions", e);
                }
            }

            LOGGER.trace("Approvers at the stage {} (before potential group expansion) are: {}", stageDef, approverRefs);
            if (stageDef.getGroupExpansion() == GroupExpansionType.ON_WORK_ITEM_CREATION) {
            	approverRefs = MidpointUtil.expandGroups(approverRefs);
				LOGGER.trace("Approvers at the stage {} (after group expansion) are: {}", stageDef, approverRefs);
			}

            if (approverRefs.isEmpty()) {
                LOGGER.debug("No approvers at the stage '{}' for process {} (id {}) - response is {}", stageDef.getName(),
						execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
						execution.getProcessInstanceId(), stageDef.getOutcomeIfNoApprovers());
                predeterminedOutcome = stageDef.getOutcomeIfNoApprovers();
                switch (predeterminedOutcome) {
					case APPROVE:
						recordAutoCompletionDecision(wfTask.getOid(), APPROVE, NO_APPROVERS_FOUND, stageNumber, opResult);
						break;
					case REJECT:
						recordAutoCompletionDecision(wfTask.getOid(), REJECT, NO_APPROVERS_FOUND, stageNumber, opResult);
						break;
					case SKIP:
						// do nothing, just silently skip the stage
						break;
					default:
						throw new IllegalStateException("Unexpected outcome: " + stageDef.getOutcomeIfNoApprovers() + " in " + stageDef);
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
        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_NAME, stageDef.getName());
        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_DISPLAY_NAME, stageDef.getDisplayName());
        execution.setVariableLocal(ProcessVariableNames.APPROVERS_IN_STAGE, ActivitiUtil.toLightweightReferences(approverRefs));
        execution.setVariableLocal(ProcessVariableNames.LOOP_APPROVERS_IN_STAGE_STOP, stop);

        if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Approval process instance {} (id {}), stage {}: predetermined outcome: {}, approvers: {}",
					execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
					execution.getProcessInstanceId(),
					WfContextUtil.getStageDiagName(stageDef), predeterminedOutcome, approverRefs);
		}
		getActivitiInterface().notifyMidpointAboutProcessEvent(execution);		// store stage information in midPoint task
    }

	private void recordAutoCompletionDecision(String taskOid, ApprovalLevelOutcomeType outcome,
			AutomatedDecisionReasonType reason, int stageNumber, OperationResult opResult) {
    	StageCompletionEventType event = new StageCompletionEventType();
		event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
		event.setStageNumber(stageNumber);
		//event.setStageName(level.getName());
		//event.setStageDisplayName(level.getDisplayName());
		event.setAutomatedDecisionReason(reason);
		event.setOutcome(ApprovalUtils.toUri(outcome));
		MidpointUtil.recordEventInTask(event, null, taskOid, opResult);
	}
}