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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.*;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.*;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.*;

/**
 * @author mederly
 */
public class InitializeLoopThroughApproversInStage implements JavaDelegate {

	private static final Trace LOGGER = TraceManager.getTrace(InitializeLoopThroughApproversInStage.class);

    @Override
    public void execute(DelegateExecution execution) {

		PrismContext prismContext = getPrismContext();
    	WfStageComputeHelper stageComputeHelper = SpringApplicationContextHolder.getStageComputeHelper();

        LOGGER.trace("Executing the delegate; execution = {}", execution);

		OperationResult opResult = new OperationResult(InitializeLoopThroughApproversInStage.class.getName() + ".execute");
		Task opTask = getTaskManager().createTaskInstance();
        Task wfTask = ActivitiUtil.getTask(execution, opResult);
		ApprovalStageDefinitionType stageDef = ActivitiUtil.getAndVerifyCurrentStage(execution, wfTask, false, prismContext);
		int stageNumber = stageDef.getNumber();
		opTask.setChannel(wfTask.getChannel());         // TODO !!!!!!!!!!

	    WfStageComputeHelper.ComputationResult computationResult =
			    stageComputeHelper.computeStageApprovers(stageDef,
					    () -> stageComputeHelper.getDefaultVariables(execution, wfTask, opResult), opTask, opResult);
	    ApprovalLevelOutcomeType predeterminedOutcome = computationResult.getPredeterminedOutcome();
	    AutomatedCompletionReasonType automatedCompletionReason = computationResult.getAutomatedCompletionReason();
	    Set<ObjectReferenceType> approverRefs = computationResult.getApproverRefs();

	    if (predeterminedOutcome != null) {
		    recordAutoCompletionDecision(wfTask.getOid(), predeterminedOutcome, automatedCompletionReason, stageNumber, opResult);
	    }

	    Boolean stop = predeterminedOutcome != null;

	    execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_NUMBER, stageNumber);
        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_NAME, stageDef.getName());
        execution.setVariable(CommonProcessVariableNames.VARIABLE_STAGE_DISPLAY_NAME, stageDef.getDisplayName());
        execution.setVariableLocal(ProcessVariableNames.APPROVERS_IN_STAGE, ActivitiUtil.toLightweightReferences(approverRefs));
        execution.setVariableLocal(ProcessVariableNames.LOOP_APPROVERS_IN_STAGE_STOP, stop);

        if (LOGGER.isDebugEnabled()) {
        	if (computationResult.noApproversFound()) {
		        LOGGER.debug("No approvers at the stage '{}' for process {} (id {}) - response is {}", stageDef.getName(),
				        execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
				        execution.getProcessInstanceId(), stageDef.getOutcomeIfNoApprovers());
	        }
	        LOGGER.debug("Approval process instance {} (id {}), stage {}: predetermined outcome: {}, approvers: {}",
					execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
					execution.getProcessInstanceId(),
					WfContextUtil.getStageDiagName(stageDef), predeterminedOutcome, approverRefs);
		}
		getActivitiInterface().notifyMidpointAboutProcessEvent(execution);		// store stage information in midPoint task
    }

	private void recordAutoCompletionDecision(String taskOid, ApprovalLevelOutcomeType outcome,
			AutomatedCompletionReasonType reason, int stageNumber, OperationResult opResult) {
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
