/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

/**
 * Interface of the Model subsystem that provides scripting (bulk actions) operations.
 */
public interface BulkActionsService {

    /**
     * Synchronously executes any scripting expression (with no input data).
     *
     * Determines and respects the execution profile with regards to the origin of the script.
     *
     * @param executeScriptCommand Scripting request to execute.
     * @param task Task in context of which the script should execute (in foreground!)
     * @param result Operation result
     *
     * TODO consider returning ExecutionContext (requires moving the context to model api)
     */
    BulkActionExecutionResult executeBulkAction(
            @NotNull ExecuteScriptConfigItem executeScriptCommand,
            @NotNull VariablesMap initialVariables,
            boolean recordProgressAndIterationStatistics,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ScriptExecutionException, SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException;
}
