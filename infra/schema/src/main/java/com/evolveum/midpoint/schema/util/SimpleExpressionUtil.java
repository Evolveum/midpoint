/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.*;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.MidPointTrustDescriptor;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.jspecify.annotations.NullMarked;

/**
 * Very simple expression utils. More advanced ones are to be found in upper layers.
 */
@NullMarked
public class SimpleExpressionUtil {

    public static @Nullable Object getConstantIfPresent(@Nullable ExpressionType expression) {
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
    public static ExpressionType velocityExpression(String velocityTemplate, @Nullable MidPointTrustDescriptor trustDescriptor) {
        return scriptExpression(EXPRESSION_LANGUAGE_VELOCITY_URL, velocityTemplate, trustDescriptor);
    }

    /**
     * Creates {@link ExpressionType} with specified Groovy code.
     */
    public static ExpressionType groovyExpression(String groovyCode, @Nullable MidPointTrustDescriptor trustDescriptor) {
        return scriptExpression(EXPRESSION_LANGUAGE_GROOVY_URL, groovyCode, trustDescriptor);
    }

    /**
     * Creates {@link ExpressionType} with specified MEL code.
     */
    public static ExpressionType melExpression(String melCode, @Nullable MidPointTrustDescriptor trustDescriptor) {
        return scriptExpression(EXPRESSION_LANGUAGE_MEL_URL, melCode, trustDescriptor);
    }

    /**
     * Creates {@link ExpressionType} with script for specific language and with specified code.
     */
    public static ExpressionType scriptExpression(
            @Nullable String languageUrl, String code, @Nullable MidPointTrustDescriptor trustDescriptor) {
        var expressionBean = new ExpressionType().expressionEvaluator(new ObjectFactory().createScript(
                new ScriptExpressionEvaluatorType()
                        .language(languageUrl)
                        .code(code)));
        expressionBean.setTrustDescriptor(trustDescriptor);
        return expressionBean;
    }

    public static ExpressionType literalExpression(Object literalValue, @Nullable MidPointTrustDescriptor trustDescriptor) {
        var expressionBean = new ExpressionType().expressionEvaluator(
                new ObjectFactory().createValue(literalValue));
        expressionBean.setTrustDescriptor(trustDescriptor);
        return expressionBean;
    }
}
