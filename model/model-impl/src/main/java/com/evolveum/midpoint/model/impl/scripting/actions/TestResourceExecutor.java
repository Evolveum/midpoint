/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class TestResourceExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(TestResourceExecutor.class);

    private static final String NAME = "test-resource";

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
            if (value instanceof PrismObjectValue && ((PrismObjectValue) value).asObjectable() instanceof ResourceType) {
                PrismObject<ResourceType> resourceTypePrismObject = ((PrismObjectValue) value).asPrismObject();
                ResourceType resourceType = resourceTypePrismObject.asObjectable();
                long started = operationsHelper.recordStart(context, resourceType);
                Throwable exception = null;
                OperationResult testResult;
                try {
                    testResult = modelService.testResource(resourceTypePrismObject.getOid(), context.getTask());
                    operationsHelper.recordEnd(context, resourceType, started, null);
                } catch (ObjectNotFoundException|RuntimeException e) {
                    operationsHelper.recordEnd(context, resourceType, started, e);
					exception = processActionException(e, NAME, value, context);
					testResult = new OperationResult(TestResourceExecutor.class.getName() + ".testResource");
					testResult.recordFatalError(e);
                }
                result.addSubresult(testResult);
                context.println("Tested " + resourceTypePrismObject + ": " + testResult.getStatus() + exceptionSuffix(exception));
                try {
					PrismObjectValue<ResourceType> resourceValue = operationsHelper.getObject(ResourceType.class, resourceTypePrismObject.getOid(), false, context, result).getValue();
					output.add(new PipelineItem(resourceValue, item.getResult()));
				} catch (ExpressionEvaluationException e) {
					throw new ScriptExecutionException("Error getting resource "+resourceTypePrismObject.getOid()+": "+e.getMessage(), e);
				}
            } else {
				//noinspection ThrowableNotThrown
				processActionException(new ScriptExecutionException("Item is not a PrismObject<ResourceType>"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return output;
    }
}
