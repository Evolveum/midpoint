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
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class PurgeSchemaExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(PurgeSchemaExecutor.class);

    private static final String NAME = "purge-schema";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        PipelineData output = PipelineData.createEmpty();

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue && ((PrismObjectValue<Objectable>) value).asObjectable() instanceof ResourceType) {
                PrismObject<ResourceType> resourceTypePrismObject = ((PrismObjectValue) value).asPrismObject();
                ResourceType resourceType = resourceTypePrismObject.asObjectable();
                long started = operationsHelper.recordStart(context, resourceType);
                ObjectDelta delta = createDelta(resourceTypePrismObject.asObjectable());
                try {
                    if (delta != null) {
                        operationsHelper.applyDelta(delta, ModelExecuteOptions.createRaw(), context, result);
                        context.println("Purged schema information from " + resourceTypePrismObject);
                        output.addValue(operationsHelper.getObject(ResourceType.class, resourceTypePrismObject.getOid(), true, context, result).getValue(), item.getResult(), item.getVariables());
                    } else {
                        context.println("There's no schema information to be purged in " + value);
                        output.addValue(resourceTypePrismObject.getValue(), item.getResult(), item.getVariables());
                    }
                    operationsHelper.recordEnd(context, resourceType, started, null);
                } catch (Throwable ex) {
                    operationsHelper.recordEnd(context, resourceType, started, ex);
					Throwable exception = processActionException(ex, NAME, value, context);
					context.println("Couldn't purge schema information from " + resourceTypePrismObject + exceptionSuffix(exception));
                }
            } else {
				//noinspection ThrowableNotThrown
				processActionException(new ScriptExecutionException("Item is not a PrismObject<ResourceType>"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return output;
    }

    private ObjectDelta createDelta(ResourceType resourceType) throws ScriptExecutionException {
        PrismContainer<XmlSchemaType> schemaContainer = resourceType.asPrismObject().findContainer(ResourceType.F_SCHEMA);
        if (schemaContainer == null || schemaContainer.isEmpty()) {
            return null;
        }
        return prismContext.deltaFactory().object().createModificationDeleteContainer(
                ResourceType.class,
                resourceType.getOid(),
                ResourceType.F_SCHEMA,
                schemaContainer.getValue().clone());
    }
}
