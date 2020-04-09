/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.expression.script.groovy.GroovyScriptEvaluator;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
public class TestGroovyExpressions extends AbstractScriptTest {

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#createEvaluator()
     */
    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector) {
        return new GroovyScriptEvaluator(prismContext, protector, localizationService);
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#getTestDir()
     */
    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "groovy");
    }

    @Test
    public void testExpressionPolyStringEquals101() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-1.xml",
                "testExpressionPolyStringEquals101",
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
                "testExpressionPolyStringEquals102",
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
                "testExpressionPolyStringEquals111",
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
                "testExpressionPolyStringEquals112",
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
                "testExpressionPolyStringEquals121",
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
                "testExpressionPolyStringEquals122",
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
                "testExpressionPolyStringEquals201",
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
                "testExpressionPolyStringEquals202",
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
                "testExpressionPolyStringEquals211",
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
                "testExpressionPolyStringEquals212",
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
                "testExpressionPolyStringEquals221",
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
                "testExpressionPolyStringEquals222",
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
                "testExpressionPolyStringEqualsStringify101",
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
                "testExpressionPolyStringEqualsStringify102",
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
                "testExpressionPolyStringEqualsStringify111",
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
                "testExpressionPolyStringEqualsStringify112",
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
                "testExpressionPolyStringEqualsStringify121",
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
                "testExpressionPolyStringEqualsStringify122",
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
                "testExpressionPolyStringEqualsStringify201",
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
                "testExpressionPolyStringEqualsStringify202",
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
                "testExpressionPolyStringEqualsStringify211",
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
                "testExpressionPolyStringEqualsStringify212",
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
                "testExpressionPolyStringEqualsStringify221",
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
                "testExpressionPolyStringEqualsStringify222",
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
                "testLookAtPoison",
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
                "testSmellPoison",
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
                "testSmellPoisonTricky",
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
                "testSmellPoisonDynamic",
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
                "testSmellPoisonVeryDynamic",
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
                "testSmellPoisonReflection",
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
                    "testDrinkPoison",
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

    protected ExpressionVariables createPoisonVariables(Poison poison) {
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
                    "testSyntaxError",
                    createPoisonVariables(poison),
                    RESULT_POISON_OK);

        } catch (ExpressionEvaluationException e) {
            // THEN
            assertTrue("Unexpected exception message" + e.getMessage(), e.getMessage().contains("unexpected token"));
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
                "testStringExec",
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
                "testListExec",
                null,
                RESULT_STRING_EXEC);

        // THEN

    }
}
