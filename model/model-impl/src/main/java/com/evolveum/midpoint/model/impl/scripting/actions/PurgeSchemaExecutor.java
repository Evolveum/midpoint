/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        return ObjectDelta.createModificationDeleteContainer(
                ResourceType.class,
                resourceType.getOid(),
                ResourceType.F_SCHEMA,
                prismContext,
                schemaContainer.getValue().clone());
    }
}
