/*
 * Copyright (c) 2010-2013 Evolveum and contributors
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
 *
 */
public class MockCycleTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(MockCycleTaskHandler.class);

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {

        LOGGER.info("MockCycle.run starting");

        OperationResult opResult = new OperationResult(MockCycleTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        task.incrementProgressAndStoreStatisticsIfTimePassed(opResult);

        opResult.recordSuccess();

        // This "run" is finished. But the task goes on ... (if finishTheHandler == false)
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        LOGGER.info("MockCycle.run stopping");
        return runResult;
    }

    @Override
    public Long heartbeat(Task task) {
        return null;        // not to overwrite progress information!
    }

    @Override
    public void refreshStatus(Task task) {
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.MOCK;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
