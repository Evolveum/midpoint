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

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class AddExecutor extends BaseActionExecutor {

    private static final String NAME = "add";

    @Autowired
    private OperationsHelper operationsHelper;

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public Data execute(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {

        boolean raw = getParamRaw(expression, input, context, result);
        boolean dryRun = getParamDryRun(expression, input, context, result);

        for (PrismValue value : input.getData()) {
            context.checkTaskStop();
            if (value instanceof PrismObjectValue) {
                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                ObjectType objectType = prismObject.asObjectable();
                long started = operationsHelper.recordStart(context, objectType);
                Throwable exception = null;
                try {
                    operationsHelper.applyDelta(createAddDelta(objectType), operationsHelper.createExecutionOptions(raw), dryRun, context, result);
                    operationsHelper.recordEnd(context, objectType, started, null);
                } catch (Throwable ex) {
                    operationsHelper.recordEnd(context, objectType, started, ex);
                    exception = processActionException(ex, NAME, value, context);
                }
				context.println((exception != null ? "Attempted to add " : "Added ") + prismObject.toString() + rawDrySuffix(raw, dryRun) + exceptionSuffix(exception));
            } else {
				//noinspection ThrowableNotThrown
				processActionException(new ScriptExecutionException("Item is not a PrismObject"), NAME, value, context);
            }
        }
        return Data.createEmpty();            // todo return oid(s) in the future
    }

	private ObjectDelta createAddDelta(ObjectType objectType) {
        return ObjectDelta.createAddDelta(objectType.asPrismObject());
    }
}
