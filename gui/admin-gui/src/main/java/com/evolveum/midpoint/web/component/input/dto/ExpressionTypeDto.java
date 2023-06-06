/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input.dto;

import java.io.Serializable;
import java.util.Objects;
import jakarta.xml.bind.JAXBElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author shood
 */
public class ExpressionTypeDto implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionTypeDto.class);

    public static final String F_DESCRIPTION = "description";
    public static final String F_TYPE = "type";
    public static final String F_LANGUAGE = "language";
    public static final String F_POLICY_REF = "policyRef";
    public static final String F_EXPRESSION = "expression";

    private ExpressionUtil.ExpressionEvaluatorType type;
    private ExpressionUtil.Language language;
    private ObjectReferenceType policyRef;
    private String expression;
    @NotNull private final ExpressionType expressionObject;

    public ExpressionTypeDto(@Nullable ExpressionType expression, @NotNull PrismContext prismContext) {
        if (expression != null) {
            expressionObject = expression;
        } else {
            expressionObject = new ExpressionType();
        }
        if (!expressionObject.getExpressionEvaluator().isEmpty()) {
            loadExpression(prismContext);
        }
    }

    private void loadExpression(PrismContext context) {
        try {
            if (expressionObject.getExpressionEvaluator().size() == 1) {
                expression = context.xmlSerializer().serialize(expressionObject.getExpressionEvaluator().get(0));
            } else {
                StringBuilder sb = new StringBuilder();

                for (JAXBElement<?> element : expressionObject.getExpressionEvaluator()) {
                    String subElement = context.xmlSerializer().serialize(element);
                    sb.append(subElement).append("\n");
                }
                expression = sb.toString();
            }

            type = ExpressionUtil.getExpressionType(expression);
            if (type != null && type.equals(ExpressionUtil.ExpressionEvaluatorType.SCRIPT)) {
                language = ExpressionUtil.getExpressionLanguage(expression);
            }

            //TODO - add algorithm to determine objectReferenceType from String expression
        } catch (SchemaException e) {
            //TODO - how can we show this error to user?
            LoggingUtils.logUnexpectedException(LOGGER, "Could not load expressions from ExpressionType.", e);
            expression = e.getMessage();
        }
    }

    public void updateExpression(PrismContext context) throws SchemaException, IllegalArgumentException {
        ExpressionUtil.parseExpressionEvaluators(expression, expressionObject, context);
    }

    public void updateExpressionType() {
        expression = ExpressionUtil.getExpressionString(type);
    }

    public void updateExpressionLanguage() {
        expression = ExpressionUtil.getExpressionString(type, language);
    }

    public void updateExpressionValuePolicyRef() {
        expression = ExpressionUtil.getExpressionString(type, policyRef);
    }

    public ExpressionUtil.ExpressionEvaluatorType getType() {
        return type;
    }

    public void setType(ExpressionUtil.ExpressionEvaluatorType type) {
        this.type = type;
    }

    public ExpressionUtil.Language getLanguage() {
        return language;
    }

    public void setLanguage(ExpressionUtil.Language language) {
        this.language = language;
    }

    public ObjectReferenceType getPolicyRef() {
        return policyRef;
    }

    public void setPolicyRef(ObjectReferenceType policyRef) {
        this.policyRef = policyRef;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @NotNull
    public ExpressionType getExpressionObject() {
        return expressionObject;
    }

    public String getDescription() {
        return expressionObject.getDescription();
    }

    public void setDescription(String description) {
        expressionObject.setDescription(description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpressionTypeDto)) {
            return false;
        }

        ExpressionTypeDto that = (ExpressionTypeDto) o;
        return Objects.equals(expression, that.expression)
                && Objects.equals(expressionObject, that.expressionObject)
                && Objects.equals(language, that.language)
                && Objects.equals(policyRef, that.policyRef)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, expressionObject, language, policyRef, type);
    }
}
