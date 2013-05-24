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
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
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
public class TestMappingDynamic {
	
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
    			"testValue",
    			"costCenter",				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueSingleShallow() throws Exception {
        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-single-shallow.xml",
    			"testValue",
    			"costCenter",				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueMultiDeep() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-multi-deep.xml",
    			"testValueMulti",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testValueMultiShallow() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-multi-shallow.xml",
    			"testValueMulti",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testValueSingleEnum() throws Exception {
        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ActivationStatusType>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-single-enum.xml",
    			"testValueSingleEnum",
    			new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, ActivationStatusType.ENABLED);
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsAdd() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-asis.xml",
    			"testAsIsAdd",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "PIRATE");
    	PrismAsserts.assertTriplePlus(outputTriple, "CAPTAIN", "SWASHBUCKLER");
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testAsIsDelete() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicDelete(
    			"mapping-asis.xml",
    			"testAsIsDelete",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"PIRATE");					// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleMinus(outputTriple, "PIRATE");    	
    }
    
    
    @Test
    public void testAsIsStringToPolyString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-asis.xml",
    			"testAsIsStringToPolyString",
    			"fullName");				// target
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("PIRATE"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathVariables() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables.xml",
    			"testPathVariables",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathVariablesNamespace() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-namespace.xml",
    			"testPathVariablesNamespace",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    
    @Test
    public void testPathVariablesPolyStringShort() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-polystring-short.xml",
    			"testPathVariablesPolyStringShort",
    			"fullName",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathVariablesPolyStringToStringShort() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-polystring-short.xml",
    			"testPathVariablesPolyStringToStringShort",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Jack Sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathVariablesPolyStringToStringLong() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-polystring-long.xml",
    			"testPathVariablesPolyStringToStringLong",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptSimpleXPath() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-simple-xpath.xml",
    			"testScriptSimpleXPath",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "fooBAR");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptSimpleGroovy() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-simple-groovy.xml",
    			"testScriptSimpleXPath",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "fooBAR");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesXPath() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-variables-xpath.xml",
    			"testScriptVariablesXPath",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesGroovy() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-variables-groovy.xml",
    			"testScriptVariablesGroovy",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testScriptVariablesPolyStringXPath() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-xpath.xml",
    			"testScriptVariablesPolyStringXPath",
    			"fullName",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain Jack Sparrow", "captain jack sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesPolyStringGroovy() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-groovy.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain Jack Sparrow", "captain jack sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesPolyStringGroovyOp() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-groovy-op.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain J. Sparrow", "captain j sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesPolyStringGroovyOrig() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-groovy-orig.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain J");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
        
    @Test
    public void testScriptVariablesPolyStringGroovyNorm() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-groovy-norm.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
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
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"fullName",				// changed property
    			PrismTestUtil.createPolyString("Barbossa"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, "Captain b");
    	PrismAsserts.assertTripleMinus(outputTriple, "Captain j");    	
    }

    @Test
    public void testAsIsVariablesPolyStringNorm() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-asis-system-variables-polystring-norm.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testAsIsVariablesPolyStringOrig() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-asis-system-variables-polystring-orig.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Jack Sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }


    @Test
    public void testScriptExtraVariablesRef() throws Exception {
    	// GIVEN
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-extra-variables.xml", 
    			"testScriptExtraVariablesRef", "employeeType", null);
    	
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	ObjectReferenceType ref = MiscSchemaUtil.createObjectReference(
        	"c0c010c0-d34d-b33f-f00d-111111111112",
        	UserType.COMPLEX_TYPE);
        vars.put(new QName(SchemaConstants.NS_C, "sailor"), ref);
        mapping.addVariableDefinitions(vars);
        
    	OperationResult opResult = new OperationResult("testScriptExtraVariablesRef");
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptExtraVariablesJaxb() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptExtraVariablesJaxb";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-extra-variables.xml", 
    			TEST_NAME, "employeeType", null);
    	
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(CommonTestConstants.OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111112.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        vars.put(new QName(SchemaConstants.NS_C, "sailor"), userType);
        mapping.addVariableDefinitions(vars);
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testScriptFullNameNoChange() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMapping(
    			"mapping-script-fullname.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName");					// target
    			
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testScriptFullNameReplaceGivenName() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-fullname.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName",					// target
    			"givenName",				// changed property
    			PrismTestUtil.createPolyString("Jackie"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie Sparrow"));
    	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }


    @Test
    public void testScriptRootNodeRef() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptRootNodeRef";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-root-node.xml", 
    			TEST_NAME, "locality", null);
    	
        mapping.setRootNode(MiscSchemaUtil.createObjectReference(
            	"c0c010c0-d34d-b33f-f00d-111111111111",
            	UserType.COMPLEX_TYPE));
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, new PolyString("Black Pearl", "black pearl"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptRootNodeJaxb() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptRootNodeJaxb";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-root-node.xml", 
    			TEST_NAME, "locality", null);
    	
    	PrismObject<UserType> user = PrismTestUtil.parseObject(new File(CommonTestConstants.OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"));
        mapping.setRootNode(user.asObjectable());
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, new PolyString("Black Pearl", "black pearl"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptListRelativeXPath() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-list-relative-xpath.xml",
    			"testScriptListRelativeXPath",
    			"organizationalUnit",					// target
    			"organizationalUnit",				// changed property
    			PrismTestUtil.createPolyString("Antropomorphic Personifications"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple,
    			PrismTestUtil.createPolyString("The Guild of Brethren of the Coast"), 
    			PrismTestUtil.createPolyString("The Guild of Davie Jones' Locker"));
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The Guild of Antropomorphic Personifications"));
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptListRelativeGroovy() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-list-relative-groovy.xml",
    			"testScriptListRelativeXPath",
    			"organizationalUnit",					// target
    			"organizationalUnit",				// changed property
    			PrismTestUtil.createPolyString("Antropomorphic Personifications"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple,
    			PrismTestUtil.createPolyString("The Guild of Brethren of the Coast"), 
    			PrismTestUtil.createPolyString("The Guild of Davie Jones' Locker"));
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The Guild of Antropomorphic Personifications"));
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    
    @Test
    public void testScriptListAbsoluteXPath() throws Exception {
    	testScriptListAbsolute("mapping-script-list-absolute-xpath.xml");
    }
    
    @Test
    public void testScriptListAbsoluteGroovy() throws Exception {
    	testScriptListAbsolute("mapping-script-list-absolute-groovy.xml");
    }
    

    public void testScriptListAbsolute(String fileName) throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			fileName,
    			"testScriptListAbsolute",
    			"organizationalUnit",					// target
    			"organizationalUnit",				// changed property
    			PrismTestUtil.createPolyString("Antropomorphic Personifications"));	// changed values
    	
    	// THEN
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
    			"testValueConditionTrue",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"DRUNKARD");				// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testValueConditionFalse() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-condition-false.xml",
    			"testValueConditionFalse",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"DRUNKARD");				// changed values
    	
    	// THEN
    	assertNull("Unexpected value in outputTriple", outputTriple);
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testConditionNonEmptyCaptain() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testConditionNonEmptyCaptain";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	user.asObjectable().getEmployeeType().add("CAPTAIN");
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-condition-nonempty.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The CAPTAIN"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testConditionNonEmptyEmpty() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testConditionNonEmptyEmpty";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	user.asObjectable().getEmployeeType().add("");
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-condition-nonempty.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple", outputTriple);
    }
    
    @Test
    public void testConditionNonEmptyNoValue() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testConditionNonEmptyNoValue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-condition-nonempty.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple", outputTriple);
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToTrueXPath";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("CAPTAIN");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, TEST_NAME, "title", delta);
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("CAPTAIN");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
    }
    
    public void testScriptTransformMulti() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToTrueXPath";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createEmptyModifyDelta(UserType.class, evaluator.USER_OLD_OID, evaluator.getPrismContext());
    	PropertyDelta<String> propDelta = delta.createPropertyModification(evaluator.toPath("employeeType"));
    	propDelta.addValueToAdd(new PrismPropertyValue<String>("CAPTAIN"));
    	propDelta.addValueToDelete(new PrismPropertyValue<String>("LANDLUBER"));
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-script-transform.xml", 
    			TEST_NAME, "organizationalUnit", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("LANDLUBER");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("The pirate deck"));
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The captain deck"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("The landluber deck"));
    }
    
    @Test
    public void testGenerateDefault() throws Exception {
    	final String TEST_NAME = "testGenerateDefault";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	final StringPolicyType stringPolicy = evaluator.getStringPolicy();
    	// GIVEN
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-generate.xml", 
    			TEST_NAME, stringPolicy, "employeeNumber", null);
    	
//    	StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
//			private ItemPath outputPath;
//			private ItemDefinition outputDefinition;
//			@Override
//			public void setOutputPath(ItemPath outputPath) {
//				this.outputPath = outputPath;
//			}
//			
//			@Override
//			public void setOutputDefinition(ItemDefinition outputDefinition) {
//				this.outputDefinition = outputDefinition;
//			}
//			
//			@Override
//			public StringPolicyType resolve() {
//				// No path. The the path is default
////				assertNotNull("Null outputPath", outputPath);
//				assertNotNull("Null outputDefinition", outputDefinition);
//				return stringPolicy;
//			}
//		};
    	
		OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN (1)
    	mapping.evaluate(opResult);

		// THEN (1)
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		String value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		System.out.println("Generated value (1): " + value1);
		assertGeneratedValue(value1, stringPolicy, null, false);

		// WHEN (2)
		mapping.evaluate(opResult);

		// THEN (2)
		outputTriple = mapping.getOutputTriple();
		String value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		System.out.println("Generated value (2): " + value2);
		assertGeneratedValue(value2, stringPolicy, null, false);
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		assertFalse("Generated the same value", value1.equals(value2));
    }

    @Test
    public void testGeneratePolicy() throws Exception {
    	final String TEST_NAME = "testGeneratePolicy";
    	generatePolicy(TEST_NAME, "mapping-generate-policy.xml", "c0c010c0-d34d-b33f-f00d-999888111111.xml", null);
    }
    
    @Test
    public void testGeneratePolicyBad() throws Exception {
    	final String TEST_NAME = "testGeneratePolicy";
    	try {
    		generatePolicy(TEST_NAME, "mapping-generate-policy-bad.xml", "c0c010c0-d34d-b33f-f00d-999888111113.xml", null);
    		AssertJUnit.fail("Unexpected success");
    	} catch (ExpressionEvaluationException e) {
    		// This is expected, the policy is broken
    	}
    }
    
    @Test
    public void testGeneratePolicyNumericString() throws Exception {
    	final String TEST_NAME = "testGeneratePolicyNumericString";
    	generatePolicy(TEST_NAME, "mapping-generate-policy-numeric.xml", "c0c010c0-d34d-b33f-f00d-999888111112.xml", 
    			PATTERN_NUMERIC);
    }
    	
    private void generatePolicy(final String TEST_NAME, String mappingFileName, String policyFileName, String pattern)
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// This is just for validation. The expression has to resolve reference of its own
    	PrismObject<ValuePolicyType> valuePolicy = PrismTestUtil.parseObject(
    			new File(CommonTestConstants.OBJECTS_DIR, policyFileName));
    	final StringPolicyType stringPolicy = valuePolicy.asObjectable().getStringPolicy();
    	// GIVEN
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping(mappingFileName, 
    			TEST_NAME, stringPolicy, "employeeNumber", null);
    	    	
		OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN (1)
    	mapping.evaluate(opResult);

		// THEN (1)
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		String value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		System.out.println("Generated value (1): " + value1);
		assertNotNull("Generated null value", value1);
		assertGeneratedValue(value1, stringPolicy, pattern, false);

		// WHEN (2)
		mapping.evaluate(opResult);

		// THEN (2)
		outputTriple = mapping.getOutputTriple();
		String value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		System.out.println("Generated value (2): " + value2);
		assertNotNull("Generated null value", value2);
		assertGeneratedValue(value2, stringPolicy, pattern, false);
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		assertFalse("Generated the same value", value1.equals(value2));
    }
        
	private void assertGeneratedValue(String value, StringPolicyType stringPolicy, String pattern, boolean ignoreMin) {
		if (stringPolicy == null) {
			assertEquals("Unexpected generated value length", GenerateExpressionEvaluator.DEFAULT_LENGTH, value.length());
		} else {
			if (!ignoreMin) {
				assertTrue("Value '"+value+"' too short, minLength="+stringPolicy.getLimitations().getMinLength()+", length="+value.length(), value.length() >= stringPolicy.getLimitations().getMinLength());
			}
			assertTrue("Value '"+value+"' too long, maxLength="+stringPolicy.getLimitations().getMaxLength()+", length="+value.length(), value.length() <= stringPolicy.getLimitations().getMaxLength());
			// TODO: better validation
		}
		if (pattern != null) {
			assertTrue("Value '"+value+"' does not match pattern '"+pattern+"'", value.matches(pattern));
		}
	}

	@Test
    public void testGeneratePolicyNumericInt() throws Exception {
    	final String TEST_NAME = "testGeneratePolicyNumericInt";
    	generatePolicyNumeric(TEST_NAME, "mapping-generate-policy-numeric.xml", 
    			"c0c010c0-d34d-b33f-f00d-999888111112.xml", "intType", Integer.class);
    }
	
	@Test
    public void testGeneratePolicyNumericInteger() throws Exception {
    	final String TEST_NAME = "testGeneratePolicyNumericInt";
    	generatePolicyNumeric(TEST_NAME, "mapping-generate-policy-numeric.xml", 
    			"c0c010c0-d34d-b33f-f00d-999888111112.xml", "integerType", Integer.class);
    }
	
	@Test
    public void testGeneratePolicyNumericLong() throws Exception {
    	final String TEST_NAME = "testGeneratePolicyNumericInt";
    	generatePolicyNumeric(TEST_NAME, "mapping-generate-policy-numeric.xml", 
    			"c0c010c0-d34d-b33f-f00d-999888111112.xml", "longType", Long.class);
    }
    	
    private <T> void generatePolicyNumeric(final String TEST_NAME, String mappingFileName,
    		String policyFileName, String extensionPropName, Class<T> clazz)
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// This is just for validation. The expression has to resolve reference of its own
    	PrismObject<ValuePolicyType> valuePolicy = PrismTestUtil.parseObject(
    			new File(CommonTestConstants.OBJECTS_DIR, policyFileName));
    	final StringPolicyType stringPolicy = valuePolicy.asObjectable().getStringPolicy();
    	// GIVEN
    	Mapping<PrismPropertyValue<T>> mapping = evaluator.createMapping(mappingFileName, 
    			TEST_NAME, stringPolicy, new ItemPath(
    					UserType.F_EXTENSION,
    					new QName(NS_EXTENSION, extensionPropName)), null);
    	    	
		OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN (1)
    	mapping.evaluate(opResult);

		// THEN (1)
		PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
		T value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		System.out.println("Generated value (1): " + value1);
		assertNotNull("Generated null value", value1);
		// We need to ignore the minLength. Conversion string -> number -> string may lose the leading zeroes
		assertGeneratedValue(value1.toString(), stringPolicy, PATTERN_NUMERIC, true);

		// WHEN (2)
		mapping.evaluate(opResult);

		// THEN (2)
		outputTriple = mapping.getOutputTriple();
		T value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		System.out.println("Generated value (2): " + value2);
		assertNotNull("Generated null value", value2);
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		assertFalse("Generated the same value", value1.equals(value2));
		assertGeneratedValue(value1.toString(), stringPolicy, PATTERN_NUMERIC, true);
    }

	@Test
    public void testGenerateProtectedString() throws Exception {
    	final String TEST_NAME = "testGenerateProtectedString";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	// GIVEN
    	Mapping<PrismPropertyValue<ProtectedStringType>> mapping = evaluator.createMapping("mapping-generate.xml", 
    			TEST_NAME, SchemaConstants.PATH_PASSWORD_VALUE, null);
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN
    	mapping.evaluate(opResult);

		// THEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = mapping.getOutputTriple();
    	ProtectedStringType value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    	
        System.out.println("Generated excrypted value: "+value1);
        assertNotNull(value1);
        assertNotNull(value1.getEncryptedData());
    }
        
}
