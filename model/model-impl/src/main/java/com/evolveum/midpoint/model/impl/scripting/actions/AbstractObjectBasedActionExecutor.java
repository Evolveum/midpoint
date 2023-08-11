/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * Action executor that can iterates over set of PrismObjects of a specified (expected) type.
 */
abstract class AbstractObjectBasedActionExecutor<T extends ObjectType> extends BaseActionExecutor {

    @FunctionalInterface
    public interface ObjectProcessor<T extends ObjectType> {
        void process(PrismObject<? extends T> object, PipelineItem item, OperationResult result) throws CommonException;
    }

    @FunctionalInterface
    public interface ConsoleFailureMessageWriter<T extends ObjectType> {
        void write(PrismObject<? extends T> object, @NotNull Throwable exception);
    }

    abstract Class<T> getObjectType();

    void iterateOverObjects(PipelineData input, ExecutionContext context, OperationResult globalResult,
            ObjectProcessor<T> consumer, ConsoleFailureMessageWriter<T> writer)
            throws ScriptExecutionException {
        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, globalResult);
            try {
                context.checkTaskStop();
                PrismObject<T> object = castToObject(value, getObjectType(), context);
                if (object != null) {
                    T objectable = object.asObjectable();
                    Operation op = operationsHelper.recordStart(context, objectable);
                    try {
                        consumer.process(object, item, result);
                        operationsHelper.recordEnd(context, op, null, result);
                    } catch (Throwable e) {
                        result.recordException(e);
                        operationsHelper.recordEnd(context, op, e, result);
                        Throwable exception = processActionException(e, getLegacyActionName(), value, context);
                        writer.write(object, exception);
                    }
                }
                operationsHelper.trimAndCloneResult(result, item.getResult());
            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close(); // just in case (should be already computed)
            }
        }
    }

    @SuppressWarnings("ThrowableNotThrown")
    private PrismObject<T> castToObject(PrismValue value, Class<T> expectedType, ExecutionContext context)
            throws ScriptExecutionException {
        if (value instanceof PrismObjectValue<?> objectValue) {
            Class<? extends Objectable> realType = objectValue.asObjectable().getClass();
            if (expectedType.isAssignableFrom(realType)) {
                //noinspection unchecked
                return (PrismObject<T>) objectValue.asPrismObject();
            } else {
                processActionException(
                        new ScriptExecutionException(
                                "Item is not a PrismObject of %s; it is %s instead".formatted(
                                        expectedType.getName(), realType.getName())),
                        getLegacyActionName(), value, context);
                return null;
            }
        } else {
            processActionException(new ScriptExecutionException("Item is not a PrismObject"), getLegacyActionName(), value, context);
            return null;
        }
    }
}
