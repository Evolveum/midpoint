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
 * @author mederly
 *
 */
public class MockLongTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(MockLongTaskHandler.class);

    private TaskManagerQuartzImpl taskManager;

	private String id;

    MockLongTaskHandler(String id, TaskManagerQuartzImpl taskManager) {
		this.id = id;
        this.taskManager = taskManager;
	}

	@Override
	public TaskRunResult run(Task task) {
		long progress = task.getProgress();
		LOGGER.info("MockLong.run starting (id = {}, progress = {})", id, progress);

		OperationResult opResult = new OperationResult(MockLongTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();

		while (task.canRun()) {
			progress++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOGGER.info("Interrupted: exiting", e);
				break;
			}
		}
		
		opResult.recordSuccess();
		
		runResult.setOperationResult(opResult);
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		
		LOGGER.info("MockLong.run stopping; progress = {}", progress);
		return runResult;
	}
	
	@Override
	public Long heartbeat(Task task) {
		return 0L;
	}

	@Override
	public void refreshStatus(Task task) {
	}
	
    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.MOCK;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }

    public TaskManagerQuartzImpl getTaskManager() {
        return taskManager;
    }

    public void setTaskManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }
}
