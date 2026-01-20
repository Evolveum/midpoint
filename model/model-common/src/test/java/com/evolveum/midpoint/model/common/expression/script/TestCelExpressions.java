/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import com.evolveum.midpoint.model.common.expression.script.cel.CelScriptEvaluator;

import org.apache.commons.lang3.SystemUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
public class TestCelExpressions extends AbstractScriptTest {

    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector) {
        return new CelScriptEvaluator(prismContext, protector, localizationService);
    }

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "cel");
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
                Boolean.TRUE);
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
                Boolean.TRUE);
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
                Boolean.TRUE);
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
    public void testExpressionPolyStringEqualsOrigFuncTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-orig-func.xml",
                "testExpressionPolyStringEqualsOrigFuncTrue",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsOrigFuncFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-orig-func.xml",
                "testExpressionPolyStringEqualsOrigFuncFalse",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsOrigFieldTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-orig-field.xml",
                "testExpressionPolyStringEqualsOrigFieldTrue",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsOrigFieldFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-orig-field.xml",
                "testExpressionPolyStringEqualsOrigFieldFalse",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsNormFieldTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-norm-field.xml",
                "testExpressionPolyStringEqualsNormFieldTrue",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsNormFieldFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-norm-field.xml",
                "testExpressionPolyStringEqualsNormFieldFalse",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsNormFuncTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-norm-func.xml",
                "testExpressionPolyStringEqualsNormFuncTrue",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsNormFuncFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-norm-func.xml",
                "testExpressionPolyStringEqualsNormFuncFalse",
                createVariables(
                        "foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
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
    public void testExpressionStringMixString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix.xml",
                "testExpressionStringMixString",
                createVariables(
                        "foo", "Foo", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FOO foo!");
    }

    @Test
    public void testExpressionStringMixPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix.xml",
                "testExpressionStringMixPolyString",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FOO foo!");
    }

    @Test
    public void testExpressionStringConcatNameString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concatname.xml",
                "testExpressionStringConcatNameString",
                createVariables(
                        "foo", "Foo", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "Foo BAR");
    }

    @Test
    public void testExpressionStringConcatNamePolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concatname.xml",
                "testExpressionStringConcatNamePolyString",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", PrismTestUtil.createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                "Foo BAR");
    }

    @Test
    public void testExpressionStringConcatNamePolyStringMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concatname.xml",
                "testExpressionStringConcatNamePolyStringMix",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "Foo BAR");
    }

    @Test
    public void testExpressionStringConcatString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concat.xml",
                "testExpressionStringConcatString",
                createVariables(
                        "foo", "Foo", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FooBAR");
    }

    @Test
    public void testExpressionStringConcatPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concat.xml",
                "testExpressionStringConcatPolyString",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrismTestUtil.createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                "FooBAR");
    }

    @Test
    public void testExpressionStringConcatPolyStringMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concat.xml",
                "testExpressionStringConcatPolyStringMix",
                createVariables(
                        PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FooBAR");
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
                    "testSyntaxError",
                    createPoisonVariables(poison),
                    RESULT_POISON_OK);

        } catch (ExpressionEvaluationException e) {
            // THEN
            assertTrue("Unexpected exception message" + e.getMessage(), e.getMessage().contains("Unexpected input"));
        }

    }

}
