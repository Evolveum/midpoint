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
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class RecomputeExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(RecomputeExecutor.class);

    private static final String NAME = "recompute";

    @Autowired
    private ContextFactory contextFactory;

    @Autowired
    private Clockwork clockwork;

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        boolean dryRun = getParamDryRun(expression, input, context, globalResult);

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue && FocusType.class.isAssignableFrom(((PrismObjectValue) value).asPrismObject().getCompileTimeClass())) {
                PrismObject<FocusType> focalPrismObject = ((PrismObjectValue) value).asPrismObject();
                FocusType focusType = focalPrismObject.asObjectable();
                long started = operationsHelper.recordStart(context, focusType);
                Throwable exception = null;
                try {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Recomputing object {} with dryRun={}", focalPrismObject, dryRun);
                    }
                    ObjectDelta<? extends FocusType> emptyDelta = ObjectDelta.createEmptyDelta(focusType.getClass(), focusType.getOid(), prismContext, ChangeType.MODIFY);
                    operationsHelper.applyDelta(emptyDelta, ModelExecuteOptions.createReconcile(), dryRun, context, result);
                    LOGGER.trace("Recomputing of object {}: {}", focalPrismObject, result.getStatus());
                    operationsHelper.recordEnd(context, focusType, started, null);
                } catch (Throwable e) {
                    operationsHelper.recordEnd(context, focusType, started, e);
					exception = processActionException(e, NAME, value, context);
                }
                context.println((exception != null ? "Attempted to recompute " : "Recomputed ") + focalPrismObject.toString() + drySuffix(dryRun) + exceptionSuffix(exception));
            } else {
				//noinspection ThrowableNotThrown
				processActionException(new ScriptExecutionException("Item is not a PrismObject<FocusType>"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return input;
    }
}
