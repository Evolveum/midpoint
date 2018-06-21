/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.query.ObjectFilter;
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

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Interface of the Model subsystem that provides scripting (bulk actions) operations.
 *
 * @author mederly
 */
public interface ScriptingService {

    /**
     * Asynchronously executes simple scripting expressions, consisting of one search command and one action.
     *
     * @param objectType Object type to search (e.g. c:UserType)
     * @param filter Filter to be applied (ObjectFilter)
     * @param actionName Action to be executed on objects found (e.g. "disable", "delete", "recompute", etc).
     * @param task Task in context of which the script should execute. The task should be "clean", i.e.
     *             (1) transient, (2) without any handler. This method puts the task into background,
     *             and assigns ScriptExecutionTaskHandler to it, to execute the script.
     * @param parentResult
     * @throws SchemaException
     *
     * TODO consider removing this method (it was meant as a simplified version of the method below)
     */
    @Deprecated
    void evaluateExpressionInBackground(QName objectType, ObjectFilter filter, String actionName, Task task,
            OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

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
			@NotNull Map<String, Object> initialVariables, boolean recordProgressAndIterationStatistics, @NotNull Task task,
			@NotNull OperationResult result)
			throws ScriptExecutionException, SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

}