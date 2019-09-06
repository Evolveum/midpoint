/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.PipelineItem;
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
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        boolean noFetch = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_NO_FETCH, input, context, false, NAME, globalResult);

        PipelineData output = PipelineData.createEmpty();

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismReferenceValue) {
                PrismReferenceValue prismReferenceValue = (PrismReferenceValue) value;
                String oid = prismReferenceValue.getOid();
                QName targetTypeQName = prismReferenceValue.getTargetType();
                if (targetTypeQName == null) {
                    throw new ScriptExecutionException("Couldn't resolve reference, because target type is unknown: " + prismReferenceValue);
                }
                Class<? extends ObjectType> typeClass = prismContext.getSchemaRegistry().determineCompileTimeClass(targetTypeQName);
                if (typeClass == null) {
                    throw new ScriptExecutionException("Couldn't resolve reference, because target type class is unknown for target type " + targetTypeQName);
                }
                try {
                    PrismObjectValue<? extends ObjectType> resolved = operationsHelper.getObject(typeClass, oid, noFetch, context, result).getValue();
                    output.add(new PipelineItem(resolved, item.getResult()));
                } catch (Throwable e) {
                    //noinspection ThrowableNotThrown
                    processActionException(e, NAME, value, context);
                    output.add(item);       // to keep track of failed item (may trigger exceptions downstream)
                }
            } else {
                //noinspection ThrowableNotThrown
                processActionException(new ScriptExecutionException("Item is not a PrismReference"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
		}
        return output;
    }
}
