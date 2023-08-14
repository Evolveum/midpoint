/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

/**
 * Executes an action of a given type. Instances of this type must be registered with ScriptingExpressionEvaluator.
 */
public interface ActionExecutor {

    /**
     * Checks if the execution is allowed; we may put this inside the {@link #execute(ActionExpressionType, PipelineData,
     * ExecutionContext, OperationResult)} method later, if needed.
     */
    void checkExecutionAllowed(ExecutionContext context) throws SecurityViolationException;

    /**
     * Executes given action command.
     *
     * @param command Command to be executed. Its parameters can be defined statically (using "new" specific subclasses
     *                in the schema) or dynamically (using "old fashioned" dynamic name-value parameters) or in a mixed style, where
     *                dynamic definitions take precedence.
     *
     * @param input Input data (pipeline) that the action has to be executed on.
     *
     * @param context Overall execution context.
     *
     * @param globalResult Global operation result. This is the parent result that receives subresults related to
     *                     actions executions. (But individual results are stored also into the pipeline, to indicate success/failure of
     *                     individual pipeline items processing.)
     */
    PipelineData execute(ActionExpressionType command, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws ScriptExecutionException, SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException;
}
