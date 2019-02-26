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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.wf.impl.processes.common.WfStageComputeHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class EngineInvocationContext implements DebugDumpable {

	@NotNull public CaseType aCase;
	@NotNull public final Task opTask;
	private boolean done;

	public EngineInvocationContext(@NotNull CaseType aCase, @NotNull Task opTask) {
		this.aCase = aCase;
		this.opTask = opTask;
	}

	public WfContextType getWfContext() {
		return aCase.getWorkflowContext();
	}

	@NotNull
	public CaseType getCase() {
		return aCase;
	}

	@NotNull
	public Task getOpTask() {
		return opTask;
	}

	@Override
	public String debugDump(int indent) {
		return aCase.getWorkflowContext().asPrismContainerValue().debugDump(indent);     // TODO
	}

	public String getChannel() {
		return opTask.getChannel();
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	@Override
	public String toString() {
		return "EngineInvocationContext{" +
				"case=" + aCase +
				", done=" + done +
				'}';
	}

	public String getCaseOid() {
		return aCase.getOid();
	}

	@NotNull
	public CaseWorkItemType findWorkItemById(long id) {
		//noinspection unchecked
		PrismContainerValue<CaseWorkItemType> workItemPcv = (PrismContainerValue<CaseWorkItemType>)
				aCase.asPrismContainerValue().find(ItemPath.create(CaseType.F_WORK_ITEM, id));
		if (workItemPcv == null) {
			throw new IllegalStateException("No work item " + id + " in " + this);
		} else {
			return workItemPcv.asContainerable();
		}
	}

	private WfStageComputeHelper.ComputationResult preStageComputationResult;
	private String currentStageOutcome;

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

	public String getProcessInstanceName() {
		return aCase.getName().getOrig();
	}
}
