/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util.expression;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.jetbrains.annotations.NotNull;

/**
 * Various methods to assist in creating (maybe later parsing?) objects of {@link ExpressionType}.
 */
public class ExpressionTypeUtil {

    public static @NotNull ExpressionType forGroovyCode(String code) {
        ExpressionType expression = new ExpressionType();
        expression.getExpressionEvaluator().add(
                new ObjectFactory().createScript(
                        new ScriptExpressionEvaluatorType().code(code)));
        return expression;
    }

    public static @NotNull ExpressionType forValue(Object value) {
        return new ExpressionType()
                .expressionEvaluator(new ObjectFactory().createValue(value));
    }
}
