/*
 * Copyright (c) 2010-2015 Evolveum and contributors
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * @author Radovan Semancik
 */
public class MockSingleTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(MockSingleTaskHandler.class);
    private static final String NS_EXT = "http://myself.me/schemas/whatever";

    private TaskManagerQuartzImpl taskManager;

    private final String id;

    MockSingleTaskHandler(String id, TaskManagerQuartzImpl taskManager) {
        this.id = id;
        this.taskManager = taskManager;
    }

    private boolean hasRun = false;
    private int executions = 0;

    private long delay;

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        LOGGER.info("MockSingle.run starting (id = " + id + ")");

        OperationResult opResult = new OperationResult(MockSingleTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();

        runResult.setOperationResult(opResult);

        // TODO
        task.incrementProgressAndStoreStatsIfNeeded();

        if (delay > 0) {
            sleep(task, delay);
        }

        opResult.recordSuccess();

        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        hasRun = true;
        executions++;

        LOGGER.info("MockSingle.run stopping");
        return runResult;
    }

    private void sleep(RunningTask task, long delay) {
        LOGGER.trace("Sleeping for {} msec", delay);
        long end = System.currentTimeMillis() + delay;
        while (task.canRun() && System.currentTimeMillis() < end) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public Long heartbeat(Task task) {
        return null;
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

    public int getExecutions() {
        return executions;
    }

    @SuppressWarnings("unused")
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

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
}
