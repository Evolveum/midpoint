/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Describes an environment in which an {@link Expression} is evaluated.
 * Contains current task and operation result (if known - but it is usually so).
 *
 * Usually contained in some kind of a thread-local holder.
 */
public class ExpressionEnvironment {

    private final Task currentTask;
    private final OperationResult currentResult;

    public ExpressionEnvironment(Task currentTask, OperationResult currentResult) {
        this.currentTask = currentTask;
        this.currentResult = currentResult;
    }

    public OperationResult getCurrentResult() {
        return currentResult;
    }

    public Task getCurrentTask() {
        return currentTask;
    }

    @Override
    public String toString() {
        return "ExpressionEnvironment(currentResult=" + getCurrentResult() + ", currentTask=" + getCurrentTask() + ")";
    }
}
