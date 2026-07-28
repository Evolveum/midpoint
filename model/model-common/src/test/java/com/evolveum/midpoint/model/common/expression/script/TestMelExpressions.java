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

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.createPolyStringType;

import static org.testng.AssertJUnit.*;

/**
 * @author Radovan Semancik
 */
public class TestMelExpressions extends AbstractScriptTest {

    private static final String FULL_NAME_RS = "Ing. Radovan \"Gildir\" Semančík, PhD.";

    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector, Clock clock) {
        FunctionLibraryBinding basicFunctionLibraryBinding = FunctionLibraryUtil.createBasicFunctionLibraryBinding(prismContext, protector, clock);
        return new MelScriptEvaluator(prismContext, protector, localizationService,
                (BasicExpressionFunctions) basicFunctionLibraryBinding.getImplementation(),
                null);
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
    public void testUserGivenNameNull() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertStringScalarNullExpression("expression-user-given-name.xml",
            createVariables(ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition())
        );
    }

    @Test
    public void testUserGivenNameIsNullNull1() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-given-name-isnull.xml",
                createVariables(ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition()),
                true);
    }

    @Test
    public void testUserGivenNameIsNullNull2() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setGivenName(null);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-given-name-isnull.xml",
                createVariables(ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()),
                true);
    }

    @Test
    public void testUserGivenNameIsNullFalse() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-given-name-isnull.xml",
                createVariables(ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()),
                false);
    }

    @Test
    public void testUserNameContainsStringTrue() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-name-contains.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition(),
                        "foo", "ack", PrimitiveType.STRING
                ),
                true);
    }

    @Test
    public void testUserNameContainsStringNull() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-name-contains.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition(),
                        "foo", "ack", PrimitiveType.STRING
                ),
                false);
    }

    @Test
    public void testUserFullNameContainsStringTrue() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-fullname-contains.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition(),
                        "foo", "ack", PrimitiveType.STRING
                ),
                true);
    }

    @Test
    public void testUserFullNameContainsStringNull() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-fullname-contains.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition(),
                        "foo", "ack", PrimitiveType.STRING
                ),
                false);
    }

    @Test
    public void testUserFullNameContainsStringMissing() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setFullName(null);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-fullname-contains.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition(),
                        "foo", "ack", PrimitiveType.STRING
                ),
                false);
    }

    @Test
    public void testUserNameSubstringString() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertStringScalarExpression(
                "expression-user-name-substring.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition()
                ),
                null);
    }

    @Test
    public void testUserNameEndsWithStringTrue() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-name-endswith.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition(),
                        "foo", "ack", PrimitiveType.STRING
                ),
                true);
    }

    @Test
    public void testUserNameEndsWithStringNull() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-name-endswith.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition(),
                        "foo", "ack", PrimitiveType.STRING
                ),
                false);
    }

    @Test
    public void testUserStringFormat() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-string-format.xml",
                createUserScriptVariables(),
                "user user:c0c010c0-d34d-b33f-f00d-111111111111(jack) : Jack Sparrow");
    }

    @Test
    public void testUserStringFormatNull() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertStringScalarExpression(
                "expression-user-string-format.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition()
                ),
                "user null : null");
    }

    @Test
    public void testUserStringFormatNullFullName() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setFullName(null);
        evaluateAndAssertStringScalarExpression(
                "expression-user-string-format.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()
                ),
                "user user:c0c010c0-d34d-b33f-f00d-111111111111(jack) : null");
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
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEquals122() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-1.xml",
                createVariables(
                        "foo", createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
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
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEquals222() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-2.xml",
                createVariables(
                        "foo", createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "FOO", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
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
                        "bar", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionStringEqualsPolyStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", createPolyStringType("Bar"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionPolyStringEqualsPolyStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsPolyStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
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
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify122() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-1.xml",
                createVariables(
                        "foo", createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
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
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionPolyStringEqualsStringify222() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-polystring-equals-stringify-2.xml",
                createVariables(
                        "foo", createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
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
    public void testExpressionEqualsIgnoreCaseGlobalStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-equalsignorecase-global.xml",
                createVariables(
                        "foo", "foobar", PrimitiveType.STRING,
                        "bar", "FooBar", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionEqualsIgnoreCaseGlobalStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-equalsignorecase-global.xml",
                createVariables(
                        "foo", "foobar", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionEqualsIgnoreCaseGlobalPolyStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-equalsignorecase-global.xml",
                createVariables(
                        "foo", createPolyStringType("foobar"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionUserAdditionalNameEqualsStringTrue() throws Exception {
        PrismObject<UserType> userBarbossa = prismContext.parseObject(USER_BARBOSSA_FILE);
        userBarbossa.asObjectable().setAdditionalName(createPolyStringType("Cursed One"));
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-additionalname-equals.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userBarbossa, userBarbossa.getDefinition(),
                        "foo", "Cursed One", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionUserAdditionalNameEqualsStringFalse() throws Exception {
        PrismObject<UserType> userBarbossa = prismContext.parseObject(USER_BARBOSSA_FILE);
        userBarbossa.asObjectable().setAdditionalName(createPolyStringType("Cursed One"));
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-additionalname-equals.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userBarbossa, userBarbossa.getDefinition(),
                        "foo", "Foobar", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionUserAdditionalNameEqualsNullStringTrue() throws Exception {
        PrismObject<UserType> userBarbossa = prismContext.parseObject(USER_BARBOSSA_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-additionalname-equals.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userBarbossa, userBarbossa.getDefinition(),
                        "foo", null, PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionUserAdditionalNameEqualsNullStringFalse() throws Exception {
        PrismObject<UserType> userBarbossa = prismContext.parseObject(USER_BARBOSSA_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-additionalname-equals.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userBarbossa, userBarbossa.getDefinition(),
                        "foo", "Foobar", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    // TODO: testExpressionUserAdditionalNameEquals*PolyString*

    @Test
    public void testExpressionEqualsIgnoreCaseGlobalPolyStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-equalsignorecase-global.xml",
                createVariables(
                        "foo", createPolyStringType("foobar"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                Boolean.FALSE);
    }


    @Test
    public void testExpressionStringPlusString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FOOBAR");
    }

    @Test
    public void testExpressionStringPlusStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", null, PrimitiveType.STRING
                ),
                "FOO");
    }

    @Test
    public void testExpressionStringNullPlusStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", null, PrimitiveType.STRING,
                        "bar", null, PrimitiveType.STRING
                ),
                null);
    }

    @Test
    public void testExpressionPolyStringPlusString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FOOBAR");
    }

    @Test
    public void testExpressionStringPlusPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                "FOOBAR");
    }

    @Test
    public void testExpressionPolyStringPlusPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                "FOOBAR");
    }

    @Test
    public void testExpressionPolyStringPlusPolyStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
                        "bar", null, PolyStringType.COMPLEX_TYPE
                ),
                "FOO");
    }

    @Test
    public void testExpressionPolyStringNullPlusPolyStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", null, PolyStringType.COMPLEX_TYPE,
                        "bar", null, PolyStringType.COMPLEX_TYPE
                ),
                null);
    }

    @Test
    public void testExpressionStringPlusEnum() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", ActivationStatusType.ENABLED,
                            prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class)
                                .findItemDefinition(ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS))
                ),
                "FOOenabled");
    }

    @Test
    public void testExpressionListContainsTrue() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpressions(
                List.of("expression-foo-contains-bar.xml",
                        "expression-foo-containsignorecase-bar.xml"),
                createVariables(
                        "foo", userJack.asObjectable().getOrganizationalUnit(), List.class,
                        "bar", "Leaders", PrimitiveType.STRING
                ),
                true);
    }

    @Test
    public void testExpressionListContainsFalse() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpressions(
                List.of("expression-foo-contains-bar.xml",
                        "expression-foo-containsignorecase-bar.xml"),
                createVariables(
                        "foo", userJack.asObjectable().getOrganizationalUnit(), List.class,
                        "bar", "Bar", PrimitiveType.STRING
                ),
                false);
    }

    @Test
    public void testExpressionListContainsIgnoreCaseTrue() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression("expression-foo-containsignorecase-bar.xml",
                createVariables(
                        "foo", userJack.asObjectable().getOrganizationalUnit(), List.class,
                        "bar", "leaders", PrimitiveType.STRING
                ),
                true);
    }

    @Test
    public void testExpressionFooDefaultString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-default.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING
                ),
               "FOO");
    }

    @Test
    public void testExpressionFooDefaultStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-default.xml",
                createVariables(
                        "foo", null, PrimitiveType.STRING
                ),
                "Meh");
    }

    @Test
    public void testExpressionDefaultString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-default.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING
                ),
                "Behold the FOO");
    }

    @Test
    public void testExpressionDefaultStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-default.xml",
                createVariables(
                        "foo", null, PrimitiveType.STRING
                ),
                "Behold the Meh");
    }

    @Test
    public void testExpressionDefaultPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-default.xml",
                createVariables(
                        "foo", createPolyStringType("polyFoo"), PolyStringType.COMPLEX_TYPE
                ),
                "Behold the polyFoo");
    }

    @Test
    public void testExpressionDefaultPolyStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-default.xml",
                createVariables(
                        "foo", null, PolyStringType.COMPLEX_TYPE
                ),
                "Behold the Meh");
    }

    @Test
    public void testExpressionPrefixStringString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-prefix-global.xml", "expression-prefix-member.xml"),
                createVariables(
                        "foo", "Foobar", PrimitiveType.STRING,
                        "prefix", "xyz", PrimitiveType.STRING
                ),
                "xyzFoobar");
    }

    @Test
    public void testExpressionPrefixNullString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-prefix-global.xml", "expression-prefix-member.xml"),
                createVariables(
                        "foo", null, PrimitiveType.STRING,
                        "prefix", "xyz", PrimitiveType.STRING
                ),
                null);
    }

    @Test
    public void testExpressionPrefixStringNull() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-prefix-global.xml", "expression-prefix-member.xml"),
                createVariables(
                        "foo", "Foobar", PrimitiveType.STRING,
                        "prefix", null, PrimitiveType.STRING
                ),
                "Foobar");
    }

    @Test
    public void testExpressionPrefixPolyStringString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-prefix-global.xml", "expression-prefix-member.xml"),
                createVariables(
                        "foo", createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE,
                        "prefix", "xyz", PrimitiveType.STRING
                ),
                "xyzFoobar");
    }

    @Test
    public void testExpressionPrefixPolyNullString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-prefix-global.xml", "expression-prefix-member.xml"),
                createVariables(
                        "foo", null, PolyStringType.COMPLEX_TYPE,
                        "prefix", "xyz", PrimitiveType.STRING
                ),
                null);
    }

    @Test
    public void testExpressionPrefixPolyStringNull() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-prefix-global.xml", "expression-prefix-member.xml"),
                createVariables(
                        "foo", createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE,
                        "prefix", null, PrimitiveType.STRING
                ),
                "Foobar");
    }

    @Test
    public void testExpressionPrefixPolyStringPolyString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-prefix-global.xml", "expression-prefix-member.xml"),
                createVariables(
                        "foo", createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE,
                        "prefix", createPolyStringType("xyz"), PolyStringType.COMPLEX_TYPE
                ),
                "xyzFoobar");
    }

    @Test
    public void testExpressionSuffixStringString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-suffix-global.xml", "expression-suffix-member.xml"),
                createVariables(
                        "foo", "Foobar", PrimitiveType.STRING,
                        "prefix", "xyz", PrimitiveType.STRING
                ),
                "Foobarxyz");
    }

    @Test
    public void testExpressionSuffixNullString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-suffix-global.xml", "expression-suffix-member.xml"),
                createVariables(
                        "foo", null, PrimitiveType.STRING,
                        "prefix", "xyz", PrimitiveType.STRING
                ),
                null);
    }

    @Test
    public void testExpressionSuffixStringNull() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-suffix-global.xml", "expression-suffix-member.xml"),
                createVariables(
                        "foo", "Foobar", PrimitiveType.STRING,
                        "prefix", null, PrimitiveType.STRING
                ),
                "Foobar");
    }

    @Test
    public void testExpressionSuffixPolyStringString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-suffix-global.xml", "expression-suffix-member.xml"),
                createVariables(
                        "foo", createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE,
                        "prefix", "xyz", PrimitiveType.STRING
                ),
                "Foobarxyz");
    }

    @Test
    public void testExpressionSuffixPolyNullString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-suffix-global.xml", "expression-suffix-member.xml"),
                createVariables(
                        "foo", null, PolyStringType.COMPLEX_TYPE,
                        "prefix", "xyz", PrimitiveType.STRING
                ),
                null);
    }

    @Test
    public void testExpressionSuffixPolyStringNull() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-suffix-global.xml", "expression-suffix-member.xml"),
                createVariables(
                        "foo", createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE,
                        "prefix", null, PrimitiveType.STRING
                ),
                "Foobar");
    }

    @Test
    public void testExpressionSuffixPolyStringPolyString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of("expression-prefix-global.xml", "expression-prefix-member.xml"),
                createVariables(
                        "foo", createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE,
                        "prefix", createPolyStringType("xyz"), PolyStringType.COMPLEX_TYPE
                ),
                "xyzFoobar");
    }

    @Test
    public void testExpressionStringNormPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-norm.xml",
                createVariables(
                        "foo", createPolyStringType(" FoôBÁR"), PolyStringType.COMPLEX_TYPE
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
                        "foo", createPolyStringType(" FoôBÁR"), PolyStringType.COMPLEX_TYPE
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
                        "foo", createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE
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
                        "foo", createPolyStringType("  "), PolyStringType.COMPLEX_TYPE
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
                        "foo", createPolyStringType("Foobar"), PolyStringType.COMPLEX_TYPE
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
    public void testExpressionStringMix1StringNational() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-1.xml",
                createVariables(
                        "foo", "TélékÉ", PrimitiveType.STRING,
                        "bar", "TölökÓ", PrimitiveType.STRING
                ),
                "TÉLÉKÉ télékéT-1TélékÉ!");
    }

    @Test
    public void testExpressionStringMix1PolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-1.xml",
                createVariables(
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FOO fooF1Xoo!");
    }

    @Test
    public void testExpressionStringMix1PolyStringNational() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-1.xml",
                createVariables(
                        "foo", createPolyStringType("TélékÉ"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("TölökÓ"), PolyStringType.COMPLEX_TYPE
                ),
                "TÉLÉKÉ télékéT-1TélékÉ!");
    }

    @Test
    public void testExpressionStringMix2PolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-2.xml",
                createVariables(
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType(" FOOBAR"), PolyStringType.COMPLEX_TYPE
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
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE
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
    public void testExpressionStringMix4PolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-4.xml",
                createVariables(
                        "foo", createPolyStringType("Foo\n\tBar"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("fooBar"), PolyStringType.COMPLEX_TYPE
                ),
                "\"Foo\\n\\tBar\" raBoof FooBar");
    }

    /**
     * Control test, making sure stock CEL interprets the expression in the same way using plain strings.
     */
    @Test
    public void testExpressionStringMix4String() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-mix-4.xml",
                createVariables(
                        "foo", "Foo\n\tBar", PrimitiveType.STRING,
                        "bar", "fooBar", PrimitiveType.STRING
                ),
                "\"Foo\\n\\tBar\" raBoof FooBar");
    }

    @Test
    public void testExpressionStringContainsPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-contains.xml",
                createVariables(
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE
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
                        "foo", createPolyStringType("Foo,Bar,Baz"), PolyStringType.COMPLEX_TYPE
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
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                "Foo BAR");
    }

    @Test
    public void testExpressionStringConcatNamePolyStringMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concatname.xml",
                createVariables(
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
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
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", createPolyStringType("BAR"), PolyStringType.COMPLEX_TYPE
                ),
                "FooBAR");
    }

    @Test
    public void testExpressionStringConcatPolyStringMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-concat.xml",
                createVariables(
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FooBAR");
    }

    @Test
    public void testExpressionSubstringMemberStringValid() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-member.xml",
                createVariables(
                        "foo", "FooBar", PrimitiveType.STRING,
                        "i", 0, PrimitiveType.INT,
                        "j", 2, PrimitiveType.INT
                ),
                "Fo");
    }

    @Test
    public void testExpressionSubstringMemberPolyStringValid() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-member.xml",
                createVariables(
                        "foo", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE,
                        "i", 0, PrimitiveType.INT,
                        "j", 2, PrimitiveType.INT
                ),
                "Fo");
    }

    // End index beyond end of string
    @Test
    public void testExpressionSubstringMemberStringBeyond() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-member.xml",
                createVariables(
                        "foo", "FooBar", PrimitiveType.STRING,
                        "i", 2, PrimitiveType.INT,
                        "j", 42, PrimitiveType.INT
                ),
                "oBar");
    }

    // End index beyond end of string
    @Test
    public void testExpressionSubstringMemberPolyStringBeyond() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-member.xml",
                createVariables(
                        "foo", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE,
                        "i", 2, PrimitiveType.INT,
                        "j", 42, PrimitiveType.INT
                ),
                "oBar");
    }

    // End index beyond end of string
    @Test
    public void testExpressionSubstringMemberStringFirstCharEmpty() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-member.xml",
                createVariables(
                        "foo", "", PrimitiveType.STRING,
                        "i", 0, PrimitiveType.INT,
                        "j", 1, PrimitiveType.INT
                ),
                "");
    }

    // End index beyond end of string
    @Test
    public void testExpressionSubstringMemberPolyStringFirstCharEmpty() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-member.xml",
                createVariables(
                        "foo", createPolyStringType(""), PolyStringType.COMPLEX_TYPE,
                        "i", 0, PrimitiveType.INT,
                        "j", 1, PrimitiveType.INT
                ),
                "");
    }

    @Test
    public void testExpressionSubstringGlobalStringValid() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-global.xml",
                createVariables(
                        "foo", "FooBar", PrimitiveType.STRING,
                        "i", 0, PrimitiveType.INT,
                        "j", 2, PrimitiveType.INT
                ),
                "Fo");
    }

    @Test
    public void testExpressionSubstringGlobalNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-global.xml",
                createVariables(
                        "foo", null, PrimitiveType.STRING,
                        "i", 0, PrimitiveType.INT,
                        "j", 2, PrimitiveType.INT
                ),
                null);
    }

    @Test
    public void testExpressionSubstringGlobalPolyStringValid() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-global.xml",
                createVariables(
                        "foo", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE,
                        "i", 0, PrimitiveType.INT,
                        "j", 2, PrimitiveType.INT
                ),
                "Fo");
    }

    @Test
    public void testExpressionSubstringGlobalPolyStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-global.xml",
                createVariables(
                        "foo", null, PolyStringType.COMPLEX_TYPE,
                        "i", 0, PrimitiveType.INT,
                        "j", 2, PrimitiveType.INT
                ),
                null);
    }

    @Test
    public void testExpressionSubstringGlobalNil() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-substring-global-nil.xml",
                createVariables(),
                null);
    }

    @Test
    public void testExpressionContainsGlobalStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-contains-global.xml",
                createVariables(
                        "foo", "FooBar", PrimitiveType.STRING,
                        "bar", "ooB", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionContainsGlobalStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-contains-global.xml",
                createVariables(
                        "foo", "FooBar", PrimitiveType.STRING,
                        "bar", "X", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionContainsGlobalStringNull() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-contains-global.xml",
                createVariables(
                        "foo", null, PrimitiveType.STRING,
                        "bar", "X", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionContainsGlobalPolyStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-contains-global.xml",
                createVariables(
                        "foo", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE,
                        "bar", "ooB", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionContainsGlobalPolyStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-contains-global.xml",
                createVariables(
                        "foo", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE,
                        "bar", "X", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionContainsGlobalPolyStringNull() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-contains-global.xml",
                createVariables(
                        "foo", null, PolyStringType.COMPLEX_TYPE,
                        "bar", "X", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionContainsGlobalNil() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-contains-global-nil.xml",
                createVariables(),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionTrimGlobalString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-trim-global.xml",
                createVariables(
                        "foo", "  FooBar ", PrimitiveType.STRING
                ),
                "FooBar");
    }

    @Test
    public void testExpressionTrimGlobalStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-trim-global.xml",
                createVariables(
                        "foo", null, PrimitiveType.STRING
                ),
                null);
    }

    @Test
    public void testExpressionTrimGlobalPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-trim-global.xml",
                createVariables(
                        "foo", createPolyStringType("  FooBar "), PolyStringType.COMPLEX_TYPE
                ),
                "FooBar");
    }

    @Test
    public void testExpressionTrimGlobalPolyStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-trim-global.xml",
                createVariables(
                        "foo", null, PolyStringType.COMPLEX_TYPE
                ),
                null);
    }

    @Test
    public void testExpressionTrimGlobalNil() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-trim-global-nil.xml",
                createVariables(),
                null);
    }

    @Test
    public void testExpressionContainsMemberReverseString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-contains-member-reverse.xml",
                createVariables("input", "foobar", PrimitiveType.STRING),
                "foobar");
    }

    @Test
    public void testExpressionContainsMemberReverseStringHash() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-contains-member-reverse.xml",
                createVariables("input", "foo#bar", PrimitiveType.STRING),
                null);
    }

    @Test
    public void testExpressionContainsMemberReverseStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-contains-member-reverse.xml",
                createVariables("input", null, PrimitiveType.STRING),
                null);
    }


    @Test
    public void testUsernameJackSubstring() throws Exception {
        usernameJack("expression-username-substring.xml");
    }

    @Test
    public void testUsernameJackFormat() throws Exception {
        usernameJack("expression-username-format.xml");
    }

    @Test
    public void testUsernameJackFormatGlobal() throws Exception {
        usernameJack("expression-username-format-global.xml");
    }

    public void usernameJack(String expressionFile) throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertStringScalarExpression(
                expressionFile,
                createJackVariables(userJack, ""),
                "jsparrow");
    }

    @Test
    public void testUsernameMadJackSubstring() throws Exception {
        usernameMadJack("expression-username-substring.xml");
    }

    @Test
    public void testUsernameMadJackFormat() throws Exception {
        usernameMadJack("expression-username-format.xml");
    }

    public void usernameMadJack(String expressionFile) throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setGivenName(createPolyStringType(" J AČk\t"));
        userJack.asObjectable().setFamilyName(createPolyStringType("\u00A0Špá\trr oW "));
        evaluateAndAssertStringScalarExpression(
                expressionFile,
                createJackVariables(userJack, ""),
                "jsparrow");
    }

    @Test
    public void testUsernameShortJackSubstring() throws Exception {
        usernameShortJack("expression-username-substring.xml");
    }

    @Test
    public void testUsernameShortJackFormat() throws Exception {
        usernameShortJack("expression-username-format.xml");
    }

    public void usernameShortJack(String expressionFile) throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setFamilyName(createPolyStringType("Spa"));
        evaluateAndAssertStringScalarExpression(
                expressionFile,
                createJackVariables(userJack, ""),
                "jspa");
    }

    @Test
    public void testUsernameSparrowSubstring() throws Exception {
        usernameSparrow("expression-username-substring.xml");
    }

    @Test
    public void testUsernameSparrowFormat() throws Exception {
        usernameSparrow("expression-username-format.xml");
    }

    public void usernameSparrow(String expressionFile) throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setGivenName(null);
        evaluateAndAssertStringScalarExpression(
                expressionFile,
                createJackVariables(userJack, ""),
                "sparrow");
    }


    @Test
    public void testUsernameNullSubstring() throws Exception {
        usernameNull("expression-username-substring.xml");
    }

    @Test
    public void testUsernameNullFormat() throws Exception {
        usernameNull("expression-username-format.xml");
    }

    public void usernameNull(String expressionFile) throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setGivenName(null);
        evaluateAndAssertStringScalarExpression(
                expressionFile,
                createVariables(
                        ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition(),
                        ExpressionConstants.VAR_ITERATION_TOKEN, "", PrimitiveType.STRING
                ),
                "");
    }

    @Test
    public void testUsernameGeneratorJSparrow() throws Exception {
        usernameGenerator("expression-username-generator.xml",
                "Jack", "Sparrow", "",
                "jsparrow");
    }

    @Test
    public void testUsernameGeneratorSparrow() throws Exception {
        usernameGenerator("expression-username-generator.xml",
                null, "Sparrow", "",
                "sparrow");
    }

    @Test
    public void testUsernameGeneratorNullNull() throws Exception {
        usernameGenerator("expression-username-generator.xml",
                null, null, "",
                "");
    }

    @Test
    public void testUsernameGeneratorFormatJSparrow() throws Exception {
        usernameGenerator("expression-username-generator-format.xml",
                "Jack", "Sparrow", "",
                "jsparrow");
    }

    @Test
    public void testUsernameGeneratorFormatNull() throws Exception {
        usernameGenerator("expression-username-generator-format.xml",
        null, null, "",
                // This is not very good generator
                // TODO: improve
                "nullnull");
    }

    @Test
    public void testUsernameGeneratorFormatPolystringJSparrow() throws Exception {
        usernameGenerator("expression-username-generator-format-polystring.xml",
                "Jack", "Sparrow", "",
                "JSparrow");
    }

    @Test
    public void testUsernameGeneratorFormatPolystringNull() throws Exception {
        usernameGenerator("expression-username-generator-format-polystring.xml",
                null, null, "",
                // This is not very good generator
                // TODO: improve
                "nullnull");
    }

    public void usernameGenerator(String scriptName, @Nullable String givenName, @Nullable String familyName, @Nullable String iterationToken, @Nullable String expectedOutput) throws Exception {
        evaluateAndAssertStringScalarExpression(
                scriptName,
                createUsernameGeneratorVariables(givenName, familyName, iterationToken),
                expectedOutput);
    }

    @Test
    public void testGivenNameNormSubstring() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertStringScalarExpression(
                "expression-givenname-norm-substring.xml",
                createUsernameGeneratorVariables("Jack", "Sparrow", ""),
                "j");
    }

    private VariablesMap createUsernameGeneratorVariables(@Nullable String givenName, @Nullable String familyName, @Nullable String iterationToken) {
        return createVariables(
                "givenName", createPolyStringTypeNullable(givenName), PolyStringType.COMPLEX_TYPE,
                "familyName", createPolyStringTypeNullable(familyName), PolyStringType.COMPLEX_TYPE,
                "iterationToken", iterationToken, PrimitiveType.STRING
        );
    }

    private VariablesMap createJackVariables(PrismObject<UserType> userJack, String iterationToken) {
        return createVariables(
                ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition(),
                ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken, PrimitiveType.STRING
        );
    }

    public static PolyStringType createPolyStringTypeNullable(@Nullable  String string) {
        if (string == null) {
            return null;
        }
        return createPolyStringType(string);
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
        evaluateAndAssertQNameScalarExpression(
                "expression-user-assignment-first-relation.xml",
                createUserScriptVariables(),
                SchemaConstants.ORG_OWNER
        );
    }

    @Test
    public void testUserAssignmentFirstTargetType() throws Exception {
        evaluateAndAssertQNameScalarExpression(
                "expression-user-assignment-first-targettype.xml",
                createUserScriptVariables(),
                RoleType.COMPLEX_TYPE
        );
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
    public void testUserAssignmentTargetOids() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-user-assignment-targetoid.xml",
                createUserScriptVariables(),
                null, "c0c010c0-d34d-b33f-f00d-001111111112", "c0c010c0-d34d-b33f-f00d-001111111111");
    }

    @Test
    public void testUserAssignmentFilterRolesTargetTypeRole() throws Exception {
        evaluateAndAssertStringListExpressions(
                List.of(
                    "expression-user-assignment-filter-roles-targettype.xml",
                    "expression-user-assignment-filter-roles-istarget.xml",
                    "expression-user-assignment-filter-roles-istargetrole.xml"
                ),
                createUserScriptVariables(
                        "ttype", "RoleType", PrimitiveType.STRING
                ),
                "c0c010c0-d34d-b33f-f00d-001111111111");
    }

    @Test
    public void testUserAssignmentFilterRolesTargetTypeOrg() throws Exception {
        evaluateAndAssertStringListExpressions(
                List.of(
                        "expression-user-assignment-filter-roles-targettype.xml",
                        "expression-user-assignment-filter-roles-istarget.xml",
                        "expression-user-assignment-filter-roles-istargetorg.xml"
                ),
                createUserScriptVariables(
                        "ttype", OrgType.COMPLEX_TYPE, PrimitiveType.QNAME
                ),
                "c0c010c0-d34d-b33f-f00d-001111111112");
    }

    @Test
    public void testUserAssignmentFilterRolesTargetTypeService() throws Exception {
        evaluateAndAssertStringListExpressions(
                List.of(
                        "expression-user-assignment-filter-roles-targettype.xml",
                        "expression-user-assignment-filter-roles-istarget.xml",
                        "expression-user-assignment-filter-roles-istargetservice.xml"
                ),
                createUserScriptVariables(
                        "ttype", ServiceType.COMPLEX_TYPE, PrimitiveType.QNAME
                )
                /* empty list */
                );
    }

    @Test
    public void testUserAssignmentFilterRolesTargetRelationOwnerString() throws Exception {
        evaluateAndAssertStringListExpressions(
                List.of(
                        "expression-user-assignment-filter-roles-targetrelation.xml",
                        "expression-user-assignment-filter-roles-hasrelation.xml",
                        "expression-user-assignment-filter-roles-hasownerrelation.xml"
                ),
                createUserScriptVariables(
                        "relation", "owner", PrimitiveType.STRING
                ),
                "c0c010c0-d34d-b33f-f00d-001111111111");
    }

    @Test
    public void testUserAssignmentFilterRolesTargetRelationOwnerQName() throws Exception {
        evaluateAndAssertStringListExpressions(
                List.of(
                        "expression-user-assignment-filter-roles-targetrelation.xml",
                        "expression-user-assignment-filter-roles-hasrelation.xml",
                        "expression-user-assignment-filter-roles-hasownerrelation.xml"
                ),
                createUserScriptVariables(
                        "relation", SchemaConstants.ORG_OWNER, PrimitiveType.QNAME
                ),
                "c0c010c0-d34d-b33f-f00d-001111111111");
    }

    @Test
    public void testUserAssignmentFilterRolesTargetRelationDefaultString() throws Exception {
        evaluateAndAssertStringListExpressions(
                List.of(
                        "expression-user-assignment-filter-roles-targetrelation.xml",
                        "expression-user-assignment-filter-roles-hasrelation.xml",
                        "expression-user-assignment-filter-roles-hasdefaultrelation.xml"
                ),
                createUserScriptVariables(
                        "relation", "default", PrimitiveType.STRING
                ),
                "c0c010c0-d34d-b33f-f00d-001111111112");
    }

    @Test
    public void testUserAssignmentFilterRolesTargetRelationDefaultQName() throws Exception {
        evaluateAndAssertStringListExpressions(
                List.of(
                        "expression-user-assignment-filter-roles-targetrelation.xml",
                        "expression-user-assignment-filter-roles-hasrelation.xml",
                        "expression-user-assignment-filter-roles-hasdefaultrelation.xml"
                ),
                createUserScriptVariables(
                        "relation", SchemaConstants.ORG_DEFAULT, PrimitiveType.QNAME
                ),
                "c0c010c0-d34d-b33f-f00d-001111111112");
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
    public void testUserAdministrativeStatusNullFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-administrative-status-null.xml",
                createUserScriptVariables(),
                false);
    }

    @Test
    public void testUserAdministrativeStatusNullTrue1() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().getActivation().setAdministrativeStatus(null);
        display("User before\n" + userJack.debugDump());
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-administrative-status-null.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()
                ),
                true);
    }

    @Test
    public void testUserAdministrativeStatusNullTrue2() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setActivation(null);
        display("User before\n" + userJack.debugDump());
        evaluateAndAssertBooleanScalarExpression(
                "expression-user-administrative-status-null.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()
                ),
                true);
    }

    @Test
    public void testUserAdministrativeStatusNull1() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().getActivation().setAdministrativeStatus(null);
        display("User before\n" + userJack.debugDump());
        VariablesMap variables = createVariables(
                ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()
        );
        List<PrismPropertyValue<String>> expressionResultList = evaluateExpression(
                "expression-user-administrative-status.xml",
                DOMUtil.XSD_STRING, true, variables);
        PrismPropertyValue<String> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNull("Expression " + getTestName() + " resulted in NON null value" + expressionResult +
                " while expecting null", expressionResult);
    }

    @Test
    public void testUserAdministrativeStatusNull2() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().setActivation(null);
        display("User before\n" + userJack.debugDump());
        VariablesMap variables = createVariables(
                ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()
        );
        List<PrismPropertyValue<String>> expressionResultList = evaluateExpression(
                "expression-user-administrative-status.xml",
                DOMUtil.XSD_STRING, true, variables);
        PrismPropertyValue<String> expressionResult = asScalar(expressionResultList, getTestName());
        displayValue("Expression result", expressionResult);
        assertNull("Expression " + getTestName() + " resulted in NON null value" + expressionResult +
                " while expecting null", expressionResult);
    }

    @Test
    public void testExpressionListFunctionFoo() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-list-function.xml",
                createVariables(
                        "foo", "Foo", PrimitiveType.QNAME
                ),
                "Foo");
    }

    @Test
    public void testExpressionListFunctionNull() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-list-function.xml",
                createVariables(
                        "foo", null, PrimitiveType.QNAME
                )
                // expected no values (empty list)
        );
    }

    @Test
    public void testExpressionJoinMemberString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-join-member.xml",
                createVariables(
                        "s1", "foo", PrimitiveType.STRING,
                        "s2", "bar", PrimitiveType.STRING
                ),
                "foobar");
    }

    @Test
    public void testExpressionJoinMemberStringNullMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-join-member.xml",
                createVariables(
                        "s1", "foo", PrimitiveType.STRING,
                        "s2", null, PrimitiveType.STRING
                ),
                "foo");
    }

    @Test
    public void testExpressionJoinMemberPolystring() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-join-member.xml",
                createVariables(
                        "s1", createPolyStringType("foo"), PolyStringType.COMPLEX_TYPE,
                        "s2", createPolyStringType("bar"), PolyStringType.COMPLEX_TYPE
                ),
                "foobar");
    }

    @Test
    public void testExpressionJoinGlobalString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-join-global.xml",
                createVariables(
                        "s1", "foo", PrimitiveType.STRING,
                        "s2", "bar", PrimitiveType.STRING,
                        "separator", ",", PrimitiveType.STRING
                ),
                "foo,bar");
    }

    @Test
    public void testExpressionJoinGlobalPolystring() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-join-global.xml",
                createVariables(
                        "s1", createPolyStringType("foo"), PolyStringType.COMPLEX_TYPE,
                        "s2", createPolyStringType("bar"), PolyStringType.COMPLEX_TYPE,
                        "separator", ",", PrimitiveType.STRING
                ),
                "foo,bar");
    }

    @Test
    public void testExpressionJoinGlobalPolystringStringMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-join-global.xml",
                createVariables(
                        "s1", "foo", PrimitiveType.STRING,
                        "s2", createPolyStringType("bar"), PolyStringType.COMPLEX_TYPE,
                        "separator", ",", PrimitiveType.STRING
                ),
                "foo,bar");
    }

    @Test
    public void testExpressionJoinGlobalStringNullMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-join-global.xml",
                createVariables(
                        "s1", "foo", PrimitiveType.STRING,
                        "s2", null, PrimitiveType.STRING,
                        "separator", ",", PrimitiveType.STRING
                ),
                "foo");
    }


    @Test
    public void testExpressionQName() throws Exception {
        evaluateAndAssertQNameScalarExpression(
                "expression-qname.xml",
                createVariables(),
                new QName("foo"));
    }

    @Test
    public void testExpressionQNameAsString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of(
                    "expression-foo.xml",
                    "expression-string-foo.xml"
                ),
                createVariables(
                        "foo", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME
                ),
                "http://example.com/q/ns#foo");
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
    public void testExpressionQNameEqualsQNameExactTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME,
                        "bar", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionQNameEqualsQNameExactFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME,
                        "bar", new QName("http://example.com/q/ns", "bar"), PrimitiveType.QNAME
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionQNameEqualsQNameNoNsTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME,
                        "bar", new QName(null, "foo"), PrimitiveType.QNAME
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionQNameEqualsQNameNoNsFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME,
                        "bar", new QName(null, "bar"), PrimitiveType.QNAME
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionQNameEqualsStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME,
                        "bar", "foo", PrimitiveType.STRING
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionQNameEqualsStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", new QName("http://example.com/q/ns", "foo"), PrimitiveType.QNAME,
                        "bar", "bar", PrimitiveType.STRING
                ),
                Boolean.FALSE);
    }

    // TODO: qname == string

    @Test
    public void testExpressionNullString() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-null.xml",
                createVariables(
                        "foo", null, String.class
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionNullUser() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-null.xml",
                createVariables(
                        "foo", null, userJack.getDefinition()
                ),
                Boolean.TRUE);
    }

    @Test
    public void testExpressionNotNullString() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-null.xml",
                createVariables(
                        "foo", "BAR", String.class
                ),
                Boolean.FALSE);
    }

    @Test
    public void testExpressionNotNullUser() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-null.xml",
                createVariables(
                        "foo", userJack, userJack.getDefinition()
                ),
                Boolean.FALSE);
    }

    @Test
    public void testSizeString() throws Exception {
        sizeTest(
                createVariables(
                        "foo", "FooBar", String.class
                ),
                6
        );
    }

    @Test
    public void testSizeStringEmpty() throws Exception {
        sizeTest(
                createVariables(
                        "foo", "", String.class
                ),
                0
        );
    }

    @Test
    public void testSizeStringNull() throws Exception {
        sizeTest(
                createVariables(
                        "foo", null, String.class
                ),
                0
        );
    }

    @Test
    public void testSizePolystring() throws Exception {
        sizeTest(
                createVariables(
                        "foo", createPolyStringType("FooBar"), PolyStringType.COMPLEX_TYPE
                ),
                6
        );
    }

    @Test
    public void testSizePolystringEmpty() throws Exception {
        sizeTest(
                createVariables(
                        "foo", createPolyStringType(""), PolyStringType.COMPLEX_TYPE
                ),
                0
        );
    }

    @Test
    public void testSizePolystringNull() throws Exception {
        sizeTest(
                createVariables(
                        "foo", null, PolyStringType.COMPLEX_TYPE
                ),
                0
        );
    }

    @Test
    public void testSizeList() throws Exception {
        PrismObject<UserType> userBarbossa = prismContext.parseObject(USER_BARBOSSA_FILE);
        sizeTest(
                createVariables(
                        "foo", userBarbossa.asObjectable().getOrganizationalUnit(), List.class
                ),
                1
        );
    }

    @Test
    public void testSizeListNull() throws Exception {
        sizeTest(
                createVariables(
                        "foo", null, List.class
                ),
                0
        );
    }

    public void sizeTest(VariablesMap variables, Integer expectedResult) throws Exception {
        evaluateAndAssertIntegerScalarExpression("expression-size.xml", variables, expectedResult);
    }

    @Test
    public void testSizeNil() throws Exception {
        evaluateAndAssertIntegerScalarExpression(
                "expression-size-nil.xml",
                createVariables(),
                0
        );
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
    public void testReFindStringMatch() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-refind.xml",
                createVariables(
                        "text", "tel. 4321 or 6543", PrimitiveType.STRING,
                        "regex", "\\d+", PrimitiveType.STRING
                ),
                "4321");
    }

    @Test
    public void testReFindStringNoMatch() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-refind.xml",
                createVariables(
                        "text", "nothing to see here", PrimitiveType.STRING,
                        "regex", "\\d+", PrimitiveType.STRING
                ),
                null);
    }

    @Test
    public void testReFindStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-refind.xml",
                createVariables(
                        "text", null, PrimitiveType.STRING,
                        "regex", "\\d+", PrimitiveType.STRING
                ),
                null);
    }

    @Test
    public void testReFindPolyStringMatch() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-refind.xml",
                createVariables(
                        "text", createPolyStringType("tel. 4321 or 6543"), PolyStringType.COMPLEX_TYPE,
                        "regex", "\\d+", PrimitiveType.STRING
                ),
                "4321");
    }

    @Test
    public void testReFindPolyStringNoMatch() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-refind.xml",
                createVariables(
                        "text", createPolyStringType("nothing to see here"), PolyStringType.COMPLEX_TYPE,
                        "regex", "\\d+", PrimitiveType.STRING
                ),
                null);
    }


    @Test
    public void testReFindAllStringMatch() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-refindall.xml",
                createVariables(
                        "text", "tel. 4321 or 6543", PrimitiveType.STRING,
                        "regex", "\\d+", PrimitiveType.STRING
                ),
                "4321", "6543");
    }


    @Test
    public void testReFindAllStringNoMatch() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-refindall.xml",
                createVariables(
                        "text", "nothing to see here", PrimitiveType.STRING,
                        "regex", "\\d+", PrimitiveType.STRING
                )
                /* empty list expected */);
    }

    @Test
    public void testReFindAllPolyStringMatch() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-refindall.xml",
                createVariables(
                        "text", createPolyStringType("tel. 4321 or 6543"), PolyStringType.COMPLEX_TYPE,
                        "regex", "\\d+", PrimitiveType.STRING
                ),
                "4321", "6543");
    }


    @Test
    public void testReFindAllPolyStringNoMatch() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-refindall.xml",
                createVariables(
                        "text", createPolyStringType("nothing to see here"), PolyStringType.COMPLEX_TYPE,
                        "regex", "\\d+", PrimitiveType.STRING
                )
                /* empty list expected */);
    }

    @Test
    public void testReFindAllStringNull() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-refindall.xml",
                createVariables(
                        "text", null, PrimitiveType.STRING,
                        "regex", "\\d+", PrimitiveType.STRING
                )
                /* empty list expected */);
    }

    @Test
    public void testReReplaceString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-rereplace.xml",
                createVariables(
                        "text", "tel. 4321 or 6543", PrimitiveType.STRING,
                        "regex", "\\d+", PrimitiveType.STRING,
                        "replacement", "X", PrimitiveType.STRING
                ),
                "tel. X or X");
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
                        "foo", createPolyStringType(FULL_NAME_RS), PolyStringType.COMPLEX_TYPE
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
        evaluateAndAssertDateTimeScalarExpression(
                "expression-timestamp-long-ago.xml",
                createVariables(),
                XmlTypeConverter.createXMLGregorianCalendarFromIso8601("1970-01-01T00:00:00.000Z")
        );
    }

    @Test
    public void testTimestampFarAhead() throws Exception {
        evaluateAndAssertDateTimeScalarExpression(
                "expression-timestamp-far-ahead.xml",
                createVariables(),
                XmlTypeConverter.createXMLGregorianCalendarFromIso8601("9999-12-31T23:59:59.000Z")
        );
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

    @Test
    public void testTimestampEpochSeconds() throws Exception {
        evaluateAndAssertLongScalarExpression(
                "expression-timestamp-epoch-second.xml",
                createVariables(
                        "t", XmlTypeConverter.createXMLGregorianCalendarFromIso8601("2026-07-15T12:34:56.987Z"), PrimitiveType.DATETIME
                ),
                1784118896L
        );
    }

    @Test
    public void testTimestampEpochMillis() throws Exception {
        evaluateAndAssertLongScalarExpression(
                "expression-timestamp-epoch-millisecond.xml",
                createVariables(
                        "t", XmlTypeConverter.createXMLGregorianCalendarFromIso8601("2026-07-15T12:34:56.987Z"), PrimitiveType.DATETIME
                ),
                1784118896987L
        );
    }

    @Test
    public void testTimestampNanos() throws Exception {
        evaluateAndAssertLongScalarExpression(
                "expression-timestamp-nanos.xml",
                createVariables(
                        "t", XmlTypeConverter.createXMLGregorianCalendarFromIso8601("2026-07-15T12:34:56.987Z"), PrimitiveType.DATETIME
                ),
                987000000L
        );
    }

    /**
     * Situation: name of variable (ldap) is the same as a function prefix (ldap.composeDn).
     * This test is checking that the expression interprets this correctly.
     */
    @Test
    public void testLdapVarMaskingString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-ldap-compose-dn-mask.xml",
                createVariables(
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
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
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
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
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
                        "bar", "FooBar", PrimitiveType.STRING
                ),
                "cn=Foo,o=FooBar");
    }

    @Test
    public void testLdapComposeDnWithSuffix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-ldap-compose-dn-with-suffix.xml",
                createVariables(
                        "foo", createPolyStringType("Foo"), PolyStringType.COMPLEX_TYPE,
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
                        "psswd", createPolyStringType("changeMe!"), PolyStringType.COMPLEX_TYPE
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
    public void testMapDefaultNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-map-default-null.xml",
                null,
                "HELLO");
    }

    @Test
    public void testStrange1Foobar() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-strange-1.xml",
                createVariables(
                        "input", "foobar", PrimitiveType.STRING
                ),
                "foobar");
    }

    @Test
    public void testStrange1SpaceFoobar() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-strange-1.xml",
                createVariables(
                        "input", " foobar", PrimitiveType.STRING
                ),
                "foobar");
    }

    @Test
    public void testStrange1Null() throws Exception {
        evaluateAndAssertStringScalarNullExpression(
                "expression-strange-1.xml",
                createVariables(
                        "input", null, PrimitiveType.STRING
                ));
    }

    @Test
    public void testConditionalNullStringFoobar() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-conditional-nil-string.xml",
                createVariables(
                        "input", "foobar", PrimitiveType.STRING
                ),
                "We have foobar");
    }

    @Test
    public void testConditionalNullStringNull() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-conditional-nil-string.xml",
                createVariables(
                        "input", null, PrimitiveType.STRING
                ),
                null);
    }


    @Test
    public void testConditionalStringNullFoobar() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-conditional-string-nil.xml",
                createVariables(
                        "input", "foobar", PrimitiveType.STRING
                ),
                "We have foobar");
    }

    @Test
    public void testHasAdministrativeStatusHas() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-has-administrativestatus.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()
                ),
                true);
    }

    @Test
    public void testHasAdministrativeStatusNullStatus() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        userJack.asObjectable().getActivation().setDisableReason("foo"); // To keep the activation container
        userJack.asObjectable().getActivation().setAdministrativeStatus(null);
        evaluateAndAssertBooleanScalarExpression(
                "expression-has-administrativestatus.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, userJack, userJack.getDefinition()
                ),
                false);
    }

    @Test
    public void testHasAdministrativeStatusNullFocus() throws Exception {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        evaluateAndAssertBooleanScalarExpression(
                "expression-has-administrativestatus.xml",
                createVariables(
                        ExpressionConstants.VAR_FOCUS, null, userJack.getDefinition()
                ),
                false);
    }

    // PATH

    static private final ItemPath PATH_ASSIGNMENT_3_TARGET_REF = ItemPath.create(UserType.F_ASSIGNMENT, new IdItemPathSegment(3L), AssignmentType.F_TARGET_REF);
    static private final String PATH_ASSIGNMENT_3_TARGET_REF_STRING = "assignment[3]/targetRef";

    @Test
    public void testPathAsString() throws Exception {
        evaluateAndAssertStringScalarExpressions(
                List.of(
                    "expression-foo.xml",
                    "expression-string-foo.xml"
                ),
                createVariables(
                        "foo", PATH_ASSIGNMENT_3_TARGET_REF, ItemPathType.COMPLEX_TYPE
                ),
                PATH_ASSIGNMENT_3_TARGET_REF_STRING);
    }

    @Test
    public void testPathPlusString() throws Exception {
        evaluateAndAssertStringScalarExpression(
        "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", PATH_ASSIGNMENT_3_TARGET_REF, ItemPathType.COMPLEX_TYPE,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                PATH_ASSIGNMENT_3_TARGET_REF_STRING + "BAR");
    }

    @Test
    public void testStringPlusPath() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-foo-plus-bar.xml",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", PATH_ASSIGNMENT_3_TARGET_REF, ItemPathType.COMPLEX_TYPE
                ),
                "FOO" + PATH_ASSIGNMENT_3_TARGET_REF_STRING);
    }

    @Test
    public void testPathEqualsStringTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", PATH_ASSIGNMENT_3_TARGET_REF, ItemPathType.COMPLEX_TYPE,
                        "bar", PATH_ASSIGNMENT_3_TARGET_REF_STRING, PrimitiveType.STRING
                ),
                 true);
    }

    @Test
    public void testPathEqualsStringFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", PATH_ASSIGNMENT_3_TARGET_REF, ItemPathType.COMPLEX_TYPE,
                        "bar", "nah", PrimitiveType.STRING
                ),
                false);
    }

    @Test
    public void testStringEqualsPathTrue() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", PATH_ASSIGNMENT_3_TARGET_REF_STRING, PrimitiveType.STRING,
                        "bar", PATH_ASSIGNMENT_3_TARGET_REF, ItemPathType.COMPLEX_TYPE
                ),
                true);
    }

    @Test
    public void testStringEqualsPathFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", PATH_ASSIGNMENT_3_TARGET_REF_STRING, PrimitiveType.STRING,
                        "bar", "nah", ItemPathType.COMPLEX_TYPE
                ),
                false);
    }

    @Test
    public void testQNameEqualsPathTrueFull() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", UserType.F_NAME, PrimitiveType.QNAME,
                        "bar", ItemPath.create(UserType.F_NAME), ItemPathType.COMPLEX_TYPE
                ),
                true);
    }

    @Test
    public void testQNameEqualsPathTruePartial() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", UserType.F_NAME, PrimitiveType.QNAME,
                        "bar", ItemPath.create(UserType.F_NAME.getLocalPart()), ItemPathType.COMPLEX_TYPE
                ),
                true);
    }

    @Test
    public void testQNameEqualsPathFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", UserType.F_NAME, PrimitiveType.QNAME,
                        "bar", ItemPath.create(UserType.F_TITLE), ItemPathType.COMPLEX_TYPE
                ),
                false);
    }

    @Test
    public void testPathEqualsQNameTrueFull() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", ItemPath.create(UserType.F_NAME), ItemPathType.COMPLEX_TYPE,
                        "bar", UserType.F_NAME, PrimitiveType.QNAME
                ),
                true);
    }

    @Test
    public void testPathEqualsQNameTruePartial() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", ItemPath.create(UserType.F_NAME.getLocalPart()), ItemPathType.COMPLEX_TYPE,
                        "bar", UserType.F_NAME, PrimitiveType.QNAME
                ),
                true);
    }

    @Test
    public void testPathEqualsQNameFalse() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", ItemPath.create(UserType.F_NAME), ItemPathType.COMPLEX_TYPE,
                        "bar", UserType.F_HONORIFIC_PREFIX, PrimitiveType.QNAME
                ),
                false);
    }

    @Test
    public void testPathEqualsPathTruePartial() throws Exception {
        evaluateAndAssertBooleanScalarExpression(
                "expression-foo-equals-bar.xml",
                createVariables(
                        "foo", ItemPath.create(UserType.F_NAME.getLocalPart()), ItemPathType.COMPLEX_TYPE,
                        "bar", ItemPath.create(UserType.F_NAME), ItemPathType.COMPLEX_TYPE
                ),
                true);
    }

    @Test
    public void testPathSize() throws Exception {
        evaluateAndAssertIntegerScalarExpression(
                "expression-size.xml",
                createVariables(
                        "foo", PATH_ASSIGNMENT_3_TARGET_REF, ItemPathType.COMPLEX_TYPE
                ),
                3);
    }

    @Test
    public void testPathSegments() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-path-segments.xml",
                createVariables(
                        "foo", PATH_ASSIGNMENT_3_TARGET_REF, ItemPathType.COMPLEX_TYPE
                ),
                QNameUtil.qNameToUri(UserType.F_ASSIGNMENT), "3", QNameUtil.qNameToUri(AssignmentType.F_TARGET_REF));
    }

    // AUDIT & DELTAS

    @Test
    public void testAuditTargetOid() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-audit-target-oid.xml",
                createAuditVariables(this::produceAddDelta),
                USER_JACK_OID);
    }

    @Test
    public void testAuditDeltaObejctOid() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-audit-delta-object-oid.xml",
                createAuditVariables(this::produceAddDelta),
                USER_JACK_OID);
    }

    @Test
    public void testAuditDeltaDeltaOid() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-audit-delta-delta-oid.xml",
                createAuditVariables(this::produceAddDelta),
                USER_JACK_OID);
    }

    @Test
    public void testAuditDeltaDeltaObjectAddName() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-audit-delta-delta-object-add-name.xml",
                createAuditVariables(this::produceAddDelta),
                "jack");
    }

    @Test
    public void testAuditDeltaDeltaObjectAddMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-audit-delta-delta-object-add-mix.xml",
                createAuditVariables(this::produceAddDelta),
                "ADD jack(c0c010c0-d34d-b33f-f00d-111111111111): success");
    }

    @Test
    public void testAuditDeltaDeltaObjectDeleteMix() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-audit-delta-delta-object-add-mix.xml",
                createAuditVariables(this::produceDeleteDelta),
                "DELETE [none](c0c010c0-d34d-b33f-f00d-111111111111): success");
    }

    @Test
    public void testAuditDeltaDeltaItemDeltaSizeAdd() throws Exception {
        evaluateAndAssertIntegerScalarExpression(
                "expression-audit-delta-delta-item-delta-size.xml",
                createAuditVariables(this::produceAddDelta),
                0);
    }

    @Test
    public void testAuditDeltaDeltaItemDeltaSizeModify() throws Exception {
        evaluateAndAssertIntegerScalarExpression(
                "expression-audit-delta-delta-item-delta-size.xml",
                createAuditVariables(this::produceModifyDelta),
                2);
    }

    @Test
    public void testAuditDeltaDeltaItemDelta0Path() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-audit-delta-delta-item-delta-0-path.xml",
                createAuditVariables(this::produceModifyDelta),
                UserType.F_TITLE.getLocalPart());
    }

    @Test
    public void testAuditDeltaDeltaItemDelta0ValuesToReplace() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-audit-delta-delta-item-delta-0-replace.xml",
                createAuditVariables(this::produceModifyDelta),
                "Captain");
    }

    @Test
    public void testAuditDeltaExecutionResultStatus() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-audit-delta-result-status.xml",
                createAuditVariables(this::produceAddDelta),
                OperationResultStatusType.SUCCESS.value());
    }

    @FunctionalInterface
    public interface DeltaProducer<O extends ObjectType> {
        ObjectDelta<O> produce(PrismObject<? extends ObjectType> object) throws SchemaException;
    }

    protected <O extends ObjectType> VariablesMap createAuditVariables(DeltaProducer<O> deltaProducer) throws SchemaException, IOException {
        PrismObject<UserType> userJack = prismContext.parseObject(USER_JACK_FILE);
        PrismContainerValue<AuditEventRecordType> auditEventRecord = createAuditEventRecord(userJack);
        fillDeltaOperation(auditEventRecord, (PrismObject<O>)userJack, deltaProducer);
        return createVariables(
                ExpressionConstants.VAR_OBJECT, auditEventRecord, auditEventRecord.getDefinition(),
                ExpressionConstants.VAR_ACTOR, userJack, userJack.getDefinition()
        );
    }

    private <O extends ObjectType> void fillDeltaOperation(
            PrismContainerValue<AuditEventRecordType> auditEventRecord,
            PrismObject<O> object,
            DeltaProducer<O> deltaProducer) throws SchemaException {
        ObjectDelta<O> delta = deltaProducer.produce(object);
        auditEventRecord.asContainerable()
                .beginDelta()
                    .objectOid(object.getOid())
                    .objectName(object.getName().getOrig())
                    .objectDelta(DeltaConvertor.toObjectDeltaType(delta))
                    .beginExecutionResult()
                        .message("Success")
                        .operation("foo.bar.op")
                        .status(OperationResultStatusType.SUCCESS);
    }

    private <O extends ObjectType> ObjectDelta<O> produceAddDelta(PrismObject<O> object) throws SchemaException {
        ObjectDelta<O> delta = prismContext.deltaFor(object.getCompileTimeClass()).asObjectDelta(object.getOid());
        delta.setOid(object.getOid());
        delta.setChangeType(ChangeType.ADD);
        delta.setObjectToAdd(object);
        return delta;
    }

    private <O extends ObjectType> ObjectDelta<O> produceModifyDelta(PrismObject<O> object) throws SchemaException {
        ObjectDelta<O> delta = prismContext.deltaFor(object.getCompileTimeClass()).asObjectDelta(object.getOid());
        delta.setOid(object.getOid());
        delta.setChangeType(ChangeType.MODIFY);
        delta.addModificationReplaceProperty(UserType.F_TITLE, PolyString.fromOrig("Captain"));
        delta.addModificationAddProperty(UserType.F_ORGANIZATION, PolyString.fromOrig("Brethren of the Coast"));
        return delta;
    }

    private <O extends ObjectType> ObjectDelta<O> produceDeleteDelta(PrismObject<O> object) throws SchemaException {
        ObjectDelta<O> delta = prismContext.deltaFor(object.getCompileTimeClass()).asObjectDelta(object.getOid());
        delta.setOid(object.getOid());
        delta.setChangeType(ChangeType.DELETE);
        return delta;
    }

    protected <T extends ObjectType> PrismContainerValue<AuditEventRecordType> createAuditEventRecord(PrismObject<T> target) throws SchemaException {
        PrismContainerDefinition<AuditEventRecordType> def = prismContext.getSchemaRegistry().findContainerDefinitionByType(AuditEventRecordType.COMPLEX_TYPE);
        PrismContainer<AuditEventRecordType> container = def.instantiate();
        PrismContainerValue<AuditEventRecordType> auditEventRecordPCV = container.createNewValue();
        AuditEventRecordType auditEventRecord = auditEventRecordPCV.asContainerable();
        auditEventRecord
                .targetRef(target.getOid(), target.getDefinition().getTypeName());
        return auditEventRecordPCV;
    }

    // CACHING

    @Test
    public void testCaching() throws Exception {
        // We need to start with a clean slate
        initializeScriptEvaluator();
        InternalMonitor.reset();

        assertScriptMonitor(0, 0, "init");

        executeCachingScript(
                "expression-string-ascii.xml",
                createVariables(
                        "foo", "FoôBÁR", PrimitiveType.STRING
                ),
                "FooBAR",
                1, 1, "1-string");

        // Should be cached. Same code, same variables.
        executeCachingScript(
                "expression-string-ascii.xml",
                createVariables(
                        "foo", "FoôBÁR", PrimitiveType.STRING
                ),
                "FooBAR",
                1, 2, "2-string");

        // Should NOT be cached. Same code, but different variable type.
        executeCachingScript(
                "expression-string-ascii.xml",
                createVariables(
                        "foo", createPolyStringType("FoôBÁR"), PolyStringType.COMPLEX_TYPE
                ),
                "FooBAR",
                2, 3, "3-polystring");

        // Should be cached now.
        executeCachingScript(
                "expression-string-ascii.xml",
                createVariables(
                        "foo", createPolyStringType("FoôBÁR"), PolyStringType.COMPLEX_TYPE
                ),
                "FooBAR",
                2, 4, "4-polystring");

        // Should still be cached.
        executeCachingScript(
                "expression-string-ascii.xml",
                createVariables(
                        "foo", "FoôBÁR", PrimitiveType.STRING
                ),
                "FooBAR",
                2, 5, "5-string");

        // TODO: different return type
        // TODO: different expression profile
    }

    private long executeCachingScript(String filename, VariablesMap variables, String expectedResult,
            int expCompilations, int expExecutions, String desc)
            throws SchemaException, SecurityViolationException, ExpressionEvaluationException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, IOException {
        long startTime = System.nanoTime();
        evaluateAndAssertStringScalarExpression(filename, variables, expectedResult);
        long endTime = System.nanoTime();
        long duration = endTime-startTime;
        display("CACHE etime " + duration + "ns : " + desc);
        assertScriptMonitor(expCompilations, expExecutions, desc);
        return duration;
    }


    /**
     * This is mostly just a symbolic test.
     * We are setting up a generic Java class (Poison) in a variable.
     * We are checking here that the drink() method of that class cannot be executed.
     * If it was executed, it throws a specific exception that we could detect in exception message.
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
            assertFalse("Expected that exception message will NOT contain " + Poison.POISON_DRINK_ERROR_MESSAGE +
                    ", but it did. It was: " + ex.getMessage(), ex.getMessage().contains(Poison.POISON_DRINK_ERROR_MESSAGE));
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
