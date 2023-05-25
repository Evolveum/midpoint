/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.GenerateValueActionExpressionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemTargetType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Executes "generate-value" action.
 */
@Component
public class GenerateValueExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    private static final String NAME = "generate-value";
    private static final String PARAMETER_ITEMS = "items";

    @Autowired private ModelInteractionService modelInteraction;

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, GenerateValueActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {

        PolicyItemsDefinitionType itemsDefinition;
        PolicyItemsDefinitionType configured = expressionHelper.getActionArgument(PolicyItemsDefinitionType.class, action,
                GenerateValueActionExpressionType.F_ITEMS, PARAMETER_ITEMS, input, context, null, PARAMETER_ITEMS, globalResult);
        if (configured != null) {
            itemsDefinition = configured;
        } else {
            itemsDefinition = new PolicyItemsDefinitionType().policyItemDefinition(
                    new PolicyItemDefinitionType()
                        .target(new PolicyItemTargetType().path(new ItemPathType(PATH_CREDENTIALS_PASSWORD_VALUE)))
                        .execute(false));
        }

        iterateOverObjects(input, context, globalResult,
                (object, item, result) -> {
                    modelInteraction.generateValue(object, itemsDefinition, context.getTask(), result);
                    context.println("Generated value(s) for " + object);
                },
                (object, exception) -> {
                    context.println("Failed to generate value(s) for " + object + exceptionSuffix(exception));
                });
        return input;
    }

    @Override
    String getActionName() {
        return NAME;
    }

    @Override
    Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }
}
