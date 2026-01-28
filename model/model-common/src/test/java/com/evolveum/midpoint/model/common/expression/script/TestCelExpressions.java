/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.model.common.expression.script.cel.CelScriptEvaluator;

import com.evolveum.midpoint.prism.PrismPropertyValue;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author Radovan Semancik
 */
public class TestCelExpressions extends AbstractScriptTest {

    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector, Clock clock) {
        FunctionLibraryBinding basicFunctionLibraryBinding = FunctionLibraryUtil.createBasicFunctionLibraryBinding(prismContext, protector, clock);
        return new CelScriptEvaluator(prismContext, protector, localizationService, (BasicExpressionFunctions) basicFunctionLibraryBinding.getImplementation());
    }

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "cel");
    }

    @Test
    public void testUserGivenNameMap() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-given-name-map.xml",
                createUserScriptVariables(),
                "Jack");
    }

    @Test
    public void testUserExtensionShipBasicFunc() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-extension-ship-basic-func.xml",
                createUserScriptVariables(),
                "Black Pearl");
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
