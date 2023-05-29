/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE;

import jakarta.xml.bind.JAXBElement;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * Very simple expression utils. More advanced ones are to be found in upper layers.
 *
 * EXPERIMENTAL. Later will be reconsidered.
 */
@Experimental
public class SimpleExpressionUtil {

    public static Object getConstantIfPresent(ExpressionType expression) {
        if (expression == null || expression.getExpressionEvaluator().size() != 1) {
            return null;
        }
        JAXBElement<?> jaxb = expression.getExpressionEvaluator().get(0);
        if (QNameUtil.match(jaxb.getName(), SchemaConstants.C_VALUE)) {
            return jaxb.getValue();
        } else {
            return null;
        }
    }

    /**
     * Creates {@link ExpressionType} for specified Velocity template.
     */
    public static ExpressionType velocityExpression(String velocityTemplate) {
        return scriptExpression(EXPRESSION_LANGUAGE_URL_BASE + "velocity", velocityTemplate);
    }

    /**
     * Creates {@link ExpressionType} with specified Groovy code.
     */
    public static ExpressionType groovyExpression(String groovyCode) {
        return scriptExpression(EXPRESSION_LANGUAGE_URL_BASE + "Groovy", groovyCode);
    }

    /**
     * Creates {@link ExpressionType} with script for specific language and with specified code.
     */
    public static ExpressionType scriptExpression(String languageUrl, String code) {
        return new ExpressionType().expressionEvaluator(new ObjectFactory().createScript(
                new ScriptExpressionEvaluatorType()
                        .language(languageUrl)
                        .code(code)));
    }

    public static ExpressionType literalExpression(Object literalValue) {
        return new ExpressionType().expressionEvaluator(
                new ObjectFactory().createValue(literalValue));
    }
}
