/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.handlers;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericIntervalWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * @author Pavol Mederly
 */
public class NoOpTaskHandler implements WorkBucketAwareTaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(NoOpTaskHandler.class);

    private static NoOpTaskHandler instance = null;
    private TaskManagerQuartzImpl taskManagerImpl;

    private NoOpTaskHandler() {}

    public static void instantiateAndRegister(TaskManagerQuartzImpl taskManager) {
        if (instance == null) {
            instance = new NoOpTaskHandler();
        }
        taskManager.registerHandler(TaskConstants.NOOP_TASK_HANDLER_URI, instance);
        taskManager.registerAdditionalHandlerUri(TaskConstants.NOOP_TASK_HANDLER_URI_1, instance);
        taskManager.registerAdditionalHandlerUri(TaskConstants.NOOP_TASK_HANDLER_URI_2, instance);
        taskManager.registerAdditionalHandlerUri(TaskConstants.NOOP_TASK_HANDLER_URI_3, instance);
        taskManager.registerAdditionalHandlerUri(TaskConstants.NOOP_TASK_HANDLER_URI_4, instance);
        instance.taskManagerImpl = taskManager;
    }

    @Override
    public TaskWorkBucketProcessingResult run(RunningTask task, WorkBucketType workBucket,
            TaskPartitionDefinitionType taskPartition, TaskWorkBucketProcessingResult previousRunResult) {

        String partition = task.getHandlerUri().substring(TaskConstants.NOOP_TASK_HANDLER_URI.length());  // empty or #1..#4

        OperationResult opResult = new OperationResult(NoOpTaskHandler.class.getName()+".run");
        TaskWorkBucketProcessingResult runResult = new TaskWorkBucketProcessingResult();
        runResult.setOperationResult(opResult);
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);     // would be overwritten when problem is encountered
        runResult.setBucketComplete(false);     // overridden later
        runResult.setShouldContinue(false);     // overridden later

        PrismProperty<Integer> delayProp = task.getExtensionPropertyOrClone(SchemaConstants.NOOP_DELAY_QNAME);
        PrismProperty<Integer> stepsProp = task.getExtensionPropertyOrClone(SchemaConstants.NOOP_STEPS_QNAME);

        PrismPropertyDefinition delayPropDef = taskManagerImpl.getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.NOOP_DELAY_QNAME);
        PrismPropertyDefinition stepsPropDef = taskManagerImpl.getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.NOOP_STEPS_QNAME);
        try {
            if (delayProp != null) {
                delayProp.applyDefinition(delayPropDef);
            }
            if (stepsProp != null) {
                stepsProp.applyDefinition(stepsPropDef);
            }
        } catch (SchemaException se) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot apply prism definition to delay and/or steps property, exiting immediately.", se);
            opResult.recordFatalError("Cannot apply prism definition to delay and/or steps property, exiting immediately.", se);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        long delay;
        if (delayProp != null && !delayProp.getValues().isEmpty()) {
            delay = delayProp.getValues().get(0).getValue();
        } else {
            delay = 0;
        }

        int steps;
        if (stepsProp != null && !stepsProp.getValues().isEmpty()) {
            steps = stepsProp.getValues().get(0).getValue();
        } else {
            steps = 1;
        }

        LOGGER.info("NoOpTaskHandler run starting; progress = {}, steps to be executed = {}, delay for one step = {},"
                + " partition = '{}', work bucket = {}, in task {}", task.getProgress(), steps, delay, partition, workBucket, task);

        int objectFrom;
        int objectTo;
        if (workBucket.getContent() instanceof NumericIntervalWorkBucketContentType) {
            NumericIntervalWorkBucketContentType interval = (NumericIntervalWorkBucketContentType) workBucket.getContent();
            objectFrom = interval.getFrom() != null ? interval.getFrom().intValue() : 0;
            objectTo = interval.getTo() != null ? interval.getTo().intValue() - 1 : objectFrom;
        } else {
            objectFrom = 0;
            objectTo = 0;
        }

outer:  for (int o = objectFrom; o <= objectTo; o++) {
            for (int i = 0; i < steps; i++) {
                LOGGER.info("NoOpTaskHandler: executing step {} of {} on object {} ({}..{}) in task {}", i + 1, steps, o, objectFrom, objectTo, task);

                // this strange construction is used to simulate non-interruptible execution of the task
                long sleepUntil = System.currentTimeMillis() + delay;
                for (;;) {
                    long delta = sleepUntil - System.currentTimeMillis();
                    if (delta > 0) {
                        try {
                            Thread.sleep(delta);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    } else {
                        break;        // we have slept enough
                    }
                }

                task.incrementProgressAndStoreStatsIfNeeded();

                if (!task.canRun()) {
                    LOGGER.info("NoOpTaskHandler: got a shutdown request, finishing task {}", task);
                    break outer;
                }
            }
        }

        opResult.computeStatusIfUnknown();

        LOGGER.info("NoOpTaskHandler run finishing; progress = {} in task {}", task.getProgress(), task);

        runResult.setShouldContinue(task.canRun());
        runResult.setBucketComplete(task.canRun());
        return runResult;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.DEMO;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
