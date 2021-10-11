/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Pavol Mederly
 *
 */
public class MockParallelTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(MockParallelTaskHandler.class);
    public static final int NUM_SUBTASKS = 500;
    public static final String NS_EXT = "http://myself.me/schemas/whatever";
    public static final ItemName DURATION_QNAME = new ItemName(NS_EXT, "duration", "m");
    private final PrismPropertyDefinition durationDefinition;

    private TaskManagerQuartzImpl taskManager;

    // a bit of hack - to reach in-memory version of last task executed
    private RunningTask lastTaskExecuted;

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
        private long duration;
        private static final long STEP = 100;

        public MyLightweightTaskHandler(Integer duration) {
            this.duration = duration != null ? duration : 86400L * 1000L * 365000L; // 1000 years
        }

        @Override
        public void run(RunningTask task) {
            LOGGER.trace("Handler for task {} running", task);
            hasRun = true;
            long end = System.currentTimeMillis() + duration;
            RunningTask parentTask = task.getParentForLightweightAsynchronousTask();
            parentTask.setOperationStatsUpdateInterval(1000L);
            while (System.currentTimeMillis() < end) {
                // hoping to get ConcurrentModificationException when setting operation result here (MID-5113)
                task.getParentForLightweightAsynchronousTask().getUpdatedOrClonedTaskObject();
                long started = System.currentTimeMillis();
                task.recordIterativeOperationStart("o1", null, UserType.COMPLEX_TYPE, "oid1");
                try {
                    Thread.sleep(STEP);
                    task.recordIterativeOperationEnd("o1", null, UserType.COMPLEX_TYPE, "oid1", started, null);
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (parentTask) {
                        parentTask.incrementProgressAndStoreStatsIfNeeded();
                    }
                } catch (InterruptedException e) {
                    LOGGER.trace("Handler for task {} interrupted", task);
                    task.recordIterativeOperationEnd("o1", null, UserType.COMPLEX_TYPE, "oid1", started, e);
                    break;
                }
            }
            hasExited = true;
        }

        public boolean hasRun() {
            return hasRun;
        }
        public boolean hasExited() {
            return hasExited;
        }
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        LOGGER.info("MockParallelTaskHandler.run starting (id = " + id + ")");

        OperationResult opResult = new OperationResult(MockParallelTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();

        PrismProperty<Integer> duration = task.getExtensionPropertyOrClone(DURATION_QNAME);
        Integer durationValue = null;
        if (duration != null) {
            durationValue = duration.getRealValue();
        }
        LOGGER.info("Duration value = {}", durationValue);
        System.out.println("task result is " + task.getResult());

        // we create and start some subtasks
        for (int i = 0; i < NUM_SUBTASKS; i++) {
            MyLightweightTaskHandler handler = new MyLightweightTaskHandler(durationValue);
            RunningTaskQuartzImpl subtask = (RunningTaskQuartzImpl) task.createSubtask(handler);
            subtask.resetIterativeTaskInformation(null);
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

    public RunningTask getLastTaskExecuted() {
        return lastTaskExecuted;
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy().maintainIterationStatistics().fromZero();
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
