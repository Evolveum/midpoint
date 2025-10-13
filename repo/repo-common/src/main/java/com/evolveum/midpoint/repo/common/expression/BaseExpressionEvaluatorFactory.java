/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.Objects;

public abstract class BaseExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {

    @NotNull
    protected <T> T getSingleEvaluatorBeanRequired(Collection<JAXBElement<?>> evaluatorElements, Class<T> expectedClass,
            String contextDescription) throws SchemaException {
        return Objects.requireNonNull(getSingleEvaluatorBean(evaluatorElements, expectedClass, contextDescription),
                () -> "Evaluator definition required in " + contextDescription);
    }

    @Nullable
    protected <T> T getSingleEvaluatorBean(Collection<JAXBElement<?>> evaluatorElements, Class<T> expectedClass,
            String contextDescription) throws SchemaException {
        JAXBElement<?> evaluatorElement = MiscUtil.extractSingleton(evaluatorElements,
                        () -> new SchemaException("More than one evaluator specified in " + contextDescription));

        Object evaluatorBean = evaluatorElement != null ? evaluatorElement.getValue() : null;
        if (evaluatorBean != null && !expectedClass.isAssignableFrom(evaluatorBean.getClass())) {
            throw new SchemaException(getClass().getName() + " cannot handle elements of type " +
                    evaluatorBean.getClass().getName() + " in " + contextDescription);
        } else {
            //noinspection unchecked
            return (T) evaluatorBean;
        }
    }
}
