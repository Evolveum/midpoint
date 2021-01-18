/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.function.Consumer;

import static com.evolveum.midpoint.model.test.AbstractModelIntegrationTest.*;
import static com.evolveum.midpoint.test.AbstractIntegrationTest.display;

/**
 * Configurable checker for "task finished" state.
 */
public class TaskFinishChecker implements Checker {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);

    private final TaskManager taskManager;
    private final String taskOid;
    private final OperationResult waitResult;
    private final boolean checkSubresult;
    private final boolean errorOk;
    private final int timeout;
    private final int showProgressEach;
    private final boolean verbose;
    private final Consumer<Task> taskConsumer;
    private final Boolean checkAlsoExecutionStatus;

    private Task freshTask;
    private long progressLastShown;

    private TaskFinishChecker(Builder builder) {
        taskManager = builder.taskManager;
        taskOid = builder.taskOid;
        waitResult = builder.waitResult;
        checkSubresult = builder.checkSubresult;
        errorOk = builder.errorOk;
        timeout = builder.timeout;
        showProgressEach = builder.showProgressEach;
        verbose = builder.verbose;
        taskConsumer = builder.taskConsumer;
        checkAlsoExecutionStatus = builder.checkAlsoExecutionStatus;
    }

    @Override
    public boolean check() throws CommonException {
        freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
        if (taskConsumer != null) {
            taskConsumer.accept(freshTask);
        }
        long currentProgress = freshTask.getProgress();
        if (showProgressEach != 0 && currentProgress - progressLastShown >= showProgressEach) {
            System.out.println("Task progress: " + currentProgress);
            progressLastShown = currentProgress;
        }
        OperationResult result = freshTask.getResult();
        if (verbose) {
            display("Task", freshTask);
        }
        if (freshTask.getExecutionStatus() == TaskExecutionStatus.WAITING) {
            return false;
        } else if (isError(result, checkSubresult)) {
            if (errorOk) {
                return executionStatusIsDone();
            } else {
                throw new AssertionError("Error in " + freshTask + ": " + TestUtil.getErrorMessage(result));
            }
        } else {
            boolean resultDone = !isUnknown(result, checkSubresult) && !isInProgress(result, checkSubresult);
            return resultDone && executionStatusIsDone();
        }
    }

    private boolean executionStatusIsDone() {
        return !shouldCheckAlsoExecutionStatus() ||
                freshTask.getExecutionStatus() == TaskExecutionStatus.CLOSED ||
                freshTask.getExecutionStatus() == TaskExecutionStatus.SUSPENDED;
    }

    private boolean shouldCheckAlsoExecutionStatus() {
        if (checkAlsoExecutionStatus != null) {
            return checkAlsoExecutionStatus;
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
        private boolean checkSubresult;
        private boolean errorOk;
        private int timeout;
        private int showProgressEach;
        private boolean verbose;
        private Consumer<Task> taskConsumer;

        /**
         * Does extra check based on execution status: the task is not considered finished if it is not CLOSED or SUSPENDED.
         * Default is true for single-run tasks and false for recurring ones.
         */
        private Boolean checkAlsoExecutionStatus;

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

        public Builder checkSubresult(boolean val) {
            checkSubresult = val;
            return this;
        }

        public Builder errorOk(boolean val) {
            errorOk = val;
            return this;
        }

        public Builder timeout(int val) {
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

        public Builder checkAlsoExecutionStatus(Boolean val) {
            checkAlsoExecutionStatus = val;
            return this;
        }

        public TaskFinishChecker build() {
            return new TaskFinishChecker(this);
        }
    }
}
