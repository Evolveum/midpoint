/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValidationResultType;

/**
 * Executes "validate" action.
 *
 * There is no static (typed) definition of this action yet.
 * Also, this code is not refactored yet.
 */
@Component
public class ValidateExecutor extends BaseActionExecutor {

    @Autowired private ResourceValidator resourceValidator;

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.VALIDATE;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        PipelineData output = PipelineData.createEmpty();

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            context.checkTaskStop();
            OperationResult result = operationsHelper.createActionResult(item, this, globalResult);
            try {
                if (value instanceof PrismObjectValue && ((PrismObjectValue<?>) value).asObjectable() instanceof ResourceType) {
                    //noinspection unchecked
                    PrismObject<ResourceType> resourceTypePrismObject = ((PrismObjectValue<ResourceType>) value).asPrismObject();
                    ResourceType resourceType = resourceTypePrismObject.asObjectable();
                    Operation op = operationsHelper.recordStart(context, resourceType);
                    try {
                        ValidationResult validationResult = resourceValidator.validate(
                                resourceTypePrismObject, Scope.THOROUGH, null, context.getTask(), result);

                        PrismContainerDefinition<ValidationResultType> pcd = prismContext.getSchemaRegistry()
                                .findContainerDefinitionByElementName(SchemaConstantsGenerated.C_VALIDATION_RESULT);
                        PrismContainer<ValidationResultType> pc = pcd.instantiate();
                        //noinspection unchecked
                        pc.add(validationResult.toValidationResultType().asPrismContainerValue());

                        context.println("Validated %s: %d issue(s)".formatted(
                                resourceTypePrismObject, validationResult.getIssues().size()));
                        operationsHelper.recordEnd(context, op, null, result);
                        output.add(new PipelineItem(pc.getValue(), item.getResult()));
                    } catch (SchemaException | RuntimeException e) {
                        operationsHelper.recordEnd(context, op, e, result);
                        context.println("Error validation " + resourceTypePrismObject + ": " + e.getMessage());
                        //noinspection ThrowableNotThrown
                        logOrRethrowActionException(e, value, context);
                        output.add(item);
                    }
                } else {
                    //noinspection ThrowableNotThrown
                    logOrRethrowActionException(
                            new UnsupportedOperationException("Item is not a PrismObject<ResourceType>"), value, context);
                }
            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close();
            }
            operationsHelper.trimAndCloneResult(result, item.getResult());
        }
        return output;
    }
}
