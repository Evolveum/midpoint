/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.execution;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Implements and represents an execution of an activity.
 */
public interface ActivityExecution extends DebugDumpable {

    /**
     * Lets the execution proceed. (I.e. executes the activity.)
     *
     * This method is responsible for carrying out the work, e.g. recomputing all the users.
     * For pure- or semi-composite activities it is also responsible for creating the children executions.
     *
     * Note that the work can be delegated to other (asynchronous) tasks. This is the case of worker tasks in multi-node
     * task execution, or of activities executed as separate subtasks.
     */
    @NotNull ActivityExecutionResult execute(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException;

    /**
     * Returns task execution that contains this activity execution.
     */
    @NotNull TaskExecution getTaskExecution();

    /** TODO */
    void setupIdentifier(Supplier<String> defaultIdentifierSupplier);

    /**
     * Returns activity identifier (unique among siblings).
     */
    @NotNull String getIdentifier();

}
