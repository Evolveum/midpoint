/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static com.evolveum.midpoint.task.quartzimpl.TestTaskManagerBasic.NS_EXT;

import static org.testng.AssertJUnit.*;

import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.Operation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Task handler that uses LATs (threads).
 */
public class MockParallelTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(MockParallelTaskHandler.class);
    static final int NUM_SUBTASKS = 15; // Shouldn't be too high because of concurrent repository access.
    private static final ItemName DURATION_QNAME = new ItemName(NS_EXT, "duration");

    private TaskManagerQuartzImpl taskManager;

    /** In-memory version of last task executed */
    private RunningTask lastTaskExecuted;

    MockParallelTaskHandler(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    private boolean hasRun = false;

    public static class MyLightweightTaskHandler implements LightweightTaskHandler {
        private boolean hasRun = false;
        private boolean hasExited = false;
        private final long duration;
        private static final long STEP = 10;

        MyLightweightTaskHandler(Integer duration) {
            this.duration = duration != null ? duration : 86400L * 1000L * 365000L; // 1000 years
        }

        @Override
        public void run(RunningLightweightTask task) {
            LOGGER.trace("Handler for task {} running", task);
            hasRun = true;
            long end = System.currentTimeMillis() + duration;
            RunningTask parentTask = task.getLightweightTaskParent();
            parentTask.setStatisticsRepoStoreInterval(1000L);

            // temporarily disabled
            //assertTrue("Subtask is not in Running LAT list of parent", isAmongRunningChildren(task, parentTask));

            while (System.currentTimeMillis() < end && task.canRun()) {
                // Check for some concurrency issues - although not related to the operation result, because
                // it is inherently not thread-safe.
                task.getLightweightTaskParent().getRawTaskObjectClonedIfNecessary();
                IterationItemInformation info = new IterationItemInformation("o1", null, UserType.COMPLEX_TYPE, "oid1");
                Operation op = task.recordIterativeOperationStart(info);
                try {
                    //noinspection BusyWait
                    Thread.sleep(STEP);
                    op.succeeded();
                    parentTask.incrementLegacyProgressTransient();
                    parentTask.updateOperationStatsInTaskPrism(false);
                    parentTask.storeStatisticsIntoRepositoryIfTimePassed(null, new OperationResult("store stats"));
                } catch (InterruptedException e) {
                    LOGGER.trace("Handler for task {} interrupted", task);
                    op.failed(e);
                    break;
                } catch (Exception e) {
                    op.failed(e);
                    break;
                }
                OperationResult result = task.getResult();
                result.createSubresult("test");
                result.summarize();
            }
            hasExited = true;
        }

        boolean hasRun() {
            return hasRun;
        }
        boolean hasExited() {
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
    public TaskRunResult run(@NotNull RunningTask task) {
        LOGGER.info("MockParallelTaskHandler.run starting");

        OperationResult opResult = task.getResult();
        TaskRunResult runResult = new TaskRunResult();

        Integer duration = task.getExtensionPropertyRealValue(DURATION_QNAME);
        LOGGER.info("Duration value = {}", duration);
        System.out.println("task result is " + task.getResult());

        // we create and start some subtasks
        for (int i = 0; i < NUM_SUBTASKS; i++) {
            MyLightweightTaskHandler handler = new MyLightweightTaskHandler(duration);
            RunningLightweightTaskImpl subtask = (RunningLightweightTaskImpl) task.createSubtask(handler);
            assertTrue("Subtask is not transient", subtask.isTransient());
            assertTrue("Subtask is not asynchronous", subtask.isAsynchronous());
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
        runResult.setProgress(task.getLegacyProgress()+1);

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

    public TaskManagerQuartzImpl getTaskManager() {
        return taskManager;
    }

    public void setTaskManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    RunningTask getLastTaskExecuted() {
        return lastTaskExecuted;
    }

}
