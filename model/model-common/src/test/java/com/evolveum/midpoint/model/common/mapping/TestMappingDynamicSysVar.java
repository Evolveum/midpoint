/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.AbstractModelCommonTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
public class TestMappingDynamicSysVar extends AbstractModelCommonTest {

    private MappingTestEvaluator evaluator;

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
        evaluator = new MappingTestEvaluator();
        evaluator.init();
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectTrueGroovy() throws Exception {
        testScriptSystemVariablesConditionAddObjectTrue("mapping-script-system-variables-condition-groovy.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectTrueSourcecontextGroovy() throws Exception {
        testScriptSystemVariablesConditionAddObjectTrue("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }

    public void testScriptSystemVariablesConditionAddObjectTrue(String filename) throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        user.asObjectable().getSubtype().clear();
        user.asObjectable().getSubtype().add("CAPTAIN");
        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(user);

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, getTestNameShort(), "title", delta);

        // WHEN
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    /**
     * Change property that is not a source in this mapping
     */
    @Test
    public void testScriptSystemVariablesConditionModifyObjectTrueGroovyUnrelated() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("employeeNumber"), "666");

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                "mapping-script-system-variables-condition-groovy.xml",
                shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple: " + outputTriple, outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseGroovy() throws Exception {
        testScriptSystemVariablesConditionAddObjectFalse("mapping-script-system-variables-condition-groovy.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseSourcecontextGroovy() throws Exception {
        testScriptSystemVariablesConditionAddObjectFalse("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }

    public void testScriptSystemVariablesConditionAddObjectFalse(String filename) throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        user.asObjectable().getSubtype().clear();
        user.asObjectable().getSubtype().add("SAILOR");
        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(user);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected output triple: " + outputTriple, outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoValGroovy() throws Exception {
        testScriptSystemVariablesConditionAddObjectFalseNoVal("mapping-script-system-variables-condition-groovy.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoValSourcecontextGroovy() throws Exception {
        testScriptSystemVariablesConditionAddObjectFalseNoVal("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }

    public void testScriptSystemVariablesConditionAddObjectFalseNoVal(String filename) throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        PrismProperty<String> employeeTypeProperty = user.findProperty(UserType.F_SUBTYPE);
        employeeTypeProperty.clear();
        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(user);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected output triple: " + outputTriple, outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoPropertyGroovy() throws Exception {
        testScriptSystemVariablesConditionAddObjectFalseNoProperty("mapping-script-system-variables-condition-groovy.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoPropertySourcecontextGroovy() throws Exception {
        testScriptSystemVariablesConditionAddObjectFalseNoProperty("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }

    public void testScriptSystemVariablesConditionAddObjectFalseNoProperty(String filename) throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        user.removeProperty(UserType.F_SUBTYPE);
        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(user);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected output triple: " + outputTriple, outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionTrueToTrueGroovy() throws Exception {
        testScriptSystemVariablesConditionTrueToTrue("mapping-script-system-variables-condition-groovy.xml");
    }

    public void testScriptSystemVariablesConditionTrueToTrue(String filename) throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("name"), PrismTestUtil.createPolyString("Jack"));

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, shortTestName, "title", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        user.asObjectable().getSubtype().add("CAPTAIN");
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain Jack"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
    }

    @Test
    public void testScriptSystemVariablesConditionFalseToFalseGroovy() throws Exception {
        testScriptSystemVariablesConditionFalseToFalse("mapping-script-system-variables-condition-groovy.xml");
    }

    public void testScriptSystemVariablesConditionFalseToFalse(String filename) throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("name"), PrismTestUtil.createPolyString("Jack"));

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple: " + outputTriple, outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionFalseToTrueGroovy() throws Exception {
        testScriptSystemVariablesConditionFalseToTrue("mapping-script-system-variables-condition-groovy.xml");
    }

    public void testScriptSystemVariablesConditionFalseToTrue(String filename) throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("name"), PrismTestUtil.createPolyString("Jack"));
        delta.addModificationAddProperty(evaluator.toPath("subtype"), "CAPTAIN");

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain Jack"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionTrueToFalseGroovy() throws Exception {
        testScriptSystemVariablesConditionTrueToFalse("mapping-script-system-variables-condition-groovy.xml");
    }

    public void testScriptSystemVariablesConditionTrueToFalse(String filename) throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("name"), PolyString.fromOrig("Jack"));
        delta.addModificationDeleteProperty(evaluator.toPath("subtype"), "CAPTAIN");

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, shortTestName, "title", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        user.asObjectable().getSubtype().add("CAPTAIN");
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
    }

    @Test
    public void testScriptSystemVariablesConditionEmptyTrue() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptyTrue";
        testScriptSystemVariablesConditionEmptyTrue(shortTestName, "mapping-script-system-variables-condition-empty.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionEmptyTrueFunction() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptyTrueFunction";
        testScriptSystemVariablesConditionEmptyTrue(shortTestName, "mapping-script-system-variables-condition-empty-function.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrue() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptySingleTrue";
        testScriptSystemVariablesConditionEmptyTrue(shortTestName, "mapping-script-system-variables-condition-empty-single.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueFunction() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptySingleTrueFunction";
        testScriptSystemVariablesConditionEmptyTrue(shortTestName, "mapping-script-system-variables-condition-empty-single-function.xml");
    }

    public void testScriptSystemVariablesConditionEmptyTrue(final String shortTestName, String filename) throws Exception {

        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("name"), PrismTestUtil.createPolyString("Jack"));

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                filename,
                shortTestName, "title", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        user.asObjectable().getSubtype().clear();
        user.asObjectable().setEmployeeNumber(null);
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Landlubber Jack"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
    }

    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseToTrue() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptySingleFalseToTrue";
        testScriptSystemVariablesConditionEmptyFalseToTrue(shortTestName, "mapping-script-system-variables-condition-empty-single.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseToTrueFunction() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptySingleFalseToTrueFunction";
        testScriptSystemVariablesConditionEmptyFalseToTrue(shortTestName, "mapping-script-system-variables-condition-empty-single-function.xml");
    }

    public void testScriptSystemVariablesConditionEmptyFalseToTrue(
            final String shortTestName, String filename) throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("employeeNumber"));

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                filename,
                shortTestName, "title", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        user.asObjectable().setEmployeeNumber("666");
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionEmptyFalse() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptyFalse";
        testScriptSystemVariablesConditionEmptyFalse(shortTestName, "mapping-script-system-variables-condition-empty.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionEmptyFalseFunction() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptyFalse";
        testScriptSystemVariablesConditionEmptyFalse(shortTestName, "mapping-script-system-variables-condition-empty-function.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalse() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptySingleFalse";
        testScriptSystemVariablesConditionEmptyFalse(shortTestName, "mapping-script-system-variables-condition-empty-single.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseFunction() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptySingleFalseFunction";
        testScriptSystemVariablesConditionEmptyFalse(shortTestName, "mapping-script-system-variables-condition-empty-single-function.xml");
    }

    public void testScriptSystemVariablesConditionEmptyFalse(final String shortTestName, String filename) throws Exception {

        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("name"), PrismTestUtil.createPolyString("Jack"));

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                filename,
                shortTestName, "title", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        user.asObjectable().getSubtype().add("SAILOR");
        user.asObjectable().setEmployeeNumber("666");
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple: " + outputTriple, outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueToFalse() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptySingleTrueToFalse";
        testScriptSystemVariablesConditionEmptyTrueToFalse(shortTestName, "mapping-script-system-variables-condition-empty-single.xml");
    }

    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueToFalseFunction() throws Exception {
        final String shortTestName = "testScriptSystemVariablesConditionEmptySingleTrueToFalseFunction";
        testScriptSystemVariablesConditionEmptyTrueToFalse(shortTestName, "mapping-script-system-variables-condition-empty-single-function.xml");
    }

    public void testScriptSystemVariablesConditionEmptyTrueToFalse(
            final String shortTestName, String filename) throws Exception {

        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        evaluator.toPath("employeeNumber"), "666");

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                filename,
                shortTestName, "title", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        user.asObjectable().setEmployeeNumber(null);
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
    }

    @Test
    public void testNpeFalseToTrue() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        UserType.F_ADDITIONAL_NAME, PolyString.fromOrig("Captain Sparrow"));

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping("mapping-npe.xml", shortTestName, "title", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("15"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testNpeTrueToFalse() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        UserType.F_ADDITIONAL_NAME);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping("mapping-npe.xml", shortTestName, "title", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        user.asObjectable().setAdditionalName(PrismTestUtil.createPolyStringType("Sultan of the Caribbean"));
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("23"));
    }

    @Test
    public void testPathEnum() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
                        SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.DISABLED);

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping =
                evaluator.createMapping("mapping-path-enum.xml", shortTestName, "costCenter", delta);

        OperationResult opResult = createOperationResult();

        // WHEN
        System.out.println("WHEN");
        logger.info("WHEN");
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
        System.out.println("\nOutput triple");
        System.out.println(outputTriple.debugDump(1));
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, ActivationStatusType.DISABLED.value());
        PrismAsserts.assertTripleMinus(outputTriple, ActivationStatusType.ENABLED.value());
    }

    @Test
    public void testSubtypeString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-employee-number.xml",
                getTestNameShort(),
                "subtype",                    // target
                "employeeNumber",                // changed property
                "666");    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "666");
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testSubtypePolyString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple =
                evaluator.evaluateMappingDynamicReplace(
                        "mapping-script-system-variables-employee-number.xml",
                        getTestNameShort(),
                        "additionalName", // target
                        "employeeNumber", // changed property
                        "666"); // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("666"));
        PrismAsserts.assertTripleNoMinus(outputTriple);

        // Make sure it is recomputed
        PolyString plusval = outputTriple.getPlusSet().iterator().next().getValue();
        System.out.println("Plus polystring\n" + plusval.debugDump());
        assertEquals("Wrong norm value", "666", plusval.getNorm());
    }

    @Test
    public void testSubtypeInt() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Integer>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-employee-number.xml",
                getTestNameShort(),
                UserType.F_EXTENSION.append(SchemaTestConstants.EXTENSION_INT_TYPE_ELEMENT),                    // target
                "employeeNumber",                // changed property
                "666");    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, 666);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testSubtypeInteger() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Integer>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-employee-number.xml",
                getTestNameShort(),
                UserType.F_EXTENSION.append(SchemaTestConstants.EXTENSION_INTEGER_TYPE_ELEMENT),                    // target
                "employeeNumber",                // changed property
                "666");    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, new BigInteger("666"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testSubtypeLong() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Long>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-employee-number.xml",
                getTestNameShort(),
                UserType.F_EXTENSION.append(SchemaTestConstants.EXTENSION_LONG_TYPE_ELEMENT),                    // target
                "employeeNumber",                // changed property
                "666");    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, 666L);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testSubtypeDecimal() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Integer>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-employee-number.xml",
                getTestNameShort(),
                UserType.F_EXTENSION.append(SchemaTestConstants.EXTENSION_DECIMAL_TYPE_ELEMENT),                    // target
                "employeeNumber",                // changed property
                "666.33");    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, new BigDecimal("666.33"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testSubtypeProtectedString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-employee-number.xml",
                getTestNameShort(),
                UserType.F_CREDENTIALS.append(CredentialsType.F_PASSWORD).append(PasswordType.F_VALUE),                    // target
                "employeeNumber",                // changed property
                "666");    // changed values

        // THEN

        evaluator.assertProtectedString("plus set", outputTriple.getPlusSet(), "666");
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testEmployeeTypeDeltaAReplaceB() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(
                        UserType.class, evaluator.USER_OLD_OID, UserType.F_SUBTYPE, "B");

        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
                "mapping-script-system-variables-employee-type.xml",
                getTestNameShort(), "subtype", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        setEmployeeType(user.asObjectable(), "A");
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        evaluator.assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "B");
        PrismAsserts.assertTripleMinus(outputTriple, "A");
    }

    @Test
    public void testEmployeeTypeDeltaNullReplaceB() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(
                        UserType.class, evaluator.USER_OLD_OID, UserType.F_SUBTYPE, "B");

        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
                "mapping-script-system-variables-employee-type.xml",
                getTestNameShort(), "subtype", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        setEmployeeType(user.asObjectable());
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        evaluator.assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "B");
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testEmployeeTypeDeltaBReplaceB() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(
                        UserType.class, evaluator.USER_OLD_OID, UserType.F_SUBTYPE, "B");

        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
                "mapping-script-system-variables-employee-type.xml",
                getTestNameShort(), "subtype", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        setEmployeeType(user.asObjectable(), "B");
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        evaluator.assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, "B");
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testEmployeeTypeDeltaAAddB() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                employeeTypeDeltaABAdd("B", "A");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "A");
        PrismAsserts.assertTriplePlus(outputTriple, "B");
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testEmployeeTypeDeltaABAddB() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                employeeTypeDeltaABAdd("B", "A", "B");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "A", "B");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testEmployeeTypeDeltaBAddB() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                employeeTypeDeltaABAdd("B", "B");

        // THEN
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, "B");
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testEmployeeTypeDeltaNullAddB() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                employeeTypeDeltaABAdd("B");

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "B");
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    public PrismValueDeltaSetTriple<PrismPropertyValue<String>> employeeTypeDeltaABAdd(
            String addVal, String... oldVals) throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationAddProperty(
                        UserType.class, evaluator.USER_OLD_OID, UserType.F_SUBTYPE, addVal);

        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
                "mapping-script-system-variables-employee-type.xml",
                getTestNameShort(), "subtype", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        setEmployeeType(user.asObjectable(), oldVals);
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        evaluator.assertResult(opResult);
        return mapping.getOutputTriple();
    }

    private void setEmployeeType(UserType userType, String... vals) {
        userType.getSubtype().clear();
        for (String val : vals) {
            userType.getSubtype().add(val);
        }
    }

    @Test
    public void testEmployeeTypeDeltaBDeleteB() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                employeeTypeDeltaDelete("B", "B");

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, "B");
    }

    @Test
    public void testEmployeeTypeDeltaABDeleteB() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                employeeTypeDeltaDelete("B", "A", "B");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "A");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, "B");
    }

    @Test
    public void testEmployeeTypeDeltaADeleteB() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                employeeTypeDeltaDelete("B", "A");

        // THEN
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, "A");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple); // because it's a phantom delete
    }

    @Test
    public void testEmployeeTypeDeltaNullDeleteB() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                employeeTypeDeltaDelete("B");

        // THEN
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple); // because it's a phantom delete
    }

    public PrismValueDeltaSetTriple<PrismPropertyValue<String>> employeeTypeDeltaDelete(
            String delVal, String... oldVals) throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationDeleteProperty(
                        UserType.class, evaluator.USER_OLD_OID, UserType.F_SUBTYPE, delVal);

        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
                "mapping-script-system-variables-employee-type.xml",
                getTestNameShort(), "subtype", delta);

        PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
        setEmployeeType(user.asObjectable(), oldVals);
        mapping.getSourceContext().recompute();

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        evaluator.assertResult(opResult);
        return mapping.getOutputTriple();
    }

    @Test
    public void testPasswordString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-password.xml",
                getTestNameShort(),
                "subtype",                    // target
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),                // changed property
                evaluator.createProtectedString("weighAnch0r"));    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "weighAnch0r");
        PrismAsserts.assertTripleMinus(outputTriple, "d3adM3nT3llN0Tal3s");
    }

    @Test
    public void testPasswordPolyString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-password.xml",
                getTestNameShort(),
                UserType.F_ADDITIONAL_NAME.getLocalPart(),                    // target
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),    // changed property
                evaluator.createProtectedString("weighAnch0r"));    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("weighAnch0r"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("d3adM3nT3llN0Tal3s"));
    }

    @Test
    public void testPasswordProtectedString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-password.xml",
                getTestNameShort(),
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),                    // target
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),    // changed property
                evaluator.createProtectedString("weighAnch0r"));    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        evaluator.assertProtectedString("plus set", outputTriple.getPlusSet(), "weighAnch0r");
        evaluator.assertProtectedString("minus set", outputTriple.getMinusSet(), "d3adM3nT3llN0Tal3s");
    }

    @Test
    public void testPasswordDecryptString() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
                "mapping-script-system-variables-password-decrypt.xml",
                getTestNameShort(),
                "subtype",                    // target
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),                // changed property
                evaluator.createProtectedString("weighAnch0r"));    // changed values

        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "weighAnch0r123");
        PrismAsserts.assertTripleMinus(outputTriple, "d3adM3nT3llN0Tal3s123");
    }
}
