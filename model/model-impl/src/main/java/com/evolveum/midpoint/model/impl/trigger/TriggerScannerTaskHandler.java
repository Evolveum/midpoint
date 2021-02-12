/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.trigger;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScannerTaskExecution;
import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScannerTaskHandler;
import com.evolveum.midpoint.repo.common.task.PartExecutionClass;
import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Task handler for the trigger scanner.
 *
 * Keeps a registry of trigger handlers.
 *
 * @author Radovan Semancik
 */
@Component
@TaskExecutionClass(TriggerScannerTaskHandler.TaskExecution.class)
@PartExecutionClass(TriggerScannerTaskPartExecution.class)
public class TriggerScannerTaskHandler
        extends AbstractScannerTaskHandler
        <TriggerScannerTaskHandler,
                TriggerScannerTaskHandler.TaskExecution> {

    // WARNING! This task handler is efficiently singleton!
    // It is a spring bean and it is supposed to handle all search task instances
    // Therefore it must not have task-specific fields. It can only contain fields specific to
    // all tasks of a specified type

    public static final String HANDLER_URI = ModelPublicConstants.TRIGGER_SCANNER_TASK_HANDLER_URI;

    private static final Trace LOGGER = TraceManager.getTrace(TriggerScannerTaskHandler.class);

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;

    public TriggerScannerTaskHandler() {
        super(LOGGER, "Trigger scan", OperationConstants.TRIGGER_SCAN);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return null; // TODO decide
    }

    public TriggerHandler getTriggerHandler(String handlerUri) {
        return triggerHandlerRegistry.getHandler(handlerUri);
    }

    /** Just to make Java compiler happy. */
    protected static class TaskExecution
            extends AbstractScannerTaskExecution<TriggerScannerTaskHandler, TriggerScannerTaskHandler.TaskExecution> {

        public TaskExecution(TriggerScannerTaskHandler taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }
}
