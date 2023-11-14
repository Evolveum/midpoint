/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import java.io.IOException;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests for mapping domain. Those are multival input and multival output.
 * <p>
 * MID-3692
 *
 * @author Radovan Semancik
 */
public class TestMappingDomain extends AbstractUnitTest
        implements InfraTestMixin {

    private static final String MAPPING_DOMAIN_FILENAME = "mapping-domain.xml";

    private MappingTestEvaluator evaluator;

    @BeforeClass
    public void setupFactory() throws SchemaException, SAXException, IOException {
        evaluator = new MappingTestEvaluator();
        evaluator.init();
    }

    /**
     * Control. All goes well here. All values in the domain.
     */
    @Test
    public void testControlReplaceSingleValue() throws Exception {
        // GIVEN

        PrismObject<UserType> userOld = evaluator.getUserOld();
        List<String> employeeTypeOld = userOld.asObjectable().getSubtype();
        employeeTypeOld.clear();
        employeeTypeOld.add("1234567890");

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_ADDITIONAL_NAME, PolyString.fromOrig("Jackie"));
        delta.addModificationReplaceProperty(UserType.F_SUBTYPE, "321");

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(MAPPING_DOMAIN_FILENAME, getTestNameShort(), "organization", delta, userOld);

        OperationResult opResult = createOperationResult();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Pirate Jackie (321)"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Pirate null (1234567890)"));
    }

    private Task createTask() {
        return new NullTaskImpl();
    }

    /**
     * Control. All goes well here. All values in the domain.
     */
    @Test
    public void testControlReplaceMultiValue() throws Exception {
        // GIVEN
        PrismObject<UserType> userOld = evaluator.getUserOld();
        List<String> employeeTypeOld = userOld.asObjectable().getSubtype();
        employeeTypeOld.clear();
        employeeTypeOld.add("001");
        employeeTypeOld.add("002");
        employeeTypeOld.add("003");

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_ADDITIONAL_NAME, PolyString.fromOrig("Jackie"));
        delta.addModificationReplaceProperty(UserType.F_SUBTYPE, "991", "992");

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(MAPPING_DOMAIN_FILENAME, getTestNameShort(), "organization", delta, userOld);

        // WHEN
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple,
                PrismTestUtil.createPolyString("Pirate Jackie (991)"),
                PrismTestUtil.createPolyString("Pirate Jackie (992)"));
        PrismAsserts.assertTripleMinus(outputTriple,
                PrismTestUtil.createPolyString("Pirate null (001)"),
                PrismTestUtil.createPolyString("Pirate null (002)"),
                PrismTestUtil.createPolyString("Pirate null (003)"));
    }

    @Test
    public void testReplaceMixedMultiValue() throws Exception {
        // GIVEN

        PrismObject<UserType> userOld = evaluator.getUserOld();
        List<String> employeeTypeOld = userOld.asObjectable().getSubtype();
        employeeTypeOld.clear();
        employeeTypeOld.add("001");
        employeeTypeOld.add("A02");
        employeeTypeOld.add("B03");
        employeeTypeOld.add("004");

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_ADDITIONAL_NAME, PolyString.fromOrig("Jackie"));
        delta.addModificationReplaceProperty(UserType.F_SUBTYPE, "X91", "992", "Y93", "994");

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(
                        MAPPING_DOMAIN_FILENAME, shortTestName, "organization", delta, userOld);

        OperationResult opResult = createOperationResult();

        // WHEN
        when();
        mapping.evaluate(createTask(), opResult);

        // THEN
        then();
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("Output triple", outputTriple);
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple,
                PrismTestUtil.createPolyString("Pirate Jackie (992)"),
                PrismTestUtil.createPolyString("Pirate Jackie (994)"));
        PrismAsserts.assertTripleMinus(outputTriple,
                PrismTestUtil.createPolyString("Pirate null (001)"),
                PrismTestUtil.createPolyString("Pirate null (004)"));
    }

    @Test
    public void testAddMixedMultiValue() throws Exception {
        // GIVEN

        PrismObject<UserType> userOld = evaluator.getUserOld();
        userOld.asObjectable().setAdditionalName(PrismTestUtil.createPolyStringType("Jackie"));
        List<String> employeeTypeOld = userOld.asObjectable().getSubtype();
        employeeTypeOld.clear();
        employeeTypeOld.add("001");
        employeeTypeOld.add("A02");
        employeeTypeOld.add("B03");
        employeeTypeOld.add("004");

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationAddProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_SUBTYPE, "X91", "992", "Y93", "994");

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(
                        MAPPING_DOMAIN_FILENAME, shortTestName, "organization", delta, userOld);

        OperationResult opResult = createOperationResult();

        // WHEN
        when();
        mapping.evaluate(createTask(), opResult);

        // THEN
        then();
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("Output triple", outputTriple);
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple,
                PrismTestUtil.createPolyString("Pirate Jackie (001)"),
                PrismTestUtil.createPolyString("Pirate Jackie (004)"));
        PrismAsserts.assertTriplePlus(outputTriple,
                PrismTestUtil.createPolyString("Pirate Jackie (992)"),
                PrismTestUtil.createPolyString("Pirate Jackie (994)"));
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testDeleteMixedMultiValue() throws Exception {
        // GIVEN

        PrismObject<UserType> userOld = evaluator.getUserOld();
        userOld.asObjectable().setAdditionalName(PrismTestUtil.createPolyStringType("Jackie"));
        List<String> employeeTypeOld = userOld.asObjectable().getSubtype();
        employeeTypeOld.clear();
        employeeTypeOld.add("001");
        employeeTypeOld.add("A02");
        employeeTypeOld.add("B03");
        employeeTypeOld.add("004");
        employeeTypeOld.add("005");
        employeeTypeOld.add("C06");
        employeeTypeOld.add("007");

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationDeleteProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_SUBTYPE, "005", "C06", "007");

        String shortTestName = getTestNameShort();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                MAPPING_DOMAIN_FILENAME, shortTestName, "organization", delta, userOld);

        OperationResult opResult = createOperationResult();

        // WHEN
        when();
        mapping.evaluate(createTask(), opResult);

        // THEN
        then();
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("Output triple", outputTriple);
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple,
                PrismTestUtil.createPolyString("Pirate Jackie (001)"),
                PrismTestUtil.createPolyString("Pirate Jackie (004)"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple,
                PrismTestUtil.createPolyString("Pirate Jackie (005)"),
                PrismTestUtil.createPolyString("Pirate Jackie (007)"));
    }

    public void displayValue(String title, Object value) {
        PrismTestUtil.display(title, value);
    }
}
