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

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ValueConstructionType;
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
public class TestValueConstructionStatic {

	private ValueConstructionTestEvaluator evaluator;
	    
    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
    	evaluator = new ValueConstructionTestEvaluator();
    	evaluator.init();
    }

    @Test
    public void testConstructionValue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-value.xml", "name", 
    			null, null, "testConstructionValue");
    	
        // THEN
        assertEquals("foobar", result.getValue().getValue());
    }
    
    @Test
    public void testConstructionValueBooleanTrue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-value-boolean-true.xml", 
    			new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED), null, null, "testConstructionValueBooleanTrue");
    	
        // THEN
        assertEquals(Boolean.TRUE, result.getValue().getValue());
    }

    @Test
    public void testConstructionValueBooleanFalse() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-value-boolean-false.xml", 
    			new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED), null, null, "testConstructionValueBooleanFalse");
    	
        // THEN
        assertEquals(Boolean.FALSE, result.getValue().getValue());
    }

    @Test
    public void testConstructionValueMulti() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-value-multi.xml", "employeeType", 
    			null, null, "testConstructionValueMulti");

        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("12345");
        expected.add("67890");
//		assertEquals(expected,result.getValues());
        assertPropertyValues("Wrong result", expected, result.getValues());
    }
    
    @Test
    public void testConstructionAsIs() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-asis.xml", "name", 
    			"barbar", null, "testConstructionAsIs");
    	
        // THEN
        assertEquals("barbar", result.getValue().getValue());
    }

    @Test
    public void testConstructionExpressionSimple() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-expression-simple.xml", "name", 
    			null, null, "testConstructionExpressionSimple");
    	
        // THEN
        assertEquals("fooBAR", result.getValue().getValue());
    }

    @Test
    public void testConstructionExpressionVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-expression-variables.xml", "name", 
    			null, null, "testConstructionExpressionVariables");
    	
        // THEN
        assertEquals("Captain jack", result.getValue().getValue());
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
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-expression-system-variables.xml", "name", 
    			null, vars , "testConstructionExpressionSystemVariablesRef");
    	
        // THEN
        assertEquals("Captain jack", result.getValue().getValue());
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
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-expression-system-variables.xml", "name", 
    			null, vars , "testConstructionExpressionSystemVariablesValueJaxb");

        // THEN
        assertEquals("Captain jack", result.getValue().getValue());
    }

    @Test
    public void testConstructionExpressionSystemVariablesValueMidPointObject() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        vars.put(ExpressionConstants.VAR_USER, userType.asPrismObject());

        // WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-expression-system-variables.xml", "name", 
    			null, vars , "testConstructionExpressionSystemVariablesValueMidPointObject");
    	
        // THEN
        assertEquals("Captain jack", result.getValue().getValue());
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
        PrismProperty result = (PrismProperty) construction.getOutput();
        Set<PolyString> expected = new HashSet<PolyString>();
        expected.add(new PolyString("Black Pearl", "black pearl"));
//		assertEquals(expected,result.getValues());
        assertPropertyValues("Wrong result", expected, result.getValues());
    }

    @Test
    public void testConstructionExpressionList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<PolyString> result = evaluator.evaluateConstructionStatic(PolyString.class, "construction-expression-list.xml", "organizationalUnit", 
    			null, null , "testConstructionExpressionList");
    	
        // THEN
        Set<PolyString> expected = new HashSet<PolyString>();
        expected.add(new PolyString("Leaders","leaders"));
        expected.add(new PolyString("Followers","followers"));
//		assertEquals(expected,result.getValues());
        assertPropertyValues("Wrong result", expected, result.getValues());
    }

    /*
      * DOES NOT WORK NOW
      * This automatic scalar-list conversion fails with:
      * net.sf.saxon.trans.XPathException: Cannot convert XPath value to Java object: required class is org.w3c.dom.NodeList; supplied value has type xs:string
      */
    @Test(enabled = false)
    public void testConstructionExpressionScalarList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {

    	// WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-expression-scalar-list.xml", "organizationalUnit", 
    			null, null , "testConstructionExpressionScalarList");
    	
        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("Pirates");
//		assertEquals(expected,result.getValues());
        assertPropertyValues("Wrong result", expected, result.getValues());
    }
    
    @Test
    public void testConstructionGenerate() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
    	final String TEST_NAME = "testConstructionGenerate";

    	ValueConstruction<PrismPropertyValue<String>> construction = evaluator.createConstruction(String.class, 
    			"construction-generate.xml", "name", 
    			null, null, TEST_NAME);

    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
        // WHEN (1)
        construction.evaluate(opResult);
        PrismProperty<String> result = (PrismProperty<String>) construction.getOutput();

        // THEN (1)
        String value1 = result.getValue().getValue();
        System.out.println("Generated value (1): "+value1);
        assertEquals(10, value1.length());

        // WHEN (2)
        construction.evaluate(opResult);
        result = (PrismProperty<String>) construction.getOutput();

        // THEN (2)
        String value2 = result.getValue().getValue();
        System.out.println("Generated value (2): "+value2);
        assertEquals(10, value2.length());
        
        assertFalse("Generated the same value", value1.equals(value2));

    }

    @Test
    public void testConstructionGenerateProtectedString() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {        
    	// WHEN
    	PrismProperty<ProtectedStringType> result = evaluator.evaluateConstructionStatic(ProtectedStringType.class, 
    			"construction-generate.xml", SchemaConstants.PATH_PASSWORD_VALUE, 
    			null, null , "testConstructionGenerateProtectedString");

        // THEN
        ProtectedStringType value1 = result.getValue().getValue();
        System.out.println("Generated excrypted value: "+value1);
        assertNotNull(value1);
        assertNotNull(value1.getEncryptedData());
    }
    
    @Test
    public void testConstructionValueConditionTrue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-value-condition-true.xml", "name", 
    			null, null, "testConstructionValueConditionTrue");
    	
    	// THEN
        assertEquals("foobar", result.getValue().getValue());
    }

    @Test
    public void testConstructionValueConditionFalse() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-value-condition-false.xml", "name", 
    			null, null, "testConstructionValueConditionTrue");
    	
    	// THEN
        assertNull("Unexpected value in result", result);
    }
    
    @Test
    public void testConstructionExpressionSystemVariablesValueJaxbConditionTrue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// GIVEN
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        vars.put(ExpressionConstants.VAR_USER, userType);

        // WHEN
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-expression-system-variables-condition.xml", "name", 
    			null, vars , "testConstructionExpressionSystemVariablesValueJaxbConditionTrue");

        // THEN
        assertEquals("Captain jack", result.getValue().getValue());
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
    	PrismProperty<String> result = evaluator.evaluateConstructionStatic(String.class, "construction-expression-system-variables-condition.xml", "name", 
    			null, vars , "testConstructionExpressionSystemVariablesValueJaxbConditionFalse");

        // THEN
        assertNull("Unexcpected result", result);
    }
    
}
