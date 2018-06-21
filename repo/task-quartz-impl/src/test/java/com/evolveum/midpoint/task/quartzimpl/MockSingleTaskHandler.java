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
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;

import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Radovan Semancik
 *
 */
public class MockSingleTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(MockSingleTaskHandler.class);
    private String MOCK_HANDLER_URI = "http://midpoint.evolveum.com/test/mock";
    private String NS_EXT = "http://myself.me/schemas/whatever";
    private QName L1_FLAG_QNAME = new QName(NS_EXT, "l1Flag", "m");
    private QName WFS_FLAG_QNAME = new QName(NS_EXT, "wfsFlag", "m");

    private TaskManagerQuartzImpl taskManager;

	private String id;

    private PrismPropertyDefinition l1FlagDefinition;
    private PrismPropertyDefinition wfsFlagDefinition;


    MockSingleTaskHandler(String id, TaskManagerQuartzImpl taskManager) {
		this.id = id;
        this.taskManager = taskManager;

        l1FlagDefinition = taskManager.getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(L1_FLAG_QNAME);
        Validate.notNull(l1FlagDefinition, "l1Flag property is unknown");
        wfsFlagDefinition = taskManager.getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(WFS_FLAG_QNAME);
        Validate.notNull(wfsFlagDefinition, "wfsFlag property is unknown");
	}

	private boolean hasRun = false;
    private int executions = 0;

	@Override
	public TaskRunResult run(Task task) {
		LOGGER.info("MockSingle.run starting (id = " + id + ")");

		OperationResult opResult = new OperationResult(MockSingleTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();

		runResult.setOperationResult(opResult);

		// TODO
		task.incrementProgressAndStoreStatsIfNeeded();

		opResult.recordSuccess();

		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

		hasRun = true;
		executions++;

        if ("L1".equals(id)) {
            PrismProperty<Boolean> l1flag = task.getExtensionProperty(L1_FLAG_QNAME);

            if (l1flag == null || !l1flag.getRealValue()) {

                LOGGER.info("L1 handler, first run - scheduling L2 handler");
                ScheduleType l2Schedule = new ScheduleType();
                l2Schedule.setInterval(2);
                task.pushHandlerUri(AbstractTaskManagerTest.L2_TASK_HANDLER_URI, l2Schedule, TaskBinding.TIGHT, ((TaskQuartzImpl) task).createExtensionDelta(l1FlagDefinition, true));
                try {
                    task.savePendingModifications(opResult);
                } catch(Exception e) {
                    throw new SystemException("Cannot schedule L2 handler", e);
                }
                runResult.setRunResultStatus(TaskRunResultStatus.RESTART_REQUESTED);
            } else {
                LOGGER.info("L1 handler, not the first run (progress = " + task.getProgress() + ", l1Flag = " + l1flag.getRealValue() + "), exiting.");
            }
        } else if ("L2".equals(id)) {
            if (task.getProgress() == 5) {
                LOGGER.info("L2 handler, fourth run - scheduling L3 handler");
                task.pushHandlerUri(AbstractTaskManagerTest.L3_TASK_HANDLER_URI, new ScheduleType(), null);
                try {
                    task.savePendingModifications(opResult);
                } catch(Exception e) {
                    throw new SystemException("Cannot schedule L3 handler", e);
                }
                runResult.setRunResultStatus(TaskRunResultStatus.RESTART_REQUESTED);
            } else if (task.getProgress() < 5) {
                LOGGER.info("L2 handler, progress = " + task.getProgress() + ", continuing.");
            } else if (task.getProgress() > 5) {
                LOGGER.info("L2 handler, progress too big, i.e. " + task.getProgress() + ", exiting.");
                try {
                    task.finishHandler(opResult);
                } catch (Exception e) {
                    throw new SystemException("Cannot finish L2 handler", e);
                }
            }
        } else if ("L3".equals(id)) {
            LOGGER.info("L3 handler, simply exiting. Progress = " + task.getProgress());
        } else if ("WFS".equals(id)) {

            PrismProperty<Boolean> wfsFlag = task.getExtensionProperty(WFS_FLAG_QNAME);

            if (wfsFlag == null || !wfsFlag.getRealValue()) {

                LOGGER.info("Wait-for-subtasks creating subtasks...");

                Task t1 = task.createSubtask();
                t1.setHandlerUri(AbstractTaskManagerTest.L3_TASK_HANDLER_URI);
                taskManager.switchToBackground(t1, opResult);

                Task t2 = task.createSubtask();
                t2.setHandlerUri(AbstractTaskManagerTest.SINGLE_TASK_HANDLER_URI);
                taskManager.switchToBackground(t2, opResult);

                try {
                    ArrayList<ItemDelta<?,?>> deltas = new ArrayList<>();
                    deltas.add(((TaskQuartzImpl) task).createExtensionDelta(wfsFlagDefinition, true));
                    runResult = ((TaskQuartzImpl) task).waitForSubtasks(2, deltas, opResult);
                    runResult.setProgress(1L);
                } catch (Exception e) {
                    throw new SystemException("WaitForSubtasks failed.", e);
                }
            } else {
                LOGGER.info("Wait-for-subtasks seems to finish successfully; progress = " + task.getProgress() + ", wfsFlag = " + wfsFlag.getRealValue());
            }

        }

		LOGGER.info("MockSingle.run stopping");
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		// TODO Auto-generated method stub
		return 0L;
	}
	@Override
	public void refreshStatus(Task task) {
		// TODO Auto-generated method stub

	}

	public boolean hasRun() {
		return hasRun;
	}

	public void resetHasRun() {
		hasRun = false;
	}

	public int getExecutions() {
		return executions;
	}

	public void resetExecutions() {
		executions = 0;
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
}
