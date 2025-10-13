/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
            @NotNull BulkActionExecutionOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, ObjectAlreadyExistsException;
}
