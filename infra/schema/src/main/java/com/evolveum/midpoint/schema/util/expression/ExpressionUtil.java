/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.expression;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.jetbrains.annotations.NotNull;

/**
 * Various methods to assist in creating (maybe later parsing?) objects of {@link ExpressionType}.
 */
public class ExpressionUtil {

    public static @NotNull ExpressionType forGroovyCode(String code) {
        ExpressionType expression = new ExpressionType();
        expression.getExpressionEvaluator().add(
                new ObjectFactory().createScript(
                        new ScriptExpressionEvaluatorType().code(code)));
        return expression;
    }
}
