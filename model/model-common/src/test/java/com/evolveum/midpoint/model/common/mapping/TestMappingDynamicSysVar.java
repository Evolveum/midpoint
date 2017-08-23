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
import java.math.BigDecimal;
import java.math.BigInteger;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 */
public class TestMappingDynamicSysVar {
	
	private static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	private static final String PATTERN_NUMERIC = "^\\d+$";
	
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
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectTrueXPath() throws Exception {
    	testScriptSystemVariablesConditionAddObjectTrue("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionAddObjectTrue(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectTrue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	user.asObjectable().getEmployeeType().add("CAPTAIN");
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
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
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectTrueGroovyUnrelated";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("employeeNumber"), evaluator.getPrismContext(), "666");
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				"mapping-script-system-variables-condition-groovy.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalse("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseSourcecontextGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalse("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseXPath() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalse("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionAddObjectFalse(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	user.asObjectable().getEmployeeType().add("SAILOR");
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected output triple: "+outputTriple, outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoValGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoVal("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoValSourcecontextGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoVal("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoValXPath() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoVal("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionAddObjectFalseNoVal(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectFalseNoVal";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	PrismProperty<String> employeeTypeProperty = user.findProperty(UserType.F_EMPLOYEE_TYPE);
    	employeeTypeProperty.clear();
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected output triple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoPropertyGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoProperty("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoPropertySourcecontextGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoProperty("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoPropertyXPath() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoProperty("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionAddObjectFalseNoProperty(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectFalseNoProperty";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.removeProperty(UserType.F_EMPLOYEE_TYPE);
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected output triple: "+outputTriple, outputTriple);
    }

    
    @Test
    public void testScriptSystemVariablesConditionTrueToTrueGroovy() throws Exception {
    	testScriptSystemVariablesConditionTrueToTrue("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionTrueToTrueXPath() throws Exception {
    	testScriptSystemVariablesConditionTrueToTrue("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionTrueToTrue(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToTrue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("CAPTAIN");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
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
    
    @Test
    public void testScriptSystemVariablesConditionFalseToFalseXPath() throws Exception {
    	testScriptSystemVariablesConditionFalseToFalse("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionFalseToFalse(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionFalseToFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionFalseToTrueGroovy() throws Exception {
    	testScriptSystemVariablesConditionFalseToTrue("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionFalseToTrueXPath() throws Exception {
    	testScriptSystemVariablesConditionFalseToTrue("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionFalseToTrue(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionFalseToTrue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	delta.addModificationAddProperty(evaluator.toPath("employeeType"), "CAPTAIN");
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, TEST_NAME, "title", delta);
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
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
    
    @Test
    public void testScriptSystemVariablesConditionTrueToFalseXPath() throws Exception {
    	testScriptSystemVariablesConditionTrueToFalse("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionTrueToFalse(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), "Jack");
    	delta.addModificationDeleteProperty(evaluator.toPath("employeeType"), "CAPTAIN");
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("CAPTAIN");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptyTrue() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptyTrue";
    	testScriptSystemVariablesConditionEmptyTrue(TEST_NAME, "mapping-script-system-variables-condition-empty.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptyTrueFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptyTrueFunction";
    	testScriptSystemVariablesConditionEmptyTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-function.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrue() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleTrue";
    	testScriptSystemVariablesConditionEmptyTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-single.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleTrueFunction";
    	testScriptSystemVariablesConditionEmptyTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-single-function.xml");
    }
    
    public void testScriptSystemVariablesConditionEmptyTrue(final String TEST_NAME, String filename) throws Exception {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().clear();
		user.asObjectable().setEmployeeNumber(null);
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Landlubber Jack"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseToTrue() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleFalseToTrue";
    	testScriptSystemVariablesConditionEmptyFalseToTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-single.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseToTrueFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleFalseToTrueFunction";
    	testScriptSystemVariablesConditionEmptyFalseToTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-single-function.xml");
    }
    
    public void testScriptSystemVariablesConditionEmptyFalseToTrue(final String TEST_NAME, String filename) throws Exception {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("employeeNumber"), evaluator.getPrismContext());
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().setEmployeeNumber("666");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionEmptyFalse() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptyFalse";
    	testScriptSystemVariablesConditionEmptyFalse(TEST_NAME, "mapping-script-system-variables-condition-empty.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptyFalseFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptyFalse";
    	testScriptSystemVariablesConditionEmptyFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-function.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalse() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleFalse";
    	testScriptSystemVariablesConditionEmptyFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-single.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleFalseFunction";
    	testScriptSystemVariablesConditionEmptyFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-single-function.xml");
    }
    
    public void testScriptSystemVariablesConditionEmptyFalse(final String TEST_NAME, String filename) throws Exception {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("SAILOR");
		user.asObjectable().setEmployeeNumber("666");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueToFalse() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleTrueToFalse";
    	testScriptSystemVariablesConditionEmptyTrueToFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-single.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueToFalseFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleTrueToFalseFunction";
    	testScriptSystemVariablesConditionEmptyTrueToFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-single-function.xml");
    }
    
    public void testScriptSystemVariablesConditionEmptyTrueToFalse(final String TEST_NAME, String filename) throws Exception {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("employeeNumber"), evaluator.getPrismContext(), "666");
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().setEmployeeNumber(null);
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
    }

    @Test
    public void testNpeFalseToTrue() throws Exception {
    	final String TEST_NAME = "testNpeFalseToTrue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext(), "Captain Sparrow");
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				"mapping-npe.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("15"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testNpeTrueToFalse() throws Exception {
    	final String TEST_NAME = "testNpeTrueToFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext());
    	
		Mapping<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> mapping = evaluator.createMapping(
				"mapping-npe.xml", 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().setAdditionalName(PrismTestUtil.createPolyStringType("Sultan of the Caribbean"));
		mapping.getSourceContext().recompute();
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("23"));
    }
    
    @Test
    public void testPathEnum() throws Exception {
    	final String TEST_NAME = "testPathEnum";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, evaluator.getPrismContext(), ActivationStatusType.DISABLED);
    	
		Mapping<PrismPropertyValue<String>,PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
				"mapping-path-enum.xml", 
    			TEST_NAME, "costCenter", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		System.out.println("Output triple");
		System.out.println(outputTriple.debugDump());
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, ActivationStatusType.DISABLED.value());
	  	PrismAsserts.assertTripleMinus(outputTriple, ActivationStatusType.ENABLED.value());
    }
    
    @Test
    public void testEmployeeNumberString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-employee-number.xml",
    			"testEmployeeNumberString",
    			"employeeType",					// target
    			"employeeNumber",				// changed property
    			"666");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, "666");
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testEmployeeNumberPolyString() throws Exception {
    	final String TEST_NAME = "testEmployeeNumberPolyString";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-employee-number.xml",
    			TEST_NAME,
    			"additionalName",					// target
    			"employeeNumber",				// changed property
    			"666");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("666"));
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    	
    	// Make sure it is recomputed
    	PolyString plusval = outputTriple.getPlusSet().iterator().next().getValue();
    	System.out.println("Plus polystring\n"+ plusval.debugDump());
    	assertEquals("Wrong norm value", "666", plusval.getNorm());
    }

    @Test
    public void testEmployeeNumberInt() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<Integer>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-employee-number.xml",
    			"testEmployeeNumberString",
    			new ItemPath(UserType.F_EXTENSION, SchemaTestConstants.EXTENSION_INT_TYPE_ELEMENT),					// target
    			"employeeNumber",				// changed property
    			"666");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, 666);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeNumberInteger() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<Integer>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-employee-number.xml",
    			"testEmployeeNumberString",
    			new ItemPath(UserType.F_EXTENSION, SchemaTestConstants.EXTENSION_INTEGER_TYPE_ELEMENT),					// target
    			"employeeNumber",				// changed property
    			"666");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, new BigInteger("666"));
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeNumberLong() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<Long>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-employee-number.xml",
    			"testEmployeeNumberString",
    			new ItemPath(UserType.F_EXTENSION, SchemaTestConstants.EXTENSION_LONG_TYPE_ELEMENT),					// target
    			"employeeNumber",				// changed property
    			"666");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, 666L);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeNumberDecimal() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<Integer>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-employee-number.xml",
    			"testEmployeeNumberString",
    			new ItemPath(UserType.F_EXTENSION, SchemaTestConstants.EXTENSION_DECIMAL_TYPE_ELEMENT),					// target
    			"employeeNumber",				// changed property
    			"666.33");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, new BigDecimal("666.33"));
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeNumberProtectedString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-employee-number.xml",
    			"testEmployeeNumberProtectedString",
    			new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),					// target
    			"employeeNumber",				// changed property
    			"666");	// changed values
    	
    	// THEN
    	
    	evaluator.assertProtectedString("plus set", outputTriple.getPlusSet(), "666");
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeTypeDeltaAreplaceB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaAreplaceB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), "B");
    	
		Mapping<PrismPropertyValue<String>,PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
				"mapping-script-system-variables-employee-type.xml", 
    			TEST_NAME, "employeeType", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		setEmployeeType(user.asObjectable(), "A");
		mapping.getSourceContext().recompute();
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		evaluator.assertResult(opResult);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "B");
	  	PrismAsserts.assertTripleMinus(outputTriple, "A");
    }
    
    @Test
    public void testEmployeeTypeDeltaNullreplaceB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaNullreplaceB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), "B");
    	
		Mapping<PrismPropertyValue<String>,PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
				"mapping-script-system-variables-employee-type.xml", 
    			TEST_NAME, "employeeType", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		setEmployeeType(user.asObjectable());
		mapping.getSourceContext().recompute();
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		evaluator.assertResult(opResult);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "B");
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeTypeDeltaBreplaceB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaBreplaceB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), "B");
    	
		Mapping<PrismPropertyValue<String>,PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
				"mapping-script-system-variables-employee-type.xml", 
    			TEST_NAME, "employeeType", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		setEmployeeType(user.asObjectable(), "B");
		mapping.getSourceContext().recompute();
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		evaluator.assertResult(opResult);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "B");
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeTypeDeltaAaddB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaAaddB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	    	
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
    			employeeTypeDeltaABAdd(TEST_NAME, "B", "A");
    	
    	// THEN
		PrismAsserts.assertTripleZero(outputTriple, "A");
	  	PrismAsserts.assertTriplePlus(outputTriple, "B");
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeTypeDeltaABaddB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaABaddB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	    	
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
    			employeeTypeDeltaABAdd(TEST_NAME, "B", "A", "B");
    	
    	// THEN
		PrismAsserts.assertTripleZero(outputTriple, "A");
	  	PrismAsserts.assertTriplePlus(outputTriple, "B");
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testEmployeeTypeDeltaBaddB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaBaddB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	    	
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
    			employeeTypeDeltaABAdd(TEST_NAME, "B", "B");
    	
    	// THEN
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "B");
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testEmployeeTypeDeltaNulladdB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaNulladdB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	    	
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
    			employeeTypeDeltaABAdd(TEST_NAME, "B");
    	
    	// THEN
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "B");
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    public PrismValueDeltaSetTriple<PrismPropertyValue<String>> employeeTypeDeltaABAdd(
    		final String TEST_NAME, String addVal, String... oldVals) throws Exception {
    	TestUtil.displayTestTitle(TEST_NAME);
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationAddProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), addVal);
    	
		Mapping<PrismPropertyValue<String>,PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
				"mapping-script-system-variables-employee-type.xml", 
    			TEST_NAME, "employeeType", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		setEmployeeType(user.asObjectable(), oldVals);
		mapping.getSourceContext().recompute();
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		evaluator.assertResult(opResult);
		return mapping.getOutputTriple();
    }
    
    private void setEmployeeType(UserType userType, String... vals) {
    	userType.getEmployeeType().clear();
		for (String val: vals) {
			userType.getEmployeeType().add(val);
		}
    }
    
    @Test
    public void testEmployeeTypeDeltaBdeleteB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaBdeleteB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	    	    	
    	// WHEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = 
				employeeTypeDeltaDelete(TEST_NAME, "B", "B");
    	
    	// THEN
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, "B");
    }

    @Test
    public void testEmployeeTypeDeltaABdeleteB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaABdeleteB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	    	    	
    	// WHEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = 
				employeeTypeDeltaDelete(TEST_NAME, "B", "A", "B");
    	
    	// THEN
		PrismAsserts.assertTripleZero(outputTriple, "A");
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, "B");
    }

    @Test
    public void testEmployeeTypeDeltaAdeleteB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaAdeleteB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	    	    	
    	// WHEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = 
				employeeTypeDeltaDelete(TEST_NAME, "B", "A");
    	
    	// THEN
		PrismAsserts.assertTripleZero(outputTriple, "A");
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, "B");
    }

    @Test
    public void testEmployeeTypeDeltaNulldeleteB() throws Exception {
    	final String TEST_NAME = "testEmployeeTypeDeltaNulldeleteB";
    	TestUtil.displayTestTitle(TEST_NAME);
    	    	    	
    	// WHEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = 
				employeeTypeDeltaDelete(TEST_NAME, "B");
    	
    	// THEN
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, "B");
    }

    public PrismValueDeltaSetTriple<PrismPropertyValue<String>> employeeTypeDeltaDelete(final String TEST_NAME, String delVal, String... oldVals) throws Exception {
    	TestUtil.displayTestTitle(TEST_NAME);
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationDeleteProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_EMPLOYEE_TYPE, evaluator.getPrismContext(), delVal);
    	
		Mapping<PrismPropertyValue<String>,PrismPropertyDefinition<String>> mapping = evaluator.createMapping(
				"mapping-script-system-variables-employee-type.xml", 
    			TEST_NAME, "employeeType", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		setEmployeeType(user.asObjectable(), oldVals);
		mapping.getSourceContext().recompute();
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		evaluator.assertResult(opResult);
		return mapping.getOutputTriple();
    }
    
    @Test
    public void testPasswordString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-password.xml",
    			"testPasswordString",
    			"employeeType",					// target
    			new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),				// changed property
    			evaluator.createProtectedString("weighAnch0r"));	// changed values
    	
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
    			"testPasswordPolyString",
    			UserType.F_ADDITIONAL_NAME.getLocalPart(),					// target
    			new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),	// changed property
    			evaluator.createProtectedString("weighAnch0r"));	// changed values
    	
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
    			"testPasswordPolyString",
    			new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),					// target
    			new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),	// changed property
    			evaluator.createProtectedString("weighAnch0r"));	// changed values
    	
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
    			"testPasswordDecryptString",
    			"employeeType",					// target
    			new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),				// changed property
    			evaluator.createProtectedString("weighAnch0r"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, "weighAnch0r123");
    	PrismAsserts.assertTripleMinus(outputTriple, "d3adM3nT3llN0Tal3s123");
    }
}
