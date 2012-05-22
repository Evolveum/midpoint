/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.valueconstruction;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertFalse;

import static com.evolveum.midpoint.prism.util.PrismAsserts.*;
import static com.evolveum.midpoint.common.valueconstruction.ValueConstructionTestEvaluator.*;

import com.evolveum.midpoint.common.crypto.EncryptionException;
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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
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
public class TestValueConstructionDynamic {

	private ValueConstructionTestEvaluator evaluator;
	    
    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
    	evaluator = new ValueConstructionTestEvaluator();
    	evaluator.init();
    }

    @Test
    public void testConstructionValue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-value.xml", "name", "rock", null, "testConstructionValue",
    			"apple", "orange");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testConstructionValueMulti() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-value-multi.xml", "telephoneNumber", "rock", null, "testConstructionValueMulti",
    			"apple", "orange");
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testConstructionAsIsAdd() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-asis.xml", "name", "rock", null, "testConstructionAsIsAdd",
    			"apple", "orange");
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "rock");
    	PrismAsserts.assertTriplePlus(outputTriple, "apple", "orange");
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testConstructionAsIsDelete() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicDelete(String.class, 
    			"construction-asis.xml", "name", "rock", null, "testConstructionAsIs",
    			"rock");
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleMinus(outputTriple, "rock");    	
    }

    @Test
    public void testConstructionPathVariables() throws Exception {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        vars.put(ExpressionConstants.VAR_USER, ref);

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-path-system-variables.xml", "name", null, vars, "testConstructionPathVariables");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testConstructionExpressionSimple() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-simple.xml", "name", null, null, "testConstructionExpressionSimple");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "fooBAR");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testConstructionExpressionVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-variables.xml", "name", null, null, "testConstructionExpressionVariables");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testConstructionExpressionVariablesPolyStringXPath() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        vars.put(ExpressionConstants.VAR_USER, ref);

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables-polystring-xpath.xml", "fullName", null, vars, "testConstructionExpressionVariablesPolyStringXPath");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain Jack Sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testConstructionExpressionVariablesPolyStringGroovy() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        vars.put(ExpressionConstants.VAR_USER, ref);

        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables-polystring-groovy.xml", "fullName", null, vars, "testConstructionExpressionVariablesPolyStringGroovy");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain Jack Sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testConstructionExpressionSystemVariablesRef() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        vars.put(ExpressionConstants.VAR_USER, ref);

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables.xml", "name", null, vars, "testConstructionExpressionSystemVariablesRef");
            	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testConstructionExpressionSystemVariablesValueJaxb() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        vars.put(ExpressionConstants.VAR_USER, userType);

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables.xml", "name", null, vars, "testConstructionExpressionSystemVariablesValueJaxb");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "Captain jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testConstructionExpressionSystemVariablesValuePrismObjectNoChange() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        vars.put(ExpressionConstants.VAR_USER, userType.asPrismObject());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables.xml", "name", null, vars, "testConstructionExpressionSystemVariablesValuePrismObjectNoChange");
    	
        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "Captain jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testConstructionExpressionSystemVariablesValuePrismObjectReplaceName() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	System.out.println("===[ testConstructionExpressionSystemVariablesValuePrismObjectReplaceName ]===");
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        PrismObject<UserType> user = userTypeElement.getValue().asPrismObject();
        PropertyDelta<String> fullNameDelta = evaluator.createReplaceDelta(String.class, "name", "sparrow");
        ObjectDeltaObject<UserType> userOdo = ObjectDeltaObject.create(user, fullNameDelta);
        vars.put(ExpressionConstants.VAR_USER, userOdo);
        
        System.out.println("Vars map:");
        System.out.println(DebugUtil.dump(vars));

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables.xml", "name", null, vars, 
    			"testConstructionExpressionSystemVariablesValuePrismObjectReplaceName");
    	
        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "Captain sparrow");
        PrismAsserts.assertTripleMinus(outputTriple, "Captain jack");
    }
    

    @Test
    public void testConstructionRootNode() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN        
    	final String TEST_NAME = "testConstructionRootNode";

    	ValueConstruction<PrismPropertyValue<String>> construction = evaluator.createConstruction(String.class, 
    			"construction-expression-root-node.xml", "locality", 
    			null, null, TEST_NAME);

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        construction.setRootNode(ref);
        
        OperationResult opresult = new OperationResult(TEST_NAME);
        
        // WHEN
        construction.evaluate(opresult);
    	
        // THEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = construction.getOutputTriple();
        PrismAsserts.assertTripleZero(outputTriple, "Black Pearl");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);        
    }

    @Test
    public void testConstructionExpressionList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-list.xml", "organizationalUnit", null, null, "testConstructionExpressionList");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Leaders", "Followers");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testConstructionGenerate() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
    	final String TEST_NAME = "testConstructionGenerate";
    	
    	ValueConstruction<PrismPropertyValue<String>> construction = evaluator.createConstruction(String.class, 
    			"construction-generate.xml", "name", 
    			null, null, TEST_NAME);
    	PropertyDelta<String> delta = evaluator.createAddDelta(String.class, "name", "apple", "orange");
    	construction.setInputDelta(delta);
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
        // WHEN (1)
        construction.evaluate(opResult);
        
        // THEN (1)
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = construction.getOutputTriple();
        String value1 = ValueConstructionTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);

        System.out.println("Generated value (1): "+value1);
        assertEquals(10, value1.length());

        // WHEN (2)
        construction.evaluate(opResult);

        // THEN (2)
        outputTriple = construction.getOutputTriple();
        String value2 = ValueConstructionTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
        System.out.println("Generated value (2): "+value2);
        assertEquals(10, value2.length());
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
        
        assertFalse("Generated the same value", value1.equals(value2));

    }

    @Test
    public void testConstructionGenerateProtectedString() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException, EncryptionException {
    	ProtectedStringType addedPs = evaluator.createProtectedString("apple");
    	ProtectedStringType oldPs = evaluator.createProtectedString("rock");
		// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = evaluator.evaluateConstructionDynamicAdd(ProtectedStringType.class, 
    			"construction-generate.xml", SchemaConstants.PATH_PASSWORD_VALUE, oldPs, null, "testConstructionGenerateProtectedString",
    			addedPs);
    	
    	// THEN
    	ProtectedStringType value1 = ValueConstructionTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    	
        System.out.println("Generated excrypted value: "+value1);
        assertNotNull(value1);
        assertNotNull(value1.getEncryptedData());
    }
    
    @Test
    public void testConstructionValueConditionTrue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-value-condition-true.xml", "name", "rock", null, "testConstructionValueConditionTrue",
    			"apple", "orange");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    	
    }

    @Test
    public void testConstructionValueConditionFalse() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-value-condition-false.xml", "name", "rock", null, "testConstructionValueConditionFalse",
    			"apple", "orange");
    	
    	// THEN
        assertNull("Unexpected value in outputTriple", outputTriple);
    }
    
    @Test
    public void testConstructionExpressionSystemVariablesValueJaxbConditionTrue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        vars.put(ExpressionConstants.VAR_USER, userType);
        
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables-condition.xml", "name", null, vars,
    					"testConstructionExpressionSystemVariablesValueJaxbConditionTrue");
    	
        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "Captain jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testConstructionExpressionSystemVariablesValueJaxbConditionFalse() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        userType.getEmployeeType().clear();
        userType.getEmployeeType().add("SAILOR");
        vars.put(ExpressionConstants.VAR_USER, userType);

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables-condition.xml", "name", null, vars,
    					"testConstructionExpressionSystemVariablesValueJaxbConditionFalse");
        
        // THEN
        assertNull("Unexcpected outputTriple", outputTriple);
    }
    
    @Test
    public void testConstructionExpressionSystemVariablesValueJaxbConditionChangeOn() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        userType.getEmployeeType().clear();
        userType.getEmployeeType().add("SAILOR");
        PrismObject<UserType> userOld = userType.asPrismObject();
        PrismObject<UserType> userNew = userOld.clone();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, userOld.getOid(), UserType.F_EMPLOYEE_TYPE, 
        		evaluator.getPrismContext(), "CAPTAIN");
        userDelta.applyTo(userNew);
        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<UserType>(userOld, userDelta, userNew);
        vars.put(ExpressionConstants.VAR_USER, userOdo);

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables-condition.xml", "name", null, vars,
    					"testConstructionExpressionSystemVariablesValueJaxbConditionFalse");
        
     // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, "Captain jack");
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testConstructionExpressionSystemVariablesValueJaxbConditionChangeOff() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        userType.getEmployeeType().clear();
        userType.getEmployeeType().add("CAPTAIN");
        PrismObject<UserType> userOld = userType.asPrismObject();
        PrismObject<UserType> userNew = userOld.clone();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, userOld.getOid(), UserType.F_EMPLOYEE_TYPE, 
        		evaluator.getPrismContext(), "SAILOR");
        userDelta.applyTo(userNew);
        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<UserType>(userOld, userDelta, userNew);
        vars.put(ExpressionConstants.VAR_USER, userOdo);

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-system-variables-condition.xml", "name", null, vars,
    					"testConstructionExpressionSystemVariablesValueJaxbConditionFalse");
        
        // THEN
        PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleMinus(outputTriple, "Captain jack");
    }
    
    @Test
    public void testConstructionExpressionInputDelta() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateConstructionDynamicAdd(String.class, 
    			"construction-expression-input.xml", "name", "rock", null, "testConstructionExpressionSimple",
    			"apple", "orange");
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Wanna rock");
    	PrismAsserts.assertTriplePlus(outputTriple, "Wanna apple", "Wanna orange");
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

}
