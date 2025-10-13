/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * Action executor that can iterates over set of PrismObjects of a specified (expected) type.
 */
abstract class AbstractObjectBasedActionExecutor<T extends ObjectType> extends BaseActionExecutor {

    @FunctionalInterface
    public interface ObjectProcessor<T extends ObjectType> {
        void process(PrismObject<? extends T> object, PipelineItem item, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
                PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
    }

    @FunctionalInterface
    public interface ConsoleFailureMessageWriter<T extends ObjectType> {
        void write(PrismObject<? extends T> object, @NotNull Throwable exception);
    }

    abstract Class<T> getObjectType();

    void iterateOverObjects(
            PipelineData input, ExecutionContext context, OperationResult globalResult, ObjectProcessor<T> consumer,
            ConsoleFailureMessageWriter<T> writer)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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
                        Throwable exception = logOrRethrowActionException(e, value, context);
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
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (value instanceof PrismObjectValue<?> objectValue) {
            Class<? extends Objectable> realType = objectValue.asObjectable().getClass();
            if (expectedType.isAssignableFrom(realType)) {
                //noinspection unchecked
                return (PrismObject<T>) objectValue.asPrismObject();
            } else {
                logOrRethrowActionException(
                        new UnsupportedOperationException(
                                "Item is not a PrismObject of %s; it is %s instead".formatted(
                                        expectedType.getName(), realType.getName())),
                        value, context);
                return null;
            }
        } else {
            logOrRethrowActionException(new UnsupportedOperationException("Item is not a PrismObject"), value, context);
            return null;
        }
    }
}
