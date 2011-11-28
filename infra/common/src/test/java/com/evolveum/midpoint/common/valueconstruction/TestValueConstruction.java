/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.valueconstruction;

import static org.testng.AssertJUnit.assertEquals;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.xpath.XPathExpressionEvaluator;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;

/**
 * @author Radovan Semancik
 *
 */
public class TestValueConstruction {

	private static File TEST_DIR = new File("src/test/resources/valueconstruction");
	private static File OBJECTS_DIR = new File("src/test/resources/objects");
	
	private ValueConstructionFactory factory;
	SchemaRegistry schemaRegistry;
	
	@BeforeClass
	public void setupFactory() throws SAXException, IOException, SchemaException {
		ExpressionFactory expressionFactory = new ExpressionFactory();
		XPathExpressionEvaluator xpathEvaluator = new XPathExpressionEvaluator();
		expressionFactory.registerEvaluator(XPathExpressionEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
		ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
		expressionFactory.setObjectResolver(resolver);
		
		factory = new ValueConstructionFactory();
		factory.setExpressionFactory(expressionFactory);
		
		schemaRegistry = new SchemaRegistry();
		schemaRegistry.initialize();
	}
	
	@Test
	public void testConstructionValue() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-value.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
		
		OperationResult opResult = new OperationResult("testConstructionValue");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "literal construction");
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		assertEquals("foobar",result.getValue(String.class));
	}

	@Test
	public void testConstructionValueMulti() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-value-multi.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"telephoneNumber"));
		
		OperationResult opResult = new OperationResult("testConstructionValueMulti");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "literal multi construction");
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		Set<String> expected = new HashSet<String>();
		expected.add("12345");
		expected.add("67890");
		assertEquals(expected,result.getValues());
	}
	
	@Test
	public void testConstructionAsIs() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-asis.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
		
		Property givenName = givenNameDef.instantiate();
		givenName.setValue("barbar");
		
		OperationResult opResult = new OperationResult("testConstructionAsIs");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "asis construction");
		construction.setInput(givenName);
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		assertEquals("barbar",result.getValue(String.class));
	}

	@Test
	public void testConstructionExpressionSimple() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-expression-simple.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
				
		OperationResult opResult = new OperationResult("testConstructionExpressionSimple");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "simple expression construction");
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		assertEquals("fooBAR",result.getValue(String.class));
	}

	@Test
	public void testConstructionExpressionVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-expression-variables.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
		
		OperationResult opResult = new OperationResult("testConstructionExpressionVariables");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "variables expression construction");
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		assertEquals("Captain Jack Sparrow",result.getValue(String.class));
	}
	
	@Test
	public void testConstructionExpressionSystemVariablesRef() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-expression-system-variables.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
				
		OperationResult opResult = new OperationResult("testConstructionExpressionSystemVariables");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "system variables expression construction");
		
		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
		ref.setType(SchemaConstants.I_USER_TYPE);
		construction.addVariableDefinition(ExpressionConstants.VAR_USER, ref);
		
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		assertEquals("Captain Jack Sparrow",result.getValue(String.class));
	}

	@Test
	public void testConstructionExpressionSystemVariablesValueJaxb() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-expression-system-variables.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		JAXBElement<UserType> userTypeElement = JAXBUtil.unmarshal(
				new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
		UserType userType = userTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
				
		OperationResult opResult = new OperationResult("testConstructionExpressionSystemVariables");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "system variables expression construction");
		
		construction.addVariableDefinition(ExpressionConstants.VAR_USER, userType);
		
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		assertEquals("Captain Jack Sparrow",result.getValue(String.class));
	}

	@Test
	public void testConstructionExpressionSystemVariablesValueMidPointObject() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-expression-system-variables.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		JAXBElement<UserType> userTypeElement = JAXBUtil.unmarshal(
				new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"), UserType.class);
		UserType userType = userTypeElement.getValue();
		
		ObjectDefinition<UserType> userDef = schemaRegistry.getCommonSchema().findObjectDefinition(UserType.class);
		PropertyDefinition givenNameDef = userDef.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
				
		OperationResult opResult = new OperationResult("testConstructionExpressionSystemVariables");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, givenNameDef, "system variables expression construction");
		
		construction.addVariableDefinition(ExpressionConstants.VAR_USER, userDef.parseObjectType(userType));
		
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		assertEquals("Captain Jack Sparrow",result.getValue(String.class));
	}

	@Test
	public void testConstructionRootNode() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-expression-root-node.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition localityDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"locality"));
				
		OperationResult opResult = new OperationResult("testConstructionRootNode");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, localityDef, "system variables expression construction");
		
		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
		ref.setType(SchemaConstants.I_USER_TYPE);
		construction.setRootNode(ref);
		
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		Set<String> expected = new HashSet<String>();
		expected.add("Black Pearl");
		assertEquals(expected,result.getValues());
	}
	
	@Test
	public void testConstructionExpressionList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-expression-list.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition ouDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"organizationalUnit"));
				
		OperationResult opResult = new OperationResult("testConstructionExpressionList");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, ouDef, "list expression construction");
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		Set<String> expected = new HashSet<String>();
		expected.add("Leaders");
		expected.add("Followers");
		assertEquals(expected,result.getValues());
	}
	
	/*
	 * DOES NOT WORK NOW
	 * This automatic scalar-list conversion fails with:
	 * net.sf.saxon.trans.XPathException: Cannot convert XPath value to Java object: required class is org.w3c.dom.NodeList; supplied value has type xs:string
	 */
	@Test(enabled=false)
	public void testConstructionExpressionScalarList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// GIVEN
		JAXBElement<ValueConstructionType> valueConstructionTypeElement = JAXBUtil.unmarshal(
				new File(TEST_DIR, "construction-expression-scalar-list.xml"), ValueConstructionType.class);
		ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();
		
		PropertyContainerDefinition userContainer = schemaRegistry.getCommonSchema().findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		PropertyDefinition ouDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"organizationalUnit"));
				
		OperationResult opResult = new OperationResult("testConstructionExpressionScalarList");
		
		// WHEN
		ValueConstruction construction = factory.createValueConstruction(valueConstructionType, ouDef, "scalar list expression construction");
		construction.evaluate(opResult);
		Property result = construction.getOutput();
		
		// THEN
		Set<String> expected = new HashSet<String>();
		expected.add("Pirates");
		assertEquals(expected,result.getValues());
	}

}
