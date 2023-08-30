/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;

import java.util.function.Consumer;

import static com.evolveum.midpoint.test.AbstractIntegrationTest.*;
import static com.evolveum.midpoint.test.AbstractIntegrationTest.display;

/**
 * Configurable checker for "task finished" state.
 */
public class TaskFinishChecker implements Checker {

    private static final Trace LOGGER = TraceManager.getTrace(TaskFinishChecker.class);

    private final TaskManager taskManager;
    private final String taskOid;
    private final OperationResult waitResult;
    private final boolean errorOk;
    private final long timeout;
    private final int showProgressEach;
    private final boolean verbose;
    private final Consumer<Task> taskConsumer;
    private final Boolean checkAlsoSchedulingState;

    private Task freshTask;
    private long progressLastShown;

    private TaskFinishChecker(Builder builder) {
        taskManager = builder.taskManager;
        taskOid = builder.taskOid;
        waitResult = builder.waitResult;
        errorOk = builder.errorOk;
        timeout = builder.timeout;
        showProgressEach = builder.showProgressEach;
        verbose = builder.verbose;
        taskConsumer = builder.taskConsumer;
        checkAlsoSchedulingState = builder.checkAlsoSchedulingState;
    }

    @Override
    public boolean check() throws CommonException {
        freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
        if (taskConsumer != null) {
            taskConsumer.accept(freshTask);
        }
        long currentProgress = freshTask.getLegacyProgress();
        if (showProgressEach != 0 && currentProgress - progressLastShown >= showProgressEach) {
            System.out.println("Task progress: " + currentProgress);
            progressLastShown = currentProgress;
        }
        OperationResult result = freshTask.getResult();
        if (verbose) {
            display("Task", freshTask);
        }
        if (freshTask.getSchedulingState() == TaskSchedulingStateType.WAITING) {
            return false;
        } else if (isError(result)) {
            if (errorOk) {
                return schedulingStateIsDone();
            } else {
                display("Failed result of task " + freshTask, freshTask.getResult());
                throw new AssertionError("Error in " + freshTask + ": " + result);
            }
        } else {
            boolean resultDone = !isUnknown(result) && !isInProgress(result);
            return resultDone && schedulingStateIsDone();
        }
    }

    private boolean schedulingStateIsDone() {
        return !shouldCheckAlsoSchedulingState() ||
                freshTask.getSchedulingState() == TaskSchedulingStateType.CLOSED ||
                freshTask.getSchedulingState() == TaskSchedulingStateType.SUSPENDED;
    }

    private boolean shouldCheckAlsoSchedulingState() {
        if (checkAlsoSchedulingState != null) {
            return checkAlsoSchedulingState;
        } else {
            return freshTask.isSingle(); // For single-run tasks we can safely check the execution status.
        }
    }

    @Override
    public void timeout() {
        try {
            Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
            OperationResult result = freshTask.getResult();
            LOGGER.debug("Result of timed-out task:\n{}", result != null ? result.debugDump() : null);
            assert false : "Timeout (" + timeout + ") while waiting for " + freshTask + " to finish. Last result " + result;
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.error("Exception during task refresh: {}", e, e);
        }
    }

    Task getLastTask() {
        return freshTask;
    }

    @SuppressWarnings("WeakerAccess")
    public static final class Builder {
        private TaskManager taskManager;
        private String taskOid;
        private OperationResult waitResult;
        private boolean errorOk;
        private long timeout;
        private int showProgressEach;
        private boolean verbose;
        private Consumer<Task> taskConsumer;

        /**
         * Does extra check based on scheduling state: the task is not considered finished if it is not CLOSED or SUSPENDED.
         * Default is true for single-run tasks and false for recurring ones.
         */
        private Boolean checkAlsoSchedulingState;

        public Builder taskManager(TaskManager val) {
            taskManager = val;
            return this;
        }

        public Builder taskOid(String val) {
            taskOid = val;
            return this;
        }

        public Builder waitResult(OperationResult val) {
            waitResult = val;
            return this;
        }

        public Builder errorOk(boolean val) {
            errorOk = val;
            return this;
        }

        public Builder timeout(long val) {
            timeout = val;
            return this;
        }

        public Builder showProgressEach(int val) {
            showProgressEach = val;
            return this;
        }

        public Builder verbose(boolean val) {
            verbose = val;
            return this;
        }

        public Builder taskConsumer(Consumer<Task> val) {
            taskConsumer = val;
            return this;
        }

        public Builder checkAlsoSchedulingState(Boolean val) {
            checkAlsoSchedulingState = val;
            return this;
        }

        public TaskFinishChecker build() {
            return new TaskFinishChecker(this);
        }
    }
}
