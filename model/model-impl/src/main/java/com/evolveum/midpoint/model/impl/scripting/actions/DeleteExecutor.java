/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Executes the "delete" action.
 */
@Component
public class DeleteExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.DELETE;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

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

    private void delete(
            ObjectType object, boolean dryRun, ModelExecuteOptions options, ExecutionContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        ObjectDelta<? extends ObjectType> deleteDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(object.getClass(), object.getOid());
        operationsHelper.applyDelta(deleteDelta, options, dryRun, context, result);
        context.println("Deleted " + object + optionsSuffix(options, dryRun));
    }

    @Override
    Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }
}
