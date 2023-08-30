/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.AbstractActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.jetbrains.annotations.NotNull;

/**
 * Executes an action of a given type. Instances of this type must be registered with BulkActionsExecutor.
 */
public interface ActionExecutor {

    /** Returns the type of action supported by this executor. */
    @NotNull BulkAction getActionType();

    /**
     * Checks if the execution is allowed: both expression profile and authorizations are checked.
     *
     * We may put this inside the {@link #execute(AbstractActionExpressionType, PipelineData, ExecutionContext, OperationResult)}
     * method later, if needed.
     */
    void checkExecutionAllowed(ExecutionContext context, OperationResult result)
            throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

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
    PipelineData execute(
            ActionExpressionType command, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /** To be used only if the "dynamic" version is not supported. */
    default PipelineData execute(
            AbstractActionExpressionType command, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        throw new UnsupportedOperationException();
    }
}
