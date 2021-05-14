/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.simple;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;

import org.jetbrains.annotations.NotNull;

/**
 * Context of the execution of a simple task.
 *
 * Task-specific fields should be provided by the subclasses.
 */
@Experimental
public abstract class ExecutionContext {

    private SimpleIterativeTaskHandler<?, ?, ?>.TaskExecution taskExecution;
    private SimpleIterativeTaskHandler<?, ?, ?>.PartExecution partExecution;

    protected abstract void initialize(OperationResult opResult) throws SchemaException, CommunicationException,
            ObjectNotFoundException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException;

    public void setTaskExecution(SimpleIterativeTaskHandler<?, ?, ?>.TaskExecution taskExecution) {
        this.taskExecution = taskExecution;
    }

    public void setPartExecution(SimpleIterativeTaskHandler<?, ?, ?>.PartExecution partExecution) {
        this.partExecution = partExecution;
    }

    public @NotNull RunningTask getLocalCoordinationTask() {
        return taskExecution.localCoordinatorTask;
    }

    public ActivityDefinitionType getPartDefinition() {
        return partExecution.getActivityDefinition();
    }
}
