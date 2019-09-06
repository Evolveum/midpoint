/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static java.util.Collections.singleton;

/**
 * @author mederly
 */
@Component
public class ResumeExecutor extends BaseActionExecutor {

    //private static final Trace LOGGER = TraceManager.getTrace(ResumeExecutor.class);

    private static final String NAME = "resume";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue) {
                @SuppressWarnings({"unchecked", "raw"})
                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                ObjectType object = prismObject.asObjectable();
                if (object instanceof TaskType) {
                    long started = operationsHelper.recordStart(context, object);
                    Throwable exception = null;
                    try {
                        taskService.resumeTasks(singleton(object.getOid()), context.getTask(), result);
                        operationsHelper.recordEnd(context, object, started, null);
                    } catch (Throwable ex) {
                        operationsHelper.recordEnd(context, object, started, ex);
                        exception = processActionException(ex, NAME, value, context);
                    }
                    context.println((exception != null ? "Attempted to resume " : "Resumed ") + prismObject.toString() + exceptionSuffix(exception));
                } else {
                    //noinspection ThrowableNotThrown
                    processActionException(new ScriptExecutionException("Item is not a task"), NAME, value, context);
                }
            } else {
				//noinspection ThrowableNotThrown
				processActionException(new ScriptExecutionException("Item is not a PrismObject"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return input;
    }
}
