/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static org.testng.AssertJUnit.*;

import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation.Operation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Task handler that uses LATs (threads).
 */
public class MockParallelTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(MockParallelTaskHandler.class);
    public static final int NUM_SUBTASKS = 500;
    public static final String NS_EXT = "http://myself.me/schemas/whatever";
    public static final ItemName DURATION_QNAME = new ItemName(NS_EXT, "duration", "m");

    private TaskManagerQuartzImpl taskManager;

    /** In-memory version of last task executed */
    private RunningTask lastTaskExecuted;

    private final String id;

    MockParallelTaskHandler(String id, TaskManagerQuartzImpl taskManager) {
        this.id = id;
        this.taskManager = taskManager;
    }

    private boolean hasRun = false;

    public static class MyLightweightTaskHandler implements LightweightTaskHandler {
        private boolean hasRun = false;
        private boolean hasExited = false;
        private final long duration;
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

            // temporarily disabled
            //assertTrue("Subtask is not in Running LAT list of parent", isAmongRunningChildren(task, parentTask));

            while (System.currentTimeMillis() < end && task.canRun()) {
                // hoping to get ConcurrentModificationException when setting operation result here (MID-5113)
                task.getParentForLightweightAsynchronousTask().getUpdatedOrClonedTaskObject();
                IterationItemInformation info = new IterationItemInformation("o1", null, UserType.COMPLEX_TYPE, "oid1");
                Operation op = task.recordIterativeOperationStart(info);
                try {
                    //noinspection BusyWait
                    Thread.sleep(STEP);
                    op.succeeded();
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (parentTask) {
                        parentTask.incrementProgressAndStoreStatsIfNeeded();
                    }
                } catch (InterruptedException e) {
                    LOGGER.trace("Handler for task {} interrupted", task);
                    op.failed(e);
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

    private static boolean isAmongRunningChildren(RunningTask task, RunningTask parentTask) {
        Set<String> runningChildren = parentTask.getRunningLightweightAsynchronousSubtasks().stream()
                .map(Task::getTaskIdentifier)
                .collect(Collectors.toSet());
        return runningChildren.contains(task.getTaskIdentifier());
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        LOGGER.info("MockParallelTaskHandler.run starting (id = " + id + ")");

        OperationResult opResult = new OperationResult(MockParallelTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();

        Integer duration = task.getExtensionPropertyRealValue(DURATION_QNAME);
        LOGGER.info("Duration value = {}", duration);
        System.out.println("task result is " + task.getResult());

        // we create and start some subtasks
        for (int i = 0; i < NUM_SUBTASKS; i++) {
            MyLightweightTaskHandler handler = new MyLightweightTaskHandler(duration);
            RunningTaskQuartzImpl subtask = (RunningTaskQuartzImpl) task.createSubtask(handler);
            subtask.resetIterativeTaskInformation(null);
            assertTrue("Subtask is not transient", subtask.isTransient());
            assertTrue("Subtask is not asynchronous", subtask.isAsynchronous());
            assertTrue("Subtask is not a LAT", subtask.isLightweightAsynchronousTask());
            assertEquals("Subtask has a wrong lightweight handler", handler, subtask.getLightweightTaskHandler());
            assertTrue("Subtask is not in LAT list of parent", task.getLightweightAsynchronousSubtasks().contains(subtask));
            assertFalse("Subtask is in Running LAT list of parent", isAmongRunningChildren(subtask, task));
            assertFalse("Subtask is marked as already started", subtask.lightweightHandlerStartRequested());

            subtask.startLightweightHandler();
            assertTrue("Subtask is not marked as already started", subtask.lightweightHandlerStartRequested());
        }

        System.out.println("Waiting for threads to finish...");
        taskManager.waitForTransientChildrenAndCloseThem(task, opResult);
        System.out.println("... done");

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

    @SuppressWarnings("unused")
    public boolean hasRun() {
        return hasRun;
    }

    @SuppressWarnings("unused")
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
