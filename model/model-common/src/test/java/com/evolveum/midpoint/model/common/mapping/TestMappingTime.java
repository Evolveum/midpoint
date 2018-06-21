/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.common.mapping;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
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
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 */
public class TestMappingTime {

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
    	final String TEST_NAME = "testBeforeTimeFrom";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), "CAPTAIN");

		MappingImpl.Builder<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> builder = evaluator.createMappingBuilder(
				MAPPING_TIME_FROM_TO_FILENAME,
    			TEST_NAME, "title", delta);
		builder.setNow(TIME_PAST);

		MappingImpl<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = builder.build();

    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNullTriple(outputTriple);
		assertNextRecompute(mapping, TIME_MAPPING_DISABLED_PLUS_1D);
    }

    @Test
    public void testBetweenTimes() throws Exception {
    	final String TEST_NAME = "testBetweenTimes";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), "CAPTAIN");

		MappingImpl.Builder<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> builder = evaluator.createMappingBuilder(
				MAPPING_TIME_FROM_TO_FILENAME,
    			TEST_NAME, "title", delta);
		builder.setNow(TIME_BETWEEN);

		MappingImpl<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = builder.build();

		OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("CAPTAIN"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("PIRATE"));

		assertNextRecompute(mapping, TIME_MAPPING_DISABLED_PLUS_10D);
    }

	@Test
    public void testAfterTimeTo() throws Exception {
    	final String TEST_NAME = "testAfterTimeTo";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), "CAPTAIN");

		MappingImpl.Builder<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> builder = evaluator.createMappingBuilder(
				MAPPING_TIME_FROM_TO_FILENAME,
    			TEST_NAME, "title", delta);
		builder.setNow(TIME_FUTURE);

		MappingImpl<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = builder.build();

    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNullTriple(outputTriple);
	
		assertNextRecompute(mapping, null);
    }

	@Test
    public void testExistenceBefore() throws Exception {
    	final String TEST_NAME = "testExistenceBefore";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
		MappingImpl.Builder<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> builder = evaluator.createMappingBuilder(
				MAPPING_TIME_ACTIVATION,
    			TEST_NAME, "title", null);

		builder.setNow(TIME_PAST);

		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinitionImpl<>(
				ExpressionConstants.OUTPUT_ELEMENT_NAME,
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		builder.setDefaultTargetDefinition(existenceDef);

		MappingImpl<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> mapping = builder.build();

    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
		assertNullTriple(outputTriple);

		assertNextRecompute(mapping, TIME_MAPPING_DISABLED_PLUS_1M);
    }

	@Test
    public void testExistenceAfter() throws Exception {
    	final String TEST_NAME = "testExistenceAfter";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
		MappingImpl.Builder<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> builder = evaluator.createMappingBuilder(
				MAPPING_TIME_ACTIVATION,
    			TEST_NAME, "title", null);

		builder.setNow(TIME_FUTURE);

		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinitionImpl<>(
				ExpressionConstants.OUTPUT_ELEMENT_NAME,
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		builder.setDefaultTargetDefinition(existenceDef);

		MappingImpl<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> mapping = builder.build();

    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, false);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleNoMinus(outputTriple);

		assertNextRecompute(mapping, null);
    }

	@Test
    public void testNoReferenceTime() throws Exception {
    	final String TEST_NAME = "testNoReferenceTime";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN

    	PrismObject<UserType> userOld = evaluator.getUserOld();
		userOld.asObjectable().getActivation().setDisableTimestamp(null);

		MappingImpl.Builder<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> builder = evaluator.createMappingBuilder(
				MAPPING_TIME_ACTIVATION,
    			TEST_NAME, "title", null, userOld);

		builder.setNow(TIME_PAST);

		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinitionImpl<>(
				ExpressionConstants.OUTPUT_ELEMENT_NAME,
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		builder.setDefaultTargetDefinition(existenceDef);

		MappingImpl<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> mapping = builder.build();

		OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
		assertNullTriple(outputTriple);

		assertNextRecompute(mapping, null);
    }

	@Test
    public void testSetReferenceTimeBefore() throws Exception {
    	final String TEST_NAME = "testSetReferenceTimeBefore";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN

    	PrismObject<UserType> userOld = evaluator.getUserOld();
    	XMLGregorianCalendar disableTimestamp = userOld.asObjectable().getActivation().getDisableTimestamp();
		userOld.asObjectable().getActivation().setDisableTimestamp(null);

		ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			new ItemPath(UserType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP), evaluator.getPrismContext(),
    			disableTimestamp);

		MappingImpl.Builder<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> builder = evaluator.createMappingBuilder(
				MAPPING_TIME_ACTIVATION,
    			TEST_NAME, "title", delta, userOld);

		builder.setNow(TIME_PAST);

		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinitionImpl<>(
				ExpressionConstants.OUTPUT_ELEMENT_NAME,
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		builder.setDefaultTargetDefinition(existenceDef);

		MappingImpl<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> mapping = builder.build();

		OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
		assertNullTriple(outputTriple);

		assertNextRecompute(mapping, TIME_MAPPING_DISABLED_PLUS_1M);
    }

	@Test
    public void testSetReferenceTimeAfter() throws Exception {
    	final String TEST_NAME = "testSetReferenceTimeAfter";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	PrismObject<UserType> userOld = evaluator.getUserOld();
    	XMLGregorianCalendar disableTimestamp = userOld.asObjectable().getActivation().getDisableTimestamp();
		userOld.asObjectable().getActivation().setDisableTimestamp(null);

		ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			new ItemPath(UserType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP), evaluator.getPrismContext(),
    			disableTimestamp);

		MappingImpl.Builder<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> builder = evaluator.createMappingBuilder(
				MAPPING_TIME_ACTIVATION,
    			TEST_NAME, "title", delta, userOld);

		builder.setNow(TIME_FUTURE);

		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinitionImpl<>(
				ExpressionConstants.OUTPUT_ELEMENT_NAME,
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		builder.setDefaultTargetDefinition(existenceDef);

		MappingImpl<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> mapping = builder.build();

		OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, false);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleNoMinus(outputTriple);

		assertNextRecompute(mapping, null);
    }

	private void assertNullTriple(PrismValueDeltaSetTriple<?> outputTriple) {
		assertNull("Unexpected output triple: "+outputTriple, outputTriple);
	}

	private void assertNextRecompute(MappingImpl<?,?> mapping, XMLGregorianCalendar expected) {
		XMLGregorianCalendar nextRecomputeTime = mapping.getNextRecomputeTime();
		assertEquals("Wrong nextRecomputeTime in mapping "+mapping, expected, nextRecomputeTime);
	}

}
