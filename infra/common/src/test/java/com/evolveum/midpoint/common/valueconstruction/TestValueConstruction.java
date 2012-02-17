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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertFalse;

import static com.evolveum.midpoint.test.util.PrismAsserts.*;

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
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.test.util.PrismTestUtil;
import com.evolveum.midpoint.util.JAXBUtil;
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
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
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
    public void testConstructionValue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-value.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "givenName"));

        OperationResult opResult = new OperationResult("testConstructionValue");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "literal construction");
        construction.evaluate(opResult);
        PrismProperty result = construction.getOutput();

        // THEN
        assertEquals("foobar", result.getValue(String.class).getValue());
    }

    @Test
    public void testConstructionValueMulti() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-value-multi.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "telephoneNumber"));

        OperationResult opResult = new OperationResult("testConstructionValueMulti");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "literal multi construction");
        construction.evaluate(opResult);
        PrismProperty result = construction.getOutput();

        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("12345");
        expected.add("67890");
//		assertEquals(expected,result.getValues());
        assertPropertyValues(expected, result.getValues());
    }

    @Test
    public void testConstructionAsIs() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-asis.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "givenName"));

        PrismProperty givenName = givenNameDef.instantiate(null);
        givenName.setValue(new PrismPropertyValue("barbar"));

        OperationResult opResult = new OperationResult("testConstructionAsIs");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "asis construction");
        construction.setInput(givenName);
        construction.evaluate(opResult);
        PrismProperty result = construction.getOutput();

        // THEN
        assertEquals("barbar", result.getValue(String.class).getValue());
    }

    @Test
    public void testConstructionExpressionSimple() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-expression-simple.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "givenName"));

        OperationResult opResult = new OperationResult("testConstructionExpressionSimple");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "simple expression construction");
        construction.evaluate(opResult);
        PrismProperty result = construction.getOutput();
        // THEN
        assertEquals("fooBAR", result.getValue(String.class).getValue());
    }

    @Test
    public void testConstructionExpressionVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-expression-variables.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "givenName"));

        OperationResult opResult = new OperationResult("testConstructionExpressionVariables");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "variables expression construction");
        construction.evaluate(opResult);
        PrismProperty result = construction.getOutput();

        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue(String.class).getValue());
    }

    @Test
    public void testConstructionExpressionSystemVariablesRef() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-expression-system-variables.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "givenName"));

        OperationResult opResult = new OperationResult("testConstructionExpressionSystemVariables");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "system variables expression construction");

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        construction.addVariableDefinition(ExpressionConstants.VAR_USER, ref);

        construction.evaluate(opResult);
        PrismProperty result = construction.getOutput();

        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue(String.class).getValue());
    }

    @Test
    public void testConstructionExpressionSystemVariablesValueJaxb() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-expression-system-variables.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();

        PrismContainerDefinition userContainer = prismContext.getSchemaRegistry().getObjectSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
        PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C, "givenName"));

        OperationResult opResult = new OperationResult("testConstructionExpressionSystemVariables");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "system variables expression construction");

        construction.addVariableDefinition(ExpressionConstants.VAR_USER, userType);

        construction.evaluate(opResult);
        PrismProperty result = construction.getOutput();

        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue(String.class).getValue());
    }

    @Test
    public void testConstructionExpressionSystemVariablesValueMidPointObject() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "construction-expression-system-variables.xml"), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();

        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().getObjectSchema().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismPropertyDefinition givenNameDef = userDef.findPropertyDefinition(new QName(SchemaConstants.NS_C, "givenName"));

        OperationResult opResult = new OperationResult("testConstructionExpressionSystemVariables");

        // WHEN
        ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "system variables expression construction");

        construction.addVariableDefinition(ExpressionConstants.VAR_USER, userDef.parseObjectType(userType));

        construction.evaluate(opResult);
        PrismProperty result = construction.getOutput();

        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue(String.class).getValue());
    }

    @Test
    public void testConstructionRootNode() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
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
        PrismProperty result = construction.getOutput();

        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("Black Pearl");
//		assertEquals(expected,result.getValues());
        assertPropertyValues(expected, result.getValues());
    }

    @Test
    public void testConstructionExpressionList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
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
        PrismProperty result = construction.getOutput();

        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("Leaders");
        expected.add("Followers");
//		assertEquals(expected,result.getValues());
        assertPropertyValues(expected, result.getValues());
    }

    /*
      * DOES NOT WORK NOW
      * This automatic scalar-list conversion fails with:
      * net.sf.saxon.trans.XPathException: Cannot convert XPath value to Java object: required class is org.w3c.dom.NodeList; supplied value has type xs:string
      */
    @Test(enabled = false)
    public void testConstructionExpressionScalarList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
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
        PrismProperty result = construction.getOutput();

        // THEN
        Set<String> expected = new HashSet<String>();
        expected.add("Pirates");
//		assertEquals(expected,result.getValues());
        assertPropertyValues(expected, result.getValues());
    }
    
    @Test
    public void testConstructionGenerate() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
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
        PrismProperty result = construction.getOutput();

        // THEN (1)
        String value1 = result.getValue(String.class).getValue();
        System.out.println("Generated value (1): "+value1);
        assertEquals(10, value1.length());

        // WHEN (2)
        construction.evaluate(opResult);
        result = construction.getOutput();

        // THEN (2)
        String value2 = result.getValue(String.class).getValue();
        System.out.println("Generated value (2): "+value2);
        assertEquals(10, value2.length());
        
        assertFalse("Generated the same value", value1.equals(value2));

    }

    @Test
    public void testConstructionGenerateProtectedString() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
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
        PrismProperty result = construction.getOutput();

        // THEN
        ProtectedStringType value1 = result.getValue(ProtectedStringType.class).getValue();
        System.out.println("Generated excrypted value: "+value1);
        assertNotNull(value1);
        assertNotNull(value1.getEncryptedData());
    }

    
}
