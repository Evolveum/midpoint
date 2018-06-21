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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;

/**
 * @author mederly
 */
public class SummarizeDecisionsInStage implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(SummarizeDecisionsInStage.class);

    public void execute(DelegateExecution execution) {

		PrismContext prismContext = getPrismContext();
		OperationResult result = new OperationResult(SummarizeDecisionsInStage.class.getName() + ".execute");
		Task wfTask = ActivitiUtil.getTask(execution, result);
		ApprovalStageDefinitionType stageDef = ActivitiUtil.getAndVerifyCurrentStage(execution, wfTask, true, prismContext);

		WfContextType wfc = ActivitiUtil.getWorkflowContext(wfTask);
		List<StageCompletionEventType> stageEvents = WfContextUtil.getEventsForCurrentStage(wfc, StageCompletionEventType.class);

		boolean approved;
		if (!stageEvents.isEmpty()) {
			String outcome = WfContextUtil.getCurrentStageOutcome(wfc, stageEvents);
			if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE)
					|| QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP)) {
				approved = true;
			} else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT)) {
				approved = false;
			} else {
				throw new IllegalStateException("Unknown outcome: " + outcome);		// TODO less draconian handling
			}
		} else {
			LOGGER.trace("****************************************** Summarizing decisions in stage {} (stage evaluation strategy = {}): ", stageDef.getName(), stageDef.getEvaluationStrategy());

			List<WorkItemCompletionEventType> itemEvents = WfContextUtil.getEventsForCurrentStage(wfc, WorkItemCompletionEventType.class);

			boolean allApproved = true;
			for (WorkItemCompletionEventType event : itemEvents) {
				LOGGER.trace(" - {}", event);
				allApproved &= ApprovalUtils.isApproved(event.getOutput());
			}
			approved = allApproved;
			if (stageDef.getEvaluationStrategy() == LevelEvaluationStrategyType.FIRST_DECIDES) {
				Set<String> outcomes = itemEvents.stream()
						.map(e -> e.getOutput().getOutcome())
						.collect(Collectors.toSet());
				if (outcomes.size() > 1) {
					LOGGER.warn("Ambiguous outcome with firstDecides strategy in {}: {} response(s), providing outcomes of {}",
							WfContextUtil.getBriefDiagInfo(wfc), itemEvents.size(), outcomes);
					itemEvents.sort(Comparator.nullsLast(Comparator.comparing(event -> XmlTypeConverter.toMillis(event.getTimestamp()))));
					WorkItemCompletionEventType first = itemEvents.get(0);
					approved = ApprovalUtils.isApproved(first.getOutput());
					LOGGER.warn("Possible race condition, so taking the first one: {} ({})", approved, first);
				}
			}
		}

		//MidpointUtil.removeAllStageTriggersForWorkItem(wfTask, result);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Approval process instance {} (id {}), stage {}: result of this stage: {}",
					execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
					execution.getProcessInstanceId(), WfContextUtil.getStageDiagName(stageDef), approved);
		}
        execution.setVariable(ProcessVariableNames.LOOP_STAGES_STOP, !approved);
    }

}
