/*
 * Copyright (C) 2013-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import java.util.Collection;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.expression.VariablesMap;
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
        given();
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_CONST_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        when();
        evaluatePropertyExpressionRestricted(expressionType, PrimitiveType.STRING, expressionContext, result);

        then();
        assertScriptExecutionIncrement(0);
    }

    @Test
    @Override
    public void test154ScriptGroovySystemDeny() throws Exception {
        given();
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_GROOVY_SYSTEM_DENY_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        when();
        evaluatePropertyExpressionRestricted(expressionType, PrimitiveType.STRING, expressionContext, result);

        then();
        assertScriptExecutionIncrement(0);
    }

    // test156 is skipped

    @Test
    @Override
    public void test160ScriptJavaScript() throws Exception {
        skipIfEcmaScriptEngineNotSupported();
        given();
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_JAVASCRIPT_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        when();
        evaluatePropertyExpressionRestricted(expressionType, PrimitiveType.STRING, expressionContext, result);

        then();
        assertScriptExecutionIncrement(0);
    }
}
