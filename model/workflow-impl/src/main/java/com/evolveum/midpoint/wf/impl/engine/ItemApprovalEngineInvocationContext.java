/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.engine.processes.ProcessOrchestrator;
import com.evolveum.midpoint.wf.impl.processes.common.WfStageComputeHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;

/**
 * Engine invocation context specific to ItemApproval process.
 */
public class ItemApprovalEngineInvocationContext extends EngineInvocationContext {

	private WfStageComputeHelper.ComputationResult preStageComputationResult;
	private String currentStageOutcome;

	public ItemApprovalEngineInvocationContext(WfContextType wfContext,
			Task wfTask,
			Task opTask) {
		super(wfContext, wfTask, opTask);
	}

	public String getCurrentStageOutcome() {
		return currentStageOutcome;
	}

	public void setCurrentStageOutcome(String currentStageOutcome) {
		this.currentStageOutcome = currentStageOutcome;
	}

	public WfStageComputeHelper.ComputationResult getPreStageComputationResult() {
		return preStageComputationResult;
	}

	public void setPreStageComputationResult(
			WfStageComputeHelper.ComputationResult preStageComputationResult) {
		this.preStageComputationResult = preStageComputationResult;
	}
}
