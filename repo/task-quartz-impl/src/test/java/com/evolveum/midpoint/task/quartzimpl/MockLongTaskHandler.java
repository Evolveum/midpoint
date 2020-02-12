/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

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
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        LOGGER.info("MockLong.run starting (id = {}, progress = {})", id, task.getProgress());

        OperationResult opResult = new OperationResult(MockLongTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();

        while (task.canRun()) {
            task.incrementProgressAndStoreStatsIfNeeded();
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
        runResult.setProgress(task.getProgress());

        LOGGER.info("MockLong.run stopping; progress = {}", task.getProgress());
        return runResult;
    }

    @Override
    public Long heartbeat(Task task) {
        return null;
    }

    @Override
    public void refreshStatus(Task task) {
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
