/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

/**
 * Executes an action of a given type. Instances of this type must be registered with ScriptingExpressionEvaluator.
 *
 * @author mederly
 */
@FunctionalInterface
public interface ActionExecutor {

    /**
     * Executes given action command.
     *
     * @param command
     * @param context
     * @param parentResult
     */
    PipelineData execute(ActionExpressionType command, PipelineData input, ExecutionContext context, OperationResult parentResult) throws ScriptExecutionException;
}
