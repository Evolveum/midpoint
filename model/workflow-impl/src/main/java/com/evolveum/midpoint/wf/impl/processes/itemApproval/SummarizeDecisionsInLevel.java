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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;

/**
 * @author mederly
 */
public class SummarizeDecisionsInLevel implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(SummarizeDecisionsInLevel.class);

    public void execute(DelegateExecution execution) {

		PrismContext prismContext = getPrismContext();
		OperationResult result = new OperationResult(SummarizeDecisionsInLevel.class.getName() + ".execute");
		Task wfTask = ActivitiUtil.getTask(execution, result);
		ApprovalLevelType level = ActivitiUtil.getAndVerifyCurrentStage(execution, wfTask, true, prismContext);

		WfContextType wfc = ActivitiUtil.getWorkflowContext(wfTask);
		List<WfStageCompletionEventType> stageEvents = WfContextUtil.getEventsForCurrentStage(wfc, WfStageCompletionEventType.class);

		boolean approved;
		if (!stageEvents.isEmpty()) {
			ApprovalLevelOutcomeType outcome = WfContextUtil.getCurrentStageOutcome(wfc, stageEvents);
			switch (outcome) {
				case APPROVE: approved = true; break;
				case REJECT: approved = false; break;
				case SKIP: approved = true; break;
				default: throw new AssertionError("outcome: " + outcome);
			}
		} else {
			LOGGER.trace("****************************************** Summarizing decisions in level {} (level evaluation strategy = {}): ", level.getName(), level.getEvaluationStrategy());

			List<WorkItemCompletionEventType> itemEvents = WfContextUtil.getEventsForCurrentStage(wfc, WorkItemCompletionEventType.class);

			boolean allApproved = true;
			for (WorkItemCompletionEventType event : itemEvents) {
				LOGGER.trace(" - {}", event);
				allApproved &= ApprovalUtils.isApproved(event.getResult());
			}
			approved = allApproved;
			if (level.getEvaluationStrategy() == LevelEvaluationStrategyType.FIRST_DECIDES) {
				Set<String> outcomes = itemEvents.stream()
						.map(e -> e.getResult().getOutcome())
						.collect(Collectors.toSet());
				if (outcomes.size() > 1) {
					LOGGER.warn("Ambiguous outcome with firstDecides strategy in {}: {} response(s), providing outcomes of {}",
							WfContextUtil.getBriefDiagInfo(wfc), itemEvents.size(), outcomes);
				}
			}
		}

		//MidpointUtil.removeAllStageTriggersForWorkItem(wfTask, result);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Approval process instance {} (id {}), level {}: result of this level: {}",
					execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
					execution.getProcessInstanceId(), WfContextUtil.getLevelDiagName(level), approved);
		}
        execution.setVariable(ProcessVariableNames.LOOP_LEVELS_STOP, !approved);
    }

}
