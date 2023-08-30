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
import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestMappingTime extends AbstractUnitTest
        implements InfraTestMixin {

    private static final String MAPPING_TIME_FROM_TO_FILENAME = "mapping-time-from-to.xml";
    private static final String MAPPING_TIME_ACTIVATION = "mapping-time-deferred-delete.xml";
    private static final XMLGregorianCalendar TIME_PAST = XmlTypeConverter.createXMLGregorianCalendar(2001, 2, 3, 4, 5, 6);
    private static final XMLGregorianCalendar TIME_BETWEEN = XmlTypeConverter.createXMLGregorianCalendar(2013, 6, 3, 12, 15, 0);
    private static final XMLGregorianCalendar TIME_FUTURE = XmlTypeConverter.createXMLGregorianCalendar(2066, 5, 4, 3, 2, 1);
    private static final XMLGregorianCalendar TIME_MAPPING_DISABLED_PLUS_1D = XmlTypeConverter.createXMLGregorianCalendar(2013, 5, 31, 9, 30, 0);
    private static final XMLGregorianCalendar TIME_MAPPING_DISABLED_PLUS_10D = XmlTypeConverter.createXMLGregorianCalendar(2013, 6, 9, 9, 30, 0);
    private static final XMLGregorianCalendar TIME_MAPPING_DISABLED_PLUS_1M = XmlTypeConverter.createXMLGregorianCalendar(2013, 6, 30, 9, 30, 0);

    private MappingTestEvaluator evaluator;

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
        evaluator = new MappingTestEvaluator();
        evaluator.init();
    }

    @Test
    public void testBeforeTimeFrom() throws Exception {
        // GIVEN
        OperationResult opResult = createOperationResult();
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_SUBTYPE, "CAPTAIN");

        MappingBuilder<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> builder =
                evaluator.createMappingBuilder(
                        MAPPING_TIME_FROM_TO_FILENAME, getTestNameShort(), "title", delta);
        builder.now(TIME_PAST);

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNullTriple(outputTriple);
        assertNextRecompute(mapping, TIME_MAPPING_DISABLED_PLUS_1D);
    }

    private Task createTask() {
        return new NullTaskImpl();
    }

    @Test
    public void testBetweenTimes() throws Exception {
        // GIVEN
        OperationResult opResult = createOperationResult();
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_SUBTYPE, "CAPTAIN");

        MappingBuilder<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> builder =
                evaluator.createMappingBuilder(
                        MAPPING_TIME_FROM_TO_FILENAME, getTestNameShort(), "title", delta);
        builder.now(TIME_BETWEEN);

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("CAPTAIN"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("PIRATE"));

        assertNextRecompute(mapping, TIME_MAPPING_DISABLED_PLUS_10D);
    }

    @Test
    public void testAfterTimeTo() throws Exception {
        // GIVEN
        OperationResult opResult = createOperationResult();
        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        UserType.F_SUBTYPE, "CAPTAIN");

        MappingBuilder<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> builder =
                evaluator.createMappingBuilder(
                        MAPPING_TIME_FROM_TO_FILENAME, getTestNameShort(), "title", delta);
        builder.now(TIME_FUTURE);

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        assertNullTriple(outputTriple);

        assertNextRecompute(mapping, null);
    }

    @Test
    public void testExistenceBefore() throws Exception {
        // GIVEN
        OperationResult opResult = createOperationResult();
        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                evaluator.createMappingBuilder(
                        MAPPING_TIME_ACTIVATION, getTestNameShort(), "title", null);

        builder.now(TIME_PAST);

        PrismPropertyDefinition<Boolean> existenceDef =
                evaluator.getPrismContext().definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
        builder.defaultTargetDefinition(existenceDef);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
        assertNullTriple(outputTriple);

        assertNextRecompute(mapping, TIME_MAPPING_DISABLED_PLUS_1M);
    }

    @Test
    public void testExistenceAfter() throws Exception {
        // GIVEN
        OperationResult opResult = createOperationResult();
        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                evaluator.createMappingBuilder(
                        MAPPING_TIME_ACTIVATION, getTestNameShort(), "title", null);

        builder.now(TIME_FUTURE);

        PrismPropertyDefinition<Boolean> existenceDef =
                evaluator.getPrismContext().definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
        builder.defaultTargetDefinition(existenceDef);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleZero(outputTriple, false);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertNextRecompute(mapping, null);
    }

    @Test
    public void testNoReferenceTime() throws Exception {
        // GIVEN
        OperationResult opResult = createOperationResult();
        PrismObject<UserType> userOld = evaluator.getUserOld();
        userOld.asObjectable().getActivation().setDisableTimestamp(null);

        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                evaluator.createMappingBuilder(
                        MAPPING_TIME_ACTIVATION, getTestNameShort(), "title", null, userOld);

        builder.now(TIME_PAST);

        PrismPropertyDefinition<Boolean> existenceDef =
                evaluator.getPrismContext().definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
        builder.defaultTargetDefinition(existenceDef);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
        assertNullTriple(outputTriple);

        assertNextRecompute(mapping, null);
    }

    @Test
    public void testSetReferenceTimeBefore() throws Exception {
        // GIVEN
        OperationResult opResult = createOperationResult();
        PrismObject<UserType> userOld = evaluator.getUserOld();
        XMLGregorianCalendar disableTimestamp = userOld.asObjectable().getActivation().getDisableTimestamp();
        userOld.asObjectable().getActivation().setDisableTimestamp(null);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
                        disableTimestamp);

        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                evaluator.createMappingBuilder(
                        MAPPING_TIME_ACTIVATION, getTestNameShort(), "title", delta, userOld);

        builder.now(TIME_PAST);

        PrismPropertyDefinition<Boolean> existenceDef =
                evaluator.getPrismContext().definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
        builder.defaultTargetDefinition(existenceDef);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
        assertNullTriple(outputTriple);

        assertNextRecompute(mapping, TIME_MAPPING_DISABLED_PLUS_1M);
    }

    @Test
    public void testSetReferenceTimeAfter() throws Exception {
        // GIVEN
        OperationResult opResult = createOperationResult();
        PrismObject<UserType> userOld = evaluator.getUserOld();
        XMLGregorianCalendar disableTimestamp = userOld.asObjectable().getActivation().getDisableTimestamp();
        userOld.asObjectable().getActivation().setDisableTimestamp(null);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, MappingTestEvaluator.USER_OLD_OID,
                        ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
                        disableTimestamp);

        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                evaluator.createMappingBuilder(
                        MAPPING_TIME_ACTIVATION, getTestNameShort(), "title", delta, userOld);

        builder.now(TIME_FUTURE);

        PrismPropertyDefinition<Boolean> existenceDef =
                evaluator.getPrismContext().definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
        builder.defaultTargetDefinition(existenceDef);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        // WHEN
        mapping.evaluate(createTask(), opResult);

        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
        PrismAsserts.assertTripleZero(outputTriple, false);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertNextRecompute(mapping, null);
    }

    private void assertNullTriple(PrismValueDeltaSetTriple<?> outputTriple) {
        assertNull("Unexpected output triple: " + outputTriple, outputTriple);
    }

    private void assertNextRecompute(MappingImpl<?, ?> mapping, XMLGregorianCalendar expected) {
        XMLGregorianCalendar nextRecomputeTime = mapping.getNextRecomputeTime();
        assertEquals("Wrong nextRecomputeTime in mapping " + mapping, expected, nextRecomputeTime);
    }
}
