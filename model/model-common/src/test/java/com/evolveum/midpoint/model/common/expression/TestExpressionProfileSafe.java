/*
 * Copyright (c) 2013-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import java.util.Collection;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * @author Radovan Semancik
 */
public class TestExpressionProfileSafe extends TestExpression {

    @Override
    protected String getExpressionProfileName() {
        return EXPRESSION_PROFILE_SAFE_NAME;
    }

    @Test
    @Override
    public void test130Const() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_CONST_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        ExpressionVariables variables = prepareBasicVariables();
        ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), null);

        // WHEN
        evaluatePropertyExpressionRestricted(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN

        assertScriptExecutionIncrement(0);
    }

    @Test
    @Override
    public void test154ScriptGroovySystemDeny() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_GROOVY_SYSTEM_DENY_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        ExpressionVariables variables = prepareBasicVariables();
        ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), null);

        // WHEN
        evaluatePropertyExpressionRestricted(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN

        assertScriptExecutionIncrement(0);
    }

    @Test
    @Override
    public void test160ScriptJavaScript() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_JAVASCRIPT_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        ExpressionVariables variables = prepareBasicVariables();
        ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), null);

        // WHEN
        evaluatePropertyExpressionRestricted(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN

        assertScriptExecutionIncrement(0);
    }
}
