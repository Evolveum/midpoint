/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

import org.apache.commons.lang.Validate;

import static com.evolveum.midpoint.task.quartzimpl.TaskTestUtil.createExtensionDelta;

/**
 * @author Radovan Semancik
 *
 */
public class MockSingleTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(MockSingleTaskHandler.class);
    private static final String NS_EXT = "http://myself.me/schemas/whatever";
    private static final ItemName L1_FLAG_QNAME = new ItemName(NS_EXT, "l1Flag", "m");

    private TaskManagerQuartzImpl taskManager;

    private String id;

    private PrismPropertyDefinition l1FlagDefinition;

    MockSingleTaskHandler(String id, TaskManagerQuartzImpl taskManager) {
        this.id = id;
        this.taskManager = taskManager;

        l1FlagDefinition = taskManager.getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(L1_FLAG_QNAME);
        Validate.notNull(l1FlagDefinition, "l1Flag property is unknown");
    }

    private boolean hasRun = false;
    private int executions = 0;

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
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
            PrismProperty<Boolean> l1flag = task.getExtensionPropertyOrClone(L1_FLAG_QNAME);

            if (l1flag == null || !l1flag.getRealValue()) {

                LOGGER.info("L1 handler, first run - scheduling L2 handler");
                ScheduleType l2Schedule = new ScheduleType();
                l2Schedule.setInterval(2);
                task.pushHandlerUri(AbstractTaskManagerTest.L2_TASK_HANDLER_URI, l2Schedule, TaskBinding.TIGHT, createExtensionDelta(l1FlagDefinition, true,
                        taskManager.getPrismContext()));
                try {
                    task.flushPendingModifications(opResult);
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
                    task.flushPendingModifications(opResult);
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
        }

        LOGGER.info("MockSingle.run stopping");
        return runResult;
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
}
