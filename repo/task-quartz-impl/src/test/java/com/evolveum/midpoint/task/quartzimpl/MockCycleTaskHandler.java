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
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.List;

/**
 * @author Radovan Semancik
 *
 */
public class MockCycleTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(MockCycleTaskHandler.class);
    private final boolean finishTheHandler;

    public MockCycleTaskHandler(boolean finishTheHandler) {
        this.finishTheHandler = finishTheHandler;
    }

    /* (non-Javadoc)
      * @see com.evolveum.midpoint.task.api.TaskHandler#run(com.evolveum.midpoint.task.api.Task)
      */
	@Override
	public TaskRunResult run(Task task) {

		LOGGER.info("MockCycle.run starting");

		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(MockCycleTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		// TODO
		progress++;

		opResult.recordSuccess();

		// This "run" is finished. But the task goes on ... (if finishTheHandler == false)
		runResult.setRunResultStatus(finishTheHandler ? TaskRunResultStatus.FINISHED_HANDLER : TaskRunResultStatus.FINISHED);

		runResult.setProgress(progress);
		LOGGER.info("MockCycle.run stopping");
		return runResult;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskHandler#heartbeat(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	public Long heartbeat(Task task) {
		return null;		// not to overwrite progress information!
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.TaskHandler#refreshStatus(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	public void refreshStatus(Task task) {
		// TODO Auto-generated method stub

	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.MOCK;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
