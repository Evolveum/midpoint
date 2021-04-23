/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartDefinitionType;

import org.jetbrains.annotations.NotNull;

public class GenericTaskExecution implements TaskExecution {

    private static final Trace LOGGER = TraceManager.getTrace(GenericTaskExecution.class);

    @NotNull private final RunningTask task;
    @NotNull private final GenericTaskHandler genericTaskHandler;

    GenericTaskExecution(@NotNull RunningTask task, @NotNull GenericTaskHandler genericTaskHandler) {
        this.task = task;
        this.genericTaskHandler = genericTaskHandler;
    }

    @Override
    public @NotNull TaskRunResult run(OperationResult result) throws CommonException, TaskException, PreconditionViolationException {
        TaskPartExecution rootPartExecution = createRootPartExecution(result);
        LOGGER.info("root part execution: {}", rootPartExecution);

        return rootPartExecution.run(result);
    }

    private TaskPartExecution createRootPartExecution(OperationResult result) throws SchemaException {
        TaskPartDefinitionType rootPartDef = task.getWorkDefinitionOrClone();
        return createPartExecution(rootPartDef, result);
    }

    private TaskPartExecution createPartExecution(TaskPartDefinitionType partDef, OperationResult result) throws SchemaException {
        return genericTaskHandler.getTaskPartExecutionFactory()
                .getFactory(partDef, null, task)
                .createPartExecution(partDef, this, result);
    }

    @Override
    public @NotNull RunningTask getTask() {
        return task;
    }
}
