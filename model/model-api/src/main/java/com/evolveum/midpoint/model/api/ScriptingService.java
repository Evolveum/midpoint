/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import org.jetbrains.annotations.NotNull;

/**
 * Interface of the Model subsystem that provides scripting (bulk actions) operations.
 *
 * @author mederly
 */
public interface ScriptingService {

    /**
     * Asynchronously executes any scripting expression.
     *
     * @param expression Expression to be executed.
     * @param task Task in context of which the script should execute.
     *             The task should be "clean", i.e. (1) transient, (2) without any handler.
     *             This method puts the task into background, and assigns ScriptExecutionTaskHandler
     *             to it, to execute the script.
     * @param parentResult
     * @throws SchemaException
     * @throws ConfigurationException
     * @throws CommunicationException
     */
    void evaluateExpressionInBackground(ScriptingExpressionType expression, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;
    void evaluateExpressionInBackground(ExecuteScriptType executeScriptCommand, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Asynchronously executes any scripting expression.
     *
     * @param executeScriptCommand ExecuteScript to be executed.
     * @param task Task in context of which the script should execute.
     *             The task should be "clean", i.e. (1) transient, (2) without any handler.
     *             This method puts the task into background, and assigns IterativeScriptExecutionTaskHandler
     *             to it, to execute the script.
     * @param parentResult
     * @throws SchemaException
     * @throws ConfigurationException
     * @throws CommunicationException
     */
    void evaluateIterativeExpressionInBackground(ExecuteScriptType executeScriptCommand, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Synchronously executes any scripting expression (with no input data).
     *
     * @param expression Scripting expression to execute.
     * @param task Task in context of which the script should execute (in foreground!)
     * @param result Operation result
     * @throws ScriptExecutionException
     *
     * TODO return ExecutionContext (requires moving the context to model api)
     */

    ScriptExecutionResult evaluateExpression(ScriptingExpressionType expression, Task task, OperationResult result)
            throws ScriptExecutionException, SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    ScriptExecutionResult evaluateExpression(@NotNull ExecuteScriptType executeScriptCommand,
            @NotNull VariablesMap initialVariables, boolean recordProgressAndIterationStatistics, @NotNull Task task,
            @NotNull OperationResult result)
            throws ScriptExecutionException, SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

}
