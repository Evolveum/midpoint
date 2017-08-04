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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract from ApprovalSchemaExecutionInformationType that could be directly displayed via the GUI as "approval process preview"
 * (either for the whole process or only the future stages).
 *
 * @author mederly
 */
public class ApprovalProcessExecutionInformationDto implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String F_PROCESS_NAME = "processName";
	public static final String F_TARGET_NAME = "targetName";
	public static final String F_STAGES = "stages";

	private final boolean wholeProcess;                                   // do we represent whole process or only the future stages?
	private final int currentStageNumber;                                 // current stage number (0 if there's no current stage, i.e. process has not started yet)
	private final int numberOfStages;
	private final String processName;
	private final String targetName;
	private final List<ApprovalStageExecutionInformationDto> stages = new ArrayList<>();
	private final boolean running;

	private ApprovalProcessExecutionInformationDto(boolean wholeProcess, int currentStageNumber, int numberOfStages,
			String processName, String targetName, boolean running) {
		this.wholeProcess = wholeProcess;
		this.currentStageNumber = currentStageNumber;
		this.numberOfStages = numberOfStages;
		this.processName = processName;
		this.targetName = targetName;
		this.running = running;
	}

	@NotNull
	public static ApprovalProcessExecutionInformationDto createFrom(ApprovalSchemaExecutionInformationType info,
			ObjectResolver resolver, boolean wholeProcess, Task opTask,
			OperationResult result) {
		int currentStageNumber = info.getCurrentStageNumber() != null ? info.getCurrentStageNumber() : 0;
		int numberOfStages = info.getStage().size();
		ObjectResolver.Session session = resolver.openResolutionSession(null);
		String processName = WfContextUtil.getProcessName(info);
		String targetName = WfContextUtil.getTargetName(info);
		WfContextType wfc = WfContextUtil.getWorkflowContext(info);
		boolean running = wfc != null && wfc.getEndTimestamp() == null;
		ApprovalProcessExecutionInformationDto rv =
				new ApprovalProcessExecutionInformationDto(wholeProcess, currentStageNumber, numberOfStages, processName, targetName, running);
		int startingStageNumber = wholeProcess ? 1 : currentStageNumber+1;
		boolean reachable = true;
		for (int i = startingStageNumber - 1; i < numberOfStages; i++) {
			ApprovalStageExecutionInformationDto stage = ApprovalStageExecutionInformationDto.createFrom(info, i, resolver, session, opTask, result);
			stage.setReachable(reachable);
			rv.stages.add(stage);
			if (stage.getOutcome() == ApprovalLevelOutcomeType.REJECT) {
				reachable = false;      // for following stages
			}
		}
		return rv;
	}

	public boolean isWholeProcess() {
		return wholeProcess;
	}

	public int getCurrentStageNumber() {
		return currentStageNumber;
	}

	public int getNumberOfStages() {
		return numberOfStages;
	}

	public List<ApprovalStageExecutionInformationDto> getStages() {
		return stages;
	}

	public String getProcessName() {
		return processName;
	}

	public String getTargetName() {
		return targetName;
	}

	public boolean isRunning() {
		return running;
	}
}
