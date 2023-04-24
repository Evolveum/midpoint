/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ApplyDefinitionActionExpressionType;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Applies definitions to relevant objects. Currently supports ShadowType and ResourceType
 * that are given definitions by provisioning module.
 */
@Component
public class ApplyDefinitionExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    private static final String NAME = "apply-definition";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, ApplyDefinitionActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input,
            ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        iterateOverObjects(input, context, globalResult,
                (object, item, result) ->
                        applyDefinition(object.asObjectable(), context, result),
                (object, exception) ->
                        context.println("Failed to apply definition to " + object + exceptionSuffix(exception))
        );

        return input;
    }

    private void applyDefinition(ObjectType object, ExecutionContext context, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (object instanceof ShadowType || object instanceof ResourceType) {
            provisioningService.applyDefinition(object.asPrismObject(), context.getTask(), result);
            context.println("Applied definition to " + object);
        }
    }

    @Override
    protected Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }

    @Override
    protected String getActionName() {
        return NAME;
    }
}
