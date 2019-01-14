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
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;

/**
 *
 */
public class EngineInvocationContext implements DebugDumpable {

	public WfContextType wfContext;
	public Task wfTask;
	public CaseType wfCase;
	public Task opTask;

	public EngineInvocationContext(WfContextType wfContext, Task wfTask,
			Task opTask) {
		this.wfContext = wfContext;
		this.wfTask = wfTask;
		this.opTask = opTask;
	}

	public WfContextType getWfContext() {
		return wfContext;
	}

	public void setWfContext(WfContextType wfContext) {
		this.wfContext = wfContext;
	}

	public Task getWfTask() {
		return wfTask;
	}

	public void setWfTask(Task wfTask) {
		this.wfTask = wfTask;
	}

	public CaseType getWfCase() {
		return wfCase;
	}

	public void setWfCase(CaseType wfCase) {
		this.wfCase = wfCase;
	}

	public Task getOpTask() {
		return opTask;
	}

	public void setOpTask(Task opTask) {
		this.opTask = opTask;
	}

	@Override
	public String debugDump(int indent) {
		return wfContext.asPrismContainerValue().debugDump(indent);     // TODO
	}

	public String getChannel() {
		return opTask.getChannel();
	}

	@Override
	public String toString() {
		return "EngineInvocationContext{" +
				"wfTask=" + wfTask +
				", wfCase=" + wfCase +
				'}';
	}

	public String getCaseOid() {
		return wfCase.getOid();
	}
}
