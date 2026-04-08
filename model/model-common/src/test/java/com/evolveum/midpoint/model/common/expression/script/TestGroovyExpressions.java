/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.Clock;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.internals.InternalMonitor;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.apache.commons.lang3.SystemUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.expression.script.groovy.GroovyScriptEvaluator;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
public class TestGroovyExpressions extends AbstractScriptTest {

    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector, Clock clock) {
        return new GroovyScriptEvaluator(prismContext, protector, localizationService);
    }

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "groovy");
    }

    @Test
    public void testExpressionPolyStringEquals101() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-1.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEquals102() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-1.xml",
                createVariables(
                        "foo", "FOOBAR", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEquals111() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                // Only true for midPoint 4.0.1 and later. Older groovy did not process Groovy operator == in the same way.
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEquals112() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEquals121() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEquals122() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEquals201() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-2.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEquals202() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-2.xml",
                createVariables(
                        "foo", "FOOBAR", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEquals211() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEquals212() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEquals221() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEquals222() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify101() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-1.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify102() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-1.xml",
                createVariables(
                        "foo", "FOOBAR", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify111() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify112() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify121() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify122() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify201() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-2.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify202() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-2.xml",
                createVariables(
                        "foo", "FOOBAR", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify211() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify212() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify221() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify222() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testLookAtPoison() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpression(
                "expression-poison-look.xml",
                createPoisonVariables(poison),
                RESULT_POISON_OK);

        // THEN
        poison.assertLookedAt();
    }

    /**
     * This should pass here. There are no restrictions about script execution here.
     */
    @Test
    public void testSmellPoison() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpression(
                "expression-poison-smell.xml",
                createPoisonVariables(poison),
                RESULT_POISON_OK);

        // THEN
        poison.assertSmelled();
    }

    /**
     * Tricky way to smell poison. It should pass here.
     */
    @Test
    public void testSmellPoisonTricky() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpression(
                "expression-poison-smell-tricky.xml",
                createPoisonVariables(poison),
                RESULT_POISON_OK);

        // THEN
        poison.assertSmelled();

    }

    /**
     * Attempt to smell poison by using dynamic invocation.
     */
    @Test
    public void testSmellPoisonDynamic() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpression(
                "expression-poison-smell-dynamic.xml",
                createPoisonVariables(poison),
                RESULT_POISON_OK);

        // THEN
        poison.assertSmelled();

    }

    /**
     * Attempt to smell poison by using a very dynamic invocation.
     */
    @Test
    public void testSmellPoisonVeryDynamic() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpression(
                "expression-poison-smell-very-dynamic.xml",
                createPoisonVariables(poison),
                RESULT_POISON_OK);

        // THEN
        poison.assertSmelled();

    }

    /**
     * Attempt to smell poison by using reflection
     */
    @Test
    public void testSmellPoisonReflection() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpression(
                "expression-poison-smell-reflection.xml",
                createPoisonVariables(poison),
                RESULT_POISON_OK);

        // THEN
        poison.assertSmelled();

    }

    /**
     * This should pass here. There are no restrictions about script execution here.
     * By passing we mean throwing an error ...
     */
    @Test
    public void testDrinkPoison() throws Exception {
        Poison poison = new Poison();

        // WHEN
        try {
            evaluateAndAssertStringScalarExpression(
                    "expression-poison-drink.xml",
                    createPoisonVariables(poison),
                    "");

            AssertJUnit.fail("Unexpected success");

        } catch (ExpressionEvaluationException ex) {
            // THEN
            assertTrue("Expected that exception message will contain " + Poison.POISON_DRINK_ERROR_MESSAGE +
                    ", but it did not. It was: " + ex.getMessage(), ex.getMessage().contains(Poison.POISON_DRINK_ERROR_MESSAGE));
            Error error = (Error) ex.getCause();
            assertEquals("Wrong error message", Poison.POISON_DRINK_ERROR_MESSAGE, error.getMessage());
        }

    }

    protected VariablesMap createPoisonVariables(Poison poison) {
        return createVariables(
                VAR_POISON, poison, Poison.class);
    }

    /**
     * Make sure that there is a meaningful error - even if sandbox is applied.
     */
    @Test
    public void testSyntaxError() throws Exception {
        Poison poison = new Poison();

        // WHEN
        try {
            evaluateAndAssertStringScalarExpression(
                    "expression-syntax-error.xml",
                    createPoisonVariables(poison),
                    RESULT_POISON_OK);

        } catch (ExpressionEvaluationException e) {
            // THEN
            assertTrue("Unexpected exception message" + e.getMessage(), e.getMessage().contains("Unexpected input"));
        }

    }

    /**
     * Almighty script can execute a process from string.
     */
    @Test
    public void testStringExec() throws Exception {
        skipTestIf(SystemUtils.IS_OS_WINDOWS, "'echo' used in script is not available Windows");

        // WHEN
        evaluateAndAssertStringScalarExpression(
                "expression-string-exec.xml",
                null,
                RESULT_STRING_EXEC);

        // THEN

    }

    /**
     * Almighty script can execute a process from list.
     */
    @Test
    public void testListExec() throws Exception {
        skipTestIf(SystemUtils.IS_OS_WINDOWS, "'echo' used in script is not available Windows");

        // WHEN
        evaluateAndAssertStringScalarExpression(
                "expression-list-exec.xml",
                null,
                RESULT_STRING_EXEC);

        // THEN

    }

    @Test
    public void testCachingGetExtensionPropertyValue() throws Exception {
        // GIVEN

        // We need to start with a clean slate
        initializeScriptEvaluator();
        InternalMonitor.reset();


        assertScriptMonitor(0, 0, "init");

        // WHEN, THEN
        long etimeFirst = executeCachingScript("expression-string-variables.xml", "FOOBAR", "first");
        assertScriptMonitor(1, 1, "first");

        long etimeSecond = executeCachingScript("expression-string-variables.xml", "FOOBAR", "second");
        assertScriptMonitor(1, 2, "second");
        assertTrue("Einstein was wrong! " + etimeFirst + " -> " + etimeSecond, etimeSecond <= etimeFirst);

        long etimeThird = executeCachingScript("expression-string-variables.xml", "FOOBAR", "second");
        assertScriptMonitor(1, 3, "third");
        assertTrue("Einstein was wrong again! " + etimeFirst + " -> " + etimeThird, etimeThird <= etimeFirst);

        // Different script. Should compile.
        long horatio1Time = executeCachingScript("expression-func-concatname.xml", "Horatio Torquemada Marley", "horatio");
        assertScriptMonitor(2, 4, "horatio");

        // Same script. No compilation.
        long etimeFourth = executeCachingScript("expression-string-variables.xml", "FOOBAR", "fourth");
        assertScriptMonitor(2, 5, "fourth");
        assertTrue("Einstein was wrong all the time! " + etimeFirst + " -> " + etimeFourth, etimeFourth <= etimeFirst);

        // Try this again. No compile.
        long horatio2Time = executeCachingScript("expression-func-concatname.xml", "Horatio Torquemada Marley", "horatio2");
        assertScriptMonitor(2, 6, "horatio2");
        assertTrue("Even Horatio was wrong! " + horatio1Time + " -> " + horatio2Time, horatio2Time <= horatio1Time);
    }

    private long executeCachingScript(String filname, String expectedResult, String desc)
            throws SchemaException, SecurityViolationException, ExpressionEvaluationException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, IOException {
        // GIVEN
        OperationResult result = createOperationResult(desc);
        ScriptExpressionEvaluatorType scriptType = parseScriptType(filname);
        ItemDefinition<?> outputDefinition =
                getPrismContext().definitionFactory().newPropertyDefinition(PROPERTY_NAME, DOMUtil.XSD_STRING);

        ScriptExpression scriptExpression = createCachingScriptExpression(scriptType, outputDefinition);

        VariablesMap variables = VariablesMap.create(getPrismContext(),
                "foo", "FOO", PrimitiveType.STRING,
                "bar", "BAR", PrimitiveType.STRING
        );

        // WHEN
        long startTime = System.currentTimeMillis();

        ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext();
        context.setVariables(variables);
        context.setEvaluateNew(false);
        context.setScriptExpression(scriptExpression);
        context.setContextDescription(desc);
        context.setResult(result);

        List<PrismPropertyValue<String>> scripResults = scriptExpression.evaluate(context);
        long endTime = System.currentTimeMillis();

        // THEN
        displayValue(
                "Script results " + desc + ", etime: " + (endTime - startTime) + " ms",
                scripResults);

        String scriptResult = asScalarString(scripResults);
        assertEquals("Wrong script " + desc + " result", expectedResult, scriptResult);

        return (endTime - startTime);
    }

    private ScriptExpression createCachingScriptExpression(
            ScriptExpressionEvaluatorType expressionType, ItemDefinition<?> outputDefinition) {
        ScriptExpression expression = new ScriptExpression(
                scriptExpressionfactory.getEvaluatorSimple(expressionType.getLanguage()),
                expressionType);
        expression.setOutputDefinition(outputDefinition);
        expression.setObjectResolver(scriptExpressionfactory.getObjectResolver());
        expression.setFunctionLibraryBindings(new ArrayList<>(scriptExpressionfactory.getBuiltInLibraryBindings()));
        return expression;
    }

    private String asScalarString(List<PrismPropertyValue<String>> expressionResultList) {
        if (expressionResultList.size() > 1) {
            AssertJUnit.fail("Expression produces a list of " + expressionResultList.size() + " while only expected a single value: " + expressionResultList);
        }
        if (expressionResultList.isEmpty()) {
            return null;
        }
        return expressionResultList.iterator().next().getValue();
    }


}
