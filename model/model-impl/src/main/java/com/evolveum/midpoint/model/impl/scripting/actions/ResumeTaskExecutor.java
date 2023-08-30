/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import static java.util.Collections.singleton;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Executes "resume" action.
 */
@Component
public class ResumeTaskExecutor extends AbstractObjectBasedActionExecutor<TaskType> {

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.RESUME;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        iterateOverObjects(input, context, globalResult,
                (object, item, result) -> {
                    taskService.resumeTasks(singleton(object.getOid()), context.getTask(), result);
                    context.println("Resumed " + object);
                }, (object, exception) ->
                        context.println("Failed to resume " + object + exceptionSuffix(exception))
        );
        return input;
    }

    @Override
    Class<TaskType> getObjectType() {
        return TaskType.class;
    }
}
