/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.DeleteActionExpressionType;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Executes the "delete" action.
 */
@Component
public class DeleteExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    private static final String NAME = "delete";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, DeleteActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        boolean dryRun = operationsHelper.getDryRun(action, input, context, globalResult);
        ModelExecuteOptions options = operationsHelper.getOptions(action, input, context, globalResult);

        iterateOverObjects(input, context, globalResult,
                (object, item, result) ->
                        delete(object.asObjectable(), dryRun, options, context, result),
                (object, exception) ->
                        context.println("Failed to delete " + object + drySuffix(dryRun) + exceptionSuffix(exception))
        );

        return input;
    }

    private void delete(ObjectType object, boolean dryRun, ModelExecuteOptions options, ExecutionContext context,
            OperationResult result) throws ScriptExecutionException {
        ObjectDelta<? extends ObjectType> deleteDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(object.getClass(), object.getOid());
        operationsHelper.applyDelta(deleteDelta, options, dryRun, context, result);
        context.println("Deleted " + object + optionsSuffix(options, dryRun));
    }

    @Override
    Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }

    @Override
    String getActionName() {
        return NAME;
    }
}
