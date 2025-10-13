/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemTargetType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.GenerateValueActionExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Executes "generate-value" action.
 */
@Component
public class GenerateValueExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    private static final String PARAMETER_ITEMS = "items";

    @Autowired private ModelInteractionService modelInteraction;

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.GENERATE_VALUE;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

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

        //noinspection CodeBlock2Expr
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
    Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }
}
