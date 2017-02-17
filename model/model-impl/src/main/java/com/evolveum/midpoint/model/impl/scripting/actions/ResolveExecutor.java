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

import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

/**
 * Resolves a reference, e.g. a linkRef into a set of accounts.
 *
 * @author mederly
 */
@Component
public class ResolveExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ResolveExecutor.class);

    private static final String NAME = "resolve";
    private static final String PARAM_NO_FETCH = "noFetch";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public Data execute(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {

        boolean noFetch = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_NO_FETCH, input, context, false, NAME, result);

        Data output = Data.createEmpty();

        for (PrismValue value : input.getData()) {
            context.checkTaskStop();
            if (value instanceof PrismReferenceValue) {
                PrismReferenceValue prismReferenceValue = (PrismReferenceValue) value;
                String oid = prismReferenceValue.getOid();
                QName targetTypeQName = prismReferenceValue.getTargetType();
                if (targetTypeQName == null) {
                    throw new ScriptExecutionException("Couldn't resolve reference, because target type is unknown: " + prismReferenceValue);
                }
                Class<? extends ObjectType> typeClass = (Class) prismContext.getSchemaRegistry().determineCompileTimeClass(targetTypeQName);
                if (typeClass == null) {
                    throw new ScriptExecutionException("Couldn't resolve reference, because target type class is unknown for target type " + targetTypeQName);
                }
                PrismObject<? extends ObjectType> prismObject = operationsHelper.getObject(typeClass, oid, noFetch, context, result);
                output.addValue(prismObject.getValue());
            } else {
                throw new ScriptExecutionException("Item could not be resolved, because it is not a PrismReference: " + value.toString());
            }
		}
        return output;
    }
}
