/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.model.common.expression.functions.LogExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.MelScriptEvaluator;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.collect.ImmutableList;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author Radovan Semancik
 */
public class TestMelExpressions extends AbstractScriptTest {

    private static final String FULL_NAME_RS = "Ing. Radovan \"Gildir\" Semančík, PhD.";

    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector, Clock clock) {
        FunctionLibraryBinding basicFunctionLibraryBinding = FunctionLibraryUtil.createBasicFunctionLibraryBinding(prismContext, protector, clock);
        return new MelScriptEvaluator(prismContext, protector, localizationService, (BasicExpressionFunctions) basicFunctionLibraryBinding.getImplementation());
    }

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "mel");
    }

    @Test
    public void testUserGivenNameMap() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-given-name-map.xml",
                createUserScriptVariables(),
                "Jack");
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
                Boolean.TRUE);
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
                Boolean.TRUE);
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
                Boolean.TRUE);
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
    public void testExpressionPolyStringEqualsStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "FOO", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "Bar", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionStringEqualsPolyStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionStringEqualsPolyStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", PrismTestUtil.createPolyStringType("Bar"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsPolyStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsPolyStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", PrismTestUtil.createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.FALSE);
    }


    @Test
    public void testExpressionPolyStringEqualsOrigFieldTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-orig-field.xml",
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
    public void testExpressionPolyStringEqualsStringNative() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-string-native.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionStringNormPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-norm.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType(" FoôBÁR"), PolyStringType.COMPLEX_TYPE
                ),
                "foobar");
    }

    /**
     * Control test, making sure stock CEL interprets the expression in the same way using plain strings.
     */
    @Test
    public void testExpressionStringNormString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-norm.xml",
                createVariables(
                        "foo", " FoôBÁR", PrimitiveType.STRING
                ),
                "foobar");
    }

    @Test
    public void testExpressionStringAsciiPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-ascii.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType(" FoôBÁR"), PolyStringType.COMPLEX_TYPE
                ),
                " FooBAR");
    }

    /**
     * Control test, making sure stock CEL interprets the expression in the same way using plain strings.
     */
    @Test
    public void testExpressionStringAsciiString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-ascii.xml",
                createVariables(
                        "foo", " FoôBÁR", PrimitiveType.STRING
                ),
                " FooBAR");
    }

    @Test
    public void testExpressionStringEmptyBlankGlobalPolyStringFull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-empty-blank-global.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE
                ),
                "false/false");
    }

    @Test
    public void testExpressionStringEmptyBlankGlobalStringBlank() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-empty-blank-global.xml",
                createVariables(
                        "foo", "  ", PrimitiveType.STRING
                ),
                "false/true");
    }

    @Test
    public void testExpressionStringEmptyBlankMemberPolyStringBlank() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-empty-blank-member.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("  "), PolyStringType.COMPLEX_TYPE
                ),
                "false/true");
    }


    /**
     * Control test, making sure stock CEL interprets the expression in the same way using plain strings.
     */
    @Test
    public void testExpressionStringEmptyBlankGlobalStringFull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-empty-blank-global.xml",
                createVariables(
                        "foo", "Foobar", PrimitiveType.STRING
                ),
                "false/false");
    }

    @Test
    public void testExpressionStringEmptyBlankMemberPolyStringFull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-empty-blank-member.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE
                ),
                "false/false");
    }

    /**
     * Control test, making sure stock CEL interprets the expression in the same way using plain strings.
     */
    @Test
    public void testExpressionStringEmptyBlankMemberStringFull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-empty-blank-member.xml",
                createVariables(
                        "foo", "Foobar", PrimitiveType.STRING
                ),
                "false/false");
    }


    @Test
    public void testExpressionStringMix1String() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-1.xml",
                createVariables(
                        "foo", "Foo", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FOO fooF1Xoo!");
    }

    @Test
    public void testExpressionStringMix1PolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-1.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FOO fooF1Xoo!");
    }

    @Test
    public void testExpressionStringMix2PolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-2.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", PrismTestUtil.createPolyStringType(" FOOBAR"), PolyStringType.COMPLEX_TYPE
                ),
                "3=3 : FOOBARoo/FO");
    }

    /**
     * Control test, making sure stock CEL interprets the expression in the same way using plain strings.
     */
    @Test
    public void testExpressionStringMix2String() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-2.xml",
                createVariables(
                        "foo", "Foo", PrimitiveType.STRING,
                        "bar", " FOOBAR", PrimitiveType.STRING
                ),
                "3=3 : FOOBARoo/FO");
    }

    @Test
    public void testExpressionStringMix3PolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-3.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", PrismTestUtil.createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE
                ),
                "true/false : true/false : true/false : true/false");
    }

    /**
     * Control test, making sure stock CEL interprets the expression in the same way using plain strings.
     */
    @Test
    public void testExpressionStringMix3String() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-3.xml",
                createVariables(
                        "foo", "Foo", PrimitiveType.STRING,
                        "bar", "FooBar", PrimitiveType.STRING
                ),
                "true/false : true/false : true/false : true/false");
    }

    @Test
    public void testExpressionStringContainsPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-contains.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", PrismTestUtil.createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE
                ),
                "true/false : false/false : true/false : true/false");
    }

    /**
     * Control test, making sure stock CEL interprets the expression in the same way using plain strings.
     */
    @Test
    public void testExpressionStringContainsString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-contains.xml",
                createVariables(
                        "foo", "Foo", PrimitiveType.STRING,
                        "bar", "FooBar", PrimitiveType.STRING
                ),
                "true/false : false/false : true/false : true/false");
    }

    @Test
    public void testExpressionStringSplit() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-string-split.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo,Bar,Baz"), PolyStringType.COMPLEX_TYPE
                ),
                "Foo", "Bar", "Baz");
    }

    @Test
    public void testExpressionStringConcatNameString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concatname.xml",
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
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", PrismTestUtil.createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                "FooBAR");
    }

    @Test
    public void testExpressionStringConcatPolyStringMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concat.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FooBAR");
    }

    @Test
    public void testUsername() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-username.xml",
                createUserScriptVariables(),
                "jsparrow");
    }

    @Test
    public void testUserAssignmentFirst() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-assignment-first.xml",
                createUserScriptVariables(),
                "First assignment");
    }

    @Test
    public void testUserAssignmentFirstRelation() throws Exception {
        VariablesMap variables = createUserScriptVariables();
        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-user-assignment-first-relation.xml");
        OperationResult opResult = createOperationResult();
        List<PrismPropertyValue<QName>> expressionResultList = evaluateExpression(scriptType, DOMUtil.XSD_QNAME, true, variables, getTestName(), opResult);
        PrismPropertyValue<QName> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertEquals("Expression " + getTestName() + " resulted in wrong value", SchemaConstants.ORG_OWNER, expressionResult.getValue());
    }

    @Test
    public void testUserAssignmentFirstRelationLocalPart() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-assignment-first-relation-local-part.xml",
                createUserScriptVariables(),
                SchemaConstants.ORG_OWNER.getLocalPart());
    }

    @Test
    public void testUserAssignmentFirstRelationNamespace() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-assignment-first-relation-namespace.xml",
                createUserScriptVariables(),
                SchemaConstants.ORG_OWNER.getNamespaceURI());
    }

    @Test
    public void testUserAssignmentSecond() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-assignment-second.xml",
                createUserScriptVariables(),
                "Second assignment");
    }

    @Test
    public void testUserAssignmentSecondMapping() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-assignment-second-mapping.xml",
                createUserScriptVariables(),
                "Second focus mapping");
    }

    @Test
    public void testUserAssignmentTargetRefOids() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-user-assignment-targetref-oids.xml",
                createUserScriptVariables(),
                "c0c010c0-d34d-b33f-f00d-001111111112", "c0c010c0-d34d-b33f-f00d-001111111111");
    }

    @Test
    public void testUserLinkRefFirstOid() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-linkref-first-oid.xml",
                createUserScriptVariables(),
                "c0c010c0-d34d-b33f-f00d-ff1111111111");
    }

    @Test
    public void testUserExtensionMapString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-extension-ship.xml",
                createUserScriptVariables(
                        "prop", "ship", PrimitiveType.STRING
                ),
                "Black Pearl");
    }

    @Test
    public void testUserExtensionMapQnameNoNamespace() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-extension-ship.xml",
                createUserScriptVariables(
                        "prop", new QName(null, "ship"), PrimitiveType.QNAME
                ),
                "Black Pearl");
    }

    @Test
    public void testUserExtensionMapQnameNamespace() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-extension-ship.xml",
                createUserScriptVariables(
                        "prop", new QName(NS_EXTENSION, "ship"), PrimitiveType.QNAME
                ),
                "Black Pearl");
    }

    @Test
    public void testExpressionQName() throws Exception {
        evaluateAndAssertQNameScalarExpression(
                "expression-qname.xml",
                createVariables(),
                new QName("foo"));
    }

    @Test
    public void testExpressionQNameParts() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-qname-parts.xml",
                createVariables(
                        "q", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME
                ),
                "http://example.com/q/ns - foo");
    }


    @Test
    public void testExpressionQNameNs() throws Exception {
        evaluateAndAssertQNameScalarExpression(
                "expression-qname-ns.xml",
                createVariables(),
                new QName(NS_EXTENSION,"foo"));
    }

    @Test
    public void testExpressionNull() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-null.xml",
                createVariables(
                        "foo", null, String.class
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionNotNull() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-null.xml",
                createVariables(
                        "foo", "BAR", String.class
                ),
                Boolean.FALSE);
    }

    @Test
    public void testSingleString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-single.xml",
                createVariables(
                        "foo", "Bar", String.class
                ),
                "Bar");
    }

    @Test
    public void testSingleNull() throws Exception {
        VariablesMap variables = createVariables(
                "foo", null, String.class
        );
        List<PrismPropertyValue<String>> expressionResultList = evaluateExpression("expression-single.xml", DOMUtil.XSD_STRING, true, variables);
        PrismPropertyValue<String> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNull("Expression " + getTestName() + " resulted in NON-null value", expressionResult);
    }

    @Test
    public void testSingleOrgunitListEmpty() throws Exception {
        VariablesMap variables = createVariables(
                "foo", ImmutableList.of(), List.class
        );
        List<PrismPropertyValue<String>> expressionResultList = evaluateExpression("expression-single.xml", DOMUtil.XSD_STRING, true, variables);
        PrismPropertyValue<String> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNull("Expression " + getTestName() + " resulted in NON-null value", expressionResult);
    }

    @Test
    public void testSingleOrgunitPropEmpty() throws Exception {
        PrismObject<UserType> userEmpty = prismContext.createObject(UserType.class);
        PrismProperty<Object> orgProp = userEmpty.findOrCreateProperty(UserType.F_ORGANIZATIONAL_UNIT);
        VariablesMap variables = createVariables(
                "foo", orgProp, orgProp.getDefinition()
        );
        List<PrismPropertyValue<String>> expressionResultList = evaluateExpression("expression-single.xml", DOMUtil.XSD_STRING, true, variables);
        PrismPropertyValue<String> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNull("Expression " + getTestName() + " resulted in NON-null value", expressionResult);
    }

    @Test
    public void testSingleOrgunitListSingle() throws Exception {
        PrismObject<UserType> userBarbossa = prismContext.parseObject(USER_BARBOSSA_FILE);
        evaluateAndAssertStringScalarExpression(
                "expression-single.xml",
                createVariables(
                        "foo", userBarbossa.asObjectable().getOrganizationalUnit(), List.class
                ),
                "Ministry of Piracy");
    }

    @Test
    public void testSingleOrgunitPropertySingle() throws Exception {
        PrismObject<UserType> userBarbossa = prismContext.parseObject(USER_BARBOSSA_FILE);
        PrismProperty<PolyStringType> orgProp = userBarbossa.findProperty(UserType.F_ORGANIZATIONAL_UNIT);
        evaluateAndAssertStringScalarExpression(
                "expression-single.xml",
                createVariables(
                        "foo", orgProp, orgProp.getDefinition()
                ),
                "Ministry of Piracy");
    }

    @Test
    public void testSingleOrgunitListMulti() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        try {
            evaluateAndAssertStringScalarExpression(
                    "expression-single.xml",
                    createVariables(
                            "foo", userJack.asObjectable().getOrganizationalUnit(), List.class
                    ),
                    "Ministry of Piracy");
            throw new RuntimeException("Unexpected success");
        } catch (ExpressionEvaluationException e) {
            displayException("Expected exception", e);
            assertTrue("Bad exception message: "+e.getMessage(), e.getMessage().contains("Attempt to get single value from a multi-valued property") );
        }
    }

    @Test
    public void testSingleOrgunitPropertyMulti() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        PrismProperty<PolyStringType> orgProp = userJack.findProperty(UserType.F_ORGANIZATIONAL_UNIT);
        try {
            evaluateAndAssertStringScalarExpression(
                    "expression-single.xml",
                    createVariables(
                            "foo", orgProp, orgProp.getDefinition()
                    ),
                    "Ministry of Piracy");
            throw new RuntimeException("Unexpected success");
        } catch (ExpressionEvaluationException e) {
            displayException("Expected exception", e);
            assertTrue("Bad exception message: "+e.getMessage(), e.getMessage().contains("Attempt to get single value from a multi-valued property") );
        }
    }

    @Test
    public void testHelloGivenName() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        PrismProperty<PolyStringType> prop = userJack.findProperty(UserType.F_GIVEN_NAME);
        evaluateAndAssertStringScalarExpression(
                "expression-hello.xml",
                createVariables(
                        "foo", prop, prop.getDefinition()
                ),
                "Hello Jack!");
    }

    @Test
    public void testHelloGivenNameValue() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        PrismProperty<PolyStringType> prop = userJack.findProperty(UserType.F_GIVEN_NAME);
        evaluateAndAssertStringScalarExpression(
                "expression-hello.xml",
                createVariables(
                        "foo", prop.getValue(), prop.getDefinition()
                ),
                "Hello Jack!");
    }


    @Test
    public void testAssignmentDescription() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        PrismContainer<AssignmentType> cont = userJack.findContainer(UserType.F_ASSIGNMENT);
        evaluateAndAssertStringScalarExpression(
                "expression-assignment-description.xml",
                createVariables(
                        "foo", cont.getValues().get(0), cont.getDefinition()
                ),
                "Hear and learn: First assignment");
    }

    @Test
    public void testLinkRefOid() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        PrismReference ref = userJack.findReference(UserType.F_LINK_REF);
        evaluateAndAssertStringScalarExpression(
                "expression-linkref-oid.xml",
                createVariables(
                        "foo", ref.getValues().get(0), ref.getDefinition()
                ),
                "The number of the beast is c0c010c0-d34d-b33f-f00d-ff1111111111");
    }


    @Test
    public void testExpressionParseGivenName() throws Exception {
        expressionParseNameTest(
                "expression-parse-given-name.xml", "Radovan:Radovan");
    }

    @Test
    public void testExpressionParseFamilyName() throws Exception {
        expressionParseNameTest(
                "expression-parse-family-name.xml", "Semančík:Semančík");
    }

    @Test
    public void testExpressionParseAdditionalName() throws Exception {
        expressionParseNameTest(
                "expression-parse-additional-name.xml", ":");
    }

    @Test
    public void testExpressionParseNickName() throws Exception {
        expressionParseNameTest(
                "expression-parse-nick-name.xml", "Gildir:Gildir");
    }

    @Test
    public void testExpressionParseHonorificPrefix() throws Exception {
        expressionParseNameTest(
                "expression-parse-honorific-prefix.xml", "Ing.:Ing.");
    }

    @Test
    public void testExpressionParseHonorificSuffix() throws Exception {
        expressionParseNameTest(
                "expression-parse-honorific-suffix.xml", "PhD.:PhD.");
    }

    private void expressionParseNameTest(String fileName, String expecetedResult) throws Exception {
        evaluateAndAssertStringScalarExpression(
                fileName,
                createVariables(
                        "foo", FULL_NAME_RS, PrimitiveType.STRING
                ),
                expecetedResult);

        evaluateAndAssertStringScalarExpression(
                fileName,
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType(FULL_NAME_RS), PolyStringType.COMPLEX_TYPE
                ),
                expecetedResult);
    }

    @Test
    public void testTimestamp() throws Exception {

        // WHEN
        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-timestamp.xml");
        List<PrismPropertyValue<XMLGregorianCalendar>> expressionResultList =
                evaluateExpression(scriptType, DOMUtil.XSD_DATETIME, true,
                        createVariables(
                                "eta", XmlTypeConverter.createXMLGregorianCalendarFromIso8601("2023-12-25T12:34:56.000Z"), PrimitiveType.DATETIME,
                                "diff", XmlTypeConverter.createDuration("PT1H15M"), PrimitiveType.DURATION
                        ),
                        getTestName(), createOperationResult());

        // THEN
        PrismPropertyValue<XMLGregorianCalendar> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNotNull("Expression " + getTestName() + " resulted in null value)", expressionResult);
        assertEquals("Expression " + getTestName() + " resulted in wrong value",
                XmlTypeConverter.createXMLGregorianCalendarFromIso8601("2023-12-25T13:49:56.000Z"), expressionResult.getValue());
    }

    @Test
    public void testNow() throws Exception {

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-now.xml");
        List<PrismPropertyValue<XMLGregorianCalendar>> expressionResultList =
                evaluateExpression(scriptType, DOMUtil.XSD_DATETIME, true,
                        createVariables(),
                        getTestName(), createOperationResult());

        // THEN
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismPropertyValue<XMLGregorianCalendar> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNotNull("Expression " + getTestName() + " resulted in null value)", expressionResult);
        TestUtil.assertBetween("Timestamp gone wrong", startTs, endTs, expressionResult.getValue());
    }

    @Test
    public void testTimestampSodEodLocal() throws Exception {

        // WHEN
        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendarFromIso8601("2023-12-25T12:34:56.000Z");
        ZonedDateTime zdtSod = timestamp
                .toGregorianCalendar()
                .toZonedDateTime()
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Z"));

        ZonedDateTime zdtEod = timestamp
                .toGregorianCalendar()
                .toZonedDateTime()
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDate()
                .atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Z"));

        XMLGregorianCalendar tsSod = XmlTypeConverter.createXMLGregorianCalendar(timestamp);
        tsSod.setTime(0, 0, 0, 0);
        XMLGregorianCalendar tsEod = XmlTypeConverter.createXMLGregorianCalendar(timestamp);
        tsEod.setTime(23, 59, 59, 999);

        evaluateAndAssertStringScalarExpression(
                "expression-timestamp-sod-eod-local.xml",
                createVariables(
                        "eta", timestamp, PrimitiveType.DATETIME
                ),
                XmlTypeConverter.createXMLGregorianCalendar(zdtSod) + " -- " + timestamp
                        + " -- " + XmlTypeConverter.createXMLGregorianCalendar(zdtEod));
    }

    @Test
    public void testTimestampSodEodZulu() throws Exception {

        // WHEN
        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendarFromIso8601("2023-12-25T12:34:56.000Z");

        evaluateAndAssertStringScalarExpression(
                "expression-timestamp-sod-eod-zulu.xml",
                createVariables(
                        "eta", timestamp, PrimitiveType.DATETIME
                ),
                "2023-12-25T00:00:00.000Z -- 2023-12-25T12:34:56.000Z -- 2023-12-25T23:59:59.999Z");
    }

    @Test
    public void testTimestampLongAgo() throws Exception {

        // WHEN
        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-timestamp-long-ago.xml");
        List<PrismPropertyValue<XMLGregorianCalendar>> expressionResultList =
                evaluateExpression(scriptType, DOMUtil.XSD_DATETIME, true,
                        createVariables(),
                        getTestName(), createOperationResult());

        // THEN
        PrismPropertyValue<XMLGregorianCalendar> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNotNull("Expression " + getTestName() + " resulted in null value)", expressionResult);
        assertEquals("Expression " + getTestName() + " resulted in wrong value",
                XmlTypeConverter.createXMLGregorianCalendarFromIso8601("1970-01-01T00:00:00.000Z"), expressionResult.getValue());
    }


    @Test
    public void testTimestampFormatParse() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-timestamp-format-parse.xml",
                createVariables(
                        "input", "25.12.2025 12:34:56", PrimitiveType.STRING
                ),
                "12/25/25 12.34.56");
    };

    @Test
    public void testTimestampFormatParseFunc() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-timestamp-format-parse-func.xml",
                createVariables(
                        "input", "25.12.2025 12:34:56", PrimitiveType.STRING
                ),
                "12/25/25 12.34.56");
    };

    @Test
    public void testTimestampStrxtime() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-timestamp-strxtime.xml",
                createVariables(
                        "input", "25.12.2025 12:33:44", PrimitiveType.STRING
                ),
                "12/25/2025 12.33.44");
    };

    @Test
    public void testTimestampStrxtimeFunc() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-timestamp-strxtime-func.xml",
                createVariables(
                        "input", "25.12.2025 12:33:44", PrimitiveType.STRING
                ),
                "12/25/2025 12.33.44");
    };

    /**
     * Situation: name of variable (ldap) is the same as a function prefix (ldap.composeDn).
     * This test is checking that the expression interprets this correctly.
     */
    @Test
    public void testLdapVarMaskingString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-ldap-compose-dn-mask.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "FooBar", PrimitiveType.STRING,
                        "ldap", "ouch", PrimitiveType.STRING
                ),
                "cn=Foo,o=FooBar - ouch");
    }

    /**
     * Situation: name of variable (ldap) is the same as a function prefix (ldap.composeDn).
     * This test is checking that the expression interprets this correctly.
     */
    @Test
    public void testLdapVarMaskingUser() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-ldap-compose-dn-mask.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "FooBar", PrimitiveType.STRING,
                        "ldap",
                        MiscSchemaUtil.createObjectReference(USER_JACK_OID, UserType.COMPLEX_TYPE),
                        // We want 'focus' variable to contain user object, not the reference. We want the reference resolved.
                        prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class)
                ),
                "cn=Foo,o=FooBar - user:c0c010c0-d34d-b33f-f00d-111111111111(jack)");
    }

    @Test
    public void testLdapComposeDn() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-ldap-compose-dn.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "FooBar", PrimitiveType.STRING
                ),
                "cn=Foo,o=FooBar");
    }

    @Test
    public void testLdapComposeDnWithSuffix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-ldap-compose-dn-with-suffix.xml",
                createVariables(
                        "foo", PrismTestUtil.createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "dc=example,dc=com", PrimitiveType.STRING
                ),
                "cn=Foo,dc=example,dc=com");
    }

    @Test
    public void testLdapHashPasswordString() throws Exception {
        String output = evaluateStringScalarExpression(
                "expression-ldap-hash-password.xml",
                createVariables(
                        "password", "nbusr123", PrimitiveType.STRING,
                        "alg", "SSHA", PrimitiveType.STRING
                ));
        assertTrue("Wrong password format: "+output, output.startsWith("{SSHA}"));
    }

    @Test
    public void testLdapHashPasswordBytes() throws Exception {
        String output = evaluateStringScalarExpression(
                "expression-ldap-hash-password.xml",
                createVariables(
                        "password",  "nbusr123".getBytes(), PrimitiveType.BASE64BINARY,
                        "alg", "SSHA", PrimitiveType.STRING
                ));
        assertTrue("Wrong password format: "+output, output.startsWith("{SSHA}"));
    }

    @Test
    public void testLdapHashPasswordProtectedString() throws Exception {
        String output = evaluateStringScalarExpression(
                "expression-ldap-hash-password.xml",
                createVariables(
                        "password", createProtectedStringType("nbusr123"), ProtectedStringType.COMPLEX_TYPE,
                        "alg", "SSHA", PrimitiveType.STRING
                ));
        assertTrue("Wrong password format: "+output, output.startsWith("{SSHA}"));
    }

    @Test
    public void testLdapDetermineSingleAttributeValue() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-ldap-determine-single-attribute-value.xml",
                createVariables(),
                "bar");
    }


    @Test
    public void testEncryptString() throws Exception {
        encryptTest(
            createVariables(
                    "psswd", "nbusr123", PrimitiveType.STRING
            ), "nbusr123");
    }

    @Test
    public void testEncryptPolyString() throws Exception {
        encryptTest(
                createVariables(
                        "psswd", PrismTestUtil.createPolyStringType("changeMe!"), PolyStringType.COMPLEX_TYPE
                ), "changeMe!");
    }

    private void encryptTest(VariablesMap variables, String expectedResult) throws Exception {
        encryptDecryptTest("expression-encrypt.xml", variables, expectedResult);
    }

    private void encryptDecryptTest(String filename, VariablesMap variables, String expectedResult) throws Exception {
        OperationResult opResult = createOperationResult();
        ScriptExpressionEvaluatorType scriptType = parseScriptType(filename);
        List<PrismPropertyValue<ProtectedStringType>> expressionResultList =
                evaluateExpression(scriptType, ProtectedStringType.COMPLEX_TYPE, true, variables, getTestName(), opResult);
        PrismPropertyValue<ProtectedStringType> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNotNull("Expression resulted in null value", expressionResult);
        ProtectedStringType expressionResultValue = expressionResult.getValue();
        String decryptedValue = protector.decryptString(expressionResultValue);
        assertEquals("Expression resulted in wrong value", expectedResult, decryptedValue);
    }

    @Test
    public void testEncryptDecryptString() throws Exception {
        encryptDecryptTest(
                "expression-encrypt-decrypt.xml",
                createVariables(
                        "psswd", createProtectedStringType("Alice"), ProtectedStringType.COMPLEX_TYPE
                ),
                "Alice2");
    }


    @Test
    public void testLogErrorSingle() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f00");
        evaluateAndAssertStringScalarExpression(
                "expression-log-error.xml",
                createVariables(
                        "foo", "f00", PrimitiveType.STRING
                ),
                "f00");
        assertTailer(tailer);
    }

    @Test
    public void testLogErrorMulti() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f11/b4r");
        evaluateAndAssertStringListExpression(
                "expression-log-error-multi.xml",
                createVariables(
                        "foo", "f11", PrimitiveType.STRING,
                        "bar", "b4r", PrimitiveType.STRING
                ),
                "f11", "b4r");
        assertTailer(tailer);
    }

    @Test
    public void testLogWarnSingle() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f00");
        evaluateAndAssertStringScalarExpression(
                "expression-log-warn.xml",
                createVariables(
                        "foo", "f00", PrimitiveType.STRING
                ),
                "f00");
        assertTailer(tailer);
    }

    @Test
    public void testLogWarnMulti() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f11/b4r");
        evaluateAndAssertStringListExpression(
                "expression-log-warn-multi.xml",
                createVariables(
                        "foo", "f11", PrimitiveType.STRING,
                        "bar", "b4r", PrimitiveType.STRING
                ),
                "f11", "b4r");
        assertTailer(tailer);
    }

    @Test
    public void testLogInfoSingle() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f00");
        evaluateAndAssertStringScalarExpression(
                "expression-log-info.xml",
                createVariables(
                        "foo", "f00", PrimitiveType.STRING
                ),
                "f00");
        assertTailer(tailer);
    }

    @Test
    public void testLogInfoMulti() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f11/b4r");
        evaluateAndAssertStringListExpression(
                "expression-log-info-multi.xml",
                createVariables(
                        "foo", "f11", PrimitiveType.STRING,
                        "bar", "b4r", PrimitiveType.STRING
                ),
                "f11", "b4r");
        assertTailer(tailer);
    }

    @Test
    public void testLogDebugSingle() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f00");
        evaluateAndAssertStringScalarExpression(
                "expression-log-debug.xml",
                createVariables(
                        "foo", "f00", PrimitiveType.STRING
                ),
                "f00");
        assertTailer(tailer);
    }

    @Test
    public void testLogDebugMulti() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f11/b4r");
        evaluateAndAssertStringListExpression(
                "expression-log-debug-multi.xml",
                createVariables(
                        "foo", "f11", PrimitiveType.STRING,
                        "bar", "b4r", PrimitiveType.STRING
                ),
                "f11", "b4r");
        assertTailer(tailer);
    }

    @Test
    public void testLogTraceSingle() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f00");
        evaluateAndAssertStringScalarExpression(
                "expression-log-trace.xml",
                createVariables(
                        "foo", "f00", PrimitiveType.STRING
                ),
                "f00");
        assertTailer(tailer);
    }

    @Test
    public void testLogTraceMulti() throws Exception {
        LogfileTestTailer tailer = prepareTailer("f11/b4r");
        evaluateAndAssertStringListExpression(
                "expression-log-trace-multi.xml",
                createVariables(
                        "foo", "f11", PrimitiveType.STRING,
                        "bar", "b4r", PrimitiveType.STRING
                ),
                "f11", "b4r");
        assertTailer(tailer);
    }


    private LogfileTestTailer prepareTailer(String expected) throws IOException {
        LogfileTestTailer tailer = new LogfileTestTailer(LogExpressionFunctions.EXPRESSION_LOGGER_NAME);
        tailer.tail();
        tailer.setExpecteMessage(String.format("EXPRESSion TeStLoG =%s=", expected));
        return tailer;
    }

    private void assertTailer(LogfileTestTailer tailer) throws IOException {
        tailer.tail();
        tailer.assertExpectedMessage();
    }

    @Test
    public void testDebugDumpObject() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        String expressionResult = evaluateStringScalarExpression(
                "expression-debug-dump.xml",
                createVariables(
                        "foo", userJack, userJack.getDefinition()
                )
        );
        assertTrue("Wrong result start",
                expressionResult.startsWith("user: (c0c010c0-d34d-b33f-f00d-111111111111, UserType)"));
        assertTrue("Result too short",
                expressionResult.length() > 200);
    }

    @Test
    public void testDebugDumpMulti() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        PrismProperty<PolyStringType> orgProp = userJack.findProperty(UserType.F_ORGANIZATIONAL_UNIT);
        String expressionResult = evaluateStringScalarExpression(
                "expression-debug-dump.xml",
                createVariables(
                        "foo", orgProp, orgProp.getDefinition()
                )
        );
        assertEquals("Expression " + getTestName() + " resulted in wrong values",
                "[PolyString(Leaders,leaders)PolyString(Followers,followers)]",
                expressionResult.replaceAll("[\\s]+", ""));
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
            displayValue("Exception", e);
            assertTrue("Unexpected exception message" + e.getMessage(), e.getMessage().contains("token recognition error"));
        }

    }

}
