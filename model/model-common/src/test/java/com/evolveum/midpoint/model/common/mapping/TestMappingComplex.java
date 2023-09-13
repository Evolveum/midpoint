/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import static org.testng.AssertJUnit.assertNull;

import java.io.IOException;

import com.evolveum.midpoint.test.util.TestUtil;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.AbstractModelCommonTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 */
public class TestMappingComplex extends AbstractModelCommonTest {

    private static final String MAPPING_COMPLEX_FILENAME = "mapping-complex-captain.xml";

    private MappingTestEvaluator evaluator;

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
        evaluator = new MappingTestEvaluator();
        evaluator.init();
    }

    @Test
    public void testModifyObjectSetAdditionalName() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class,
                        MappingTestEvaluator.USER_OLD_OID, UserType.F_ADDITIONAL_NAME, PolyString.fromOrig("Jackie"));
        delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, "321");

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(
                        MAPPING_COMPLEX_FILENAME,
                        getTestNameShort(), "title", delta);

        // WHEN
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PolyString.fromOrig("Pirate Jackie (#321)"));
        PrismAsserts.assertTripleMinus(outputTriple, PolyString.fromOrig("Pirate null (#null)"));
    }

    @Test
    public void testModifyObjectSetAdditionalNameFalse() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class,
                        MappingTestEvaluator.USER_OLD_OID, UserType.F_ADDITIONAL_NAME, PolyString.fromOrig("Jackie"));
        delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, "321");

        PrismObject<UserType> userOld = evaluator.getUserOld();
        userOld.asObjectable().getSubtype().clear();
        userOld.asObjectable().getSubtype().add("WHATEVER");
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                MAPPING_COMPLEX_FILENAME,
                getTestNameShort(), "title", delta, userOld);

        // WHEN
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple: " + outputTriple, outputTriple);
    }

    /**
     * Change property that is not a source in this mapping
     */
    @Test
    public void testModifyObjectUnrelated() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        evaluator.toPath("costCenter"), "X606");

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(
                        MAPPING_COMPLEX_FILENAME,
                        getTestNameShort(), "title", delta);

        // WHEN
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        outputTriple.checkConsistence();
        PrismAsserts.assertTripleZero(outputTriple, PolyString.fromOrig("Pirate null (#null)"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testModifyObjectUnrelatedFalse() throws Exception {
        // GIVEN
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        evaluator.toPath("costCenter"), "X606");

        PrismObject<UserType> userOld = evaluator.getUserOld();
        userOld.asObjectable().getSubtype().clear();
        userOld.asObjectable().getSubtype().add("WHATEVER");
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                MAPPING_COMPLEX_FILENAME,
                getTestNameShort(), "title", delta, userOld);

        // WHEN
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple: " + outputTriple, outputTriple);
    }

    @Test
    public void testAddObjectUnrelatedFalse() throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        user.asObjectable().getSubtype().clear();
        user.asObjectable().getSubtype().add("WHATEVER");
        ObjectDelta<UserType> delta = user.createAddDelta();

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                MAPPING_COMPLEX_FILENAME,
                getTestNameShort(), "title", delta);

        // WHEN
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple: " + outputTriple, outputTriple);
    }

    @Test
    public void testAddObjectUnrelatedEmptyFalse() throws Exception {
        // GIVEN
        PrismObject<UserType> user = evaluator.getUserOld();
        user.asObjectable().getSubtype().clear();
        ObjectDelta<UserType> delta = user.createAddDelta();

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
                MAPPING_COMPLEX_FILENAME,
                getTestNameShort(), "title", delta);

        // WHEN
        mapping.evaluate(createTask(), createOperationResult());

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNull("Unexpected value in outputTriple: " + outputTriple, outputTriple);
    }
}
