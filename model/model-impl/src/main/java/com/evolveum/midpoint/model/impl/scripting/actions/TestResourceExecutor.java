/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Executes "test-resource" action.
 */
@Component
public class TestResourceExecutor extends AbstractObjectBasedActionExecutor<ResourceType> {

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.TEST_RESOURCE;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        PipelineData output = PipelineData.createEmpty();

        iterateOverObjects(input, context, globalResult,
                (object, item, result) ->
                        test(object, output, item, context, result),
                (object, exception) ->
                        context.println("Failed to test " + object + exceptionSuffix(exception))
        );

        return output;
    }

    private void test(
            PrismObject<? extends ResourceType> object,
            PipelineData output,
            PipelineItem item,
            ExecutionContext context,
            OperationResult result)
            throws ObjectNotFoundException, ExpressionEvaluationException, SchemaException, ConfigurationException,
            SecurityViolationException, CommunicationException {
        String oid = object.getOid();
        OperationResult testResult = modelService.testResource(oid, context.getTask(), result);
        context.println("Tested " + object + ": " + testResult.getStatus());

        PrismObjectValue<ResourceType> resourceAfter =
                operationsHelper.getObject(ResourceType.class, oid, false, context, result).getValue();
        output.add(new PipelineItem(resourceAfter, item.getResult()));
    }

    @Override
    Class<ResourceType> getObjectType() {
        return ResourceType.class;
    }
}
