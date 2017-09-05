/*
 * Copyright (c) 2010-2017 Evolveum
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

import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

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
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests for mapping domain. Those are multival input and multival output.
 *
 * MID-3692
 * @author Radovan Semancik
 */
public class TestMappingDomain {

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
    	final String TEST_NAME = "testControlReplaceSingleValue";
    	TestUtil.displayTestTitle(this, TEST_NAME);

    	// GIVEN

    	PrismObject<UserType> userOld = evaluator.getUserOld();
    	List<String> employeeTypeOld = userOld.asObjectable().getEmployeeType();
    	employeeTypeOld.clear();
    	employeeTypeOld.add("1234567890");

    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext(), "Jackie");
    	delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_TYPE, "321");

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_DOMAIN_FILENAME,
    			TEST_NAME, "organization", delta, userOld);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		outputTriple.checkConsistence();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Pirate Jackie (321)"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Pirate null (1234567890)"));
    }

    /**
     * Control. All goes well here. All values in the domain.
     */
    @Test
    public void testControlReplaceMultiValue() throws Exception {
    	final String TEST_NAME = "testControlReplaceMultiValue";
    	TestUtil.displayTestTitle(this, TEST_NAME);

    	// GIVEN

    	PrismObject<UserType> userOld = evaluator.getUserOld();
    	List<String> employeeTypeOld = userOld.asObjectable().getEmployeeType();
    	employeeTypeOld.clear();
    	employeeTypeOld.add("001");
    	employeeTypeOld.add("002");
    	employeeTypeOld.add("003");

    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext(), "Jackie");
    	delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_TYPE, "991", "992");

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_DOMAIN_FILENAME,
    			TEST_NAME, "organization", delta, userOld);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

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
    	final String TEST_NAME = "testReplaceMixedMultiValue";
    	TestUtil.displayTestTitle(this, TEST_NAME);

    	// GIVEN

    	PrismObject<UserType> userOld = evaluator.getUserOld();
    	List<String> employeeTypeOld = userOld.asObjectable().getEmployeeType();
    	employeeTypeOld.clear();
    	employeeTypeOld.add("001");
    	employeeTypeOld.add("A02");
    	employeeTypeOld.add("B03");
    	employeeTypeOld.add("004");

    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext(), "Jackie");
    	delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_TYPE, "X91", "992", "Y93", "994");

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_DOMAIN_FILENAME,
    			TEST_NAME, "organization", delta, userOld);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
    	TestUtil.displayWhen(TEST_NAME);
		mapping.evaluate(null, opResult);

    	// THEN
		TestUtil.displayThen(TEST_NAME);
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		display("Output triple", outputTriple);
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
    	final String TEST_NAME = "testAddMixedMultiValue";
    	TestUtil.displayTestTitle(this, TEST_NAME);

    	// GIVEN

    	PrismObject<UserType> userOld = evaluator.getUserOld();
    	userOld.asObjectable().setAdditionalName(PrismTestUtil.createPolyStringType("Jackie"));
    	List<String> employeeTypeOld = userOld.asObjectable().getEmployeeType();
    	employeeTypeOld.clear();
    	employeeTypeOld.add("001");
    	employeeTypeOld.add("A02");
    	employeeTypeOld.add("B03");
    	employeeTypeOld.add("004");

    	ObjectDelta<UserType> delta = ObjectDelta.createModificationAddProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), "X91", "992", "Y93", "994");

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_DOMAIN_FILENAME,
    			TEST_NAME, "organization", delta, userOld);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
    	TestUtil.displayWhen(TEST_NAME);
		mapping.evaluate(null, opResult);

    	// THEN
		TestUtil.displayThen(TEST_NAME);
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		display("Output triple", outputTriple);
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
    	final String TEST_NAME = "testDeleteMixedMultiValue";
    	TestUtil.displayTestTitle(this, TEST_NAME);

    	// GIVEN

    	PrismObject<UserType> userOld = evaluator.getUserOld();
    	userOld.asObjectable().setAdditionalName(PrismTestUtil.createPolyStringType("Jackie"));
    	List<String> employeeTypeOld = userOld.asObjectable().getEmployeeType();
    	employeeTypeOld.clear();
    	employeeTypeOld.add("001");
    	employeeTypeOld.add("A02");
    	employeeTypeOld.add("B03");
    	employeeTypeOld.add("004");
    	employeeTypeOld.add("005");
    	employeeTypeOld.add("C06");
    	employeeTypeOld.add("007");

    	ObjectDelta<UserType> delta = ObjectDelta.createModificationDeleteProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), "005", "C06", "007");

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_DOMAIN_FILENAME,
    			TEST_NAME, "organization", delta, userOld);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
    	TestUtil.displayWhen(TEST_NAME);
		mapping.evaluate(null, opResult);

    	// THEN
		TestUtil.displayThen(TEST_NAME);
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		display("Output triple", outputTriple);
		outputTriple.checkConsistence();
		PrismAsserts.assertTripleZero(outputTriple,
	  			PrismTestUtil.createPolyString("Pirate Jackie (001)"),
	  			PrismTestUtil.createPolyString("Pirate Jackie (004)"));
		PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple,
	  			PrismTestUtil.createPolyString("Pirate Jackie (005)"),
	  			PrismTestUtil.createPolyString("Pirate Jackie (007)"));
    }

}
