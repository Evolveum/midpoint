/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an execution of a task part.
 */
public interface TaskPartExecution {

    /**
     * Executes the work associated with the task part.
     *
     * Note that the work can be delegated to other (asynchronous) tasks. This is the case of worker tasks in multi-node
     * task execution, or of parts executed as separate subtasks.
     * @param result
     */
    @NotNull TaskRunResult run(OperationResult result) throws SchemaException, TaskException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException, PolicyViolationException, PreconditionViolationException;

    /**
     * Returns containing task execution object.
     */
    @NotNull TaskExecution getTaskExecution();
}
