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

package com.evolveum.midpoint.schema.processor;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;

public class SchemaProcessorTest {

	private static final String SCHEMA_NS = "http://foo.com/xml/ns/schema";
	
	@Test
	public void testAccessList() throws Exception {
		Document schemaDom = DOMUtil.parseFile("src/test/resources/processor/schema.xsd");
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		
		final String defaultNS = "http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";
		final String icfNS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-1.xsd";
		ResourceObjectDefinition objectDef = (ResourceObjectDefinition) schema.findContainerDefinitionByType(new QName(defaultNS, "AccountObjectClass"));
		
		ResourceObjectAttributeDefinition attrDef = objectDef.findAttributeDefinition(new QName(icfNS, "uid"));
		AssertJUnit.assertTrue(attrDef.canRead());
		AssertJUnit.assertFalse(attrDef.canUpdate());
		AssertJUnit.assertFalse(attrDef.canCreate());
		
		attrDef = objectDef.findAttributeDefinition(new QName(defaultNS, "title"));
		AssertJUnit.assertTrue(attrDef.canRead());
		AssertJUnit.assertTrue(attrDef.canUpdate());
		AssertJUnit.assertTrue(attrDef.canCreate());
		
		attrDef = objectDef.findAttributeDefinition(new QName(defaultNS, "photo"));
		AssertJUnit.assertFalse(attrDef.canRead());
		AssertJUnit.assertTrue(attrDef.canUpdate());
		AssertJUnit.assertTrue(attrDef.canCreate());
	}
	
	@Test
	public void testRoundTripGeneric() throws SchemaException {
		// GIVEN
		Schema schema = new Schema(SCHEMA_NS);
		// Ordinary property
		schema.createPropertyDefinition("number1", DOMUtil.XSD_INTEGER);
		
		// Property container
		PropertyContainerDefinition containerDefinition = schema.createPropertyContainerDefinition("ContainerType");
		// ... in it ordinary property
		containerDefinition.createPropertyDefinition("login", DOMUtil.XSD_STRING);
		// ... and local property with a type from another schema
		containerDefinition.createPropertyDefinition("password", SchemaConstants.R_PROTECTED_STRING_TYPE);
		// ... property reference
		containerDefinition.createPropertyDefinition(SchemaConstants.I_CREDENTIALS, SchemaConstants.I_CREDENTIALS_TYPE);
		// ... read-only int property 
		PropertyDefinition counterProperty = containerDefinition.createPropertyDefinition("counter", DOMUtil.XSD_INTEGER);
		counterProperty.setReadOnly();

		System.out.println("Generic schema before serializing to XSD: ");
		System.out.println(schema.dump());
		System.out.println();

		// WHEN
		
		Document xsd = schema.serializeToXsd();
		
		String stringXmlSchema = DOMUtil.serializeDOMToString(xsd);
		
		System.out.println("Generic schema after serializing to XSD: ");
		System.out.println(stringXmlSchema);
		System.out.println();
		
		Document parsedXsd = DOMUtil.parseDocument(stringXmlSchema);
		
		Schema newSchema = Schema.parse(DOMUtil.getFirstChildElement(parsedXsd));

		System.out.println("Generic schema after parsing from XSD: ");
		System.out.println(newSchema.dump());
		System.out.println();
		
		// THEN
	
		PropertyDefinition number1def = newSchema.findItemDefinition(new QName(SCHEMA_NS,"number1"), PropertyDefinition.class);
		assertEquals(new QName(SCHEMA_NS,"number1"),number1def.getName());
		assertEquals(DOMUtil.XSD_INTEGER,number1def.getTypeName());
		
		PropertyContainerDefinition newContainerDef = schema.findContainerDefinitionByType(new QName(SCHEMA_NS,"ContainerType"));
		assertEquals(new QName(SCHEMA_NS,"ContainerType"),newContainerDef.getTypeName());
		
		PropertyDefinition loginDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"login"));
		assertEquals(new QName(SCHEMA_NS,"login"), loginDef.getName());
		assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
		assertTrue("Read flag is wrong",loginDef.canRead());
		assertTrue("Create flag is wrong",loginDef.canCreate());
		assertTrue("Update flag is wrong",loginDef.canUpdate());

		PropertyDefinition passwdDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"password"));
		assertEquals(new QName(SCHEMA_NS,"password"), passwdDef.getName());
		assertEquals(SchemaConstants.R_PROTECTED_STRING_TYPE, passwdDef.getTypeName());

		PropertyDefinition credDef = newContainerDef.findPropertyDefinition(new QName(SchemaConstants.NS_C,"credentials"));
		assertEquals(new QName(SchemaConstants.NS_C,"credentials"), credDef.getName());
		assertEquals(new QName(SchemaConstants.NS_C,"CredentialsType"), credDef.getTypeName());

		PropertyDefinition countDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"counter"));
		assertEquals(new QName(SCHEMA_NS,"counter"), countDef.getName());
		assertEquals(DOMUtil.XSD_INTEGER, countDef.getTypeName());
		assertTrue("Read flag is wrong",countDef.canRead());
		assertFalse("Create flag is wrong",countDef.canCreate());
		assertFalse("Update flag is wrong",countDef.canUpdate());
	}

	
	@Test
	public void testRoundTripResource() throws SchemaException {
		// GIVEN
		Schema schema = new Schema(SCHEMA_NS);
		
		// Property container
		ResourceObjectDefinition containerDefinition = schema.createResourceObjectDefinition("AccountObjectClass");
		containerDefinition.setAccountType(true);
		containerDefinition.setDefaultAccountType(true);
		containerDefinition.setNativeObjectClass("ACCOUNT");
		// ... in it ordinary attribute - an identifier
		ResourceObjectAttributeDefinition xloginDef = containerDefinition.createAttributeDefinition("login", DOMUtil.XSD_STRING);
		containerDefinition.getIdentifiers().add(xloginDef);
		xloginDef.setNativeAttributeName("LOGIN");
		containerDefinition.setDisplayNameAttribute(xloginDef.getName());
		// ... and local property with a type from another schema
		ResourceObjectAttributeDefinition xpasswdDef = containerDefinition.createAttributeDefinition("password", SchemaConstants.R_PROTECTED_STRING_TYPE);
		xpasswdDef.setNativeAttributeName("PASSWORD");
		// ... property reference
		containerDefinition.createAttributeDefinition(SchemaConstants.I_CREDENTIALS, SchemaConstants.I_CREDENTIALS_TYPE);
		// ... ignored attribute
		ResourceObjectAttributeDefinition xSepDef = containerDefinition.createAttributeDefinition("sep", DOMUtil.XSD_STRING);
		xSepDef.setIgnored(true);

		System.out.println("Resource schema before serializing to XSD: ");
		System.out.println(schema.dump());
		System.out.println();

		// WHEN
		
		Document xsd = schema.serializeToXsd();
		
		String stringXmlSchema = DOMUtil.serializeDOMToString(xsd);
		
		System.out.println("Resource schema after serializing to XSD: ");
		System.out.println(stringXmlSchema);
		System.out.println();
		
		Document parsedXsd = DOMUtil.parseDocument(stringXmlSchema);
		
		Schema newSchema = Schema.parse(DOMUtil.getFirstChildElement(parsedXsd));

		System.out.println("Resource schema after parsing from XSD: ");
		System.out.println(newSchema.dump());
		System.out.println();
		
		// THEN
			
		PropertyContainerDefinition newContainerDef = newSchema.findContainerDefinitionByType(new QName(SCHEMA_NS,"AccountObjectClass"));
		assertEquals(new QName(SCHEMA_NS,"AccountObjectClass"),newContainerDef.getTypeName());
		assertTrue(newContainerDef instanceof ResourceObjectDefinition);
		ResourceObjectDefinition rod = (ResourceObjectDefinition) newContainerDef;
		assertTrue(rod.isAccountType());
		assertTrue(rod.isDefaultAccountType());
		
		PropertyDefinition loginDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"login"));
		assertEquals(new QName(SCHEMA_NS,"login"), loginDef.getName());
		assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
		assertFalse(loginDef.isIgnored());

		PropertyDefinition passwdDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"password"));
		assertEquals(new QName(SCHEMA_NS,"password"), passwdDef.getName());
		assertEquals(SchemaConstants.R_PROTECTED_STRING_TYPE, passwdDef.getTypeName());
		assertFalse(passwdDef.isIgnored());

		PropertyContainerDefinition credDef = newContainerDef.findPropertyContainerDefinition(new QName(SchemaConstants.NS_C,"credentials"));
		assertEquals(new QName(SchemaConstants.NS_C,"credentials"), credDef.getName());
		assertEquals(new QName(SchemaConstants.NS_C,"CredentialsType"), credDef.getTypeName());
		assertFalse(credDef.isIgnored());
		
		PropertyDefinition sepDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"sep"));
		assertEquals(new QName(SCHEMA_NS,"sep"), sepDef.getName());
		assertEquals(DOMUtil.XSD_STRING, sepDef.getTypeName());
		assertTrue(sepDef.isIgnored());
		
	}

}
