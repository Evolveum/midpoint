/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.task.TaskExecution;
import com.evolveum.midpoint.repo.common.task.TaskPartExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskRunResult;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public abstract class AbstractMockTaskPartExecution implements TaskPartExecution {

    public static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/repo-common-test/extension";
    public static final QName MOCK_PARAMETERS_TYPE_QNAME = new QName(NS_EXT, "MockParametersType");
    public static final ItemName IDENTIFIER_NAME = new ItemName(NS_EXT, "identifier");
    public static final ItemName DELAY_NAME = new ItemName(NS_EXT, "delay");
    public static final ItemName STEPS_NAME = new ItemName(NS_EXT, "steps");

    public static final String COMMON_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/mock/handler-3";

    public static final String OPENING_RELATIVE_URI = "opening";
    public static final String OPENING_PART_HANDLER_URI = COMMON_HANDLER_URI + "#" + OPENING_RELATIVE_URI;
    public static final String CLOSING_RELATIVE_URI = "closing";
    public static final String CLOSING_PART_HANDLER_URI = COMMON_HANDLER_URI + "#" + CLOSING_RELATIVE_URI;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractMockTaskPartExecution.class);

    @NotNull private final TaskExecution taskExecution;

    public AbstractMockTaskPartExecution(@NotNull TaskExecution taskExecution) {
        this.taskExecution = taskExecution;
    }

    @Override
    public @NotNull TaskRunResult run(OperationResult result) {
        LOGGER.info("MockSingle.run starting (id = {})", "TODO");

        TaskRunResult runResult = new TaskRunResult();

        runResult.setOperationResult(result);

        RunningTask task = taskExecution.getTask();

        task.incrementProgressAndStoreStatisticsIfTimePassed(result);

//        if (delay > 0) {
//            sleep(task, delay);
//        }
//
//        result.recordSuccess();
//
        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
//
//        hasRun = true;
//        executions++;

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
    public @NotNull TaskExecution getTaskExecution() {
        return taskExecution;
    }


}
