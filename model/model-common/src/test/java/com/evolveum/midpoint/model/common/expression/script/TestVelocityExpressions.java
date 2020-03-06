/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.model.common.expression.script.velocity.VelocityScriptEvaluator;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Radovan Semancik
 * @author Pavol Mederly
 */
public class TestVelocityExpressions extends AbstractScriptTest {

    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector) {
        return new VelocityScriptEvaluator(prismContext, protector, localizationService);
    }

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "velocity");
    }

    @Test
    public void testExpressionList() throws Exception {
        evaluateAndAssertStringScalarExpression(        // velocity has no support for output other than String
                "expression-list.xml",
                "testExpressionList",
                createVariables(
                        "jack",
                        MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE),
                        prismContext.definitionFactory()
                            .createReferenceDefinition(UserType.F_PERSONA_REF, UserType.COMPLEX_TYPE)
                ),
                "[Leaders, Followers]");
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
                Boolean.TRUE);            // velocity calls '==' on toString value
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
                Boolean.TRUE);            // velocity calls '==' on toString value
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
                Boolean.TRUE);            // velocity calls '==' on toString value
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
                Boolean.TRUE);            // velocity calls '==' on toString value
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

}
