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

import com.evolveum.midpoint.common.crypto.AESProtector;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.xpath.XPathExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
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
public class TestValueConstruction {

    private static final String KEYSTORE_PATH = "src/test/resources/crypto/test-keystore.jceks";
	private static File TEST_DIR = new File("src/test/resources/valueconstruction");
    private static File OBJECTS_DIR = new File("src/test/resources/objects");

    private ValueConstructionFactory factory;
    private PrismContext prismContext;
    
    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
    	DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
    	prismContext = PrismTestUtil.createInitializedPrismContext();
    	
        ExpressionFactory expressionFactory = new ExpressionFactory();
        XPathExpressionEvaluator xpathEvaluator = new XPathExpressionEvaluator();
        expressionFactory.registerEvaluator(XPathExpressionEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
        ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
        expressionFactory.setObjectResolver(resolver);

        factory = new ValueConstructionFactory();
        factory.setExpressionFactory(expressionFactory);
        
        AESProtector protector = new AESProtector();
        protector.setKeyStorePath(KEYSTORE_PATH);
        protector.setKeyStorePassword("changeit");
        protector.setPrismContext(prismContext);
        protector.init();
        factory.setProtector(protector);
    }

    @Test
    public void testConstructionValue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-value.xml", "givenName", 
    			null, null, "testConstructionValue");
    	
        // THEN
        assertEquals("foobar", result.getValue().getValue());
    }

    @Test
    public void testConstructionValueMulti() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-value-multi.xml", "telephoneNumber", 
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
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-asis.xml", "givenName", 
    			new PrismPropertyValue<String>("barbar"), null, "testConstructionAsIs");
    	
        // THEN
        assertEquals("barbar", result.getValue().getValue());
    }

    @Test
    public void testConstructionExpressionSimple() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-expression-simple.xml", "givenName", 
    			null, null, "testConstructionExpressionSimple");
    	
        // THEN
        assertEquals("fooBAR", result.getValue().getValue());
    }

    @Test
    public void testConstructionExpressionVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	// WHEN
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-expression-variables.xml", "givenName", 
    			null, null, "testConstructionExpressionVariables");
    	
        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue().getValue());
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
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-expression-system-variables.xml", "givenName", 
    			null, vars , "testConstructionExpressionSystemVariablesRef");
    	
        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue().getValue());
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
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-expression-system-variables.xml", "givenName", 
    			null, vars , "testConstructionExpressionSystemVariablesValueJaxb");

        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue().getValue());
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
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-expression-system-variables.xml", "givenName", 
    			null, vars , "testConstructionExpressionSystemVariablesValueMidPointObject");
    	
        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue().getValue());
    }

    @Test
    public void testConstructionRootNode() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-expression-root-node.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition localityDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "locality"));

        OperationResult opResult = new OperationResult("testConstructionRootNode");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, localityDef, "system variables expression construction");

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        construction.setRootNode(ref);

        construction.evaluate(opResult);
        PrismProperty<String> result = (PrismProperty<String>) construction.getOutput();

        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("Black Pearl");
//		assertEquals(expected,result.getValues());
        assertPropertyValues("Wrong result", expected, result.getValues());
    }

    @Test
    public void testConstructionExpressionList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-expression-list.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition ouDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "organizationalUnit"));

        OperationResult opResult = new OperationResult("testConstructionExpressionList");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, ouDef, "list expression construction");
        construction.evaluate(opResult);
        PrismProperty<String> result = (PrismProperty<String>) construction.getOutput();

        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("Leaders");
        expected.add("Followers");
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
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-expression-scalar-list.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition ouDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "organizationalUnit"));

        OperationResult opResult = new OperationResult("testConstructionExpressionScalarList");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, ouDef, "scalar list expression construction");
        construction.evaluate(opResult);
        PrismProperty<String> result = (PrismProperty<String>) construction.getOutput();

        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("Pirates");
//		assertEquals(expected,result.getValues());
        assertPropertyValues("Wrong result", expected, result.getValues());
    }
    
    @Test
    public void testConstructionGenerate() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-generate.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "givenName"));

        PrismProperty givenName = givenNameDef.instantiate(null);

        OperationResult opResult = new OperationResult("testConstructionGenerate");

        // WHEN (1)
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "generate construction");
        construction.setInput(givenName);
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
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-generate.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition propDef = userContainer.findPropertyDefinition(SchemaConstants.PATH_PASSWORD_VALUE);

        PrismProperty prop = propDef.instantiate(null);

        OperationResult opResult = new OperationResult("testConstructionGenerateProtectedString");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, propDef, "generate protected construction");
        construction.setInput(prop);
        construction.evaluate(opResult);
        PrismProperty<ProtectedStringType> result = (PrismProperty<ProtectedStringType>) construction.getOutput();

        // THEN
        ProtectedStringType value1 = result.getValue().getValue();
        System.out.println("Generated excrypted value: "+value1);
        assertNotNull(value1);
        assertNotNull(value1.getEncryptedData());
    }
    
    @Test
    public void testConstructionValueConditionTrue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-value-condition-true.xml", "givenName", 
    			null, null, "testConstructionValueConditionTrue");
    	
    	// THEN
        assertEquals("foobar", result.getValue().getValue());
    }

    @Test
    public void testConstructionValueConditionFalse() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // WHEN
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-value-condition-false.xml", "givenName", 
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
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-expression-system-variables-condition.xml", "givenName", 
    			null, vars , "testConstructionExpressionSystemVariablesValueJaxbConditionTrue");

        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue().getValue());
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
    	PrismProperty<String> result = evaluateConstruction(String.class, "construction-expression-system-variables-condition.xml", "givenName", 
    			null, vars , "testConstructionExpressionSystemVariablesValueJaxbConditionFalse");

        // THEN
        assertNull("Unexcpected result", result);
    }

    
    private <T> PrismProperty<T> evaluateConstruction(Class<T> type, String filename, String propertyName, 
    		PrismPropertyValue<?> inputPropertyValue, Map<QName, Object> extraVariables, String testName) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, filename), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismPropertyDefinition propDef = userDef.findPropertyDefinition(new QName(SchemaConstants.NS_C, propertyName));


        
        OperationResult opResult = new OperationResult(testName);

        // WHEN
        ValueConstruction<PrismPropertyValue<T>> construction = factory.createValueConstruction(valueConstructionType, propDef, testName);
        if (inputPropertyValue != null) {
        	PrismProperty inputProperty = propDef.instantiate();
        	inputProperty.setValue(inputPropertyValue);
        	construction.setInput(inputProperty);
        }
        if (extraVariables != null) {
        	construction.addVariableDefinitions(extraVariables);
        }
        construction.evaluate(opResult);
        PrismProperty<T> result = (PrismProperty<T>) construction.getOutput();        

        return result;
    }

    
}
