/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Options for {@link BulkActionsService#executeBulkAction(ExecuteScriptConfigItem, VariablesMap, BulkActionExecutionOptions,
 * Task, OperationResult)}.
 */
public record BulkActionExecutionOptions(
        boolean recordProgressAndIterationStatistics,
        boolean privileged,
        boolean executionPhase) {

    public static BulkActionExecutionOptions create() {
        return new BulkActionExecutionOptions(false, false, false);
    }

    /**
     * Should the executor do its own progress and iteration stats reporting?
     * Used e.g. for non-iterative bulk actions in activities.
     */
    public BulkActionExecutionOptions withRecordProgressAndIterationStatistics() {
        return new BulkActionExecutionOptions(true, privileged, executionPhase);
    }

    /**
     * The difference of "privileged", compared to the regular execution is in the default expression profile used.
     * Here, it is {@link ExpressionProfile#full()} even for unprivileged users.
     */
    public BulkActionExecutionOptions withPrivileged() {
        return new BulkActionExecutionOptions(recordProgressAndIterationStatistics, true, executionPhase);
    }

    /**
     * If true, authorizations are checked for execution phase only.
     */
    public BulkActionExecutionOptions withExecutionPhase() {
        return new BulkActionExecutionOptions(recordProgressAndIterationStatistics, privileged, true);
    }
}
