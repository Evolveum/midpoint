/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.QNAME_UID;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertTripleNoMinus;
import static com.evolveum.midpoint.prism.util.PrismAsserts.assertTripleZero;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.io.File;
import java.io.IOException;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.AbstractModelCommonTest;
import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
public class TestMappingDynamicSimple extends AbstractModelCommonTest {

    private static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
    private static final String PATTERN_NUMERIC = "^\\d+$";

    private MappingTestEvaluator evaluator;

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
        evaluator = new MappingTestEvaluator();
        evaluator.init();
    }

    @Test
    public void testValueSingleDeep() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-value-single-deep.xml",
                getTestNameShort(),
                "costCenter",                // target
                "subtype",                // changed property
                "CAPTAIN");                    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "foobar");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueSingleShallow() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-value-single-shallow.xml",
                getTestNameShort(),
                "costCenter",                // target
                "subtype",                // changed property
                "CAPTAIN");                    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "foobar");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueMultiDeep() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-value-multi-deep.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN");                    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueMultiShallow() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-value-multi-shallow.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN");                    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueSingleEnum() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<ActivationStatusType>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-value-single-enum.xml",
                getTestNameShort(),
                PATH_ACTIVATION_ADMINISTRATIVE_STATUS,                // target
                "subtype",                // changed property
                "CAPTAIN");                    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, ActivationStatusType.ENABLED);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsAdd() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-asis.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "PIRATE");
        PrismAsserts.assertTriplePlus(outputTriple, "CAPTAIN", "SWASHBUCKLER");
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsDelete() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicDelete(
                "mapping-asis.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "PIRATE");                    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, "PIRATE");
    }

    @Test
    public void testAsIsStringToPolyString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-asis.xml", getTestNameShort(), "fullName");

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("PIRATE"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsStringToProtectedString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple =
                evaluator.evaluateMapping(
                        "mapping-asis.xml", getTestNameShort(), PATH_CREDENTIALS_PASSWORD_VALUE);

        // THEN
        outputTriple.checkConsistence();
        evaluator.assertProtectedString("output zero set", outputTriple.getZeroSet(), "PIRATE");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsProtectedStringToProtectedString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple =
                evaluator.evaluateMapping(
                        "mapping-asis-password.xml", getTestNameShort(), PATH_CREDENTIALS_PASSWORD_VALUE);

        // THEN
        outputTriple.checkConsistence();
        evaluator.assertProtectedString("output zero set", outputTriple.getZeroSet(), "d3adM3nT3llN0Tal3s");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsProtectedStringToString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-asis-password.xml", getTestNameShort(), UserType.F_EMPLOYEE_NUMBER);

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "d3adM3nT3llN0Tal3s");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsProtectedStringToPolyString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMapping(
                "mapping-asis-password.xml", getTestNameShort(), UserType.F_FULL_NAME); // target

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("d3adM3nT3llN0Tal3s"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testPathVariables() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-path-system-variables.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "jack");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testPathExtensionProperty() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-path-extension-variable.xml", getTestNameShort(), ShadowType.F_NAME);

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleEmpty(outputTriple);
    }

    @Test
    public void testPathVariablesNamespace() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-path-system-variables-namespace.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "jack");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testPathVariablesPolyStringShort() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-path-system-variables-polystring-short.xml",
                getTestNameShort(),
                "fullName",                    // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testPathVariablesPolyStringToStringShort() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-path-system-variables-polystring-short.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "Jack Sparrow");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testPathVariablesExtension() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-path-system-variables-polystring-long.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "jack sparrow");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testPathVariablesPolyStringToStringLong() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-path-system-variables-polystring-long.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "jack sparrow");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptSimpleGroovy() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-script-simple-groovy.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "fooBAR");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptVariablesGroovy() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-script-variables-groovy.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptVariablesPolyStringGroovy() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-script-system-variables-polystring-groovy.xml",
                getTestNameShort(),
                "fullName",                    // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain Jack Sparrow", "captain jack sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptVariablesPolyStringGroovyOp() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-script-system-variables-polystring-groovy-op.xml",
                getTestNameShort(),
                "fullName",                    // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain J. Sparrow", "captain j sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptVariablesPolyStringGroovyOrig() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-script-system-variables-polystring-groovy-orig.xml",
                getTestNameShort(),
                "description",                    // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "Captain J");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptVariablesPolyStringGroovyNorm() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-script-system-variables-polystring-groovy-norm.xml",
                getTestNameShort(),
                "description",                    // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "Captain j");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptVariablesPolyStringGroovyNormReplace() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-polystring-groovy-norm.xml",
                getTestNameShort(),
                "description",                    // target
                "fullName",                // changed property
                PrismTestUtil.createPolyString("Barbossa"));    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "Captain b");
        PrismAsserts.assertTripleMinus(outputTriple, "Captain j");
    }

    @Test
    public void testScriptVariablesPolyStringGroovyNormReplaceNull() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-polystring-groovy-norm.xml",
                getTestNameShort(),
                "description",                    // target
                "fullName"                // changed property
        );    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, "Captain j");
    }

    @Test
    public void testAsIsVariablesPolyStringNorm() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-asis-system-variables-polystring-norm.xml",
                getTestNameShort(),
                "description",                    // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "jack sparrow");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsVariablesPolyStringOrig() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-asis-system-variables-polystring-orig.xml",
                getTestNameShort(),
                "description",                    // target
                "subtype",                // changed property
                "CAPTAIN", "SWASHBUCKLER");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "Jack Sparrow");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptExtraVariablesRef() throws Exception {
        // GIVEN
        var result = createOperationResult();
        MappingBuilder<PrismPropertyValue<String>, PrismPropertyDefinition<String>> builder = evaluator.createMappingBuilder("mapping-script-extra-variables.xml",
                "testScriptExtraVariablesRef", "subtype", null);

        VariablesMap vars = new VariablesMap();
        ObjectReferenceType ref = MiscSchemaUtil.createObjectReference(
                "c0c010c0-d34d-b33f-f00d-111111111112",
                UserType.COMPLEX_TYPE);
        vars.put("sailor", ref,
                PrismTestUtil.getPrismContext().definitionFactory().createReferenceDefinition(
                        new QName(SchemaConstants.NS_C, "sailor"), UserType.COMPLEX_TYPE));
        builder.addVariableDefinitions(vars);

        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), result);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptExtraVariablesJaxb() throws Exception {
        // GIVEN
        var result = createOperationResult();
        MappingBuilder<PrismPropertyValue<String>, PrismPropertyDefinition<String>> builder =
                evaluator.createMappingBuilder(
                        "mapping-script-extra-variables.xml", getTestNameShort(), "subtype", null);

        VariablesMap vars = new VariablesMap();
        UserType userType = (UserType) PrismTestUtil.parseObject(
                new File(MidPointTestConstants.OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111112.xml")).asObjectable();
        vars.put("sailor", userType, userType.asPrismObject().getDefinition());
        builder.addVariableDefinitions(vars);
        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), result);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptFullNameNoChange() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMapping(
                "mapping-script-fullname.xml", getTestNameShort(), "fullName");

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptFullNameReplaceGivenName() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-fullname.xml",
                getTestNameShort(),
                "fullName", // target
                "givenName", // changed property
                PrismTestUtil.createPolyString("Jackie")); // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie Sparrow"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }

    @Test
    public void testScriptFullNameDeleteGivenName() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationDeleteProperty(
                        UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Jack"));

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(
                        "mapping-script-fullname.xml",
                        shortTestName,
                        "fullName", // target
                        delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Sparrow"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }

    /**
     * We are deleting given name Jack from null given name. This is a phantom deletion.
     */
    @Test
    public void testScriptFullNameDeleteGivenNameFromNull() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationDeleteProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Jack"));

        PrismObject<UserType> userOld = evaluator.getUserOld();
        userOld.asObjectable().setGivenName(null);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                "mapping-script-fullname.xml",
                shortTestName,
                "fullName", // target
                delta, userOld);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("output triple", outputTriple);

        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptFullNameDeleteGivenNameFamilyName() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationDeleteProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Jack"));
        delta.addModificationDeleteProperty(UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString("Sparrow"));

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                "mapping-script-fullname.xml",
                shortTestName,
                "fullName",                    // target
                delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("John Doe"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }

    /**
     * Change an unrelated property. See that it does not die.
     */
    @Test
    public void testScriptFullNameReplaceEmployeeNumber() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-fullname.xml",
                getTestNameShort(),
                "fullName",                    // target
                "employeeNumber",                // changed property
                "666");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    /**
     * Return type is a date, script returns string.
     */
    @Test
    public void testScriptDateGroovy() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-date-groovy.xml",
                getTestNameShort(),
                PATH_ACTIVATION_VALID_FROM,    // target
                "employeeNumber",                // changed property
                "1975-05-30");    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptCustomEnum() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<ActivationStatusType>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-custom-enum.xml",
                getTestNameShort(),
                ItemPath.create(UserType.F_EXTENSION, new QName("tShirtSize")),                // target
                "subtype",                // changed property
                "CAPTAIN");                    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "xl");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptListRelativeGroovy() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-script-list-relative-groovy.xml",
                getTestNameShort(),
                "organizationalUnit",                    // target
                "organizationalUnit",                // changed property
                PrismTestUtil.createPolyString("Antropomorphic Personifications"));    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple,
                PrismTestUtil.createPolyString("The Guild of Brethren of the Coast"),
                PrismTestUtil.createPolyString("The Guild of Davie Jones' Locker"));
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The Guild of Antropomorphic Personifications"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptListAbsoluteGroovy() throws Exception {
        testScriptListAbsolute("mapping-script-list-absolute-groovy.xml");
    }

    public void testScriptListAbsolute(String fileName) throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                fileName,
                getTestNameShort(),
                "organizationalUnit",                    // target
                "organizationalUnit",                // changed property
                PrismTestUtil.createPolyString("Antropomorphic Personifications"));    // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple,
                PrismTestUtil.createPolyString("Brethren of the Coast"),
                PrismTestUtil.createPolyString("Davie Jones' Locker"));
        PrismAsserts.assertTriplePlus(outputTriple,
                PrismTestUtil.createPolyString("Antropomorphic Personifications"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueConditionTrue() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-value-condition-true.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "DRUNKARD");                // changed values

        // THEN
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, "foobar");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueConditionFalse() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
                "mapping-value-condition-false.xml",
                getTestNameShort(),
                "subtype",                // target
                "subtype",                // changed property
                "DRUNKARD");                // changed values

        // THEN
        assertNull("Unexpected value in outputTriple", outputTriple);
    }

    @Test
    public void testConditionNonEmptyCaptain() throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        user.asObjectable().getSubtype().clear();
        user.asObjectable().getSubtype().add("CAPTAIN");
        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(user);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                "mapping-condition-nonempty.xml",
                shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The CAPTAIN"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testConditionNonEmptyEmpty() throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        user.asObjectable().getSubtype().clear();
        user.asObjectable().getSubtype().add("");
        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(user);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                "mapping-condition-nonempty.xml",
                shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple", outputTriple);
    }

    @Test
    public void testConditionNonEmptyNoValue() throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        user.asObjectable().getSubtype().clear();
        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(user);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                "mapping-condition-nonempty.xml",
                shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple", outputTriple);
    }

    @Test
    public void testScriptTransformMultiAddDelete() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, MappingTestEvaluator.USER_OLD_OID
                );
        PropertyDelta<String> propDelta = delta.createPropertyModification(evaluator.toPath("subtype"));
        propDelta.addRealValuesToAdd("CAPTAIN");
        propDelta.addRealValuesToDelete("LANDLUBER");
        delta.addModification(propDelta);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                "mapping-script-transform.xml", shortTestName, "organizationalUnit", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        user.asObjectable().getSubtype().add("LANDLUBER");
        mapping.getSourceContext().recompute();
        displayValue("user before", user);
        displayValue("delta", delta);

        // WHEN
        when();
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        then();
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("The pirate deck"));
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The captain deck"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("The landluber deck"));
    }

    /**
     * MID-3700
     */
    @Test
    public void testScriptTransformMultiReplace() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, MappingTestEvaluator.USER_OLD_OID);
        PropertyDelta<String> propDelta = delta.createPropertyModification(evaluator.toPath("subtype"));
        propDelta.setRealValuesToReplace("CAPTAIN");
        delta.addModification(propDelta);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                "mapping-script-transform.xml", shortTestName, "organizationalUnit", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();

        displayValue("user before", user);
        displayValue("delta", delta);

        // WHEN
        when();
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        then();
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayValue("output triple", outputTriple);
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The captain deck"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("The pirate deck"));
    }

    @Test
    public void testInboundMapping() throws Exception {
        PrismContext prismContext = evaluator.getPrismContext();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(MappingTestEvaluator.TEST_DIR, "account-inbound-mapping.xml"));
        // We need to provide some definitions for account attributes here,
        // otherwise the tests will fail on unknown data types
        PrismObjectDefinition<ShadowType> shadowDef = account.getDefinition().deepClone(DeepCloneOperation.ultraDeep());
        PrismContainerDefinition<Containerable> attrsDef = shadowDef.findContainerDefinition(ShadowType.F_ATTRIBUTES);
        attrsDef.toMutable().createPropertyDefinition(QNAME_UID, PrimitiveType.STRING.getQname());
        attrsDef.toMutable().createPropertyDefinition(SchemaConstants.ICFS_NAME, PrimitiveType.STRING.getQname());
        account.setDefinition(shadowDef);
        IntegrationTestTools.display("Account", account);

        PrismProperty<String> oldItem = account.findProperty(ICFS_NAME_PATH);
        PropertyDelta<?> delta = prismContext.deltaFactory().property().createModificationAddProperty(
                ICFS_NAME_PATH,
                oldItem.getDefinition(),
                oldItem.getAnyValue().getValue());

        PrismObject<UserType> user = evaluator.getUserDefinition().instantiate();

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createInboundMapping(
                        "mapping-inbound.xml",
                        shortTestName,
                        delta,
                        user.asObjectable(),
                        account.asObjectable(),
                        null,
                        null);

        when();
        mapping.evaluate(createTask(), createOperationResult());

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        assertTripleZero(outputTriple, PrismTestUtil.createPolyString("pavolr"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testGenerateDefault() throws Exception {
        final ValuePolicyType stringPolicy = evaluator.getValuePolicy();
        // GIVEN
        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping =
                evaluator.createMapping("mapping-generate.xml",
                        shortTestName, stringPolicy, "employeeNumber", null);

        // WHEN (1)
        mapping.evaluate(createTask(), createOperationResult());

        // THEN (1)
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
        String value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        System.out.println("Generated value (1): " + value1);
        assertGeneratedValue(value1, stringPolicy, null, false, false);

        mapping = evaluator.createMapping("mapping-generate.xml", shortTestName, stringPolicy, "employeeNumber", null);

        // WHEN (2)
        mapping.evaluate(createTask(), createOperationResult());

        // THEN (2)
        outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        String value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        System.out.println("Generated value (2): " + value2);
        assertGeneratedValue(value2, stringPolicy, null, false, false);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertFalse("Generated the same value", value1.equals(value2));
    }

    @Test
    public void testGeneratePolicy() throws Exception {
        generatePolicy("mapping-generate-policy.xml", "c0c010c0-d34d-b33f-f00d-999888111111.xml", null, false);
    }

    @Test
    public void testGeneratePolicyEmpty() throws Exception {
        generatePolicy("mapping-generate-policy-empty.xml", "c0c010c0-d34d-b33f-f00d-999888111114.xml", null, true);
    }

    @Test
    public void testGeneratePolicyBad() throws Exception {
        try {
            generatePolicy("mapping-generate-policy-bad.xml", "c0c010c0-d34d-b33f-f00d-999888111113.xml", null, false);
            AssertJUnit.fail("Unexpected success");
        } catch (ExpressionEvaluationException e) {
            // This is expected, the policy is broken
        }
    }

    @Test
    public void testGeneratePolicyNumericString() throws Exception {
        generatePolicy("mapping-generate-policy-numeric.xml",
                "c0c010c0-d34d-b33f-f00d-999888111112.xml", PATTERN_NUMERIC, false);
    }

    private void generatePolicy(String mappingFileName, String policyFileName, String pattern, boolean ignoreMax)
            throws Exception {

        // This is just for validation. The expression has to resolve reference of its own
        PrismObject<ValuePolicyType> valuePolicy = PrismTestUtil.parseObject(
                new File(MidPointTestConstants.OBJECTS_DIR, policyFileName));
        final ValuePolicyType stringPolicy = valuePolicy.asObjectable();
        // GIVEN
        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping =
                evaluator.createMapping(
                        mappingFileName, getTestNameShort(), stringPolicy, "employeeNumber", null);

        // WHEN (1)
        mapping.evaluate(createTask(), createOperationResult());

        // THEN (1)
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        String value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        System.out.println("Generated value (1): " + value1);
        assertNotNull("Generated null value", value1);
        assertGeneratedValue(value1, stringPolicy, pattern, false, ignoreMax);

        // GIVEN (2)
        mapping = evaluator.createMapping(
                mappingFileName, getTestNameShort(), stringPolicy, "employeeNumber", null);

        // WHEN (2)
        mapping.evaluate(createTask(), createOperationResult());

        // THEN (2)
        outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        String value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        System.out.println("Generated value (2): " + value2);
        assertNotNull("Generated null value", value2);
        assertGeneratedValue(value2, stringPolicy, pattern, false, ignoreMax);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertFalse("Generated the same value", value1.equals(value2));
    }

    private void assertGeneratedValue(String value, ValuePolicyType valuePolicy, String pattern, boolean ignoreMin, boolean ignoreMax) {
        StringPolicyType stringPolicy = valuePolicy.getStringPolicy();
        if (stringPolicy == null) {
            assertEquals("Unexpected generated value length", GenerateExpressionEvaluator.DEFAULT_LENGTH, value.length());
        } else {
            if (!ignoreMin) {
                assertTrue("Value '" + value + "' too short, minLength=" + stringPolicy.getLimitations().getMinLength() + ", length=" + value.length(), value.length() >= stringPolicy.getLimitations().getMinLength());
            }
            if (!ignoreMax) {
                assertTrue("Value '" + value + "' too long, maxLength=" + stringPolicy.getLimitations().getMaxLength() + ", length=" + value.length(), value.length() <= stringPolicy.getLimitations().getMaxLength());
            }
            // TODO: better validation
        }
        if (pattern != null) {
            assertTrue("Value '" + value + "' does not match pattern '" + pattern + "'", value.matches(pattern));
        }
    }

    @Test
    public void testGeneratePolicyNumericInt() throws Exception {
        generatePolicyNumeric("mapping-generate-policy-numeric.xml",
                "c0c010c0-d34d-b33f-f00d-999888111112.xml", "intType");
    }

    @Test
    public void testGeneratePolicyNumericInteger() throws Exception {
        generatePolicyNumeric("mapping-generate-policy-numeric.xml",
                "c0c010c0-d34d-b33f-f00d-999888111112.xml", "integerType");
    }

    @Test
    public void testGeneratePolicyNumericLong() throws Exception {
        generatePolicyNumeric("mapping-generate-policy-numeric.xml",
                "c0c010c0-d34d-b33f-f00d-999888111112.xml", "longType");
    }

    @SuppressWarnings("SameParameterValue")
    private <T> void generatePolicyNumeric(
            String mappingFileName, String policyFileName, String extensionPropName)
            throws Exception {
        OperationResult opResult = createOperationResult();
        // This is just for validation. The expression has to resolve reference of its own
        PrismObject<ValuePolicyType> valuePolicy = PrismTestUtil.parseObject(
                new File(MidPointTestConstants.OBJECTS_DIR, policyFileName));
        final ValuePolicyType valuePolicyType = valuePolicy.asObjectable();
        // GIVEN
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                evaluator.<T>createMappingBuilder(
                        mappingFileName, getTestNameShort(), valuePolicyType,
                        ItemPath.create(UserType.F_EXTENSION, new QName(NS_EXTENSION, extensionPropName)),
                        null)
                .build();

        // WHEN (1)
        mapping.evaluate(createTask(), opResult);

        // THEN (1)
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        T value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        System.out.println("Generated value (1): " + value1);
        assertNotNull("Generated null value", value1);
        // We need to ignore the minLength. Conversion string -> number -> string may lose the leading zeroes
        assertGeneratedValue(value1.toString(), valuePolicyType, PATTERN_NUMERIC, true, false);

        // GIVEN (2)

        mapping = evaluator.<T>createMappingBuilder(
                mappingFileName, getTestNameShort(), valuePolicyType,
                ItemPath.create(UserType.F_EXTENSION, new QName(NS_EXTENSION, extensionPropName)), null)
                .build();

        // WHEN (2)
        mapping.evaluate(createTask(), opResult);

        // THEN (2)
        outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        T value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        System.out.println("Generated value (2): " + value2);
        assertNotNull("Generated null value", value2);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertFalse("Generated the same value", value1.equals(value2));
        assertGeneratedValue(value1.toString(), valuePolicyType, PATTERN_NUMERIC, true, false);
    }

    @Test
    public void testGenerateProtectedString() throws Exception {
        // GIVEN
        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>> mapping =
                evaluator.createMapping("mapping-generate.xml",
                        shortTestName, SchemaConstants.PATH_PASSWORD_VALUE, null);
        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        ProtectedStringType value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        System.out.println("Generated encrypted value: " + value1);
        assertNotNull(value1);
        assertNotNull(value1.getEncryptedDataType());
    }

}
