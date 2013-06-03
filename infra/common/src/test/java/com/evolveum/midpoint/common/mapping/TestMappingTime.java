/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common.mapping;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertFalse;

import static com.evolveum.midpoint.prism.util.PrismAsserts.*;
import static com.evolveum.midpoint.common.mapping.MappingTestEvaluator.*;

import com.evolveum.midpoint.common.CommonTestConstants;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;

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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				MAPPING_TIME_FROM_TO_FILENAME, 
    			TEST_NAME, "title", delta);
		mapping.setNow(TIME_PAST);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				MAPPING_TIME_FROM_TO_FILENAME, 
    			TEST_NAME, "title", delta);
		mapping.setNow(TIME_BETWEEN);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				MAPPING_TIME_FROM_TO_FILENAME, 
    			TEST_NAME, "title", delta);
		mapping.setNow(TIME_FUTURE);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
		Mapping<PrismPropertyValue<Boolean>> mapping = evaluator.createMapping(
				MAPPING_TIME_ACTIVATION, 
    			TEST_NAME, "title", null);
		
		mapping.setNow(TIME_PAST);
		
		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinition<Boolean>(
				ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		mapping.setDefaultTargetDefinition(existenceDef);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
		Mapping<PrismPropertyValue<Boolean>> mapping = evaluator.createMapping(
				MAPPING_TIME_ACTIVATION, 
    			TEST_NAME, "title", null);
		
		mapping.setNow(TIME_FUTURE);
		
		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinition<Boolean>(
				ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		mapping.setDefaultTargetDefinition(existenceDef);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<Boolean>> mapping = evaluator.createMapping(
				MAPPING_TIME_ACTIVATION, 
    			TEST_NAME, "title", null, userOld);
		
		mapping.setNow(TIME_PAST);
		
		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinition<Boolean>(
				ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		mapping.setDefaultTargetDefinition(existenceDef);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<Boolean>> mapping = evaluator.createMapping(
				MAPPING_TIME_ACTIVATION, 
    			TEST_NAME, "title", delta, userOld);
		
		mapping.setNow(TIME_PAST);
		
		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinition<Boolean>(
				ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		mapping.setDefaultTargetDefinition(existenceDef);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<Boolean>> mapping = evaluator.createMapping(
				MAPPING_TIME_ACTIVATION, 
    			TEST_NAME, "title", delta, userOld);
		
		mapping.setNow(TIME_FUTURE);
		
		PrismPropertyDefinition<Boolean> existenceDef = new PrismPropertyDefinition<Boolean>(
				ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, evaluator.getPrismContext());
		mapping.setDefaultTargetDefinition(existenceDef);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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

	private void assertNextRecompute(Mapping<?> mapping, XMLGregorianCalendar expected) {
		XMLGregorianCalendar nextRecomputeTime = mapping.getNextRecomputeTime();
		assertEquals("Wrong nextRecomputeTime in mapping "+mapping, expected, nextRecomputeTime);
	}
                
}
