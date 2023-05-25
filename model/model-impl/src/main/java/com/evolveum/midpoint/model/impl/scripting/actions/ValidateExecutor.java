/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.schema.statistics.Operation;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValidationResultType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

/**
 * Executes "validate" action.
 *
 * There is no static (typed) definition of this action yet.
 * Also, this code is not refactored yet.
 */
@Component
public class ValidateExecutor extends BaseActionExecutor {

    @Autowired
    private ResourceValidator resourceValidator;

    private static final String NAME = "validate";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        PipelineData output = PipelineData.createEmpty();

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue && ((PrismObjectValue<?>) value).asObjectable() instanceof ResourceType) {
                //noinspection unchecked
                PrismObject<ResourceType> resourceTypePrismObject = ((PrismObjectValue<ResourceType>) value).asPrismObject();
                ResourceType resourceType = resourceTypePrismObject.asObjectable();
                Operation op = operationsHelper.recordStart(context, resourceType);
                try {
                    ValidationResult validationResult = resourceValidator.validate(resourceTypePrismObject, Scope.THOROUGH, null, context.getTask(), result);

                    PrismContainerDefinition<ValidationResultType> pcd = prismContext.getSchemaRegistry()
                            .findContainerDefinitionByElementName(SchemaConstantsGenerated.C_VALIDATION_RESULT);
                    PrismContainer<ValidationResultType> pc = pcd.instantiate();
                    //noinspection unchecked
                    pc.add(validationResult.toValidationResultType().asPrismContainerValue());

                    context.println("Validated " + resourceTypePrismObject + ": " + validationResult.getIssues().size() + " issue(s)");
                    operationsHelper.recordEnd(context, op, null, result);
                    output.add(new PipelineItem(pc.getValue(), item.getResult()));
                } catch (SchemaException|RuntimeException e) {
                    operationsHelper.recordEnd(context, op, e, result);
                    context.println("Error validation " + resourceTypePrismObject + ": " + e.getMessage());
                    //noinspection ThrowableNotThrown
                    processActionException(e, NAME, value, context);
                    output.add(item);
                }
            } else {
                //noinspection ThrowableNotThrown
                processActionException(new ScriptExecutionException("Item is not a PrismObject<ResourceType>"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, item.getResult());
        }
        return output;
    }

    @Override
    String getActionName() {
        return NAME;
    }
}
