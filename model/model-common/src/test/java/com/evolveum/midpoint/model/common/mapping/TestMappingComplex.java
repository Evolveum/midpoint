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

import java.io.IOException;

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
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 */
public class TestMappingComplex {

	private static final String MAPPING_COMPLEX_FILENAME = "mapping-complex-captain.xml";

	private MappingTestEvaluator evaluator;

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
    	evaluator = new MappingTestEvaluator();
    	evaluator.init();
    }

    @Test
    public void testModifyObjectSetAdditionalName() throws Exception {
    	final String TEST_NAME = "testModifyObjectSetAdditionalName";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext(), "Jackie");
    	delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, "321");

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_COMPLEX_FILENAME,
    			TEST_NAME, "title", delta);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		outputTriple.checkConsistence();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Pirate Jackie (#321)"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Pirate null (#null)"));
    }

    @Test
    public void testModifyObjectSetAdditionalNameFalse() throws Exception {
    	final String TEST_NAME = "testModifyObjectSetAdditionalNameFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext(), "Jackie");
    	delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, "321");

		PrismObject<UserType> userOld = evaluator.getUserOld();
		userOld.asObjectable().getEmployeeType().clear();
		userOld.asObjectable().getEmployeeType().add("WHATEVER");
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_COMPLEX_FILENAME,
    			TEST_NAME, "title", delta, userOld);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }

    /**
     * Change property that is not a source in this mapping
     */
    @Test
    public void testModifyObjectUnrelated() throws Exception {
    	final String TEST_NAME = "testModifyObjectUnrelated";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			evaluator.toPath("costCenter"), evaluator.getPrismContext(), "X606");

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_COMPLEX_FILENAME,
    			TEST_NAME, "title", delta);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		outputTriple.checkConsistence();
		PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Pirate null (#null)"));
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testModifyObjectUnrelatedFalse() throws Exception {
    	final String TEST_NAME = "testModifyObjectUnrelatedFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID,
    			evaluator.toPath("costCenter"), evaluator.getPrismContext(), "X606");

    	PrismObject<UserType> userOld = evaluator.getUserOld();
		userOld.asObjectable().getEmployeeType().clear();
		userOld.asObjectable().getEmployeeType().add("WHATEVER");
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_COMPLEX_FILENAME,
    			TEST_NAME, "title", delta, userOld);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }

    @Test
    public void testAddObjectUnrelatedFalse() throws Exception {
    	final String TEST_NAME = "testAddObjectUnrelatedFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	PrismObject<UserType> user = evaluator.getUserOld();
		user.asObjectable().getEmployeeType().clear();
		user.asObjectable().getEmployeeType().add("WHATEVER");
		ObjectDelta<UserType> delta = user.createAddDelta();

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_COMPLEX_FILENAME,
    			TEST_NAME, "title", delta);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }

    @Test
    public void testAddObjectUnrelatedEmptyFalse() throws Exception {
    	final String TEST_NAME = "testAddObjectUnrelatedEmptyFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");

    	// GIVEN
    	PrismObject<UserType> user = evaluator.getUserOld();
		user.asObjectable().getEmployeeType().clear();
		ObjectDelta<UserType> delta = user.createAddDelta();

		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				MAPPING_COMPLEX_FILENAME,
    			TEST_NAME, "title", delta);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);

    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }

}
