/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Pavol Mederly
 *
 */
public class MockParallelTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(MockParallelTaskHandler.class);
    public static final int NUM_SUBTASKS = 5;
	public static String NS_EXT = "http://myself.me/schemas/whatever";
	public static QName DURATION_QNAME = new QName(NS_EXT, "duration", "m");
	private final PrismPropertyDefinition durationDefinition;

	private TaskManagerQuartzImpl taskManager;

	// a bit of hack - to reach in-memory version of last task executed
	private Task lastTaskExecuted;

	private String id;

    MockParallelTaskHandler(String id, TaskManagerQuartzImpl taskManager) {
		this.id = id;
        this.taskManager = taskManager;
		durationDefinition = taskManager.getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(DURATION_QNAME);
		Validate.notNull(durationDefinition, "durationDefinition property is unknown");
	}

	private boolean hasRun = false;

	public class MyLightweightTaskHandler implements LightweightTaskHandler {
		private boolean hasRun = false;
		private boolean hasExited = false;
		private Integer duration;

		public MyLightweightTaskHandler(Integer duration) {
			this.duration = duration;
		}

		@Override
		public void run(Task task) {
			LOGGER.trace("Handler for task {} running", task);
			hasRun = true;
			try {
				if (duration == null) {
					for (; ; ) {
						Thread.sleep(3600000L);
					}
				} else {
					Thread.sleep(duration);
				}
			} catch (InterruptedException e) {
				LOGGER.trace("Handler for tash {} interrupted", task);
			} finally {
				hasExited = true;
			}
		}

		public boolean hasRun() {
			return hasRun;
		}
		public boolean hasExited() {
			return hasExited;
		}
	}

	@Override
	public TaskRunResult run(Task task) {
		LOGGER.info("MockParallelTaskHandler.run starting (id = " + id + ")");

		OperationResult opResult = new OperationResult(MockParallelTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();

		PrismProperty<Integer> duration = task.getExtensionProperty(DURATION_QNAME);
		Integer durationValue = null;
		if (duration != null) {
			durationValue = duration.getRealValue();
		}
		LOGGER.info("Duration value = {}", durationValue);

        // we create and start some subtasks
        for (int i = 0; i < NUM_SUBTASKS; i++) {
			MyLightweightTaskHandler handler = new MyLightweightTaskHandler(durationValue);
            TaskQuartzImpl subtask = (TaskQuartzImpl) task.createSubtask(handler);
			assertTrue("Subtask is not transient", subtask.isTransient());
			assertTrue("Subtask is not asynchronous", subtask.isAsynchronous());
			assertTrue("Subtask is not a LAT", subtask.isLightweightAsynchronousTask());
			assertEquals("Subtask has a wrong lightweight handler", handler, subtask.getLightweightTaskHandler());
			assertTrue("Subtask is not in LAT list of parent", task.getLightweightAsynchronousSubtasks().contains(subtask));
			assertFalse("Subtask is in Running LAT list of parent", task.getRunningLightweightAsynchronousSubtasks().contains(subtask));
			assertFalse("Subtask is marked as already started", subtask.lightweightHandlerStartRequested());

			subtask.startLightweightHandler();
			assertTrue("Subtask is not in Running LAT list of parent", task.getRunningLightweightAsynchronousSubtasks().contains(subtask));
			assertTrue("Subtask is not marked as already started", subtask.lightweightHandlerStartRequested());
        }

		opResult.recordSuccess();

		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(task.getProgress()+1);
        runResult.setOperationResult(opResult);

		hasRun = true;
		lastTaskExecuted = task;

		LOGGER.info("MockParallelTaskHandler.run stopping");
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		return 0L;
	}
	@Override
	public void refreshStatus(Task task) {
	}

	public boolean hasRun() {
		return hasRun;
	}

	public void resetHasRun() {
		hasRun = false;
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.MOCK;
    }

    public TaskManagerQuartzImpl getTaskManager() {
        return taskManager;
    }

    public void setTaskManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

	public Task getLastTaskExecuted() {
		return lastTaskExecuted;
	}
}
